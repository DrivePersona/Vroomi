package com.example.voicecompanion.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.BuildConfig
import com.example.voicecompanion.presentation.event.CompanionTouch
import com.example.voicecompanion.presentation.event.CompanionUiEffect
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.presentation.viewmodel.CompanionViewModel
import com.example.voicecompanion.ui.component.CompanionBadge
import com.example.voicecompanion.ui.component.CompanionBadgeTone
import com.example.voicecompanion.ui.component.CompanionCard
import com.example.voicecompanion.ui.component.CompanionSectionHeader
import com.example.voicecompanion.ui.theme.CompanionTokens
import kotlinx.coroutines.flow.collect

/** Compose 화면 진입점과 반응형 레이아웃만 담당한다. 카드별 내용은 별도 파일에 둔다. */
@Composable
internal fun CompanionScreen(viewModel: CompanionViewModel, onStartVrEffect: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is CompanionUiEffect.MoveTaskToBackForVr) onStartVrEffect()
        }
    }
    CompanionContent(state, viewModel::onTouch)
}

@Composable
@SuppressLint("UnusedBoxWithConstraintsScope")
private fun CompanionContent(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (state.driving) {
            DrivingContent(state, onTouch)
        } else {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Column(Modifier.fillMaxSize().padding(CompanionTokens.PagePadding)) {
                    CompanionTopHeader(state)
                    Row(
                        Modifier.fillMaxWidth().weight(1f).padding(top = CompanionTokens.SectionGap),
                        horizontalArrangement = Arrangement.spacedBy(CompanionTokens.SectionGap),
                    ) {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(CompanionTokens.SectionGap),
                        ) {
                            ScreenCard(state, onTouch)
                            CustomCard(state, onTouch)
                        }
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(CompanionTokens.SectionGap),
                        ) {
                            if (BuildConfig.DEBUG) SimulatorCard(state, onTouch)
                            LogCard(state, onTouch)
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(CompanionTokens.CompactPagePadding),
                    verticalArrangement = Arrangement.spacedBy(CompanionTokens.SectionGap),
                ) {
                    CompanionTopHeader(state)
                    ScreenCard(state, onTouch)
                    if (BuildConfig.DEBUG) SimulatorCard(state, onTouch)
                    LogCard(state, onTouch)
                    CustomCard(state, onTouch)
                }
            }
        }
    }
}

@Composable
private fun CompanionTopHeader(state: CompanionUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("보이스 컴패니언", style = MaterialTheme.typography.headlineSmall)
            CompanionBadge(
                label = if (state.driving) "주행 중" else "정차 중",
                tone = if (state.driving) CompanionBadgeTone.DRIVING else CompanionBadgeTone.PARKED,
            )
        }
        Text(
            if (state.driving) "차량 상태를 짧고 안전하게 음성으로 알려드려요."
            else "내 차의 목소리와 캐릭터를 편안하게 설정해 보세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 주행 중 화면은 안전 안내와 최소 상태만 조립한다. */
@Composable
private fun DrivingContent(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CompanionTokens.PagePadding),
        verticalArrangement = Arrangement.spacedBy(CompanionTokens.SectionGap),
    ) {
        CompanionTopHeader(state)
        CompanionCard {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("│▂▅▂│", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("목소리로만 알려드릴게요", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    if (state.voiceWhileDriving) "설정과 캐릭터는 정차하면 다시 사용할 수 있어요."
                    else "현재 차량 정책에 따라 음성 반응도 잠시 멈췄어요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        CompanionCard {
            CompanionSectionHeader("현재 상태", "주행 중에는 필요한 상태만 간단히 보여드려요.")
            StatusMetric("음성 반응", if (state.voiceWhileDriving) "켜짐" else "꺼짐")
            StatusMetric("차량 서비스", state.pleosStatus)
            StatusMetric("주행 규제", state.carUxStatus)
        }
        if (BuildConfig.DEBUG) {
            CompanionSectionHeader("개발 도구", "디버그 빌드에서만 표시돼요.")
            SimulatorCard(state, onTouch)
            LogCard(state, onTouch)
        }
    }
}
