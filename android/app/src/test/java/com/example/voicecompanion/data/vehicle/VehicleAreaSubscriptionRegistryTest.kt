package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import com.example.voicecompanion.domain.model.VehicleValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleAreaSubscriptionRegistryTest {
    @Test
    fun onlyCapabilityReturnedAreasAreRegisteredAndSameAreasAreUnregistered() {
        val states = VehicleStateRepository({}, { _, _ -> })
        val unregistered = mutableListOf<String>()
        val cleanup = mutableListOf<() -> Unit>()
        val registry = VehicleAreaSubscriptionRegistry(states, { false }, { cleanup += it }) { _, _ -> }
        val fallback = definition("feature:window-position")
        val supported = listOf("ROW_1_LEFT", "ROW_1_RIGHT").map { area ->
            val key = "window-position:$area"
            VehicleAreaSubscription(
                definition(key),
                AreaBounds(0, 100),
                register = {
                    val unregisterSameArea = { unregistered += area }
                    unregisterSameArea
                },
            )
        }

        registry.applyCapability(fallback, readable = true, supportedAreas = supported)

        val snapshot = states.snapshot().associateBy { it.definition.dataKey }
        assertEquals(VehicleAvailability.ACTIVE, snapshot.getValue("window-position:ROW_1_LEFT").availability)
        assertEquals(VehicleAvailability.ACTIVE, snapshot.getValue("window-position:ROW_1_RIGHT").availability)
        assertTrue(snapshot.getValue("window-position:ROW_1_LEFT").definition.dataKey.endsWith("ROW_1_LEFT"))
        assertEquals(2, cleanup.size)

        cleanup.forEach { it() }
        assertEquals(listOf("ROW_1_LEFT", "ROW_1_RIGHT"), unregistered)
        states.close()
    }

    @Test
    fun missingReadCapabilityMarksFallbackUnavailableWithoutRegisteringAreas() {
        val states = VehicleStateRepository({}, { _, _ -> })
        var registered = false
        val registry = VehicleAreaSubscriptionRegistry(states, { false }, { }, { _, _ -> })

        registry.applyCapability(
            fallback = definition("feature:door-lock"),
            readable = false,
            supportedAreas = listOf(VehicleAreaSubscription(definition("door-lock:ROW_1_LEFT")) {
                registered = true
                { }
            }),
        )

        assertEquals(
            VehicleAvailability.UNSUPPORTED,
            states.snapshot().single { it.definition.dataKey == "feature:door-lock" }.availability,
        )
        assertEquals(false, registered)
        states.close()
    }

    private fun definition(key: String) = VehicleDataDefinition(
        dataKey = key,
        displayName = key,
        domain = VehicleDomain.CONVENIENCE,
        capability = "checkCapability",
        permission = "pleos.car.permission.CAR_WINDOWS",
        valueType = VehicleValueType.INTEGER,
        reactionEvent = VehicleEvent.WINDOW,
        reactionPolicy = VehicleReactionPolicy.POSITION_BUCKET,
        allowWhileDriving = false,
    )
}
