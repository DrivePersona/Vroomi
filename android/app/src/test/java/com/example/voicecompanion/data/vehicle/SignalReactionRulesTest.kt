package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 값이 바뀐 순간(이전 값 → 새 값)에서 말할 이벤트를 고르는 규칙. 안드로이드·Pleos SDK 없이 돈다. */
class SignalReactionRulesTest {
    @Test
    fun lampSpeaksOnlyWhenItTurnsOn() {
        val rule = SignalReactionRules.turnsOn(VehicleEvent.HIGH_BEAM_ON)
        assertEquals(VehicleEvent.HIGH_BEAM_ON, rule("OFF", "ON"))
        assertEquals(VehicleEvent.HIGH_BEAM_ON, rule("DAYTIME_RUNNING", "ON"))
        assertNull(rule("ON", "OFF"))
        assertNull(rule("OFF", "DAYTIME_RUNNING"))
        assertNull(rule("ON", "ON"))
    }

    @Test
    fun gearSpeaksDriveParkAndReverseAndNothingElse() {
        val rule = SignalReactionRules.gear
        assertEquals(VehicleEvent.SHIFT_TO_DRIVE, rule("GEAR_PARK", "GEAR_DRIVE"))
        assertEquals(VehicleEvent.SHIFT_TO_PARK, rule("GEAR_DRIVE", "GEAR_PARK"))
        assertEquals(VehicleEvent.SHIFT_TO_REVERSE, rule("GEAR_PARK", "GEAR_REVERSE"))
        assertNull(rule("GEAR_DRIVE", "GEAR_NEUTRAL"))
        assertNull(rule("GEAR_DRIVE", "GEAR_UNKNOWN"))
        assertNull(rule("GEAR_PARK", "GEAR_1"))
    }

    @Test
    fun cruiseSpeaksWhenItEngagesAndWhenItIsReleased() {
        val rule = SignalReactionRules.cruise
        assertEquals(VehicleEvent.CRUISE_ON, rule("ENABLED", "ACTIVATED"))
        assertEquals(VehicleEvent.CRUISE_ON, rule("OTHER", "ACTIVATED"))
        assertEquals(VehicleEvent.CRUISE_OFF, rule("ACTIVATED", "ENABLED"))
        assertEquals(VehicleEvent.CRUISE_OFF, rule("USER_OVERRIDE", "ENABLED"))
        assertEquals(VehicleEvent.CRUISE_OFF, rule("SUSPENDED", "ENABLED"))
        assertEquals(VehicleEvent.CRUISE_OFF, rule("ACTIVATED", "OTHER"))
        // 페달로 잠깐 덮어쓰거나 일시 정지했다가 돌아오는 것은 켜짐도 해제도 아니다.
        assertNull(rule("ACTIVATED", "USER_OVERRIDE"))
        assertNull(rule("USER_OVERRIDE", "ACTIVATED"))
        assertNull(rule("ACTIVATED", "SUSPENDED"))
        assertNull(rule("SUSPENDED", "ACTIVATED"))
        assertNull(rule("ACTIVATED", "FORCED_DEACTIVATION_WARNING"))
        assertNull(rule("ENABLED", "OTHER"))
    }

    @Test
    fun steeringSpeaksEachTimeTheWheelCrossesTheLimitFromBelow() {
        val rule = SignalReactionRules.steeringBig
        assertEquals(VehicleEvent.STEER_BIG, rule("0.00", "400.00"))
        assertEquals(VehicleEvent.STEER_BIG, rule("100.00", "-400.00")) // 방향과 무관하다. 콘솔은 부호가 반대다.
        assertEquals(VehicleEvent.STEER_BIG, rule("359.99", "360.00")) // 기준에 닿는 순간
        assertNull(rule("0.00", "359.99"))
        assertNull(rule("400.00", "500.00")) // 이미 넘은 채 더 꺾는 건 새 반응이 아니다
        assertNull(rule("-400.00", "-500.00")) // 반대 방향도 같다
        assertNull(rule("500.00", "100.00")) // 돌아오는 것도 아니다
        assertNull(rule("abc", "400.00")) // 읽을 수 없는 값은 무시한다
    }

    @Test
    fun odometerSpeaksWhenItPassesAThousandKilometreMark() {
        val rule = SignalReactionRules.odometerMilestone
        assertEquals(VehicleEvent.ODO_MILESTONE, rule("999.90", "1000.10"))
        assertEquals(VehicleEvent.ODO_MILESTONE, rule("1999.00", "2000.00"))
        assertEquals(VehicleEvent.ODO_MILESTONE, rule("500.00", "3234.00")) // 한 번에 건너뛰어도 한 번이다
        assertNull(rule("0.00", "12345.00")) // 0은 '모름'이다. 시동 때마다 이정표를 말하면 안 된다
        assertNull(rule("1000.10", "1500.00"))
        assertNull(rule("1500.00", "1000.00")) // 줄어드는 건 이정표가 아니다
        assertNull(rule("0.00", "0.50"))
    }

    @Test
    fun rangeSpeaksOnceEachTimeItDropsBelowALimit() {
        val rule = SignalReactionRules.rangeRemaining
        assertEquals(VehicleEvent.RANGE_LOW, rule("250000.00", "49000.00"))
        assertEquals(VehicleEvent.RANGE_LOW, rule("50001.00", "50000.00"))
        assertEquals(VehicleEvent.RANGE_CRITICAL, rule("49000.00", "19000.00"))
        assertEquals(VehicleEvent.RANGE_CRITICAL, rule("250000.00", "15000.00")) // 한 번에 건너뛰면 더 심한 쪽만
        assertNull(rule("49000.00", "45000.00")) // 이미 아래
        assertNull(rule("19000.00", "10000.00"))
        assertNull(rule("40000.00", "250000.00")) // 올라가는 건 말하지 않는다
        assertNull(rule("250000.00", "0.00")) // 0은 '모름'이다
        assertNull(rule("0.00", "40000.00")) // 모름에서 시작한 값은 비교할 기준이 없다
    }
}
