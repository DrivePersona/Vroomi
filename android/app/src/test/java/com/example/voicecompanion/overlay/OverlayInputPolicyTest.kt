package com.example.voicecompanion.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayInputPolicyTest {
    // Pleos Connect-S 2560x1440: 위 90, 아래 1300이 시스템 바라 usable은 [0,90][2560,1300].
    private val usable = PxRect(0, 90, 2560, 1300)

    @Test
    fun `왼쪽 driving view 폭만큼 영역에서 뺀다`() {
        val area = OverlayArea.exceptDrivingView(usable, screenWidth = 2560, minWidth = 900)
        assertEquals(PxRect(853, 90, 2560, 1300), area)
    }

    @Test
    fun `시스템 바가 이미 더 오른쪽까지 차지하면 그 값을 따른다`() {
        val area = OverlayArea.exceptDrivingView(PxRect(1000, 90, 2560, 1300), screenWidth = 2560, minWidth = 900)
        assertEquals(1000, area.left)
    }

    @Test
    fun `영역이 너무 좁아지면 제외하지 않는다`() {
        val small = PxRect(0, 0, 1200, 700)
        assertEquals(small, OverlayArea.exceptDrivingView(small, screenWidth = 1200, minWidth = 900))
    }

    @Test
    fun `메뉴 안내는 메뉴를 열어 본 적이 없을 때만 최대 횟수까지 보인다`() {
        assertTrue(MenuHintPolicy.shouldShow(shownCount = 0, menuEverOpened = false))
        assertTrue(MenuHintPolicy.shouldShow(shownCount = MenuHintPolicy.MAX_SHOWS - 1, menuEverOpened = false))
        assertFalse(MenuHintPolicy.shouldShow(shownCount = MenuHintPolicy.MAX_SHOWS, menuEverOpened = false))
        assertFalse(MenuHintPolicy.shouldShow(shownCount = 0, menuEverOpened = true))
    }

    @Test
    fun `눌림 표시는 눌려 있고 페이드·교체 중이 아닐 때만 한다`() {
        assertTrue(PressFeedback.canShow(pressed = true, fading = false, switching = false))
        assertFalse(PressFeedback.canShow(pressed = false, fading = false, switching = false))
        assertFalse(PressFeedback.canShow(pressed = true, fading = true, switching = false))
        assertFalse(PressFeedback.canShow(pressed = true, fading = false, switching = true))
        assertEquals(1f, PressFeedback.alphaFor(false), 0f)
        assertTrue(PressFeedback.alphaFor(true) < 1f)
    }
}
