package com.example.voicecompanion.domain


import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleEvent.BRAKE
import com.example.voicecompanion.domain.model.VehicleEvent.TURN
import com.example.voicecompanion.domain.model.VehicleEvent.WARNING
import com.example.voicecompanion.domain.model.VehicleEvent.CRUISE_OFF
import com.example.voicecompanion.domain.model.VehicleEvent.HIGH_BEAM_ON
import com.example.voicecompanion.domain.model.VehicleEvent.LOW_BEAM_ON
import com.example.voicecompanion.domain.model.VehicleEvent.ODO_MILESTONE
import com.example.voicecompanion.domain.model.VehicleEvent.SHIFT_TO_DRIVE
import com.example.voicecompanion.domain.model.VehicleEvent.TAIL_LAMP_ON
import com.example.voicecompanion.domain.model.DropReason
import com.example.voicecompanion.domain.model.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 반응 엔진 규칙 테스트. 웹 시뮬레이터의 engine.test.js와 같은 케이스(알 수 없는 이벤트만 빠짐 — enum이라 만들 수 없다).
// 명세는 기획서 v0.2 §5(이벤트 매핑)·§6(음성 UI·안전 원칙).

private data class Spoke(val text: String, val neutral: Boolean, val volume: Float)

/** 호출을 전부 기록하는 가짜 플랫폼. */
private class FakePlatform : Platform {
    val spoke = mutableListOf<Spoke>()
    val avatars = mutableListOf<Pair<String, String>>()
    val logs = mutableListOf<LogEntry>()
    var stops = 0
    var hides = 0
    var focus = true

    override fun speak(event: VehicleEvent, text: String, neutral: Boolean, volume: Float) {
        spoke += Spoke(text, neutral, volume)
    }
    override fun stop() { stops++ }
    override fun requestFocus() = focus
    override fun showAvatar(face: String, text: String) { avatars += face to text }
    override fun hideAvatar() { hides++ }
    override fun log(entry: LogEntry) { logs += entry }
}

/** 시계와 타이머를 손으로 돌리는 엔진 한 벌. */
private class Rig {
    val platform = FakePlatform()
    var time = 1_000_000L
    val timers = mutableListOf<() -> Unit>()
    val engine = ReactionEngine(
        platform,
        now = { time },
        schedule = { _, block -> timers += block; { timers.remove(block); Unit } },
    )

    fun fire(vararg events: VehicleEvent) {
        events.forEach(engine::emit)
        engine.flush()
    }

    val spoken get() = platform.spoke.map { it.text }
    val drops get() = platform.logs.filterIsInstance<LogEntry.Drop>().map { it.event to it.why }

    /** 걸려 있는 타이머를 모두 돌린다. 도는 동안 새로 걸린 타이머도 이어서 돌린다. */
    fun drain() {
        while (timers.isNotEmpty()) timers.removeAt(0).invoke()
    }
}

class ReactionEngineTest {
    @Test
    fun `정차 중 브레이크는 대사를 말하고 아바타를 보여 준다 음량은 상한 이하`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(BRAKE)
        val s = r.platform.spoke.single()
        assertEquals("으앗!", s.text)
        assertEquals(false, s.neutral)
        assertTrue(s.volume > 0f && s.volume <= 0.8f)
        assertEquals(listOf("😱" to "으앗!"), r.platform.avatars)
    }

    @Test
    fun `같은 이벤트는 쿨다운 안에서 한 번만 말하고 지나면 다시 말한다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(BRAKE)
        r.time += 5_000
        r.fire(BRAKE)
        assertEquals(listOf("으앗!"), r.spoken)
        assertEquals(listOf(BRAKE to DropReason.COOLDOWN), r.drops)
        r.time += 5_001
        r.fire(BRAKE)
        assertEquals(listOf("으앗!", "으앗!"), r.spoken)
    }

    @Test
    fun `같은 순간 여러 이벤트는 안전 상태 연출 순으로 하나만 말하고 나머지는 버린다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(BRAKE, TURN, WARNING)
        assertEquals(listOf("차량 경고가 있어요."), r.spoken)
        assertEquals(setOf(BRAKE to DropReason.PRIORITY, TURN to DropReason.PRIORITY), r.drops.toSet())
    }

    @Test
    fun `상태 보고인 방향지시등이 연출인 브레이크보다 먼저다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(BRAKE, TURN)
        assertEquals(listOf("깜빡이 켰어~"), r.spoken)
    }

    @Test
    fun `쿨다운 중인 이벤트는 우선순위 경쟁에서 빠진다 쿨다운이 먼저`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(WARNING)
        r.time += 1_000
        r.fire(WARNING, BRAKE)
        assertEquals(listOf("차량 경고가 있어요.", "으앗!"), r.spoken)
    }

    @Test
    fun `안전 이벤트는 연출 없이 중립 음성으로 말하고 아바타도 반응하지 않는다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(WARNING)
        assertTrue(r.platform.spoke.single().neutral)
        assertTrue(r.platform.avatars.isEmpty())
    }

    @Test
    fun `오디오 포커스가 없으면 말하지 않고 쿨다운도 쓰지 않는다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.platform.focus = false
        r.fire(BRAKE)
        assertTrue(r.spoken.isEmpty())
        assertEquals(listOf(BRAKE to DropReason.FOCUS), r.drops)
        r.platform.focus = true
        r.fire(BRAKE)
        assertEquals(listOf("으앗!"), r.spoken)
    }

    @Test
    fun `포커스를 잃으면 재생을 바로 멈춘다`() {
        val r = Rig()
        r.engine.focusLost()
        assertEquals(1, r.platform.stops)
    }

    @Test
    fun `주행 중에는 아바타 없이 목소리만 낸다`() {
        val r = Rig()
        r.engine.setDriving(true)
        r.fire(BRAKE)
        assertEquals(listOf("으앗!"), r.spoken)
        assertTrue(r.platform.avatars.isEmpty())
    }

    @Test
    fun `주행 중 음성이 불허되면 정차 중에만 반응한다`() {
        val r = Rig()
        r.engine.setVoiceWhileDriving(false)
        r.engine.setDriving(true)
        r.fire(BRAKE)
        assertTrue(r.spoken.isEmpty())
        assertEquals(listOf(BRAKE to DropReason.DRIVING_SILENT), r.drops)
        r.engine.setDriving(false)
        r.fire(BRAKE)
        assertEquals(listOf("으앗!"), r.spoken)
    }

    @Test
    fun `음성 불허 상태에서 주행이 시작되면 말하던 소리도 멈춘다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.engine.setVoiceWhileDriving(false)
        assertEquals(0, r.platform.stops)
        r.engine.setDriving(true)
        assertEquals(1, r.platform.stops)
    }

    @Test
    fun `주행 중에 음성이 불허로 바뀌면 말하던 소리를 바로 멈춘다`() {
        val r = Rig()
        r.engine.setDriving(true)
        r.engine.setVoiceWhileDriving(false)
        assertEquals(1, r.platform.stops)
    }

    @Test
    fun `상태를 아직 모르는 기동 직후는 주행으로 보고 아바타를 닫아 둔다`() {
        val r = Rig() // setDriving 호출 없음
        r.fire(BRAKE)
        assertTrue(r.platform.avatars.isEmpty())
    }

    @Test
    fun `주행이 시작되면 아바타를 바로 숨긴다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.engine.setDriving(true)
        assertEquals(1, r.platform.hides)
    }

    @Test
    fun `대사를 바꾸면 다음 반응부터 바뀐 대사로 말하고 다른 엔진에는 새지 않는다`() {
        val a = Rig()
        val b = Rig()
        a.engine.setDriving(false)
        b.engine.setDriving(false)
        a.engine.setLine(BRAKE, "앗 차가워!")
        a.fire(BRAKE)
        b.fire(BRAKE)
        assertEquals(listOf("앗 차가워!"), a.spoken)
        assertEquals(listOf("으앗!"), b.spoken)
    }

    @Test
    fun `대사를 비우면 기본 대사로 돌아간다 직전에 바꾼 대사로 남지 않는다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.engine.setLine(BRAKE, "앗 차가워!")
        r.engine.setLine(BRAKE, "  ")
        r.fire(BRAKE)
        assertEquals(listOf("으앗!"), r.spoken)
    }

    @Test
    fun `같은 이벤트가 한 창 안에서 여러 번 와도 한 번만 처리한다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(BRAKE, BRAKE)
        assertEquals(listOf("으앗!"), r.spoken)
        assertTrue(r.drops.isEmpty())
    }

    @Test
    fun `신호 대사는 정차 중에도 기본 음성으로만 말하고 아바타를 띄우지 않는다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(HIGH_BEAM_ON)
        val s = r.platform.spoke.single()
        assertEquals("상향등이 켜졌어요.", s.text)
        assertTrue(s.neutral)
        assertTrue(r.platform.avatars.isEmpty())
    }

    @Test
    fun `하향등과 후미등이 한 창에 같이 오면 하향등만 말한다`() {
        val r = Rig()
        r.engine.setDriving(true)
        r.fire(TAIL_LAMP_ON, LOW_BEAM_ON) // 후미등이 먼저 와도
        assertEquals(listOf("하향등이 켜졌어요."), r.spoken)
        assertEquals(listOf(TAIL_LAMP_ON to DropReason.PRIORITY), r.drops)
    }

    @Test
    fun `크루즈 해제는 같은 창의 상태 안내보다 먼저 말한다`() {
        val r = Rig()
        r.engine.setDriving(true)
        r.fire(SHIFT_TO_DRIVE, CRUISE_OFF)
        assertEquals(listOf("크루즈가 해제됐어요. 직접 운전해 주세요."), r.spoken)
        assertEquals(listOf(SHIFT_TO_DRIVE to DropReason.PRIORITY), r.drops)
    }

    @Test
    fun `정차 후 말할 이벤트는 정차 중이면 바로 말한다`() {
        val r = Rig()
        r.engine.setDriving(false)
        r.fire(ODO_MILESTONE)
        assertEquals(listOf("누적 주행거리 이정표를 지났어요."), r.spoken)
    }

    @Test
    fun `정차 후 말할 이벤트는 주행 중엔 버리지 않고 보관했다가 정차하고 잠시 뒤에 말한다`() {
        val r = Rig()
        r.engine.setDriving(true)
        r.fire(ODO_MILESTONE, ODO_MILESTONE) // 같은 이벤트는 하나로 보관한다
        assertTrue(r.spoken.isEmpty())
        assertTrue(r.drops.isEmpty()) // 버린 게 아니다

        r.engine.setDriving(false)
        assertTrue("도착 안내와 겹치지 않게 바로 말하지 않는다", r.spoken.isEmpty())
        r.drain()
        assertEquals(listOf("누적 주행거리 이정표를 지났어요."), r.spoken)

        r.engine.setDriving(true)
        r.engine.setDriving(false)
        r.drain()
        assertEquals("한 번 말한 이벤트는 다시 말하지 않는다", 1, r.spoken.size)
    }

    @Test
    fun `정차했다가 잠시 뒤에 다시 주행하면 보관한 이벤트는 계속 보관한다`() {
        val r = Rig()
        r.engine.setDriving(true)
        r.fire(ODO_MILESTONE)
        r.engine.setDriving(false)
        r.engine.setDriving(true) // 말하기 전에 다시 출발
        r.drain()
        assertTrue(r.spoken.isEmpty())

        r.engine.setDriving(false)
        r.drain()
        assertEquals(listOf("누적 주행거리 이정표를 지났어요."), r.spoken)
    }

    @Test
    fun `emit만 호출해도 타이머가 창 안의 이벤트를 모아 한 번에 처리한다`() {
        val r = Rig()
        r.engine.setDriving(false)
        listOf(BRAKE, TURN, WARNING).forEach(r.engine::emit)
        assertEquals(1, r.timers.size) // 창은 하나만 연다
        assertTrue(r.spoken.isEmpty()) // 창이 닫히기 전엔 말하지 않는다
        r.timers.toList().forEach { it() }
        assertEquals(listOf("차량 경고가 있어요."), r.spoken)
    }
}
