package com.example.voicecompanion.data.mapper

import com.example.voicecompanion.domain.model.VehicleDataState
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleInput

/** SDK 콜백의 원시 값과 Domain 입력 모델 사이의 변환을 한 곳에 둔다. */
object VehicleInputMapper {
    fun event(event: VehicleEvent, allowWhileDriving: Boolean): VehicleInput =
        VehicleInput.Event(event, allowWhileDriving)

    fun driving(isDriving: Boolean): VehicleInput = VehicleInput.Driving(isDriving)

    fun status(source: VehicleInput.Source, message: String): VehicleInput =
        VehicleInput.Status(source, message)

    fun catalog(states: List<VehicleDataState>): VehicleInput = VehicleInput.Catalog(states)
}
