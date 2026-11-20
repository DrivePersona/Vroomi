package com.example.voicecompanion.ui.overlay

/** Samples a 20 ms PCM envelope at the AudioTrack playback position. */
internal object MouthTimeline {
    private const val STEP_MS = 20L

    fun levelAt(levels: IntArray, positionMs: Long?): Float {
        if (positionMs == null || positionMs < 0 || levels.isEmpty()) return 0f
        val index = positionMs / STEP_MS
        if (index >= levels.size) return 0f
        val current = levels[index.toInt()].coerceIn(0, 255)
        val next = levels.getOrNull(index.toInt() + 1)?.coerceIn(0, 255) ?: 0
        val fraction = (positionMs % STEP_MS).toFloat() / STEP_MS
        return (current + (next - current) * fraction) / 255f
    }
}
