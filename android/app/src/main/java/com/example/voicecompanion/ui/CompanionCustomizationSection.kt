package com.example.voicecompanion.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.domain.model.VoicePreset
import com.example.voicecompanion.presentation.event.CompanionTouch
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.ui.component.CompanionCard
import com.example.voicecompanion.ui.component.CompanionSectionHeader
import com.example.voicecompanion.ui.theme.CompanionTokens

/** 대사·기본 목소리 설정만 담당하는 카드. 캐릭터 팩 선택은 CharacterPackSection에 둔다. */
@Composable
internal fun CustomCard(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    CompanionCard {
        CompanionSectionHeader(
            title = "내 컴패니언",
            description = if (state.driving) "정차하면 다시 설정할 수 있어요." else "대사와 목소리를 내 취향에 맞게 바꿔 보세요.",
        )

        Text("대사", style = MaterialTheme.typography.titleSmall)
        if (state.dialogueRows.isNotEmpty()) {
            state.activeCharacterPack?.let { character ->
                val preview = remember(character.id) {
                    character.previewPng?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                }
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = "${character.name} 캐릭터 이미지",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    )
                }
            }
            state.dialogueRows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.label, style = MaterialTheme.typography.labelMedium)
                    row.variants.forEach { variant ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("${variant.index + 1}. ${variant.text}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (row.audioReady) {
                                TextButton(
                                    onClick = { onTouch(CompanionTouch.DialoguePreviewRequested(row.eventId, variant.index)) },
                                    enabled = !state.driving,
                                ) { Text("듣기") }
                            } else {
                                Text("음성 준비 중", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            HelperText("선택한 캐릭터의 13개 대사예요. 준비된 WAV만 듣기를 제공하고, 나머지는 문구 승인 후 음성을 생성해요.")
        } else {
            state.availableEvents.forEach { event ->
                val fixed = state.fixedLine(event)
                OutlinedTextField(
                    value = fixed ?: state.typedLines[event] ?: event.defaultLine,
                    onValueChange = { onTouch(CompanionTouch.LineChanged(event, it)) },
                    label = { Text(event.label + if (event.neutral) " · 중립 음성" else "") },
                    supportingText = if (fixed != null) ({ Text("녹음된 대사라 수정할 수 없어요") }) else null,
                    singleLine = true,
                    enabled = !state.driving && fixed == null,
                    shape = RoundedCornerShape(CompanionTokens.RowRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "대사를 비우면 기본 대사가 적용돼요. 안전 경고는 항상 중립 음성으로 읽어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("기본 목소리", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoicePreset.entries.forEach { preset ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { onTouch(CompanionTouch.PresetSelected(preset)) },
                    enabled = !state.driving,
                    label = { Text(preset.label) },
                    shape = RoundedCornerShape(100.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}
