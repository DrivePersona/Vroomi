package com.example.voicecompanion.ui.overlay

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 오버레이 입력·배치의 순수 정책이다. 안드로이드를 모르고 좌표만 다룬다.
 *
 * 정하는 것:
 * 1. 캐릭터와 메뉴·시트가 있을 수 있는 영역([OverlayArea]) — 왼쪽 계기판(driving view)을 제외한다.
 * 2. 길게 누름 안내([MenuHintPolicy]) — 메뉴를 한 번 열 때까지 몇 번 다시 보여 준다.
 * 3. 누르는 동안의 피드백([PressFeedback]).
 */
internal object OverlayArea {
    /**
     * 왼쪽 driving view(계기판·텔테일·안전 벨트 경고 등)가 차지하는 폭 비율.
     * Pleos Connect v2.1.0 AVD(Connect-S 2560x1440)에서 `DrivingViewActivity` 창이 [0,0][853,1300]이고 앱 영역(지도)이
     * [853,90][2535,1300]이라 853/2560 = 1/3로 잡았다. Pleos 공개 문서(Window Policy)에는 이 영역의 크기도, 오버레이를 둘 수 있는 곳도 없다.
     * 화면 종류(Connect-W 등)마다 다를 수 있어, 확인되기 전까지는 닫힌 쪽(더 좁은 영역)으로 둔다.
     */
    const val DRIVING_VIEW_WIDTH_FRACTION = 1f / 3f

    /**
     * 시스템 바를 뺀 [usable]에서 왼쪽 driving view 폭만큼을 더 뺀 영역. [screenWidth]는 화면 전체 폭(px)이다.
     * 영역이 너무 좁아지면(작은 화면) 제외하지 않고 [usable]을 그대로 돌려줘 캐릭터가 갈 곳이 없어지는 일을 막는다.
     */
    fun exceptDrivingView(usable: PxRect, screenWidth: Int, minWidth: Int): PxRect {
        val left = max(usable.left, (screenWidth * DRIVING_VIEW_WIDTH_FRACTION).roundToInt())
        return if (usable.right - left >= minWidth) usable.copy(left = left) else usable
    }
}

internal object MenuHintPolicy {
    /** 메뉴를 한 번도 열지 않은 사용자에게 안내를 다시 보여 주는 최대 횟수. */
    const val MAX_SHOWS = 5

    /** 길게 누름이 메뉴의 유일한 입구이므로, 메뉴를 열어 본 적이 없을 때만 최대 [MAX_SHOWS]번까지 안내한다. */
    fun shouldShow(shownCount: Int, menuEverOpened: Boolean): Boolean = !menuEverOpened && shownCount < MAX_SHOWS
}

/** 누르는 동안 캐릭터를 살짝 어둡게 해 눌림을 알려 주는 값. 짧게 탭하면 보이지 않도록 늦게 시작한다. */
internal object PressFeedback {
    const val DELAY_MS = 100L
    const val PRESSED_ALPHA = 0.8f

    /** 눌림 표시를 해도 되는지. 교체 연출(페이드)이 창 투명도를 쓰는 동안에는 건드리지 않는다. */
    fun canShow(pressed: Boolean, fading: Boolean, switching: Boolean): Boolean = pressed && !fading && !switching

    fun alphaFor(pressedShown: Boolean): Float = if (pressedShown) PRESSED_ALPHA else 1f
}
