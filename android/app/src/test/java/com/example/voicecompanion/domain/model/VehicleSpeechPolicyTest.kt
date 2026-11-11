package com.example.voicecompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 앱이 신호값에서 직접 만든 한 줄 대사들. 음성 팩 슬롯이 아니라 기본 음성(시스템 TTS)이 읽는다. */
private val signalEvents = listOf(
    VehicleEvent.HIGH_BEAM_ON, VehicleEvent.LOW_BEAM_ON, VehicleEvent.TAIL_LAMP_ON,
    VehicleEvent.FRONT_FOG_ON, VehicleEvent.REAR_FOG_ON,
    VehicleEvent.SHIFT_TO_DRIVE, VehicleEvent.SHIFT_TO_PARK, VehicleEvent.SHIFT_TO_REVERSE,
    VehicleEvent.CRUISE_ON, VehicleEvent.CRUISE_OFF, VehicleEvent.CRUISE_TARGET_SET,
    VehicleEvent.STEER_BIG, VehicleEvent.ODO_MILESTONE,
    VehicleEvent.RANGE_LOW, VehicleEvent.RANGE_CRITICAL,
)

class VehicleSpeechPolicyTest {
    @Test
    fun `every vehicle event has an explicit output`() {
        VehicleEvent.entries.forEach { VehicleSpeechPolicy.outputFor(it) } // 빠진 이벤트가 있으면 NoSuchElementException
    }

    @Test
    fun `signal lines use the built-in voice and never enter the voice-pack or gesture contract`() {
        assertEquals(15, signalEvents.size)
        signalEvents.forEach { event ->
            assertEquals(event.name, SpeechOutput.SYSTEM_TTS, VehicleSpeechPolicy.outputFor(event))
            val id = VehicleSpeechPolicy.canonicalEventId(event)
            assertFalse(id, VehicleSpeechPolicy.voicePackClipEventIds.contains(id))
            assertFalse(id, VehicleSpeechPolicy.characterGestureEventIds.contains(id))
        }
        assertEquals(setOf(VehicleEvent.WARNING, VehicleEvent.ENERGY_URGENT), VehicleSpeechPolicy.safetyEvents)
    }

    @Test
    fun `signal lines are short sentences that never read a value`() {
        signalEvents.forEach { event ->
            assertTrue(event.name, event.defaultLine.isNotBlank())
            assertTrue(event.name, event.defaultLine.length <= 40)
            assertTrue("${event.name} must not read a number", event.defaultLine.none { it.isDigit() })
            assertTrue("${event.name} is delivered without character acting", event.neutral)
        }
        assertEquals(signalEvents.size, signalEvents.map { it.defaultLine }.toSet().size)
    }

    @Test
    fun `lines that hand the driving back or warn about running out are safety rank`() {
        assertEquals(Rank.SAFETY, VehicleEvent.CRUISE_OFF.rank)
        assertEquals(Rank.SAFETY, VehicleEvent.RANGE_CRITICAL.rank)
        assertTrue(Rank.STATUS < Rank.FLOURISH)
        assertTrue(VehicleEvent.LOW_BEAM_ON.rank < VehicleEvent.TAIL_LAMP_ON.rank) // 같이 켜지는 하향등이 후미등을 이긴다
    }

    @Test
    fun `only the odometer milestone waits for a stop`() {
        assertEquals(listOf(VehicleEvent.ODO_MILESTONE), VehicleEvent.entries.filter { it.speakAfterStop })
    }

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
