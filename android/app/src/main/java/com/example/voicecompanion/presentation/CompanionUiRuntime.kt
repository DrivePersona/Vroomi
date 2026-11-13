package com.example.voicecompanion.presentation

import com.example.voicecompanion.domain.model.VoicePreset
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.domain.model.VehicleEvent

/** ViewModel이 앱 동작에 접근하는 계약. 화면·테스트는 구체 Runtime 구현에 묶이지 않는다. */
interface CompanionUiRuntime {
    fun start()
    fun snapshot(): CompanionUiState
    fun setLine(event: VehicleEvent, text: String)
    fun choosePreset(value: VoicePreset)
    fun previewDialogue(eventId: String, variantIndex: Int)
    fun selectCharacterPack(id: String)
    fun changeVrEnabled(value: Boolean)
    fun openVrOverlayPermissionSettings()
    fun beginVr(): Boolean
    fun changeDriving(value: Boolean)
    fun changeVoiceWhileDriving(value: Boolean)
    fun changeNavBusy(value: Boolean)
    fun emit(event: VehicleEvent)
    fun burst()
    fun clearLogs()
    fun setSettingsVisible(value: Boolean)
}
