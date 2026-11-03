package com.example.voicecompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleEnergyProfileTest {
    @Test
    fun profileUsesOnlyEvAndFuelCapabilities() {
        assertNull(VehicleEnergyProfileResolver.resolve(null, true))
        assertEquals(VehicleEnergyProfile.EV, VehicleEnergyProfileResolver.resolve(true, false))
        assertEquals(VehicleEnergyProfile.FUEL, VehicleEnergyProfileResolver.resolve(false, true))
        assertEquals(VehicleEnergyProfile.HYBRID, VehicleEnergyProfileResolver.resolve(true, true))
        assertEquals(VehicleEnergyProfile.UNAVAILABLE, VehicleEnergyProfileResolver.resolve(false, false))
    }

    @Test
    fun energyBandsUseRequiredPercentBoundariesAndHysteresis() {
        val tracker = EnergyLevelTracker(hysteresisPoints = 3)
        assertNull(tracker.observe(60)) // initial sample is a baseline
        assertEquals(EnergyBand.COMFORTABLE, tracker.observe(59))
        assertNull(tracker.observe(60)) // moving up needs 63 to return to sufficient
        assertNull(tracker.observe(62))
        assertEquals(EnergyBand.SUFFICIENT, tracker.observe(63))

        val low = EnergyLevelTracker(hysteresisPoints = 3)
        assertNull(low.observe(15))
        assertEquals(EnergyBand.URGENT, low.observe(14))
        assertNull(low.observe(16)) // urgent clears only at 18
        assertEquals(EnergyBand.ATTENTION, low.observe(18))
        assertNull(low.observe(30)) // attention clears only at 33
        assertEquals(EnergyBand.COMFORTABLE, low.observe(33))
    }

    @Test
    fun largeChangesMoveDirectlyToFinalBandAndPercentIsClamped() {
        val tracker = EnergyLevelTracker()
        assertNull(tracker.observe(101))
        assertEquals(EnergyBand.URGENT, tracker.observe(-4))
        assertEquals(EnergyBand.SUFFICIENT, tracker.observe(100))
    }

    @Test
    fun eachEnergyBandMapsToItsOwnReactionAndUrgentIsNeutral() {
        assertEquals(VehicleEvent.ENERGY_SUFFICIENT, EnergyBand.SUFFICIENT.toVehicleEvent())
        assertEquals(VehicleEvent.ENERGY_COMFORTABLE, EnergyBand.COMFORTABLE.toVehicleEvent())
        assertEquals(VehicleEvent.ENERGY_ATTENTION, EnergyBand.ATTENTION.toVehicleEvent())
        assertEquals(VehicleEvent.ENERGY_URGENT, EnergyBand.URGENT.toVehicleEvent())
        assertEquals(true, VehicleEvent.ENERGY_URGENT.neutral)
    }
}
