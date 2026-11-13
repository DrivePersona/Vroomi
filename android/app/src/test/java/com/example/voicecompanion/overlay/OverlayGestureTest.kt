package com.example.voicecompanion.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayGestureTest {
    // 창은 (1000, 500)에서 시작해 x 0..2280, y 90..1020 안에서만 움직인다. 손가락은 (500, 500)에서 누르고 touchSlop은 10이다.
    private fun gesture() = OverlayGesture(touchSlop = 10)

    private fun OverlayGesture.press(): Boolean = down(500f, 500f, 1000, 500, 0, 2280, 90, 1020)

    @Test
    fun `제자리에서 떼면 탭이다`() {
        val g = gesture()
        assertTrue(g.press())
        assertEquals(OverlayGesture.Result.TAP, g.up())
    }

    @Test
    fun `탭 범위 안에서 흔들려도 탭이다`() {
        val g = gesture()
        g.press()
        assertNull(g.move(505f, 504f))
        assertNull(g.move(497f, 496f))
        assertEquals(OverlayGesture.Result.TAP, g.up())
    }

    @Test
    fun `탭 범위 경계 정확히 slop까지는 탭이고 한 픽셀 넘으면 끌기다`() {
        val onBoundary = gesture().apply { press() }
        assertNull(onBoundary.move(510f, 500f)) // 정확히 touchSlop(10)
        assertEquals(OverlayGesture.Result.TAP, onBoundary.up())

        val pastBoundary = gesture().apply { press() }
        assertEquals(1011 to 500, pastBoundary.move(511f, 500f))
        assertEquals(OverlayGesture.Result.DRAG_END, pastBoundary.up())
    }

    @Test
    fun `끌기는 손가락이 움직인 만큼 창을 옮기고 영역 밖으로는 나가지 않는다`() {
        val g = gesture().apply { press() }
        assertEquals(1030 to 470, g.move(530f, 470f))
        assertEquals(2280 to 1020, g.move(9000f, 9000f))
    }

    @Test
    fun `탭 범위 안에서 길게 누르면 길게 누름이고 떼도 탭이 아니다`() {
        val g = gesture().apply { press() }
        assertNull(g.move(504f, 503f)) // 손떨림은 길게 누름을 깨지 않는다
        assertTrue(g.longPress())
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `길게 누름이 확정된 뒤에는 움직여도 끌기가 되지 않는다`() {
        val g = gesture().apply { press() }
        assertTrue(g.longPress())
        assertNull(g.move(700f, 700f))
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `길게 누름은 입력당 한 번만 확정된다`() {
        val g = gesture().apply { press() }
        assertTrue(g.longPress())
        assertFalse(g.longPress())
    }

    @Test
    fun `끌기가 시작된 뒤 울린 길게 누름 타이머는 무시한다`() {
        val g = gesture().apply { press() }
        assertNotNull(g.move(540f, 500f))
        assertFalse(g.longPress())
        assertEquals(OverlayGesture.Result.DRAG_END, g.up())
    }

    @Test
    fun `끌다가 제자리로 돌아와도 끌기다`() {
        val g = gesture().apply { press() }
        g.move(560f, 500f)
        assertEquals(1000 to 500, g.move(500f, 500f))
        assertFalse(g.longPress())
        assertEquals(OverlayGesture.Result.DRAG_END, g.up())
    }

    @Test
    fun `손가락을 뗀 뒤 늦게 울린 타이머는 무시한다`() {
        val g = gesture().apply { press() }
        assertEquals(OverlayGesture.Result.TAP, g.up())
        assertFalse(g.longPress())
    }

    @Test
    fun `취소하면 탭도 길게 누름도 만들지 않는다`() {
        val g = gesture().apply { press() }
        assertEquals(OverlayGesture.Result.NONE, g.cancel())
        assertFalse(g.longPress())
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `끌던 중 취소하면 위치를 남기게 하고 그 뒤의 입력은 무시한다`() {
        val g = gesture().apply { press() }
        g.move(560f, 500f)
        assertEquals(OverlayGesture.Result.DRAG_END, g.cancel())
        assertNull(g.move(600f, 500f))
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `잠기면 새 입력은 판정하지 않는다`() {
        val g = gesture()
        g.lock()
        assertFalse(g.press())
        assertNull(g.move(900f, 900f))
        assertFalse(g.longPress())
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `누르는 중 잠기면 탭과 길게 누름이 취소된다`() {
        val g = gesture().apply { press() }
        assertEquals(OverlayGesture.Result.NONE, g.lock())
        assertFalse(g.longPress())
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `끄는 중 잠기면 위치를 남기게 한다`() {
        val g = gesture().apply { press() }
        g.move(560f, 500f)
        assertEquals(OverlayGesture.Result.DRAG_END, g.lock())
        assertEquals(OverlayGesture.Result.NONE, g.up())
    }

    @Test
    fun `잠금을 풀면 다음 입력은 다시 판정한다`() {
        val g = gesture()
        g.lock()
        assertFalse(g.press())
        g.up()
        g.unlock()
        assertTrue(g.press())
        assertEquals(OverlayGesture.Result.TAP, g.up())
    }

    @Test
    fun `새 입력은 이전 끌기 상태를 지운다`() {
        val g = gesture().apply { press() }
        g.move(560f, 500f)
        g.up()
        assertTrue(g.press())
        assertNull(g.move(503f, 500f))
        assertEquals(OverlayGesture.Result.TAP, g.up())
    }

    @Test
    fun `어떤 순서로 입력이 와도 한 입력은 탭 길게 누름 끌기 중 하나로만 끝난다`() {
        val alphabet = listOf("small", "big", "long", "up", "cancel")
        fun sequences(length: Int): List<List<String>> =
            if (length == 0) listOf(emptyList()) else sequences(length - 1).flatMap { head -> alphabet.map { head + it } }

        var checked = 0
        for (length in 1..5) for (events in sequences(length)) {
            val g = gesture().apply { press() }
            val outcomes = mutableSetOf<String>()
            for (event in events) {
                when (event) {
                    "small" -> g.move(503f, 502f)?.let { outcomes += "drag" }
                    "big" -> g.move(600f, 500f)?.let { outcomes += "drag" }
                    "long" -> if (g.longPress()) outcomes += "long"
                    "up" -> if (g.up() == OverlayGesture.Result.TAP) outcomes += "tap"
                    "cancel" -> g.cancel()
                }
            }
            assertTrue("$events → $outcomes", outcomes.size <= 1)
            checked++
        }
        assertTrue(checked > 3000)
    }
}
