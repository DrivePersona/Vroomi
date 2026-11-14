package com.example.voicecompanion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TDS(Toss Design System, https://tossmini-docs.toss.im/tds-mobile/) 토큰의 Compose 이식.
 *
 * - 색: foundation/colors 의 grey·blue·red·green·orange 스케일 값 그대로.
 * - 글자: foundation/typography 의 Typography 1~7 (크기/줄 높이는 TDS 값, 단위는 sp).
 * - 간격: components/ListRow 의 verticalPadding(small 8 · medium 12 · large 16)과 horizontalPadding(small 20 · medium 24).
 *
 * TDS 문서에 값이 없는 것(시트 모서리 반경, 스위치 크기, 버튼 높이·반경, 다크 모드 색, 딤 색)은 TDS 값이라고 주장하지 않고
 * [TdsProjectValues]에 프로젝트 값으로 따로 둔다. 정식 인증은 주장하지 않는다.
 */
object TdsColors {
    val grey50 = Color(0xFFF9FAFB)
    val grey100 = Color(0xFFF2F4F6)
    val grey200 = Color(0xFFE5E8EB)
    val grey300 = Color(0xFFD1D6DB)
    val grey400 = Color(0xFFB0B8C1)
    val grey500 = Color(0xFF8B95A1)
    val grey600 = Color(0xFF6B7684)
    val grey700 = Color(0xFF4E5968)
    val grey800 = Color(0xFF333D4B)
    val grey900 = Color(0xFF191F28)

    val blue50 = Color(0xFFE8F3FF)
    val blue100 = Color(0xFFC9E2FF)
    val blue200 = Color(0xFF90C2FF)
    val blue300 = Color(0xFF64A8FF)
    val blue400 = Color(0xFF4593FC)
    val blue500 = Color(0xFF3182F6)
    val blue600 = Color(0xFF2272EB)
    val blue700 = Color(0xFF1B64DA)
    val blue800 = Color(0xFF1957C2)
    val blue900 = Color(0xFF194AA6)

    val red50 = Color(0xFFFFEEEE)
    val red500 = Color(0xFFF04452)
    val red600 = Color(0xFFE42939)
    val red700 = Color(0xFFD22030)

    val green50 = Color(0xFFF0FAF6)
    val green500 = Color(0xFF03B26C)
    val green700 = Color(0xFF029359)

    val orange50 = Color(0xFFFFF3E0)
    val orange500 = Color(0xFFFE9800)
    val orange800 = Color(0xFFED6700)

    /** colors.background / layeredBackground / floatedBackground */
    val background = Color(0xFFFFFFFF)
}

/** foundation/typography 의 토큰. 값은 (글자 크기, 줄 높이) 순으로 TDS 표 그대로다. */
object TdsType {
    private fun style(size: Float, line: Float, weight: FontWeight) =
        TextStyle(fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

    val t3 = style(22f, 31f, FontWeight.SemiBold) // 일반 제목
    val t4 = style(20f, 29f, FontWeight.SemiBold) // 작은 제목 — BottomSheet.Header
    val t5 = style(17f, 25.5f, FontWeight.Normal) // 일반 본문
    val t5Medium = style(17f, 25.5f, FontWeight.Medium)
    val t6 = style(15f, 22.5f, FontWeight.Normal) // 작은 본문 — BottomSheet.HeaderDescription
    val t6Medium = style(15f, 22.5f, FontWeight.Medium)
    val t7 = style(13f, 19.5f, FontWeight.Normal) // 안 읽어도 되는 보조
    val t7Medium = style(13f, 19.5f, FontWeight.Medium)
}

/** components/ListRow 의 여백 값. */
object TdsSpacing {
    val rowVerticalSmall = 8.dp
    val rowVerticalMedium = 12.dp // 기본값
    val rowVerticalLarge = 16.dp
    val rowHorizontalSmall = 20.dp
    val rowHorizontalMedium = 24.dp // 기본값
}

/**
 * TDS 문서에 값이 없어 이 프로젝트가 정한 값. 차량(Pleos) 화면과 안전 규칙을 우선한다.
 * 근거와 이유는 android/DESIGN_SYSTEM.md 의 "오버레이 시트 — TDS 기준" 절에 적는다.
 */
object TdsProjectValues {
    /** 시트 모서리 반경. TDS BottomSheet 문서에 없다. */
    val SheetRadius = 20.dp

    /** 버튼·행 모서리 반경. TDS Button 문서에 없다. */
    val ControlRadius = 14.dp

    /** 최소 터치 높이. 차량 화면 규칙(프로젝트 52dp)이 TDS 기본 행 높이보다 우선한다. */
    val MinimumTouchHeight = CompanionTokens.MinimumTouchHeight

    /** 오버레이 글자 배율. TDS 값은 폰 기준이라 160dpi 2560x1440 차 화면에서 멀리서 읽히도록 키운다. */
    const val OverlayFontScale = 1.25f

    /** TDS 다크 값이 문서에 없어, 같은 grey 스케일을 뒤집어 쓴 파생 값. */
    val DarkBackground = TdsColors.grey900
    val DarkSurface = TdsColors.grey800
    val DarkSurfaceVariant = TdsColors.grey700
}

private val TdsLightColors = lightColorScheme(
    // primary 는 TDS blue500 이 아니라 blue700 이다. 흰 글자와의 대비가 blue500 은 3.71:1 로 Pleos 가이드(4.5:1)에 못 미친다.
    primary = TdsColors.blue700,
    onPrimary = Color.White,
    primaryContainer = TdsColors.blue50,
    onPrimaryContainer = TdsColors.blue800,
    // 상태 글자·비활성 행 글자. grey600 은 grey100 위에서 4.19:1 이라 grey700(6.45:1)을 쓴다.
    secondary = TdsColors.grey700,
    onSecondary = Color.White,
    secondaryContainer = TdsColors.grey100,
    onSecondaryContainer = TdsColors.grey900,
    background = TdsColors.background,
    onBackground = TdsColors.grey900,
    surface = TdsColors.background,
    onSurface = TdsColors.grey900,
    surfaceVariant = TdsColors.grey100,
    onSurfaceVariant = TdsColors.grey600,
    outline = TdsColors.grey300,
    outlineVariant = TdsColors.grey200,
    error = TdsColors.red500,
    onError = Color.White,
    errorContainer = TdsColors.red50,
    onErrorContainer = TdsColors.red700,
)

private val TdsDarkColors = darkColorScheme(
    primary = TdsColors.blue400,
    onPrimary = TdsColors.grey900,
    primaryContainer = TdsColors.blue900,
    onPrimaryContainer = TdsColors.blue100,
    secondary = TdsColors.grey300,
    onSecondary = TdsColors.grey900,
    secondaryContainer = TdsProjectValues.DarkSurfaceVariant,
    onSecondaryContainer = TdsColors.grey50,
    background = TdsProjectValues.DarkBackground,
    onBackground = TdsColors.grey50,
    surface = TdsProjectValues.DarkSurface,
    onSurface = TdsColors.grey50,
    surfaceVariant = TdsProjectValues.DarkSurfaceVariant,
    onSurfaceVariant = TdsColors.grey400,
    outline = TdsColors.grey600,
    outlineVariant = TdsColors.grey700,
    error = TdsColors.red500,
    onError = Color.White,
    errorContainer = TdsColors.red700,
    onErrorContainer = TdsColors.red50,
)

/** Material 역할을 TDS 타이포그래피로 매핑한다. 시트 코드는 이 역할만 참조한다. */
private val TdsTypography = Typography(
    titleLarge = TdsType.t3,
    titleMedium = TdsType.t4, // BottomSheet.Header
    titleSmall = TdsType.t6Medium, // 목록 구역 제목
    bodyLarge = TdsType.t5,
    bodyMedium = TdsType.t6, // BottomSheet.HeaderDescription, ListRow 설명
    labelLarge = TdsType.t6Medium, // 버튼·메뉴 라벨
    labelMedium = TdsType.t7,
)

/** 오버레이(플로팅 메뉴·시트) 전용 TDS 테마. 앱의 다른 화면은 [CompanionTheme]을 그대로 쓴다. */
@Composable
fun TdsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) TdsDarkColors else TdsLightColors,
        typography = TdsTypography,
        content = content,
    )
}
