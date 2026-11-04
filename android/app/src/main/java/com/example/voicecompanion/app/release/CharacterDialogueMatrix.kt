package com.example.voicecompanion.app.release

import com.example.voicecompanion.app.model.VoiceDialogueRow
import com.example.voicecompanion.app.model.VoiceDialogueVariant
import com.example.voicecompanion.voicepack.domain.GREETING_EVENT
import com.example.voicecompanion.voicepack.domain.VoicePack

/** Fixed display order for the greeting and the 12 v1 canonical vehicle dialogue IDs. */
internal object CharacterDialogueMatrix {
    private val slots = listOf(GREETING_EVENT to "인사말") + listOf(
        "BRAKE" to "브레이크", "TURN" to "방향지시등", "MODE" to "주행 모드",
        "TIRE" to "타이어", "WARNING" to "차량 경고", "DOOR" to "도어",
        "WINDOW" to "창문", "AMBIENT_LIGHT" to "실내 조명", "HVAC" to "공조",
        "WIPER" to "와이퍼", "CHARGE" to "충전구", "ENERGY_LOW" to "에너지 부족",
    )

    fun rows(characterVoicePackId: String?, pack: VoicePack): List<VoiceDialogueRow> {
        if (characterVoicePackId == null || characterVoicePackId != pack.id) return emptyList()
        return slots.mapNotNull { (eventId, label) ->
            val variants = (0 until pack.variantCount(eventId)).mapNotNull { index ->
                pack.lineFor(eventId, index)?.let { VoiceDialogueVariant(index, it) }
            }
            VoiceDialogueRow(eventId, label, variants).takeIf { variants.isNotEmpty() }
        }
    }
}
