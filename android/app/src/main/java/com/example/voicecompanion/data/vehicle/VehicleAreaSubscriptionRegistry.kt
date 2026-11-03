package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataDefinition

internal data class VehicleAreaSubscription(
    val definition: VehicleDataDefinition,
    val bounds: AreaBounds<Int>? = null,
    val register: () -> (() -> Unit),
)

/** Registers only capability-returned areas and retains the exact unregister closure for each one. */
internal class VehicleAreaSubscriptionRegistry(
    private val states: VehicleStateRepository,
    private val isStopped: () -> Boolean,
    private val addUndo: (() -> Unit) -> Unit,
    private val onFailure: (String, Exception) -> Unit,
) {
    fun applyCapability(
        fallback: VehicleDataDefinition,
        readable: Boolean,
        supportedAreas: List<VehicleAreaSubscription>,
    ) {
        if (!readable || supportedAreas.isEmpty()) {
            states.registerDefinition(fallback)
            states.markUnsupported(
                fallback.dataKey,
                if (readable) "지원 가능한 영역이 없어요" else "읽기 capability가 없어요",
            )
            return
        }
        supportedAreas.forEach { area -> subscribe(area.definition, area.bounds, area.register) }
    }

    fun subscribe(
        definition: VehicleDataDefinition,
        bounds: AreaBounds<Int>? = null,
        register: () -> (() -> Unit),
    ) {
        states.registerDefinition(definition)
        bounds?.let { states.setRange(definition.dataKey, it.minValue, it.maxValue) }
        if (isStopped()) return
        try {
            addUndo(register())
            states.markActive(definition.dataKey)
        } catch (e: Exception) {
            states.markError(definition.dataKey, e.message ?: e.javaClass.simpleName)
            onFailure(definition.displayName, e)
        }
    }
}
