package com.example.voicecompanion.domain.model

enum class VehicleEnergyProfile { EV, FUEL, HYBRID, UNAVAILABLE }

object VehicleEnergyProfileResolver {
    /** Returns null until both feature capability checks have completed. */
    fun resolve(evReadable: Boolean?, fuelReadable: Boolean?): VehicleEnergyProfile? {
        if (evReadable == null || fuelReadable == null) return null
        return when {
            evReadable && fuelReadable -> VehicleEnergyProfile.HYBRID
            evReadable -> VehicleEnergyProfile.EV
            fuelReadable -> VehicleEnergyProfile.FUEL
            else -> VehicleEnergyProfile.UNAVAILABLE
        }
    }
}

enum class EnergyBand { SUFFICIENT, COMFORTABLE, ATTENTION, URGENT }

fun EnergyBand.toVehicleEvent(): VehicleEvent = when (this) {
    EnergyBand.SUFFICIENT -> VehicleEvent.ENERGY_SUFFICIENT
    EnergyBand.COMFORTABLE -> VehicleEvent.ENERGY_COMFORTABLE
    EnergyBand.ATTENTION -> VehicleEvent.ENERGY_ATTENTION
    EnergyBand.URGENT -> VehicleEvent.ENERGY_URGENT
}

/** Uses 3 percentage points of hysteresis on upward transitions to prevent threshold chatter. */
class EnergyLevelTracker(private val hysteresisPoints: Int = 3) {
    private var band: EnergyBand? = null

    fun observe(percent: Int): EnergyBand? {
        val value = percent.coerceIn(0, 100)
        val previous = band
        val next = when (previous) {
            null -> bandAt(value)
            EnergyBand.URGENT -> when {
                value >= 63 -> EnergyBand.SUFFICIENT
                value >= 33 -> EnergyBand.COMFORTABLE
                value >= 15 + hysteresisPoints -> EnergyBand.ATTENTION
                else -> EnergyBand.URGENT
            }
            EnergyBand.ATTENTION -> when {
                value < 15 -> EnergyBand.URGENT
                value >= 63 -> EnergyBand.SUFFICIENT
                value >= 30 + hysteresisPoints -> EnergyBand.COMFORTABLE
                else -> EnergyBand.ATTENTION
            }
            EnergyBand.COMFORTABLE -> when {
                value < 15 -> EnergyBand.URGENT
                value < 30 -> EnergyBand.ATTENTION
                value >= 60 + hysteresisPoints -> EnergyBand.SUFFICIENT
                else -> EnergyBand.COMFORTABLE
            }
            EnergyBand.SUFFICIENT -> when {
                value < 15 -> EnergyBand.URGENT
                value < 30 -> EnergyBand.ATTENTION
                value < 60 -> EnergyBand.COMFORTABLE
                else -> EnergyBand.SUFFICIENT
            }
        }
        band = next
        return next.takeIf { previous != null && it != previous }
    }

    fun current(): EnergyBand? = band

    private fun bandAt(percent: Int) = when {
        percent >= 60 -> EnergyBand.SUFFICIENT
        percent >= 30 -> EnergyBand.COMFORTABLE
        percent >= 15 -> EnergyBand.ATTENTION
        else -> EnergyBand.URGENT
    }
}
