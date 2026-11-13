package com.example.voicecompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.BuildConfig
import com.example.voicecompanion.characterpack.model.CharacterPack
import com.example.voicecompanion.characterpack.model.CharacterPackState
import com.example.voicecompanion.presentation.event.CompanionTouch
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.ui.component.CompanionListRow
import com.example.voicecompanion.ui.component.CompanionPrimaryButton
import com.example.voicecompanion.ui.component.CompanionSecondaryButton
import com.example.voicecompanion.ui.component.CompanionSectionHeader
import com.example.voicecompanion.ui.theme.CompanionTokens

/** 캐릭터 팩 선택과 VR 표시 설정을 담당하는 섹션. */
@Composable
internal fun CharacterPackPicker(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    CompanionSectionHeader("캐릭터 팩", "정차 중 일반 배경 위에 표시할 캐릭터를 고르세요.")
    state.characterPackList.forEach { CharacterPackRow(state, it, onTouch) }
    if (!state.characterPacksLoaded) {
        HelperText("캐릭터 팩을 읽는 중이에요…")
    } else if (state.characterPackList.isEmpty()) {
        HelperText("출시 로스터의 음성·권리·표현 검증을 통과한 캐릭터가 없어 VR 표시를 사용할 수 없어요.")
    } else if (BuildConfig.DEBUG && state.characterPackList.any { it.info?.voicePackId == null }) {
        HelperText("내부 후보 미리보기예요. 음성 팩과 상업 이용 승인을 마치기 전까지 출시 목록에는 들어가지 않아요.")
    }
    Text("VR 표시", style = MaterialTheme.typography.titleSmall)
    SwitchRow(
        label = "VR 캐릭터 켜기",
        hint = "정차 중에만 일반 배경 위에 떠요. 캐릭터를 누르면 이 설정 화면으로 돌아와요.",
        checked = state.vrEnabled,
        onChange = { onTouch(CompanionTouch.VrEnabledChanged(it)) },
    )
    HelperText(state.vrOverlayStatus)
    if (state.vrOverlayPermissionRequired) {
        CompanionSecondaryButton("VR 표시 권한 설정", { onTouch(CompanionTouch.VrPermissionRequested) })
    }
    CompanionPrimaryButton("VR 시작", { onTouch(CompanionTouch.VrStarted) }, enabled = state.canStartVr)
    HelperText("주행이 시작되면 캐릭터를 즉시 숨기고, 음성 반응 규칙은 그대로 유지해요.")
}

@Composable
internal fun CharacterPackRow(state: CompanionUiState, pack: CharacterPack, onTouch: (CompanionTouch) -> Unit) {
    val ready = pack.state is CharacterPackState.Ready
    val selected = state.activeCharacterPack?.id == pack.id
    var showInfo by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CompanionListRow(
            title = pack.name,
            description = characterPackStateText(pack),
            selected = selected,
            enabled = ready && !state.driving,
            onClick = { onTouch(CompanionTouch.CharacterPackSelected(pack.id)) },
        )
        if (ready) {
            TextButton({ showInfo = !showInfo }, Modifier.padding(start = 8.dp)) { Text(if (showInfo) "정보 접기" else "정보") }
            if (showInfo) HelperText(characterPackInfoText(pack))
        }
    }
}

private fun characterPackStateText(pack: CharacterPack): String = when (val state = pack.state) {
    is CharacterPackState.Ready -> when {
        pack.model != null && pack.info?.voicePackId == null -> "내부 미리보기 · GLB 3D 모델"
        pack.model != null -> "준비됨 · GLB 3D 모델"
        pack.sprite != null -> "준비됨 · 2D 애니메이션 스프라이트"
        else -> "사용할 수 없어요 · 표현 파일이 없어요"
    }
    is CharacterPackState.Invalid -> "사용할 수 없어요 · ${state.reason}"
}

private fun characterPackInfoText(pack: CharacterPack): String {
    val info = pack.info ?: return "정보를 읽을 수 없어요"
    return buildString {
        append("버전 ${info.version} · 라이선스 ${info.provenance.license} · 상업 이용 ${if (info.provenance.commercialUse) "가능" else "불가"}")
        info.provenance.attribution?.let { append("\n출처 표기: $it") }
        append("\n")
        append(if (info.provenance.character == "original") "독자 캐릭터 선언" else "참조 기반 캐릭터 · 디버그 전용")
        append(" · ")
        append(if (pack.model != null) "GLB 애니메이션 검증 완료" else "스프라이트 프레임 검증 완료")
    }
}

@Composable
internal fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(CompanionTokens.RowRadius),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = CompanionTokens.MinimumTouchHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(checked = checked, onCheckedChange = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
