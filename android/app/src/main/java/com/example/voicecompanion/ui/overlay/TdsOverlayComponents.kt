package com.example.voicecompanion.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.voicecompanion.ui.theme.TdsColors
import com.example.voicecompanion.ui.theme.TdsProjectValues
import com.example.voicecompanion.ui.theme.TdsSpacing

/*
 * TDS(https://tossmini-docs.toss.im/tds-mobile/) 컴포넌트 구조를 오버레이 시트용으로 옮긴 것이다.
 * 이름은 TDS 컴포넌트를 따르고, 문서에 값이 없는 부분은 TdsProjectValues 를 쓴다.
 */

/** BottomSheet.Header(t4 제목) + BottomSheet.HeaderDescription(t6 설명). 닫기 버튼은 시트 닫기 규칙(X)을 위해 둔다. */
@Composable
internal fun TdsSheetHeader(title: String, description: String?, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = TdsSpacing.rowHorizontalSmall, end = 8.dp, top = 8.dp, bottom = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = TdsProjectValues.MinimumTouchHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Surface(
                modifier = Modifier
                    .size(TdsProjectValues.MinimumTouchHeight)
                    .semantics { contentDescription = "닫기" }
                    .clickable(role = Role.Button, onClick = onClose),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        if (description != null) {
            Text(
                description,
                modifier = Modifier.padding(end = TdsSpacing.rowHorizontalSmall - 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 목록 위의 구역 제목. */
@Composable
internal fun TdsSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = TdsSpacing.rowHorizontalSmall, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 목록 아래의 안내 문구(t7). */
@Composable
internal fun TdsFootnote(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = TdsSpacing.rowHorizontalSmall, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * ListRow. left(생략) · contents(제목+설명) · right(상태·스위치·버튼) 구조, 여백은 TDS 값(세로 medium 12, 가로 small 20)이다.
 * 눌러서 쓰는 행은 [onClick]을 준다. 비활성 행은 TDS disabledStyle type2처럼 회색 배경으로 이유를 함께 보인다.
 */
@Composable
internal fun TdsListRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    disabled: Boolean = false,
    emphasized: Boolean = false,
    right: @Composable RowScope.() -> Unit = {},
) {
    val base = modifier
        .fillMaxWidth()
        .heightIn(min = TdsProjectValues.MinimumTouchHeight)
    val clickable = if (onClick != null && !disabled) base.clickable(role = role, onClick = onClick) else base
    Surface(
        color = when {
            disabled -> MaterialTheme.colorScheme.surfaceVariant
            emphasized -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(TdsProjectValues.ControlRadius),
    ) {
        Row(
            clickable.padding(horizontal = TdsSpacing.rowHorizontalSmall, vertical = TdsSpacing.rowVerticalMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (disabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            right()
        }
    }
}

/** 들여쓴 구분선(ListRow border = indented). */
@Composable
internal fun TdsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = TdsSpacing.rowHorizontalSmall),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** 행 오른쪽에 쓰는 상태 글자. 색만으로 전달하지 않도록 항상 글자를 쓴다. */
@Composable
internal fun TdsStatusText(text: String, highlighted: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        // 장착 중 글자는 blue500(blue50 위 3.31:1)이 아니라 blue800(5.87:1)으로 4.5:1 이상을 지킨다.
        color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary,
    )
}

/** Switch. 켜짐은 TDS blue500, 꺼짐 트랙은 grey300(크기는 TDS 문서에 없어 Material 기본). 행 전체가 눌림 대상이다. */
@Composable
internal fun TdsSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = TdsColors.blue500,
            checkedBorderColor = TdsColors.blue500,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = TdsColors.grey300,
            uncheckedBorderColor = TdsColors.grey300,
        ),
    )
}

/** Button. fill = 주요 행동(blue500), weak = 보조 행동(연한 blue). 높이는 차량 최소 터치 높이를 따른다. */
@Composable
internal fun TdsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    enabled: Boolean = true,
) {
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        fill -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.secondary
        fill -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier
            .heightIn(min = TdsProjectValues.MinimumTouchHeight)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = RoundedCornerShape(TdsProjectValues.ControlRadius),
        color = container,
        contentColor = content,
    ) {
        Box(Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
