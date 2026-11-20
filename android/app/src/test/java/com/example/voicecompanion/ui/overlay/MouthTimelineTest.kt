package com.example.voicecompanion.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class MouthTimelineTest {
    @Test
    fun `preroll silence stays closed and audio position selects later levels`() {
        val levels = intArrayOf(0, 0, 255, 0)
        assertEquals(0f, MouthTimeline.levelAt(levels, 20), 0f)
        assertEquals(0.5f, MouthTimeline.levelAt(levels, 30), 0.01f)
        assertEquals(1f, MouthTimeline.levelAt(levels, 40), 0f)
        assertEquals(0f, MouthTimeline.levelAt(levels, 80), 0f)
    }

    @Test
    fun `missing playback position keeps mouth closed`() {
        assertEquals(0f, MouthTimeline.levelAt(intArrayOf(255), null), 0f)
    }
}
