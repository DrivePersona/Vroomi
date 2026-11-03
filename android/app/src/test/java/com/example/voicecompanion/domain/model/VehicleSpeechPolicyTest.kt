package com.example.voicecompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleSpeechPolicyTest {
    @Test
    fun `every predefined vehicle event has one explicit output and safety never enters clip manifests`() {
        val clipEvents = VehicleSpeechPolicy.characterClipEventIds

        assertEquals(VehicleEvent.entries.size, VehicleSpeechPolicy.voicePackClipEventIds.size)
        assertEquals(setOf(VehicleEvent.WARNING, VehicleEvent.ENERGY_URGENT), VehicleSpeechPolicy.safetyEvents)
        assertFalse(VehicleSpeechPolicy.characterGestureEventIds.contains(VehicleEvent.WARNING.name))
        assertFalse(VehicleSpeechPolicy.characterGestureEventIds.contains(VehicleEvent.ENERGY_URGENT.name))
        assertTrue(VehicleSpeechPolicy.neutralClipEventIds.contains(VehicleEvent.WARNING.name))
        assertTrue(VehicleSpeechPolicy.neutralClipEventIds.contains(VehicleEvent.ENERGY_URGENT.name))
        assertTrue(clipEvents.contains(VehicleEvent.ENERGY_SUFFICIENT.name))
        assertTrue(clipEvents.contains(VehicleEvent.ENERGY_ATTENTION.name))
    }

    @Test
    fun `safety events use neutral voice-pack clips without character gestures`() {
        VehicleSpeechPolicy.safetyEvents.forEach { event ->
            assertEquals(SpeechOutput.NEUTRAL_CLIP, VehicleSpeechPolicy.outputFor(event))
            assertTrue(VehicleSpeechPolicy.voicePackClipEventIds.contains(event.name))
            assertFalse(VehicleSpeechPolicy.characterGestureEventIds.contains(event.name))
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
        assertEquals(SpeechOutput.CHARACTER_CLIP, VehicleSpeechPolicy.outputFor(VehicleEvent.ENERGY_COMFORTABLE))
        assertEquals(SpeechOutput.NEUTRAL_CLIP, VehicleSpeechPolicy.outputFor(VehicleEvent.ENERGY_URGENT))
    }
}
