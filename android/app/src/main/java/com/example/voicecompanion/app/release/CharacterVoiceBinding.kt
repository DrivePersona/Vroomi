package com.example.voicecompanion.app.release

import com.example.voicecompanion.characterpack.model.CharacterPack
import com.example.voicecompanion.voicepack.domain.PackState
import com.example.voicecompanion.voicepack.domain.VoicePack

/** Resolves the selected character's manifest ID to its installed READY voice pack. */
internal object CharacterVoiceBinding {
    fun resolve(character: CharacterPack?, voices: List<VoicePack>): VoicePack? {
        val voicePackId = character?.info?.voicePackId ?: return null
        return voices.firstOrNull { it.id == voicePackId && it.state is PackState.Ready }
    }
}
