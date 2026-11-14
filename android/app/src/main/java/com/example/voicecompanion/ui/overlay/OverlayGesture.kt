package com.example.voicecompanion.ui.overlay

/**
 * 오버레이 캐릭터에 닿은 포인터 하나(DOWN~UP/CANCEL)를 탭·길게 누름·끌기 중 정확히 하나로 판정한다.
 * 안드로이드를 모르고 좌표와 타이머 신호만 받는다. 끌기 계산은 [OverlayDrag]가 맡는다.
 *
 * - 탭 범위(touchSlop) 안에서 떼면 탭이다.
 * - 탭 범위 안에서 [LONG_PRESS_MS]가 지나 호스트가 [longPress]를 부르면 길게 누름이다. 이후엔 움직여도 끌기가 되지 않고 떼도 탭이 아니다.
 * - 탭 범위를 한 번이라도 벗어나면 끌기다. 이후엔 길게 누름도 탭도 아니다.
 * - [lock]된 동안(주행·상태 미확인)에는 어떤 입력도 판정하지 않는다. 제스처보다 우선한다.
 */
internal class OverlayGesture(touchSlop: Int) {
    /** 손가락을 뗄 때의 판정. 끌기가 끝나면 호스트가 위치를 저장한다. */
    enum class Result { NONE, TAP, DRAG_END }

    private enum class State { IDLE, PRESSED, DRAGGING, HELD, IGNORED }

    private val drag = OverlayDrag(touchSlop)
    private var state = State.IDLE
    private var locked = false

    /** 손가락이 닿아 있고 아직 탭·끌기·길게 누름 어느 쪽으로도 확정되지 않았다. 눌림 표시를 해도 되는 때다. */
    val pressed: Boolean get() = state == State.PRESSED

    /**
     * 새 포인터 입력을 시작한다. 잠겨 있으면 false이고 이 입력은 끝까지 무시한다.
     * true면 호스트가 [LONG_PRESS_MS] 뒤에 [longPress]를 부르도록 타이머를 건다.
     */
    fun down(rawX: Float, rawY: Float, x: Int, y: Int, minX: Int, maxX: Int, minY: Int, maxY: Int): Boolean {
        if (locked) {
            state = State.IGNORED
            return false
        }
        drag.down(rawX, rawY, x, y, minX, maxX, minY, maxY)
        state = State.PRESSED
        return true
    }

    /** 끄는 중이면 창 왼쪽 위의 새 좌표를 돌려준다. 탭 범위 안이거나 끌기가 아니면 null이다. */
    fun move(rawX: Float, rawY: Float): Pair<Int, Int>? {
        if (state != State.PRESSED && state != State.DRAGGING) return null
        val position = drag.move(rawX, rawY) ?: return null
        state = State.DRAGGING
        return position
    }

    /** 길게 누름 타이머가 울렸다. 아직 탭 범위 안에서 누르고 있을 때만 true이고, 그러면 이 입력은 길게 누름으로 확정된다. */
    fun longPress(): Boolean {
        if (state != State.PRESSED) return false
        state = State.HELD
        return true
    }

    fun up(): Result {
        val result = when (state) {
            State.PRESSED -> Result.TAP
            State.DRAGGING -> Result.DRAG_END
            else -> Result.NONE
        }
        state = State.IDLE
        return result
    }

    /** ACTION_CANCEL이나 둘째 손가락처럼 이 입력을 더 판정할 수 없을 때. 탭은 만들지 않고, 끌던 중이면 위치만 남기게 한다. */
    fun cancel(): Result {
        val result = if (state == State.DRAGGING) Result.DRAG_END else Result.NONE
        state = State.IDLE
        return result
    }

    /** 주행·상태 미확인. 진행 중인 입력을 취소하고 [unlock]까지 새 입력도 무시한다. */
    fun lock(): Result {
        locked = true
        return cancel()
    }

    fun unlock() {
        locked = false
    }

    companion object {
        /** 제자리 길게 누름의 초기 기준(이슈 #37). */
        const val LONG_PRESS_MS = 500L
    }
}
