package com.example.voicecompanion.app.model

import com.example.voicecompanion.data.audio.VoicePreset
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleDataState
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleEnergyProfile
import com.example.voicecompanion.domain.model.isEnergyEvent
import com.example.voicecompanion.characterpack.model.CharacterPack

/** 화면이 렌더링하는 읽기 전용 상태. UI는 Runtime의 가변 상태를 직접 읽지 않는다. */
data class CompanionUiState(
    val driving: Boolean,
    val voiceWhileDriving: Boolean,
    val navBusy: Boolean,
    val preset: VoicePreset,
    val pleosStatus: String,
    val vehicleData: List<VehicleDataState>,
    val carUxStatus: String,
    val ttsStatus: String,
    val typedLines: Map<VehicleEvent, String>,
    val logs: List<ReactionLogItem>,
    val packStatus: String?,
    val dialogueRows: List<VoiceDialogueRow>,
    val fixedLines: Map<VehicleEvent, String?>,
    val characterPackList: List<CharacterPack>,
    val activeCharacterPack: CharacterPack?,
    val characterPacksLoaded: Boolean,
    val characterPackNotice: String?,
    val vrEnabled: Boolean,
    val vrOverlayStatus: String,
    val vrOverlayPermissionRequired: Boolean,
    val canStartVr: Boolean,
) {
    fun fixedLine(event: VehicleEvent): String? = fixedLines[event]

    val energyProfileAvailable: Boolean
        get() = vehicleData.any {
            it.definition.dataKey == "vehicle-energy-profile" &&
                it.availability == VehicleAvailability.ACTIVE &&
                it.value?.let { value -> runCatching { VehicleEnergyProfile.valueOf(value) }.getOrNull() }?.let { it != VehicleEnergyProfile.UNAVAILABLE } == true
        }

    val availableEvents: List<VehicleEvent>
        get() = VehicleEvent.entries.filter { !it.isEnergyEvent || energyProfileAvailable }
}

data class ReactionLogItem(
    val time: String,
    val text: String,
    val note: String,
)

data class VoiceDialogueRow(
    val eventId: String,
    val label: String,
    val variants: List<VoiceDialogueVariant>,
    /** False means the caption is approved text but its WAV is not shipped yet. */
    val audioReady: Boolean = true,
)

data class VoiceDialogueVariant(val index: Int, val text: String)
