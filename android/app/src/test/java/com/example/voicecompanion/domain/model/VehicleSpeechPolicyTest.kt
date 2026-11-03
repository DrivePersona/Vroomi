package com.example.voicecompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleSpeechPolicyTest {
    @Test
    fun `every predefined vehicle event has one explicit output and safety never enters clip manifests`() {
        val clipEvents = VehicleSpeechPolicy.characterClipEventIds

        assertEquals(VehicleEvent.entries.size, clipEvents.size + VehicleSpeechPolicy.safetyEvents.size)
        assertEquals(setOf(VehicleEvent.WARNING, VehicleEvent.ENERGY_URGENT), VehicleSpeechPolicy.safetyEvents)
        assertFalse(clipEvents.contains(VehicleEvent.WARNING.name))
        assertFalse(clipEvents.contains(VehicleEvent.ENERGY_URGENT.name))
        assertTrue(clipEvents.contains(VehicleEvent.ENERGY_SUFFICIENT.name))
        assertTrue(clipEvents.contains(VehicleEvent.ENERGY_ATTENTION.name))
    }

    @Test
    fun `safety events use neutral system tts without character clips`() {
        VehicleSpeechPolicy.safetyEvents.forEach { event ->
            assertEquals(SpeechOutput.SYSTEM_TTS, VehicleSpeechPolicy.outputFor(event))
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
        assertEquals(SpeechOutput.SYSTEM_TTS, VehicleSpeechPolicy.outputFor(VehicleEvent.ENERGY_URGENT))
    }
}
