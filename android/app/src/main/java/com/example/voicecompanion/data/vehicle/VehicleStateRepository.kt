package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleDataState
import com.example.voicecompanion.domain.model.VehicleEvent

/** Vehicle SDK values, capability outcomes, and change events share one state owner. */
class VehicleStateRepository(
    private val onSnapshot: (List<VehicleDataState>) -> Unit,
    private val onReaction: (VehicleEvent) -> Unit,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private val states = VehicleDataCatalog.initialState.associateBy { it.definition.dataKey }.toMutableMap()

    fun snapshot(): List<VehicleDataState> = synchronized(lock) {
        VehicleDataCatalog.definitions.mapNotNull { states[it.dataKey] }
    }

    fun resetChecking() {
        updateAll(VehicleAvailability.CHECKING, detail = null)
    }

    fun markActive(dataKey: String) = update(dataKey, VehicleAvailability.ACTIVE, detail = null)

    fun markUnsupported(dataKey: String, detail: String = "이 차량에서 지원하지 않아요") =
        update(dataKey, VehicleAvailability.UNSUPPORTED, detail = detail)

    fun markError(dataKey: String, detail: String) = update(dataKey, VehicleAvailability.ERROR, detail = detail)

    fun observe(dataKey: String, value: String?) {
        if (value == null) {
            markError(dataKey, "차량 서비스가 값을 보내지 않았어요")
            return
        }
        val (next, reaction) = synchronized(lock) {
            val current = states[dataKey] ?: return
            val previous = current.value
            val next = current.copy(
                availability = VehicleAvailability.ACTIVE,
                value = value,
                updatedAtEpochMs = nowEpochMs(),
                detail = null,
            )
            states[dataKey] = next
            next to if (previous == null || previous == value) null else reactionFor(dataKey, previous, value)
        }
        onSnapshot(snapshot())
        reaction?.let(onReaction)
    }

    private fun update(dataKey: String, availability: VehicleAvailability, detail: String?) {
        val updated = synchronized(lock) {
            val current = states[dataKey] ?: return
            current.copy(availability = availability, detail = detail).also { states[dataKey] = it }
        }
        onSnapshot(snapshot().map { if (it.definition.dataKey == dataKey) updated else it })
    }

    private fun updateAll(availability: VehicleAvailability, detail: String?) {
        val next = synchronized(lock) {
            VehicleDataCatalog.definitions.map { definition ->
                VehicleDataState(definition, availability, detail = detail).also { states[definition.dataKey] = it }
            }
        }
        onSnapshot(next)
    }

    private fun reactionFor(dataKey: String, previous: String, value: String): VehicleEvent? = when (dataKey) {
        VehicleDataCatalog.BRAKE -> if (previous == "false" && value == "true") VehicleEvent.BRAKE else null
        VehicleDataCatalog.TURN_SIGNAL -> if (value in ACTIVE_TURN_STATES && value != previous) VehicleEvent.TURN else null
        VehicleDataCatalog.DRIVING_MODE -> VehicleEvent.MODE
        VehicleDataCatalog.SEAT_BELT_LEFT, VehicleDataCatalog.SEAT_BELT_RIGHT ->
            if (previous == "false" && value == "true") VehicleEvent.WARNING else null
        else -> null
    }

    private companion object {
        val ACTIVE_TURN_STATES = setOf("LEFT", "RIGHT")
    }
}
