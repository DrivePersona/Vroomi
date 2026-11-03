package com.example.voicecompanion.app.release

import com.example.voicecompanion.characterpack.model.CharacterAnimations
import com.example.voicecompanion.characterpack.model.SupportedGesture

/** Resolves one event to at most one explicitly supported, one-shot character gesture. */
internal object CharacterGestureResolver {
    fun resolve(animations: CharacterAnimations?, eventId: String): SupportedGesture? {
        val mappedGestureId = animations?.reactions?.get(eventId) ?: return null
        return animations.supportedGestures[mappedGestureId]?.takeIf { it.oneShot }
    }
}
