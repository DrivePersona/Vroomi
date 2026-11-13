package com.example.voicecompanion.domain.usecase

import com.example.voicecompanion.domain.model.VehicleDataState
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleInput
import com.example.voicecompanion.domain.model.isEnergyEvent

/** 차량 입력을 앱 정책이 이해하는 typed action으로 바꾼다. Android/Pleos를 모른다. */
class ProcessVehicleInputUseCase {
    fun execute(input: VehicleInput, energyProfileAvailable: Boolean): VehicleInputAction = when (input) {
        is VehicleInput.Event ->
            if (input.event.isEnergyEvent && !energyProfileAvailable) VehicleInputAction.Ignore
            else VehicleInputAction.Emit(input.event, input.allowWhileDriving)
        is VehicleInput.Driving -> VehicleInputAction.Driving(input.isDriving)
        is VehicleInput.Status -> VehicleInputAction.Status(input.source, input.message)
        is VehicleInput.Catalog -> VehicleInputAction.Catalog(input.states)
    }
}

sealed interface VehicleInputAction {
    data class Emit(val event: VehicleEvent, val allowWhileDriving: Boolean) : VehicleInputAction
    data class Driving(val isDriving: Boolean) : VehicleInputAction
    data class Status(val source: VehicleInput.Source, val message: String) : VehicleInputAction
    data class Catalog(val states: List<VehicleDataState>) : VehicleInputAction
    data object Ignore : VehicleInputAction
}
