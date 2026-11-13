package com.example.voicecompanion.domain.usecase

import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleInput
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessVehicleInputUseCaseTest {
    private val useCase = ProcessVehicleInputUseCase()

    @Test
    fun energy_event_is_ignored_until_an_energy_profile_is_available() {
        val result = useCase.execute(
            VehicleInput.Event(VehicleEvent.ENERGY_URGENT, allowWhileDriving = false),
            energyProfileAvailable = false,
        )

        assertEquals(VehicleInputAction.Ignore, result)
    }

    @Test
    fun non_energy_event_remains_an_event() {
        val input = VehicleInput.Event(VehicleEvent.BRAKE, allowWhileDriving = false)

        assertEquals(
            VehicleInputAction.Emit(input.event, input.allowWhileDriving),
            useCase.execute(input, energyProfileAvailable = false),
        )
    }

    @Test
    fun non_event_inputs_stay_typed_for_the_presentation_coordinator() {
        val driving = VehicleInput.Driving(true)
        val status = VehicleInput.Status(VehicleInput.Source.CAR_UX, "주행 규제")

        assertEquals(VehicleInputAction.Driving(true), useCase.execute(driving, false))
        assertEquals(VehicleInputAction.Status(status.source, status.message), useCase.execute(status, false))
    }
}
