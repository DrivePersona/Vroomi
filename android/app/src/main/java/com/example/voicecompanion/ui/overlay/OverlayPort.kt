package com.example.voicecompanion.ui.overlay

import com.example.voicecompanion.presentation.model.OverlayUiState

/**
 * 캐릭터 곁 오버레이가 앱과 주고받는 창구. 호스트와 시트는 Runtime·ViewModel·CompanionTouch를 직접 알지 않는다.
 * 모든 호출은 메인 스레드에서 한다.
 */
internal interface OverlayPort {
    /** 시트가 그릴 상태. Compose 안에서 읽으면 값이 바뀔 때 다시 그려진다. */
    fun state(): OverlayUiState

    /** 짧은 탭(TOUCH). */
    fun touchCharacter()

    /** 캐릭터와 연결된 음성 팩을 함께 교체한다. 보유하지 않은 후보는 Runtime이 거절한다. */
    fun equipCharacter(characterId: String)

    fun previewDialogue(eventId: String, variantIndex: Int)

    fun setGreetingEnabled(enabled: Boolean)

    /** 정차 중 캐릭터 표시. 끄면 오버레이 전체가 사라진다. */
    fun setParkedAvatarVisible(enabled: Boolean)
}
