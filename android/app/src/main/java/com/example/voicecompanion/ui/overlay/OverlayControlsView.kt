package com.example.voicecompanion.ui.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.domain.model.CharacterCollection
import com.example.voicecompanion.domain.model.CollectionSlot
import com.example.voicecompanion.presentation.model.OverlayUiState
import com.example.voicecompanion.ui.theme.TdsProjectValues
import com.example.voicecompanion.ui.theme.TdsSpacing
import com.example.voicecompanion.ui.theme.TdsTheme

// 문구는 harness/avatar-overlay-mockup-2026-11-12.html 기준이다. 사용자 확정 전이라 바뀔 수 있다.

/** Compose가 읽는 컨트롤 창 상태. 값은 호스트만 바꾼다. 좌표는 컨트롤 창 왼쪽 위가 원점인 px이다. */
internal class OverlayControlsUi {
    var surface: OverlaySurface by mutableStateOf(OverlaySurface.Closed)
    var menu: Map<MenuSlot, PxRect> by mutableStateOf(emptyMap())
    var sheet: PxRect? by mutableStateOf(null)

    /** 설정 시트에서 '정차 중 캐릭터 표시'를 끄기 전 확인 중인지. */
    var confirmHide: Boolean by mutableStateOf(false)
}

internal class OverlayControlsActions(
    val openSheet: (OverlaySheet) -> Unit,
    val close: () -> Unit,
    val equip: (String) -> Unit,
    val resetPosition: () -> Unit,
    /** 정차 중 캐릭터 표시를 켜고 끈다. 끄면 이 창이 떼어지므로 호스트가 클릭 처리 밖으로 미룬다. */
    val setParkedAvatarVisible: (Boolean) -> Unit,
)

/**
 * 캐릭터 오버레이 위에 한 장 더 올리는 투명 창의 내용. 창은 안전 영역 전체를 덮고 바깥 탭을 받아 닫는다(캐릭터 위 탭도 닫기다).
 * 메뉴와 시트는 동시에 보이지 않는다. 색·간격·최소 터치 높이는 앱 디자인 토큰을 따른다.
 */
@Composable
internal fun OverlayControlsContent(ui: OverlayControlsUi, port: OverlayPort, actions: OverlayControlsActions) {
    OverlayTheme {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { actions.close() } },
        ) {
            when (val surface = ui.surface) {
                OverlaySurface.Closed -> Unit
                OverlaySurface.Menu -> ui.menu.forEach { (slot, rect) -> MenuButton(slot, rect, actions) }
                is OverlaySurface.Sheet -> ui.sheet?.let { SheetFrame(surface.kind, it, actions, ui, port) }
            }
        }
    }
}

/**
 * 오버레이 전용 TDS 테마(색·글자·여백은 TDS 값, 문서에 없는 값은 TdsProjectValues). 차 화면(Pleos 2560x1440, 160dpi)에서 멀리서도
 * 읽히도록 글자만 [TdsProjectValues.OverlayFontScale]배 키운다. 크기(dp)와 최소 터치 높이는 그대로다.
 */
@Composable
private fun OverlayTheme(content: @Composable () -> Unit) {
    TdsTheme {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * TdsProjectValues.OverlayFontScale),
        ) { content() }
    }
}

/** 캐릭터가 처음 나타날 때 한 번 보이는 힌트. 누를 수 없는 창에 그린다. */
@Composable
internal fun MenuHint() {
    OverlayTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("길게 누르면 메뉴가 열려요", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MenuButton(slot: MenuSlot, rect: PxRect, actions: OverlayControlsActions) {
    val (label, onClick) = when (slot) {
        MenuSlot.CHARACTERS -> "캐릭터 바꾸기" to { actions.openSheet(OverlaySheet.CHARACTERS) }
        MenuSlot.COLLECTION -> "가챠 · 보관함" to { actions.openSheet(OverlaySheet.COLLECTION) }
        MenuSlot.DIALOGUE -> "대사 듣기" to { actions.openSheet(OverlaySheet.DIALOGUE) }
        MenuSlot.SETTINGS -> "설정" to { actions.openSheet(OverlaySheet.SETTINGS) }
        MenuSlot.CLOSE -> "×" to actions.close
    }
    val density = LocalDensity.current
    Surface(
        modifier = Modifier
            .offset { IntOffset(rect.left, rect.top) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            .semantics { if (slot == MenuSlot.CLOSE) contentDescription = "메뉴 닫기" }
            .clickable(role = Role.Button, onClick = onClick),
        shape = if (slot == MenuSlot.CLOSE) CircleShape else RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = if (slot == MenuSlot.CLOSE) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SheetFrame(
    kind: OverlaySheet,
    rect: PxRect,
    actions: OverlayControlsActions,
    ui: OverlayControlsUi,
    port: OverlayPort,
) {
    val density = LocalDensity.current
    val state = port.state()
    val title = when (kind) {
        OverlaySheet.CHARACTERS -> "캐릭터 바꾸기"
        OverlaySheet.COLLECTION -> "가챠 · 보관함"
        OverlaySheet.DIALOGUE -> "캐릭터 대사"
        OverlaySheet.SETTINGS -> "캐릭터 설정"
    }
    val description = when (kind) {
        OverlaySheet.CHARACTERS -> "선택한 캐릭터의 전용 음성이 함께 적용돼요."
        OverlaySheet.COLLECTION -> "첫 후보 ${CharacterCollection.POOL_SLOTS}개예요. 뽑기 비용, 확률, 중복 규칙은 정책 결정 전이라 보여 주지 않아요."
        OverlaySheet.DIALOGUE ->
            if (state.dialogueRows.isEmpty()) "이 캐릭터에 연결된 음성이 없어 대사를 들을 수 없어요."
            else "${state.activeCharacterName ?: "선택한 캐릭터"}의 고정 대사예요. 문구 편집은 없고, 듣기를 누르면 연결된 음성을 재생해요."
        OverlaySheet.SETTINGS -> "짧은 설정만 여기서 바꿀 수 있어요."
    }
    Surface(
        modifier = Modifier
            .offset { IntOffset(rect.left, rect.top) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            // 시트 안쪽의 빈 곳을 눌러도 바깥 탭으로 번져 시트가 닫히지 않게 먹는다.
            .pointerInput(Unit) { detectTapGestures { } },
        shape = RoundedCornerShape(TdsProjectValues.SheetRadius),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 16.dp,
    ) {
        Column {
            // BottomSheet 구조: header → children(스크롤) → cta(아래 고정)
            TdsSheetHeader(title, description, actions.close)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                when (kind) {
                    OverlaySheet.CHARACTERS -> CharacterSheet(state, actions.equip)
                    OverlaySheet.COLLECTION -> CollectionSheet(state)
                    OverlaySheet.DIALOGUE -> DialogueSheet(state, port::previewDialogue)
                    OverlaySheet.SETTINGS -> SettingsSheet(state, port, actions)
                }
            }
            if (kind == OverlaySheet.COLLECTION) {
                // BottomSheet.CTA: 비활성인 이유를 함께 보인다. 정책이 확정되기 전에는 어떤 경로로도 보유 상태를 만들지 않는다.
                Column(Modifier.fillMaxWidth().padding(horizontal = TdsSpacing.rowHorizontalSmall, vertical = 12.dp)) {
                    TdsButton("뽑기", onClick = {}, modifier = Modifier.fillMaxWidth(), fill = true, enabled = false)
                    TdsFootnote("뽑기는 획득 정책이 정해지면 열려요.")
                }
            }
        }
    }
}

@Composable
private fun CharacterSheet(state: OverlayUiState, onEquip: (String) -> Unit) {
    if (state.internalPreview) TdsFootnote("내부 후보 미리보기예요. 앞의 5개를 기본, 다음 11개를 가챠 후보로 보여 줘요. 기본·가챠 명단은 아직 정해지지 않았어요.")
    state.notice?.let { TdsFootnote(it) }
    TdsSectionTitle("기본 ${CharacterCollection.BASIC_SLOTS}")
    SlotRows(state.collection.basic, onEquip)
    TdsSectionTitle("가챠 ${CharacterCollection.POOL_SLOTS} · 보유 ${state.collection.ownedPoolCount}")
    SlotRows(state.collection.pool, onEquip)
    TdsFootnote("보유한 가챠 캐릭터만 고를 수 있어요. 보유와 장착은 별개예요.")
}

@Composable
private fun CollectionSheet(state: OverlayUiState) {
    if (state.internalPreview) TdsFootnote("내부 후보 미리보기예요. 후보 명단은 아직 정해지지 않았어요.")
    TdsSectionTitle("보유 ${state.collection.ownedPoolCount} / ${CharacterCollection.POOL_SLOTS}")
    SlotRows(state.collection.pool, onEquip = null)
}

/** 캐릭터 한 칸 = ListRow 한 줄. 상태는 오른쪽 글자로 보이고(색만으로 전달하지 않는다), 선택 불가 칸은 회색 배경이다. */
@Composable
private fun SlotRows(slots: List<CollectionSlot>, onEquip: ((String) -> Unit)?) {
    slots.forEachIndexed { index, slot ->
        val id = slot.characterId
        val status = when {
            id == null -> "비어 있음"
            slot.equipped -> "장착 중"
            slot.owned -> "보유"
            else -> if (onEquip == null) "미보유" else "잠김"
        }
        val selectable = onEquip != null && id != null && slot.owned && !slot.equipped
        TdsListRow(
            title = slot.name,
            onClick = if (selectable) ({ onEquip?.invoke(id!!) }) else null,
            disabled = id == null || (!slot.owned && !slot.equipped),
            emphasized = slot.equipped,
            right = { TdsStatusText(status, highlighted = slot.equipped) },
        )
        if (index < slots.lastIndex) TdsRowDivider()
    }
}

@Composable
private fun DialogueSheet(state: OverlayUiState, onPreview: (String, Int) -> Unit) {
    state.dialogueRows.forEach { row ->
        TdsSectionTitle(row.label)
        row.variants.forEachIndexed { index, variant ->
            TdsListRow(
                title = "${variant.index + 1}. ${variant.text}",
                right = {
                    if (row.audioReady) {
                        TdsButton("듣기", onClick = { onPreview(row.eventId, variant.index) })
                    } else {
                        TdsStatusText("음성 준비 중")
                    }
                },
            )
            if (index < row.variants.lastIndex) TdsRowDivider()
        }
    }
}

@Composable
private fun SettingsSheet(state: OverlayUiState, port: OverlayPort, actions: OverlayControlsActions) {
    TdsListRow(
        title = "세션 시작 인사",
        description = "정차 중 캐릭터가 처음 나타날 때 한 번 인사해요.",
        onClick = { port.setGreetingEnabled(!state.greetingEnabled) },
        role = Role.Switch,
        right = { TdsSwitch(state.greetingEnabled) },
    )
    TdsRowDivider()
    // '정차 중 캐릭터 표시' 끄기는 제공하지 않는다. 끄면 캐릭터가 사라져 설정을 다시 열 수 없고,
    // 되돌릴 앱 화면도 없기 때문이다(Activity 제거). 캐릭터는 정차 중 항상 표시한다.
    TdsListRow(
        title = "캐릭터 위치 초기화",
        right = { TdsButton("초기화", onClick = actions.resetPosition) },
    )
}
