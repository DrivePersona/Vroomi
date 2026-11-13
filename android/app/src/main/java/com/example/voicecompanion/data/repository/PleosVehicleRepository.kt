package com.example.voicecompanion.data.repository

import android.content.Context
import com.example.voicecompanion.data.datasource.VehicleDataSource
import com.example.voicecompanion.data.vehicle.PleosVehicleInputRepository
import com.example.voicecompanion.domain.model.VehicleInput
import com.example.voicecompanion.domain.repository.VehicleRepository

/** Domain 차량 포트를 Pleos/AAOS DataSource에 연결하는 Repository 구현체. */
class PleosVehicleRepository(context: Context) : VehicleRepository {
    private val source: VehicleDataSource = PleosVehicleInputRepository(context)

    override fun start(onInput: (VehicleInput) -> Unit) = source.start(onInput)

    override fun stop() = source.stop()
}
