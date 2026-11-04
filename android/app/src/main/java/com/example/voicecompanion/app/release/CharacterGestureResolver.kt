package com.example.voicecompanion.app.release

import com.example.voicecompanion.characterpack.model.CharacterAnimations
import com.example.voicecompanion.characterpack.model.SupportedGesture

/** Resolves one event to at most one explicitly supported, one-shot character gesture. */
internal object CharacterGestureResolver {
    fun resolve(animations: CharacterAnimations?, eventId: String, clipGestureId: String? = null): SupportedGesture? {
        val characterAnimations = animations ?: return null
        val gestureId = clipGestureId ?: characterAnimations.reactions[eventId] ?: return null
        return characterAnimations.supportedGestures[gestureId]?.takeIf { it.oneShot }
    }
}
