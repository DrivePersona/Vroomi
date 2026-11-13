package com.example.voicecompanion.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.voicecompanion.presentation.event.CompanionTouch
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.ui.component.CompanionCard
import com.example.voicecompanion.ui.component.CompanionPrimaryButton
import com.example.voicecompanion.ui.component.CompanionSecondaryButton
import com.example.voicecompanion.ui.component.CompanionSectionHeader

/** 실제 차량에는 없는 개발용 입력 패널. */
@Composable
internal fun SimulatorCard(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    CompanionCard {
        CompanionSectionHeader("시뮬레이터", "개발용 도구예요. 실제 차량에는 표시되지 않아요.")
        SwitchRow("주행 중", "끄면 정차 상태예요.", state.driving) { onTouch(CompanionTouch.DrivingChanged(it)) }
        SwitchRow("주행 중 음성 허용", "끄면 주행 중 음성 반응을 멈춰요.", state.voiceWhileDriving) { onTouch(CompanionTouch.VoiceWhileDrivingChanged(it)) }
        SwitchRow("내비 안내 중", "오디오 포커스를 잃어 반응이 멈춰요.", state.navBusy) { onTouch(CompanionTouch.NavigationBusyChanged(it)) }
        Text("차량 이벤트", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        state.availableEvents.forEach { event ->
            CompanionSecondaryButton(event.label, { onTouch(CompanionTouch.VehicleEventRequested(event)) })
        }
        CompanionPrimaryButton("동시에 3종 반응 확인", { onTouch(CompanionTouch.EventBurstRequested) })
        HelperText("같은 순간 여러 이벤트가 오면 안전도가 가장 높은 하나만 말해요.")
    }
}
