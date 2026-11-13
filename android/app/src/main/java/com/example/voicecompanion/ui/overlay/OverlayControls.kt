package com.example.voicecompanion.ui.overlay

/** 캐릭터 주변 floating 메뉴가 여는 compact 시트 4종. */
internal enum class OverlaySheet { CHARACTERS, COLLECTION, DIALOGUE, SETTINGS }

/** 캐릭터 곁에 열려 있는 것. 메뉴와 시트는 동시에 열리지 않는다. */
internal sealed interface OverlaySurface {
    data object Closed : OverlaySurface
    data object Menu : OverlaySurface
    data class Sheet(val kind: OverlaySheet) : OverlaySurface
}

/**
 * floating 메뉴·시트의 열림 상태. 안드로이드를 모른다.
 * X와 바깥 탭은 [close]로 지금 열린 메뉴 또는 시트만 닫고 캐릭터 오버레이는 건드리지 않는다.
 * [lock](주행·상태 미확인)은 무엇이 열려 있든 즉시 닫고, [unlock]까지 열리지 않게 한다.
 */
internal class OverlayControls {
    var surface: OverlaySurface = OverlaySurface.Closed
        private set
    private var locked = false

    /** 길게 누름. 시트가 열려 있었으면 닫고 메뉴를 연다. 잠겨 있으면 열지 않는다. */
    fun openMenu(): Boolean {
        if (locked) return false
        surface = OverlaySurface.Menu
        return true
    }

    /** 메뉴 버튼. 메뉴가 열려 있을 때만 시트로 넘어가고, 그때 메뉴는 닫힌다. */
    fun openSheet(kind: OverlaySheet): Boolean {
        if (locked || surface != OverlaySurface.Menu) return false
        surface = OverlaySurface.Sheet(kind)
        return true
    }

    fun close() {
        surface = OverlaySurface.Closed
    }

    fun lock() {
        locked = true
        close()
    }

    fun unlock() {
        locked = false
    }
}

/**
 * 캐릭터 교체 순서: SWITCH_OUT → (끝난 뒤 캐릭터와 음성 팩을 함께 교체) → SWITCH_IN.
 * OUT이 끝나기 전에 [lock]이 오면(주행 시작) 교체를 보류하고 이전 캐릭터를 그대로 둔다. IN은 이미 교체한 뒤의 연출이라 잠가도 교체는 되돌리지 않는다.
 */
internal class CharacterSwitch {
    enum class Phase { IDLE, OUT, IN }

    var phase = Phase.IDLE
        private set
    private var target: String? = null
    private var locked = false

    /** 새 교체를 시작한다. 잠겨 있거나 다른 교체 중이거나 이미 그 캐릭터이면 false. */
    fun begin(targetId: String, currentId: String?): Boolean {
        if (locked || phase != Phase.IDLE || targetId == currentId) return false
        phase = Phase.OUT
        target = targetId
        return true
    }

    /** OUT 연출이 끝났다. 교체해도 되면 새 캐릭터 id를 돌려주고 IN으로 넘어간다. 중간에 잠겼거나 OUT이 아니면 null — 이전 캐릭터를 유지한다. */
    fun outFinished(): String? {
        if (phase != Phase.OUT) return null
        val id = target
        target = null
        phase = Phase.IN
        return id
    }

    fun inFinished() {
        if (phase == Phase.IN) phase = Phase.IDLE
    }

    fun lock() {
        locked = true
        phase = Phase.IDLE
        target = null
    }

    fun unlock() {
        locked = false
    }
}
