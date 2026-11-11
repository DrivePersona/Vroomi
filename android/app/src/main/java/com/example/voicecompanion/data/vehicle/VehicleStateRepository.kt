package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDataState
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Vehicle values, capability outcomes, and debounced reaction events share one state owner. */
class VehicleStateRepository(
    private val onSnapshot: (List<VehicleDataState>) -> Unit,
    private val onReaction: (VehicleEvent, allowWhileDriving: Boolean) -> Unit,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
    private val nowMonotonicMs: () -> Long = { System.nanoTime() / 1_000_000 },
    private val schedule: ((Long, () -> Unit) -> (() -> Unit))? = null,
) : AutoCloseable {
    private val lock = Any()
    private val definitions = LinkedHashMap<String, VehicleDataDefinition>()
    private val states = LinkedHashMap<String, VehicleDataState>()
    private val ranges = HashMap<String, IntRange>()
    private val pendingDebounce = HashMap<String, () -> Unit>()
    private val lastReactionMs = HashMap<String, Long>()
    private val lastStableBucket = HashMap<String, String>()
    private val lastStableValue = HashMap<String, String>()
    private val executor = ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
    private val scheduleBlock = schedule ?: { delayMs, block ->
        val future = executor.schedule(block, delayMs, TimeUnit.MILLISECONDS)
        val cancel: () -> Unit = { future.cancel(false) }
        cancel
    }

    init { VehicleDataCatalog.definitions.forEach(::addDefinitionLocked) }

    fun snapshot(): List<VehicleDataState> = synchronized(lock) { states.values.toList() }

    fun registerDefinition(definition: VehicleDataDefinition) {
        val changed = synchronized(lock) {
            if (definition.dataKey in definitions) false
            else {
                addDefinitionLocked(definition)
                true
            }
        }
        if (changed) onSnapshot(snapshot())
    }

    fun setRange(dataKey: String, min: Int, max: Int) {
        if (max <= min) return
        synchronized(lock) { ranges[dataKey] = min..max }
    }

    fun resetChecking() {
        val next = synchronized(lock) {
            states.replaceAll { _, state -> state.copy(availability = VehicleAvailability.CHECKING, detail = null) }
            states.values.toList()
        }
        onSnapshot(next)
    }

    fun markActive(dataKey: String) = update(dataKey, VehicleAvailability.ACTIVE, detail = null)

    fun markUnsupported(dataKey: String, detail: String = "이 차량에서 지원하지 않아요") =
        update(dataKey, VehicleAvailability.UNSUPPORTED, detail = detail)

    fun markError(dataKey: String, detail: String) = update(dataKey, VehicleAvailability.ERROR, detail = detail)

    fun emitReaction(dataKey: String, event: VehicleEvent) = dispatchIfAllowed(dataKey, event)

    fun observe(dataKey: String, value: String?) {
        if (value == null) {
            markError(dataKey, "차량 서비스가 값을 보내지 않았어요")
            return
        }
        val result = synchronized(lock) {
            val current = states[dataKey] ?: return
            val previous = current.value
            val updated = current.copy(
                availability = VehicleAvailability.ACTIVE,
                value = value,
                updatedAtEpochMs = nowEpochMs(),
                detail = null,
            )
            states[dataKey] = updated
            val definition = definitions.getValue(dataKey)
            val immediate = if (previous == null || previous == value) null else {
                when (definition.reactionPolicy) {
                    VehicleReactionPolicy.CHANGE -> definition.reactionEvent
                    VehicleReactionPolicy.RULE -> definition.reactionRule?.invoke(previous, value)
                    VehicleReactionPolicy.BRIGHTNESS_BUCKET, VehicleReactionPolicy.POSITION_BUCKET,
                    VehicleReactionPolicy.DEBOUNCED_CHANGE -> null
                    null -> legacyReactionFor(dataKey, previous, value)
                }
            }
            val baselineBucket = if (previous == null) bucketFor(definition, dataKey, value) else null
            Triple(updated, immediate, baselineBucket)
        }
        onSnapshot(snapshot())
        if (result.first.definition.reactionPolicy == VehicleReactionPolicy.DEBOUNCED_CHANGE) {
            synchronized(lock) { lastStableValue.putIfAbsent(dataKey, result.first.value!!) }
        }
        if (result.third != null) synchronized(lock) { lastStableBucket.putIfAbsent(dataKey, result.third!!) }
        result.second?.let { dispatchIfAllowed(dataKey, it) }
        when (result.first.definition.reactionPolicy) {
            VehicleReactionPolicy.BRIGHTNESS_BUCKET, VehicleReactionPolicy.POSITION_BUCKET -> scheduleContinuousReaction(dataKey)
            VehicleReactionPolicy.DEBOUNCED_CHANGE -> scheduleDebouncedChange(dataKey)
            else -> Unit
        }
    }

    private fun scheduleDebouncedChange(dataKey: String) {
        val cancelPrevious = synchronized(lock) { pendingDebounce.remove(dataKey) }
        cancelPrevious?.invoke()
        val definition = synchronized(lock) { definitions[dataKey] } ?: return
        val cancel = scheduleBlock(definition.debounceMs) {
            val event = synchronized(lock) {
                pendingDebounce.remove(dataKey)
                val stable = lastStableValue[dataKey]
                val value = states[dataKey]?.value
                if (stable == value) null
                else {
                    if (value != null) lastStableValue[dataKey] = value
                    definitions[dataKey]?.reactionEvent
                }
            }
            event?.let { dispatchIfAllowed(dataKey, it) }
        }
        synchronized(lock) {
            if (dataKey in states) pendingDebounce[dataKey] = cancel else cancel.invoke()
        }
    }

    private fun scheduleContinuousReaction(dataKey: String) {
        val cancelPrevious = synchronized(lock) { pendingDebounce.remove(dataKey) }
        cancelPrevious?.invoke()
        val delay = synchronized(lock) { definitions[dataKey]?.debounceMs ?: 800L }
        val cancel = scheduleBlock(delay) {
            val reaction = synchronized(lock) {
                pendingDebounce.remove(dataKey)
                val current = states[dataKey] ?: return@synchronized null
                val definition = current.definition
                val value = current.value ?: return@synchronized null
                val bucket = bucketFor(definition, dataKey, value) ?: return@synchronized null
                val previousBucket = lastStableBucket.put(dataKey, bucket)
                if (previousBucket == null || previousBucket == bucket) null else definition.reactionEvent
            }
            reaction?.let { dispatchIfAllowed(dataKey, it) }
        }
        synchronized(lock) {
            if (dataKey in states) pendingDebounce[dataKey] = cancel else cancel.invoke()
        }
    }

    private fun dispatchIfAllowed(dataKey: String, event: VehicleEvent) {
        val definition = synchronized(lock) { definitions[dataKey] } ?: return
        val shouldEmit = synchronized(lock) {
            val now = nowMonotonicMs()
            val last = lastReactionMs[dataKey]
            if (last != null && now - last < definition.cooldownMs) false
            else {
                lastReactionMs[dataKey] = now
                true
            }
        }
        if (shouldEmit) onReaction(event, definition.allowWhileDriving)
    }

    private fun bucketFor(definition: VehicleDataDefinition, dataKey: String, value: String): String? {
        val intValue = value.toIntOrNull() ?: return null
        val range = synchronized(lock) { ranges[dataKey] } ?: return null
        val ratio = ((intValue - range.first).toFloat() / (range.last - range.first).toFloat()).coerceIn(0f, 1f)
        return when (definition.reactionPolicy) {
            VehicleReactionPolicy.BRIGHTNESS_BUCKET -> when {
                ratio < 0.34f -> "dark"
                ratio < 0.67f -> "normal"
                else -> "bright"
            }
            VehicleReactionPolicy.POSITION_BUCKET -> when {
                ratio < 0.10f -> "closed"
                ratio < 0.85f -> "partly-open"
                else -> "open"
            }
            else -> null
        }
    }

    private fun update(dataKey: String, availability: VehicleAvailability, detail: String?) {
        val changed = synchronized(lock) {
            val current = states[dataKey] ?: return
            current.copy(availability = availability, detail = detail).also { states[dataKey] = it }
        }
        onSnapshot(snapshot().map { if (it.definition.dataKey == dataKey) changed else it })
    }

    private fun addDefinitionLocked(definition: VehicleDataDefinition) {
        definitions[definition.dataKey] = definition
        states[definition.dataKey] = VehicleDataState(definition, VehicleAvailability.CHECKING)
    }

    private fun legacyReactionFor(dataKey: String, previous: String, value: String): VehicleEvent? = when (dataKey) {
        VehicleDataCatalog.BRAKE -> if (previous == "false" && value == "true") VehicleEvent.BRAKE else null
        VehicleDataCatalog.TURN_SIGNAL -> if (value in ACTIVE_TURN_STATES && value != previous) VehicleEvent.TURN else null
        VehicleDataCatalog.DRIVING_MODE -> VehicleEvent.MODE
        VehicleDataCatalog.SEAT_BELT_LEFT, VehicleDataCatalog.SEAT_BELT_RIGHT ->
            if (previous == "false" && value == "true") VehicleEvent.WARNING else null
        else -> null
    }

    override fun close() {
        val cancels = synchronized(lock) {
            pendingDebounce.values.toList().also { pendingDebounce.clear() }
        }
        cancels.forEach { runCatching(it) }
        executor.shutdownNow()
    }

    private companion object {
        val ACTIVE_TURN_STATES = setOf("LEFT", "RIGHT")
    }
}
