package com.example.voicecompanion.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingMenuLayoutTest {
    // Pleos 2560x1440 화면에서 시스템 바를 뺀 영역과 캐릭터 창(300x400)·버튼(240x72, X 72) 크기.
    private val safe = PxRect(0, 90, 2560, 1350)
    private val pill = PxSize(240, 72)
    private val close = 72
    private val gap = 16
    private fun anchorAt(x: Int, y: Int) = PxRect(x, y, x + 300, y + 400)
    private fun menu(anchor: PxRect, area: PxRect = safe) = FloatingMenuLayout.menu(anchor, area, pill, close, gap)

    @Test
    fun `가운데에서는 캐릭터 양옆에 링으로 놓고 캐릭터를 가리지 않는다`() {
        val anchor = anchorAt(1130, 500)
        val m = menu(anchor)
        listOf(MenuSlot.CHARACTERS, MenuSlot.DIALOGUE).forEach { assertTrue("$it 왼쪽", m.getValue(it).right <= anchor.left) }
        listOf(MenuSlot.COLLECTION, MenuSlot.SETTINGS, MenuSlot.CLOSE).forEach { assertTrue("$it 오른쪽", m.getValue(it).left >= anchor.right) }
        // 위쪽 줄(캐릭터 바꾸기·가챠)이 같은 높이이고, X는 그 사이에 있다.
        assertEquals(m.getValue(MenuSlot.CHARACTERS).top, m.getValue(MenuSlot.COLLECTION).top)
        assertEquals(m.getValue(MenuSlot.DIALOGUE).top, m.getValue(MenuSlot.SETTINGS).top)
        assertTrue(m.getValue(MenuSlot.CLOSE).top > m.getValue(MenuSlot.COLLECTION).bottom)
        assertTrue(m.getValue(MenuSlot.CLOSE).bottom < m.getValue(MenuSlot.SETTINGS).top)
    }

    @Test
    fun `어느 위치의 캐릭터에서도 메뉴는 안전 영역 안이고 서로 겹치지 않는다`() {
        var checked = 0
        for (x in 0..2260 step 70) for (y in 90..950 step 60) {
            val m = menu(anchorAt(x, y))
            val slots = m.entries.toList()
            slots.forEach { assertTrue("($x,$y) ${it.key} 안전 영역 밖", safe.contains(it.value)) }
            for (i in slots.indices) for (j in i + 1 until slots.size) {
                assertFalse("($x,$y) ${slots[i].key}와 ${slots[j].key}가 겹침", slots[i].value.intersects(slots[j].value))
            }
            checked++
        }
        assertTrue(checked > 400)
    }

    @Test
    fun `양옆 중 한쪽에라도 자리가 있으면 캐릭터를 가리지 않는다`() {
        for (x in 0..2260 step 70) for (y in 90..950 step 60) {
            val anchor = anchorAt(x, y)
            val room = maxOf(anchor.left - gap - safe.left, safe.right - anchor.right - gap)
            if (room < pill.width) continue
            menu(anchor).forEach { (slot, r) -> assertFalse("($x,$y) ${slot}가 캐릭터를 가림", r.intersects(anchor)) }
        }
    }

    @Test
    fun `오른쪽 끝에서는 왼쪽에 한 줄로 쌓는다`() {
        val anchor = anchorAt(2260, 500)
        val m = menu(anchor)
        m.values.forEach { assertTrue(it.right <= anchor.left) }
        assertEquals(1, m.values.map { it.left }.distinct().size)
    }

    @Test
    fun `왼쪽 끝에서는 오른쪽에 한 줄로 쌓는다`() {
        val anchor = anchorAt(0, 500)
        menu(anchor).values.forEach { assertTrue(it.left >= anchor.right) }
    }

    @Test
    fun `위쪽 끝이어도 메뉴 블록 전체가 안전 영역 안으로 내려온다`() {
        val m = menu(anchorAt(1130, 90))
        assertTrue(m.values.minOf { it.top } >= safe.top)
        val bottomEdge = menu(anchorAt(1130, 950))
        assertTrue(bottomEdge.values.maxOf { it.bottom } <= safe.bottom)
    }

    @Test
    fun `캐릭터가 영역 가로를 거의 다 차지해도 안전 영역 안에 서로 겹치지 않게 둔다`() {
        val narrow = PxRect(0, 90, 400, 1350)
        val anchor = PxRect(40, 300, 340, 700)
        val m = menu(anchor, narrow)
        m.values.forEach { assertTrue(narrow.contains(it)) }
        val all = m.values.toList()
        for (i in all.indices) for (j in i + 1 until all.size) assertFalse(all[i].intersects(all[j]))
    }

    @Test
    fun `시트는 캐릭터 오른쪽에 자리가 있으면 오른쪽 위쪽 높이에 놓는다`() {
        val anchor = anchorAt(1130, 500)
        val s = FloatingMenuLayout.sheet(anchor, safe, PxSize(640, 800), gap = 32)
        assertEquals(anchor.right + 32, s.left)
        assertEquals(anchor.top, s.top)
        assertEquals(640, s.width)
        assertEquals(800, s.height)
    }

    @Test
    fun `오른쪽에 자리가 없으면 왼쪽에 놓는다`() {
        val anchor = anchorAt(2100, 500)
        val s = FloatingMenuLayout.sheet(anchor, safe, PxSize(640, 600), gap = 32)
        assertEquals(anchor.left - 32, s.right)
        assertTrue(safe.contains(s))
        assertFalse(s.intersects(anchor))
    }

    @Test
    fun `시트는 안전 영역보다 크지 않고 위아래도 안으로 맞춘다`() {
        val s = FloatingMenuLayout.sheet(anchorAt(1130, 900), safe, PxSize(640, 2000), gap = 32)
        assertTrue(safe.contains(s))
        assertEquals(safe.height, s.height)
        val low = FloatingMenuLayout.sheet(anchorAt(1130, 950), safe, PxSize(640, 800), gap = 32)
        assertEquals(safe.bottom, low.bottom)
    }

    @Test
    fun `힌트는 캐릭터 왼쪽 위에서 30퍼센트 높이에 놓고 자리가 없으면 오른쪽에 놓는다`() {
        val size = PxSize(300, 56)
        val anchor = anchorAt(1130, 500)
        val h = FloatingMenuLayout.hint(anchor, safe, size, gap)
        assertEquals(anchor.left - gap, h.right)
        assertEquals(anchor.top + 120, h.top + 28) // 위에서 30%(120px)에 알약 가운데
        assertTrue(safe.contains(h))

        val atLeftEdge = FloatingMenuLayout.hint(anchorAt(100, 500), safe, size, gap)
        assertEquals(anchorAt(100, 500).right + gap, atLeftEdge.left)
        assertTrue(safe.contains(atLeftEdge))

        // 위쪽 끝에서도 안전 영역 안이다.
        assertTrue(safe.contains(FloatingMenuLayout.hint(anchorAt(1130, 90), safe, size, gap)))
    }

    @Test
    fun `양쪽 다 자리가 없으면 여유가 많은 쪽에 놓고 안으로 눌러 담는다`() {
        val narrow = PxRect(0, 90, 700, 1350)
        val s = FloatingMenuLayout.sheet(PxRect(100, 300, 400, 700), narrow, PxSize(640, 600), gap = 32)
        assertTrue(narrow.contains(s))
    }
}
