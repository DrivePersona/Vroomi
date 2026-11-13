package com.example.voicecompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.presentation.event.CompanionTouch
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.ui.component.CompanionCard
import com.example.voicecompanion.ui.component.CompanionSectionHeader
import com.example.voicecompanion.ui.theme.CompanionTokens

/** 반응 기록과 기록 지우기만 담당하는 카드. */
@Composable
internal fun LogCard(state: CompanionUiState, onTouch: (CompanionTouch) -> Unit) {
    CompanionCard {
        CompanionSectionHeader("반응 기록", "요청과 실제 재생 시작을 구분해 보여드려요.")
        if (state.logs.isEmpty()) {
            HelperText("아직 반응이 없어요. 시뮬레이터에서 차량 이벤트를 눌러 보세요.")
        } else {
            state.logs.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(CompanionTokens.RowRadius),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.text + item.note, style = MaterialTheme.typography.bodyMedium)
                        Text(item.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton({ onTouch(CompanionTouch.LogsCleared) }) { Text("기록 지우기") }
        }
    }
}
