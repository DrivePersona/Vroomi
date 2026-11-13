package com.example.voicecompanion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.BuildConfig
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.presentation.event.CompanionTouch
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.ui.component.CompanionCard
import com.example.voicecompanion.ui.component.CompanionSectionHeader

/** 차량 상태와 서비스 상태를 보여주는 읽기 전용 카드. */
@Composable
internal fun ScreenCard(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    val title = when {
        !state.driving -> "정차 중이에요"
        state.voiceWhileDriving -> "목소리로만 알려드릴게요"
        else -> "정차할 때까지 조용히 있을게요"
    }
    val description = when {
        !state.driving -> "캐릭터와 목소리를 바꾸고, 미리 들어볼 수 있어요."
        state.voiceWhileDriving -> "주행 중에는 캐릭터와 설정을 잠시 숨겨요."
        else -> "현재 차량 정책에 따라 주행 중 음성 반응을 멈췄어요."
    }
    CompanionCard(emphasized = !state.driving) {
        CompanionSectionHeader(title, description)
        if (!state.driving) {
            val character = state.activeCharacterPack
            StatusMetric("VR 캐릭터", character?.name ?: "선택한 캐릭터가 없어요")
            StatusMetric("VR 표시", state.vrOverlayStatus)
            CharacterPackPicker(state, onTouch)
        }
        StatusMetric("차량 서비스", state.pleosStatus)
        StatusMetric("주행 규제", state.carUxStatus)
        StatusMetric("음성 합성", state.ttsStatus)
        state.packStatus?.let { StatusMetric("연결된 목소리", it) }
        state.characterPackNotice?.let { StatusMetric("캐릭터 팩", it) }
        if (BuildConfig.DEBUG) VehicleDataDebugCard(state)
    }
}

@Composable
internal fun VehicleDataDebugCard(state: CompanionUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Vehicle 데이터 디버그", style = MaterialTheme.typography.titleSmall)
        state.vehicleData.forEach { item ->
            val timestamp = item.updatedAtEpochMs?.let {
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(it))
            }
            val availability = when (item.availability) {
                VehicleAvailability.CHECKING -> "확인 중"
                VehicleAvailability.ACTIVE -> "수신 중"
                VehicleAvailability.UNSUPPORTED -> "미지원"
                VehicleAvailability.ERROR -> "오류"
            }
            Text(
                "${item.definition.displayName} · $availability${item.value?.let { " · $it" } ?: ""}" +
                    (timestamp?.let { " · $it" } ?: "") +
                    "\n${item.definition.capability} · ${item.definition.permission}" +
                    (item.detail?.let { "\n$it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.availability == VehicleAvailability.ERROR) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun StatusMetric(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.widthIn(min = 72.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
