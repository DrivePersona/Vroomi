package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleValueRule
import kotlin.math.abs
import kotlin.math.floor

/**
 * 신호값이 바뀐 순간(이전 값, 새 값)에서 말할 이벤트를 고르는 규칙. 첫 값(기준값)은 [VehicleStateRepository]가
 * 걸러 주므로 여기에는 항상 "바뀐" 값만 온다. 안드로이드·Pleos SDK를 모르는 순수 함수다.
 *
 * 값은 문자열로 온다: 열거형은 이름(`ON`, `GEAR_DRIVE`, `ACTIVATED` ...), 숫자는 소수 둘째 자리([decimalText]).
 */
internal object SignalReactionRules {
    // ponytail: 아래 기준값은 모두 임시값이다. 차종·실측 뒤에 조정한다.
    /** 조향각(도). 핸들 한 바퀴. */
    const val STEER_BIG_DEGREES = 360f

    /** 누적 주행거리(km) 이정표 간격. */
    const val ODOMETER_MILESTONE_KM = 1_000f

    /** 남은 주행거리(m)의 1차·2차 기준. */
    const val RANGE_LOW_METERS = 50_000f
    const val RANGE_CRITICAL_METERS = 20_000f

    /** 램프가 켜질 때만 말한다. 꺼짐과 주간주행등(DAYTIME_RUNNING)은 말하지 않는다. */
    fun turnsOn(event: VehicleEvent): VehicleValueRule = { previous, value ->
        if (value == "ON" && previous != "ON") event else null
    }

    val gear: VehicleValueRule = { _, value ->
        when (value) {
            "GEAR_DRIVE" -> VehicleEvent.SHIFT_TO_DRIVE
            "GEAR_PARK" -> VehicleEvent.SHIFT_TO_PARK
            "GEAR_REVERSE" -> VehicleEvent.SHIFT_TO_REVERSE
            else -> null // 중립·모름·수동 단수는 말하지 않는다
        }
    }

    private val cruiseEngaged = setOf("ACTIVATED", "USER_OVERRIDE", "SUSPENDED", "FORCED_DEACTIVATION_WARNING")
    private val cruiseReleased = setOf("ENABLED", "OTHER")

    /** 크루즈가 걸릴 때(ACTIVATED)와 풀릴 때만. 페달로 덮어쓰거나 일시 정지했다가 돌아오는 건 말하지 않는다. */
    val cruise: VehicleValueRule = { previous, value ->
        when {
            value == "ACTIVATED" && previous !in cruiseEngaged -> VehicleEvent.CRUISE_ON
            previous in cruiseEngaged && value in cruiseReleased -> VehicleEvent.CRUISE_OFF
            else -> null
        }
    }

    /** 조향각의 절댓값이 기준을 아래에서 위로 넘는 순간. 방향은 가리지 않는다(콘솔은 부호가 VHAL과 반대다). */
    val steeringBig: VehicleValueRule = { previous, value ->
        val before = previous.toFloatOrNull()
        val now = value.toFloatOrNull()
        if (before != null && now != null && abs(before) < STEER_BIG_DEGREES && abs(now) >= STEER_BIG_DEGREES) VehicleEvent.STEER_BIG else null
    }

    /** 누적 주행거리(km)가 이정표 단위를 지나는 순간. 한 번에 여러 개를 건너뛰어도 한 번이다. */
    val odometerMilestone: VehicleValueRule = { previous, value ->
        val before = previous.toFloatOrNull()
        val now = value.toFloatOrNull()
        if (before != null && now != null && before > 0f && floor(now / ODOMETER_MILESTONE_KM) > floor(before / ODOMETER_MILESTONE_KM)) VehicleEvent.ODO_MILESTONE else null
    }

    /** 남은 주행거리(m)가 기준 아래로 내려가는 순간. 올라가는 값과 0(모름)은 말하지 않는다. */
    val rangeRemaining: VehicleValueRule = { previous, value ->
        val before = previous.toFloatOrNull()
        val now = value.toFloatOrNull()
        when {
            before == null || now == null || now <= 0f -> null
            before > RANGE_CRITICAL_METERS && now <= RANGE_CRITICAL_METERS -> VehicleEvent.RANGE_CRITICAL
            before > RANGE_LOW_METERS && now <= RANGE_LOW_METERS -> VehicleEvent.RANGE_LOW
            else -> null
        }
    }
}
