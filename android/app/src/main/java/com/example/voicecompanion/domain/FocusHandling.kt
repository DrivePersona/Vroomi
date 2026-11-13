package com.example.voicecompanion.domain

/** 오디오 포커스 변화(안드로이드 AUDIOFOCUS_* 값을 네 가지로 줄인 것). */
enum class FocusChange { GAIN, LOSS, LOSS_TRANSIENT, CAN_DUCK }

/**
 * Pleos 오디오 포커스 정책에 따른 우리의 응답. 안드로이드를 몰라서 JVM 테스트가 된다.
 * - GAIN: 낮췄던 음량을 되돌린다.
 * - CAN_DUCK: 음량만 낮춰 계속 말한다. 낮출 수 없는 음성(합성 엔진이 소리를 내는 TTS)이면 멈춘다.
 * - LOSS·LOSS_TRANSIENT: 멈춘다. 짧은 반응이라 밀린 소리는 의미가 없어 재개하지 않는다.
 *   (Pleos 음성 비서가 말할 땐 일시 상실에 즉시 정지하라는 가이드와도 맞다.)
 * [duck]은 실제로 음량을 바꿨는지를 돌려준다.
 */
fun handleFocusChange(change: FocusChange, duck: (Boolean) -> Boolean, stop: () -> Unit) {
    when (change) {
        FocusChange.GAIN -> duck(false)
        FocusChange.CAN_DUCK -> if (!duck(true)) stop()
        FocusChange.LOSS, FocusChange.LOSS_TRANSIENT -> stop()
    }
}
