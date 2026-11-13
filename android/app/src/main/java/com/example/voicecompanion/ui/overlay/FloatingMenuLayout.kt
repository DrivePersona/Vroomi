package com.example.voicecompanion.ui.overlay

import kotlin.math.max
import kotlin.math.min

/** 화면 좌표(px)의 사각형. android.graphics.Rect 대신 써서 JVM 테스트가 된다. */
internal data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun intersects(other: PxRect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    fun contains(other: PxRect): Boolean =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

    fun offset(dx: Int, dy: Int) = PxRect(left + dx, top + dy, right + dx, bottom + dy)
}

internal data class PxSize(val width: Int, val height: Int)

internal enum class MenuSlot { CHARACTERS, COLLECTION, DIALOGUE, SETTINGS, CLOSE }

/**
 * 캐릭터 곁에 놓는 floating 메뉴와 compact 시트의 위치 계산. 안드로이드를 모른다.
 *
 * [anchor]는 캐릭터가 차지하는 영역이다. 캐릭터 팩 SDK가 GLB 머리 anchor를 주지 않는 동안(0.3.3~0.3.8에 없다)
 * 오버레이 창 경계를 쓴다(이슈 #37의 'model bounds fallback'). 머리 anchor가 생기면 이 Rect만 좁히면 된다.
 * 결과는 항상 [safe] 안이다. 캐릭터는 가리지 않는다 — 양옆 어느 쪽에도 자리가 없을 때만 어쩔 수 없이 겹친다.
 */
internal object FloatingMenuLayout {
    /**
     * 양옆에 자리가 있으면 와이어프레임처럼 링으로 둔다(왼쪽 캐릭터 바꾸기·대사 듣기, 오른쪽 가챠·X·설정).
     * 한쪽에만 있으면 그쪽에 한 줄로 쌓고, 양쪽 다 없으면 여유가 많은 쪽에 쌓은 뒤 [safe] 안으로 눌러 담는다.
     */
    fun menu(anchor: PxRect, safe: PxRect, pill: PxSize, closeSize: Int, gap: Int): Map<MenuSlot, PxRect> {
        val leftRoom = anchor.left - gap - safe.left
        val rightRoom = safe.right - anchor.right - gap
        val ring = leftRoom >= pill.width && rightRoom >= pill.width
        val items = if (ring) ring(anchor, pill, closeSize, gap) else column(anchor, pill, closeSize, gap, onRight = rightRoom >= leftRoom)

        // 블록 전체를 세로로 옮겨 안에 넣는다(버튼마다 따로 누르면 서로 겹친다). 가로는 마지막에 버튼별로 맞춘다.
        val blockTop = items.minOf { it.second.top }
        val blockBottom = items.maxOf { it.second.bottom }
        val top = blockTop.coerceIn(safe.top, max(safe.top, safe.bottom - (blockBottom - blockTop)))
        return items.associate { (slot, r) ->
            val moved = r.offset(0, top - blockTop)
            val left = moved.left.coerceIn(safe.left, max(safe.left, safe.right - moved.width))
            slot to moved.offset(left - moved.left, 0)
        }
    }

    /** 시트는 캐릭터 오른쪽(자리가 없으면 왼쪽)에 캐릭터 위쪽 높이부터 놓는다. */
    fun sheet(anchor: PxRect, safe: PxRect, size: PxSize, gap: Int): PxRect {
        val width = min(size.width, safe.width)
        val height = min(size.height, safe.height)
        val rightX = anchor.right + gap
        val leftX = anchor.left - gap - width
        val x = when {
            rightX + width <= safe.right -> rightX
            leftX >= safe.left -> leftX
            else -> (if (safe.right - anchor.right >= anchor.left - safe.left) rightX else leftX)
                .coerceIn(safe.left, max(safe.left, safe.right - width))
        }
        val y = anchor.top.coerceIn(safe.top, max(safe.top, safe.bottom - height))
        return PxRect(x, y, x + width, y + height)
    }

    /** 첫 인사 때 한 번 보이는 힌트 알약 자리. 캐릭터 왼쪽(자리가 없으면 오른쪽)에서 위로부터 30% 높이에 가운데를 맞춘다. */
    fun hint(anchor: PxRect, safe: PxRect, size: PxSize, gap: Int): PxRect {
        val leftX = anchor.left - gap - size.width
        val x = if (leftX >= safe.left) leftX else (anchor.right + gap).coerceIn(safe.left, max(safe.left, safe.right - size.width))
        val centerY = anchor.top + anchor.height * 30 / 100
        val y = (centerY - size.height / 2).coerceIn(safe.top, max(safe.top, safe.bottom - size.height))
        return PxRect(x, y, x + size.width, y + size.height)
    }

    private fun ring(anchor: PxRect, pill: PxSize, closeSize: Int, gap: Int): List<Pair<MenuSlot, PxRect>> {
        val leftX = anchor.left - gap - pill.width
        val rightX = anchor.right + gap
        val top = anchor.top + anchor.height * 4 / 100
        val middle = top + pill.height + gap
        val bottom = middle + closeSize + gap
        return listOf(
            MenuSlot.CHARACTERS to rect(leftX, top, pill.width, pill.height),
            MenuSlot.COLLECTION to rect(rightX, top, pill.width, pill.height),
            MenuSlot.CLOSE to rect(rightX, middle, closeSize, closeSize),
            MenuSlot.DIALOGUE to rect(leftX, bottom, pill.width, pill.height),
            MenuSlot.SETTINGS to rect(rightX, bottom, pill.width, pill.height),
        )
    }

    private fun column(anchor: PxRect, pill: PxSize, closeSize: Int, gap: Int, onRight: Boolean): List<Pair<MenuSlot, PxRect>> {
        val x = if (onRight) anchor.right + gap else anchor.left - gap - pill.width
        var y = anchor.top
        fun next(slot: MenuSlot, w: Int, h: Int) = (slot to rect(x, y, w, h)).also { y += h + gap }
        return listOf(
            next(MenuSlot.CHARACTERS, pill.width, pill.height),
            next(MenuSlot.COLLECTION, pill.width, pill.height),
            next(MenuSlot.DIALOGUE, pill.width, pill.height),
            next(MenuSlot.SETTINGS, pill.width, pill.height),
            next(MenuSlot.CLOSE, closeSize, closeSize),
        )
    }

    private fun rect(x: Int, y: Int, w: Int, h: Int) = PxRect(x, y, x + w, y + h)
}
