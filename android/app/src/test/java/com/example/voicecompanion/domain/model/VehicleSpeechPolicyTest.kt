package com.example.voicecompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleSpeechPolicyTest {
    @Test
    fun `every predefined vehicle event has an explicit output and safety never enters gesture manifests`() {
        val clipEvents = VehicleSpeechPolicy.characterClipEventIds

        assertEquals(12, VehicleSpeechPolicy.voicePackClipEventIds.size)
        assertEquals(setOf(VehicleEvent.WARNING, VehicleEvent.ENERGY_URGENT), VehicleSpeechPolicy.safetyEvents)
        assertFalse(VehicleSpeechPolicy.characterGestureEventIds.contains("WARNING"))
        assertFalse(VehicleSpeechPolicy.characterGestureEventIds.contains("ENERGY_LOW"))
        assertTrue(VehicleSpeechPolicy.neutralClipEventIds.contains("WARNING"))
        assertTrue(VehicleSpeechPolicy.neutralClipEventIds.contains("ENERGY_LOW"))
        assertTrue(clipEvents.contains("AMBIENT_LIGHT"))
        assertTrue(clipEvents.contains("HVAC"))
        assertTrue(clipEvents.contains("CHARGE"))
        assertTrue(VehicleSpeechPolicy.voicePackClipEventIds.contains("WIPER"))
        assertFalse(clipEvents.contains("WIPER"))
        assertFalse(clipEvents.contains(VehicleEvent.ENERGY_SUFFICIENT.name))
        assertFalse(clipEvents.contains(VehicleEvent.ENERGY_ATTENTION.name))
    }

    @Test
    fun `safety events use neutral voice-pack clips without character gestures`() {
        VehicleSpeechPolicy.safetyEvents.forEach { event ->
            assertEquals(SpeechOutput.NEUTRAL_CLIP, VehicleSpeechPolicy.outputFor(event))
            val canonicalId = VehicleSpeechPolicy.canonicalEventId(event)
            assertTrue(VehicleSpeechPolicy.voicePackClipEventIds.contains(canonicalId))
            assertFalse(VehicleSpeechPolicy.characterGestureEventIds.contains(canonicalId))
        }
    }

    @Test
    fun `exact values diagnostics and unsupported content stay silent`() {
        assertEquals(SpeechOutput.SILENCE, VehicleSpeechPolicy.outputFor(SpeechContent.EXACT_VALUE))
        assertEquals(SpeechOutput.SILENCE, VehicleSpeechPolicy.outputFor(SpeechContent.DIAGNOSTIC))
        assertEquals(SpeechOutput.SILENCE, VehicleSpeechPolicy.outputFor(SpeechContent.UNSUPPORTED))
    }

    @Test
    fun `energy numbers map to predefined categories rather than dynamic speech`() {
        assertEquals(VehicleEvent.ENERGY_SUFFICIENT, EnergyBand.SUFFICIENT.toVehicleEvent())
        assertEquals(VehicleEvent.ENERGY_COMFORTABLE, EnergyBand.COMFORTABLE.toVehicleEvent())
        assertEquals(VehicleEvent.ENERGY_ATTENTION, EnergyBand.ATTENTION.toVehicleEvent())
        assertEquals(VehicleEvent.ENERGY_URGENT, EnergyBand.URGENT.toVehicleEvent())
        assertEquals(SpeechOutput.SILENCE, VehicleSpeechPolicy.outputFor(VehicleEvent.ENERGY_COMFORTABLE))
        assertEquals(SpeechOutput.NEUTRAL_CLIP, VehicleSpeechPolicy.outputFor(VehicleEvent.ENERGY_URGENT))
        assertEquals("ENERGY_LOW", VehicleSpeechPolicy.canonicalEventId(VehicleEvent.ENERGY_URGENT))
    }

    @Test
    fun `runtime event names map to canonical pack event ids`() {
        assertEquals("AMBIENT_LIGHT", VehicleSpeechPolicy.canonicalEventId(VehicleEvent.LIGHT))
        assertEquals("HVAC", VehicleSpeechPolicy.canonicalEventId(VehicleEvent.COMFORT))
        assertEquals("CHARGE", VehicleSpeechPolicy.canonicalEventId(VehicleEvent.CHARGE_PORT))
        assertEquals("ENERGY_LOW", VehicleSpeechPolicy.canonicalEventId(VehicleEvent.ENERGY_URGENT))
        assertEquals(
            setOf("BRAKE", "TURN", "MODE", "TIRE", "WARNING", "DOOR", "WINDOW", "AMBIENT_LIGHT", "HVAC", "CHARGE", "ENERGY_LOW", "WIPER"),
            VehicleSpeechPolicy.voicePackClipEventIds,
        )
    }
}
