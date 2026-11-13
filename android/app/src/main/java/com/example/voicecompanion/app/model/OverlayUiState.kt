package com.example.voicecompanion.app.model

/** 캐릭터 곁 오버레이 시트가 그리는 읽기 전용 상태. 화면 Activity의 [CompanionUiState]와 별개로 시트에 필요한 것만 담는다. */
data class OverlayUiState(
    val collection: CharacterCollection,
    val activeCharacterId: String?,
    val activeCharacterName: String?,
    val dialogueRows: List<VoiceDialogueRow>,
    val greetingEnabled: Boolean,
    val parkedAvatarVisible: Boolean,
    val notice: String?,
    /** 출시 로스터가 아니라 내부 후보 미리보기 목록이다. */
    val internalPreview: Boolean,
)
