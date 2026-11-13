package com.example.voicecompanion.domain.repository

import com.example.voicecompanion.domain.model.VehicleInput

/**
 * 차량 입력 포트.
 *
 * Domain은 Pleos SDK, AAOS, Android Context를 알지 못하고 이 계약만 소비한다.
 */
interface VehicleRepository {
    fun start(onInput: (VehicleInput) -> Unit)
    fun stop()
}
