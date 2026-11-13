package com.example.voicecompanion.data.vehicle

import android.content.Context
import com.example.voicecompanion.data.datasource.VehicleDataSource
import com.example.voicecompanion.data.mapper.VehicleInputMapper
import com.example.voicecompanion.domain.model.VehicleInput

/** 이전 패키지 호환용 이름. 새 코드에서는 [VehicleDataSource]를 사용한다. */
@Deprecated("Use VehicleDataSource", ReplaceWith("VehicleDataSource"))
interface VehicleInputRepository : VehicleDataSource

/** Pleos Vehicle SDK와 CarUxRestrictions를 공통 [VehicleInput] 모델로 바꾼다. */
class PleosVehicleInputRepository(
    private val context: Context,
) : VehicleInputRepository {
    private var carUx: CarUxSource? = null
    private var pleos: PleosVehicleSource? = null
    private var started = false

    override fun start(onInput: (VehicleInput) -> Unit) {
        if (started) return
        started = true
        carUx = CarUxSource(
            context = context,
            onDriving = { onInput(VehicleInputMapper.driving(it)) },
            onStatus = { onInput(VehicleInputMapper.status(VehicleInput.Source.CAR_UX, it)) },
        ).also { it.start() }
        pleos = PleosVehicleSource(
            context = context,
            onEvent = { event, allowWhileDriving -> onInput(VehicleInputMapper.event(event, allowWhileDriving)) },
            onStatus = { onInput(VehicleInputMapper.status(VehicleInput.Source.PLEOS_VEHICLE, it)) },
            onCatalog = { onInput(VehicleInputMapper.catalog(it)) },
        ).also { it.start() }
    }

    override fun stop() {
        carUx?.stop()
        pleos?.stop()
        carUx = null
        pleos = null
        started = false
    }
}
