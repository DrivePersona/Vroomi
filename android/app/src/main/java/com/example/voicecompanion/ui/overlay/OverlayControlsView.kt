package com.example.voicecompanion.ui.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.domain.model.CharacterCollection
import com.example.voicecompanion.domain.model.CollectionSlot
import com.example.voicecompanion.presentation.model.OverlayUiState
import com.example.voicecompanion.ui.component.CompanionSecondaryButton
import com.example.voicecompanion.ui.theme.CompanionTheme
import com.example.voicecompanion.ui.theme.CompanionTokens

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

/** 앱 테마에서 글자만 키운 오버레이 테마. 차 화면(Pleos 2560x1440, 160dpi)에서 멀리서도 읽히게 한다. 크기(dp)와 최소 터치 높이는 그대로다. */
@Composable
private fun OverlayTheme(content: @Composable () -> Unit) {
    CompanionTheme {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * OVERLAY_FONT_SCALE)) { content() }
    }
}

private const val OVERLAY_FONT_SCALE = 1.25f

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
    Surface(
        modifier = Modifier
            .offset { IntOffset(rect.left, rect.top) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            // 시트 안쪽의 빈 곳을 눌러도 바깥 탭으로 번져 시트가 닫히지 않게 먹는다.
            .pointerInput(Unit) { detectTapGestures { } },
        shape = RoundedCornerShape(CompanionTokens.CardRadius),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 16.dp,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CompanionTokens.MinimumTouchHeight)
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Surface(
                    modifier = Modifier
                        .size(CompanionTokens.MinimumTouchHeight)
                        .semantics { contentDescription = "닫기" }
                        .clickable(role = Role.Button, onClick = actions.close),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("×", style = MaterialTheme.typography.titleLarge) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SheetBody {
                when (kind) {
                    OverlaySheet.CHARACTERS -> CharacterSheet(state, actions.equip)
                    OverlaySheet.COLLECTION -> CollectionSheet(state)
                    OverlaySheet.DIALOGUE -> DialogueSheet(state, port::previewDialogue)
                    OverlaySheet.SETTINGS -> SettingsSheet(state, ui, port, actions)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SheetBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun CharacterSheet(state: OverlayUiState, onEquip: (String) -> Unit) {
    Note("선택한 캐릭터의 전용 음성이 함께 적용돼요.")
    if (state.internalPreview) Note("내부 후보 미리보기예요. 앞의 5개를 기본, 다음 11개를 가챠 후보로 보여 줘요. 기본·가챠 명단은 아직 정해지지 않았어요.")
    state.notice?.let { Note(it) }
    Section("기본 ${CharacterCollection.BASIC_SLOTS}")
    SlotGrid(state.collection.basic, onEquip)
    Section("가챠 ${CharacterCollection.POOL_SLOTS} · 보유 ${state.collection.ownedPoolCount}")
    SlotGrid(state.collection.pool, onEquip)
    Note("보유한 가챠 캐릭터만 고를 수 있어요. 보유와 장착은 별개예요.")
}

@Composable
private fun CollectionSheet(state: OverlayUiState) {
    Note("첫 후보 ${CharacterCollection.POOL_SLOTS}개예요. 뽑기 비용, 확률, 중복 규칙은 정책 결정 전이라 보여 주지 않아요.")
    if (state.internalPreview) Note("내부 후보 미리보기예요. 후보 명단은 아직 정해지지 않았어요.")
    Section("보유 ${state.collection.ownedPoolCount} / ${CharacterCollection.POOL_SLOTS}")
    SlotGrid(state.collection.pool, onEquip = null)
    // 비활성인 이유를 함께 보인다. 정책이 확정되기 전에는 어떤 경로로도 보유 상태를 만들지 않는다.
    CompanionSecondaryButton("뽑기", onClick = {}, enabled = false)
    Note("뽑기는 획득 정책이 정해지면 열려요.")
}

@Composable
private fun SlotGrid(slots: List<CollectionSlot>, onEquip: ((String) -> Unit)?) {
    slots.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { SlotCell(it, Modifier.weight(1f), onEquip) }
            if (pair.size == 1) Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SlotCell(slot: CollectionSlot, modifier: Modifier, onEquip: ((String) -> Unit)?) {
    val id = slot.characterId
    val status = when {
        id == null -> "비어 있음"
        slot.equipped -> "장착 중"
        slot.owned -> "보유"
        else -> if (onEquip == null) "미보유" else "잠김"
    }
    val clickable = onEquip != null && id != null && slot.owned && !slot.equipped
    Surface(
        modifier = modifier
            .heightIn(min = CompanionTokens.MinimumTouchHeight)
            .alpha(if (id != null && (slot.owned || slot.equipped)) 1f else 0.6f)
            .clickable(enabled = clickable, role = Role.Button) { if (id != null) onEquip?.invoke(id) },
        shape = RoundedCornerShape(CompanionTokens.RowRadius),
        color = if (slot.equipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (slot.equipped) 1.5.dp else 1.dp,
            if (slot.equipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(slot.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DialogueSheet(state: OverlayUiState, onPreview: (String, Int) -> Unit) {
    if (state.dialogueRows.isEmpty()) {
        Note("이 캐릭터에 연결된 음성이 없어 대사를 들을 수 없어요.")
        return
    }
    Note("${state.activeCharacterName ?: "선택한 캐릭터"}의 고정 대사예요. 문구 편집은 없고, 듣기를 누르면 연결된 음성을 재생해요.")
    state.dialogueRows.forEach { row ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            row.variants.forEach { variant ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = CompanionTokens.MinimumTouchHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${variant.index + 1}. ${variant.text}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (row.audioReady) {
                        TextButton(onClick = { onPreview(row.eventId, variant.index) }, modifier = Modifier.heightIn(min = CompanionTokens.MinimumTouchHeight)) {
                            Text("듣기", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Text("음성 준비 중", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSheet(state: OverlayUiState, ui: OverlayControlsUi, port: OverlayPort, actions: OverlayControlsActions) {
    Note("짧은 설정만 여기서 바꿀 수 있어요.")
    SwitchRow("세션 시작 인사", "정차 중 캐릭터가 처음 나타날 때 한 번 인사해요.", state.greetingEnabled) { port.setGreetingEnabled(it) }
    // '정차 중 캐릭터 표시' 끄기는 제공하지 않는다. 끄면 캐릭터가 사라져 설정을 다시 열 수 없고,
    // 되돌릴 앱 화면도 없기 때문이다(Activity 제거). 캐릭터는 정차 중 항상 표시한다.
    Row(
        Modifier.fillMaxWidth().heightIn(min = CompanionTokens.MinimumTouchHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("캐릭터 위치 초기화", style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = actions.resetPosition, modifier = Modifier.heightIn(min = CompanionTokens.MinimumTouchHeight)) {
            Text("초기화", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(CompanionTokens.RowRadius),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = CompanionTokens.MinimumTouchHeight).padding(horizontal = 16.dp, vertical = 12.dp),
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
