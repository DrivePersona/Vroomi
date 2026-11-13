package com.example.voicecompanion.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayControlsTest {
    @Test
    fun `처음에는 아무것도 열려 있지 않다`() {
        assertEquals(OverlaySurface.Closed, OverlayControls().surface)
    }

    @Test
    fun `길게 누르면 메뉴가 열린다`() {
        val controls = OverlayControls()
        assertTrue(controls.openMenu())
        assertEquals(OverlaySurface.Menu, controls.surface)
    }

    @Test
    fun `메뉴에서 시트를 고르면 메뉴는 닫히고 그 시트만 열린다`() {
        val controls = OverlayControls().apply { openMenu() }
        assertTrue(controls.openSheet(OverlaySheet.DIALOGUE))
        assertEquals(OverlaySurface.Sheet(OverlaySheet.DIALOGUE), controls.surface)
    }

    @Test
    fun `메뉴가 열려 있을 때만 시트로 넘어간다`() {
        val closed = OverlayControls()
        assertFalse(closed.openSheet(OverlaySheet.SETTINGS))
        assertEquals(OverlaySurface.Closed, closed.surface)

        val sheet = OverlayControls().apply { openMenu(); openSheet(OverlaySheet.CHARACTERS) }
        assertFalse(sheet.openSheet(OverlaySheet.SETTINGS))
        assertEquals(OverlaySurface.Sheet(OverlaySheet.CHARACTERS), sheet.surface)
    }

    @Test
    fun `X와 바깥 탭은 지금 열린 메뉴나 시트를 닫는다`() {
        val menu = OverlayControls().apply { openMenu(); close() }
        assertEquals(OverlaySurface.Closed, menu.surface)

        val sheet = OverlayControls().apply { openMenu(); openSheet(OverlaySheet.COLLECTION); close() }
        assertEquals(OverlaySurface.Closed, sheet.surface)
    }

    @Test
    fun `시트가 열려 있다가 길게 누르면 시트는 닫히고 메뉴가 열린다`() {
        val controls = OverlayControls().apply { openMenu(); openSheet(OverlaySheet.COLLECTION) }
        assertTrue(controls.openMenu())
        assertEquals(OverlaySurface.Menu, controls.surface)
    }

    @Test
    fun `메뉴가 열린 채 주행이 시작되면 즉시 닫히고 잠금을 풀 때까지 열리지 않는다`() {
        val controls = OverlayControls().apply { openMenu() }
        controls.lock()
        assertEquals(OverlaySurface.Closed, controls.surface)
        assertFalse(controls.openMenu())
        assertFalse(controls.openSheet(OverlaySheet.CHARACTERS))
        assertEquals(OverlaySurface.Closed, controls.surface)

        controls.unlock()
        assertTrue(controls.openMenu())
    }

    @Test
    fun `시트가 열린 채 주행이 시작돼도 즉시 닫힌다`() {
        val controls = OverlayControls().apply { openMenu(); openSheet(OverlaySheet.SETTINGS) }
        controls.lock()
        assertEquals(OverlaySurface.Closed, controls.surface)
    }
}

class CharacterSwitchTest {
    @Test
    fun `OUT이 끝나면 새 캐릭터 id를 한 번 돌려주고 IN으로 넘어간다`() {
        val switch = CharacterSwitch()
        assertTrue(switch.begin("b", currentId = "a"))
        assertEquals(CharacterSwitch.Phase.OUT, switch.phase)
        assertEquals("b", switch.outFinished())
        assertEquals(CharacterSwitch.Phase.IN, switch.phase)
        assertNull(switch.outFinished())
        switch.inFinished()
        assertEquals(CharacterSwitch.Phase.IDLE, switch.phase)
    }

    @Test
    fun `이미 장착한 캐릭터이거나 교체 중이면 시작하지 않는다`() {
        val switch = CharacterSwitch()
        assertFalse(switch.begin("a", currentId = "a"))
        assertTrue(switch.begin("b", currentId = "a"))
        assertFalse(switch.begin("c", currentId = "a"))
    }

    @Test
    fun `OUT 중에 주행이 시작되면 교체를 보류하고 이전 캐릭터를 유지한다`() {
        val switch = CharacterSwitch()
        switch.begin("b", currentId = "a")
        switch.lock()
        assertNull(switch.outFinished()) // 호출한 쪽은 아무것도 고르지 않는다
        assertEquals(CharacterSwitch.Phase.IDLE, switch.phase)
    }

    @Test
    fun `IN 중에 잠기면 연출만 멈추고 이미 한 교체는 되돌리지 않는다`() {
        val switch = CharacterSwitch()
        switch.begin("b", currentId = "a")
        assertEquals("b", switch.outFinished())
        switch.lock()
        assertEquals(CharacterSwitch.Phase.IDLE, switch.phase)
    }

    @Test
    fun `잠겨 있으면 시작하지 않고 풀면 다시 시작한다`() {
        val switch = CharacterSwitch()
        switch.lock()
        assertFalse(switch.begin("b", currentId = "a"))
        switch.unlock()
        assertTrue(switch.begin("b", currentId = "a"))
    }

    @Test
    fun `OUT이 아닐 때는 교체 지점을 주지 않는다`() {
        val switch = CharacterSwitch()
        assertNull(switch.outFinished())
        switch.inFinished()
        assertEquals(CharacterSwitch.Phase.IDLE, switch.phase)
    }
}
