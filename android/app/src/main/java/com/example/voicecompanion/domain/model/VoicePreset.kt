package com.example.voicecompanion.domain.model

/** 화면과 음성 어댑터가 공유하는 목소리 표현 정책. */
enum class VoicePreset(val label: String, val pitch: Float, val rate: Float) {
    BRIGHT("밝은", 1.6f, 1.15f),
    CALM("차분한", 1.0f, 0.95f),
    DEEP("굵은", 0.6f, 0.9f),
}
