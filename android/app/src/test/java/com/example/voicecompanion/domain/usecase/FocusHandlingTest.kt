package com.example.voicecompanion.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

// Pleos 오디오 포커스 정책: CAN_DUCK은 음량을 낮추고, 못 낮추면 멈춘다. LOSS 계열은 멈춘다. GAIN은 되돌린다.
class FocusHandlingTest {
    private class Probe(val canDuck: Boolean) {
        val ducks = mutableListOf<Boolean>()
        var stops = 0
        fun handle(change: FocusChange) = handleFocusChange(change, { on -> ducks += on; canDuck }, { stops++ })
    }

    @Test
    fun `CAN_DUCK은 낮출 수 있으면 멈추지 않고 음량만 낮춘다`() {
        val p = Probe(canDuck = true)
        p.handle(FocusChange.CAN_DUCK)
        assertEquals(listOf(true), p.ducks)
        assertEquals(0, p.stops)
    }

    @Test
    fun `CAN_DUCK인데 낮출 음성이 없으면 멈춘다`() {
        val p = Probe(canDuck = false)
        p.handle(FocusChange.CAN_DUCK)
        assertEquals(listOf(true), p.ducks)
        assertEquals(1, p.stops)
    }

    @Test
    fun `GAIN은 음량을 되돌리고 멈추지 않는다`() {
        val p = Probe(canDuck = true)
        p.handle(FocusChange.GAIN)
        assertEquals(listOf(false), p.ducks)
        assertEquals(0, p.stops)
    }

    @Test
    fun `LOSS와 LOSS_TRANSIENT는 낮추지 않고 바로 멈춘다`() {
        val p = Probe(canDuck = true)
        p.handle(FocusChange.LOSS)
        p.handle(FocusChange.LOSS_TRANSIENT)
        assertEquals(emptyList<Boolean>(), p.ducks)
        assertEquals(2, p.stops)
    }
}
