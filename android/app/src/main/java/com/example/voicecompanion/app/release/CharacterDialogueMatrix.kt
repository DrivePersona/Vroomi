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

    private val base = mapOf(
        GREETING_EVENT to "준비 완료", "BRAKE" to "감속 확인", "TURN" to "방향 신호 확인",
        "MODE" to "주행 모드 변경", "TIRE" to "타이어 상태 확인", "WARNING" to "차량 경고 확인",
        "DOOR" to "도어 상태 확인", "WINDOW" to "창문 상태 확인", "AMBIENT_LIGHT" to "실내 조명 변경",
        "HVAC" to "공조 설정 변경", "WIPER" to "와이퍼 상태 확인", "CHARGE" to "충전구 상태 확인",
        "ENERGY_LOW" to "에너지 잔량 낮음",
    )

    /** Text-only fallbacks mirror the private matrix until the remaining Qwen WAVs are shipped. */
    private fun pendingLine(packId: String, eventId: String): String? {
        val b = base[eventId] ?: return null
        return when {
            packId.contains("cooldere") -> if (eventId == GREETING_EVENT) "……시스템 준비 완료. 함께해." else "……$b. 계속해."
            packId.contains("clumsy") -> if (eventId == GREETING_EVENT) "선배! 준비, 준비했어요!" else "선배! $b… 했, 했어요!"
            packId.contains("tsundere") -> if (eventId == GREETING_EVENT) "오호호호! 준비는 됐거든요." else "기사, ${b}하세요. …아, 칭찬 아니에요!"
            packId.contains("chuunibyo") -> if (eventId == GREETING_EVENT) "크하하, 봉인 해제! …준비는 됐다." else "크하하, $b! …이게 안전하니까."
            packId.contains("gentle-older-sister") -> if (eventId == GREETING_EVENT) "어머, 준비됐어요. 천천히 함께 가요." else "어머, $b. 잘하고 있어요~"
            packId.contains("magical-girl") -> if (eventId == GREETING_EVENT) "파트너, 출발 마법 준비! 반짝!" else "파트너, ${b} 마법, 발동!"
            packId.contains("car-geek") -> if (eventId == GREETING_EVENT) "오오, 시스템 준비 오케이! 오늘도 차 얘기해요." else "오오, $b! 좋아, 가자!"
            packId.contains("fox-spirit") -> if (eventId == GREETING_EVENT) "오냐, 부적은 준비되었도다." else "오냐, $b. 기특하도다."
            packId.contains("stoic-guardian") -> if (eventId == GREETING_EVENT) "……준비 완료. 맡겨라." else "……$b. 맡겨라."
            packId.contains("in-car-streamer") -> if (eventId == GREETING_EVENT) "여러분~ 방송 준비 완료! 오늘도 함께 가요~" else "와, $b 인증! 여러분 보셨죠~?"
            else -> null
        }
    }

    fun rows(characterVoicePackId: String?, pack: VoicePack): List<VoiceDialogueRow> {
        if (characterVoicePackId == null || characterVoicePackId != pack.id) return emptyList()
        return slots.mapNotNull { (eventId, label) ->
            val variants = (0 until pack.variantCount(eventId)).mapNotNull { index ->
                pack.lineFor(eventId, index)?.let { VoiceDialogueVariant(index, it) }
            }
            if (variants.isNotEmpty()) VoiceDialogueRow(eventId, label, variants, audioReady = true)
            else pendingLine(pack.id, eventId)?.let { VoiceDialogueRow(eventId, label, listOf(VoiceDialogueVariant(0, it)), audioReady = false) }
        }
    }
}
