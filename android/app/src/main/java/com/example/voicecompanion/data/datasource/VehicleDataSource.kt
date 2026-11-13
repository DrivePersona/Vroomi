package com.example.voicecompanion.data.datasource

import com.example.voicecompanion.domain.model.VehicleInput

/** Pleos·AAOS·시뮬레이터별 입력을 공통 Domain 모델로 내보내는 Data 포트. */
interface VehicleDataSource {
    fun start(onInput: (VehicleInput) -> Unit)
    fun stop()
}
