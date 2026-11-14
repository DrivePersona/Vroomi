package com.example.voicecompanion.ui.overlay

import com.example.voicecompanion.characterpack.model.CharacterPack
import com.example.voicecompanion.characterpack.model.CharacterOverlaySpec
import com.example.voicecompanion.characterpack.ui.CharacterSurfaceView
import com.example.voicecompanion.characterpack.ui.SpriteCharacterView


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * WindowManager 위에 정차 중 캐릭터만 올리는 Android 경계다. 캐릭터는 끌어서 옮길 수 있고, 놓은 위치는 prefs에 남는다.
 *
 * 캐릭터 자체가 입구다(이슈 #37). 한 번의 터치는 입력 규칙([OverlayGesture])에 따라 탭(TOUCH)·길게 누름(floating 메뉴)·끌기(이동) 중 하나로만 끝난다.
 * 메뉴와 시트는 캐릭터 창을 건드리지 않고 그 위에 한 장 더 올리는 투명 창(Compose)에 그린다 — Filament 창을 다시 만들면 검은 사각형이 되기 때문이다.
 * 주행·상태 미확인으로 [hide]되면 캐릭터·메뉴·시트·진행 중인 교체와 입력이 한꺼번에 끊긴다.
 */
internal class CharacterOverlayHost(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val port: OverlayPort,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private val params = layoutParams(sizeFor(CharacterOverlaySpec.DEFAULT))
    private var root: FrameLayout? = null
    private var pressedShown = false
    private var characterView: View? = null
    private var previewView: ImageView? = null
    private var previewPackId: String? = null
    private var overlaySize: OverlaySize? = null
    private var attached = false

    private val main = Handler(Looper.getMainLooper())
    // 차 안 터치는 손떨림이 커서, 탭이 끌기로 오인되지 않게 기본 slop의 2배로 둔다.
    private val gesture = OverlayGesture(ViewConfiguration.get(context).scaledTouchSlop * 2)
    private val controls = OverlayControls()
    private val characterSwitch = CharacterSwitch()
    private val controlsUi = OverlayControlsUi()
    private val controlsActions = OverlayControlsActions(
        openSheet = ::openSheet,
        close = ::closeControls,
        equip = ::equip,
        resetPosition = ::resetPosition,
        // '그래도 끄기'를 누르면 Runtime이 lock()으로 이 창을 동기로 떼는데, 그러면 자기 클릭을 처리하는 도중에 창이 사라진다.
        // closeControls와 같은 이유로 클릭 처리가 끝난 다음 루프로 미룬다. 주행 잠금(lock)은 이 길을 거치지 않으니 그대로 즉시다.
        setParkedAvatarVisible = { enabled -> main.post { port.setParkedAvatarVisible(enabled) } },
    )
    private val controlsWindow = ComposeOverlayWindow(context, windowManager)
    private val hintWindow = ComposeOverlayWindow(context, windowManager)
    private var fade: ValueAnimator? = null
    private val longPress = Runnable {
        if (gesture.longPress()) {
            clearPressFeedback()
            characterView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            openMenu()
        }
    }
    private val pressFeedback = Runnable {
        if (PressFeedback.canShow(gesture.pressed, fading = fade != null, switching = characterSwitch.phase != CharacterSwitch.Phase.IDLE)) {
            pressedShown = true
            setWindowAlpha(PressFeedback.alphaFor(true))
        }
    }
    private val hideHint = Runnable { hintWindow.remove() }

    val permissionGranted: Boolean
        get() = Settings.canDrawOverlays(context)

    fun show(pack: CharacterPack, animation: String): OverlayShowResult {
        if (!permissionGranted) return OverlayShowResult.PERMISSION_REQUIRED
        unlock() // 정차 상태·권한이 확인된 호출이다. 입력을 다시 받는다.
        val requestedSize = sizeFor(pack.overlay)
        // GLB SurfaceView와 2D View는 창 수명 모델이 다르다. 다른 표현으로 바뀌면 창을 새로 만들어
        // 이전 Filament Surface와 스프라이트 재생 콜백을 함께 정리한다.
        if (root != null && (!supports(pack) || overlaySize != requestedSize)) detach()

        val overlay = root ?: FrameLayout(context).also { frame ->
            val view = createCharacterView(pack) ?: return OverlayShowResult.REJECTED
            view.contentDescription = "캐릭터: 짧게 누르면 반응하고 길게 누르면 메뉴가 열려요"
            view.setOnTouchListener(gestureListener())
            // 손가락 터치는 위 리스너가 직접 판정한다. TalkBack 두 번 탭·스위치 접근은 터치 이벤트 없이 클릭/길게 클릭 동작으로 들어오므로,
            // 탭 판정(finish의 TAP)도 이 클릭 리스너로 보내 두 길이 같은 곳(onCharacterClick)에서 만나게 한다.
            view.setOnClickListener { onCharacterClick() }
            view.setOnLongClickListener { openMenu(); true }
            view.isSoundEffectsEnabled = false // performClick이 새 클릭음을 내지 않게 한다(지금까지 탭에는 소리가 없었다)
            frame.addView(view, FrameLayout.LayoutParams(requestedSize.width, requestedSize.height))
            characterView = view
            overlaySize = requestedSize
            root = frame
        }
        if (!attached) {
            // 저장한 위치가 없으면 영역 가운데. 화면 크기가 달라졌어도 창이 영역 밖에서 시작하지 않게 안쪽으로 맞춘다.
            // 저장한 위치는 정차가 확인돼 이 함수가 불린 때에만 되살린다.
            params.width = requestedSize.width
            params.height = requestedSize.height
            placeFromPrefs(requestedSize)
            try {
                windowManager.addView(overlay, params)
                attached = true
            } catch (_: SecurityException) {
                return OverlayShowResult.PERMISSION_REQUIRED
            } catch (_: WindowManager.BadTokenException) {
                return OverlayShowResult.REJECTED
            }
            showMenuHintIfNeeded()
        }
        syncPreview(pack, requestedSize)
        present(pack, animation)
        return OverlayShowResult.SHOWING
    }

    /**
     * 3D가 그려지기 전이나 못 그리는 기기에서 보이는 2D 프리뷰. 같은 창에서 캐릭터가 바뀌면(오버레이 안에서 교체) 이전 캐릭터의 프리뷰가
     * 남지 않게 바꾼다. 프리뷰는 캐릭터 뷰 아래(맨 앞 자식)에 둔다.
     *
     * 프리뷰는 창이 아니라 기준 틀(300×400dp)에 맞춰 그리고, 창은 그 틀에서 캐릭터가 보이는 부분만 잘라 보여 준다. 3D가 같은 기준 틀에서
     * 잘라 그리는 것과 같은 방식이라 창 크기가 캐릭터마다 달라도 두 그림이 어긋나지 않는다. 기본 크기(틀과 같음)에서는 이전과 같다.
     */
    private fun syncPreview(pack: CharacterPack, size: OverlaySize) {
        val frame = root ?: return
        if (previewPackId == pack.id) return
        previewPackId = pack.id
        previewView?.let(frame::removeView)
        previewView = pack.previewPng?.let { png ->
            BitmapFactory.decodeByteArray(png, 0, png.size)?.let { bitmap ->
                val stageWidth = dp(STAGE_WIDTH_DP)
                val stageHeight = dp(STAGE_HEIGHT_DP)
                val inset = minOf(stageWidth, stageHeight) / 20
                ImageView(context).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(inset, inset, inset, inset)
                    contentDescription = "${pack.name} 캐릭터 프리뷰"
                }.also {
                    val params = FrameLayout.LayoutParams(stageWidth, stageHeight)
                    params.leftMargin = (size.width - stageWidth) / 2 - size.offsetX
                    params.topMargin = (size.height - stageHeight) / 2 - size.offsetY
                    frame.addView(it, 0, params)
                }
            }
        }
    }

    private fun createCharacterView(pack: CharacterPack): View? = when {
        pack.model != null -> CharacterSurfaceView(context, transparentBackground = true)
        pack.sprite != null -> SpriteCharacterView(context)
        else -> null
    }

    private fun supports(pack: CharacterPack): Boolean = when (characterView) {
        is CharacterSurfaceView -> pack.model != null
        is SpriteCharacterView -> pack.sprite != null
        else -> false
    }

    private fun present(pack: CharacterPack, animation: String) {
        when (val view = characterView) {
            is CharacterSurfaceView -> view.present(pack, animation)
            is SpriteCharacterView -> view.present(pack, animation)
        }
    }

    /** 주행·상태 미확인·설정 화면 등 표시 조건이 깨졌다. 메뉴·시트·교체·입력을 먼저 끊고 캐릭터 창을 뗀다. */
    fun hide() {
        lock()
        detach()
    }

    /** 캐릭터 창만 정리한다. 다음 [show]에서 새로 만든다. */
    private fun detach() {
        dismissHint()
        removeControlsWindow()
        if (!attached) return
        try {
            root?.let(windowManager::removeViewImmediate)
        } catch (_: IllegalArgumentException) {
            // 시스템이 이미 창을 정리한 경우다. 다음 show에서 새로 만든다.
        } finally {
            attached = false
            // ModelViewer는 창에서 떨어질 때 엔진까지 해제한다. 같은 뷰를 다시 붙이면 캐릭터 없는 검은 사각형이 되니(기기에서 확인) 버리고 show마다 새로 만든다.
            root = null
            characterView = null
            previewView = null
            previewPackId = null
            overlaySize = null
        }
    }

    /** 주행이 제스처보다 우선이다. 진행 중인 입력·타이머·페이드·메뉴·시트를 즉시 끊고, 교체 중이었으면 보류해 이전 캐릭터를 유지한다. */
    private fun lock() {
        if (controls.surface != OverlaySurface.Closed || characterSwitch.phase != CharacterSwitch.Phase.IDLE) {
            Log.d(TAG, "오버레이 숨김(주행·상태 미확인·표시 끔 등): 메뉴/시트=${controls.surface} 교체=${characterSwitch.phase} 즉시 끊음")
        }
        main.removeCallbacks(longPress)
        main.removeCallbacks(pressFeedback)
        pressedShown = false
        fade?.let {
            it.removeAllListeners()
            it.cancel()
        }
        fade = null
        params.alpha = 1f
        if (gesture.lock() == OverlayGesture.Result.DRAG_END) savePosition()
        controls.lock()
        characterSwitch.lock()
        dismissHint()
        removeControlsWindow()
    }

    private fun unlock() {
        gesture.unlock()
        controls.unlock()
        characterSwitch.unlock()
    }

    /** 시스템 설정 화면을 열었으면 true. Pleos 에뮬레이터 이미지처럼 그 화면이 없는 기기에선 false다(그대로 열면 앱이 죽는다). */
    fun openPermissionSettings(): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /**
     * 오버레이가 머무를 수 있는 화면 영역(시스템 바 제외, 화면 좌표).
     * 앱 컨텍스트의 displayMetrics·currentWindowMetrics는 Pleos에서 마지막 액티비티 창 크기(앱 영역 1682x1210)를 돌려줘서(기기에서 확인) 쓰지 않고,
     * 화면 전체는 maximumWindowMetrics에서 읽는다.
     * ponytail: 시스템 바 안쪽이면 어디든 허용한다. Pleos가 가려도 되는 영역(harness/pleos-overlay-questions.md 11번)이 정해지면 그 영역으로 좁힌다.
     * 메뉴와 시트도 이 영역 안에 둔다. 또 TOP|START 좌표가 화면 좌표와 같다고 본다(부모 프레임 원점이 0,0 — Pleos에서 확인). 컷아웃 등으로 어긋나면 getLocationOnScreen으로 보정한다.
     */
    @Suppress("DEPRECATION") // API 30 미만 폴백
    private fun usableArea(): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.maximumWindowMetrics
            val bars = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val b = metrics.bounds
            return Rect(b.left + bars.left, b.top + bars.top, b.right - bars.right, b.bottom - bars.bottom)
        }
        val size = Point().also { windowManager.defaultDisplay.getRealSize(it) }
        return Rect(0, 0, size.x, size.y)
    }

    /**
     * 캐릭터·메뉴·시트가 있어도 되는 영역. [usableArea]에서 왼쪽 계기판(driving view)을 뺀다(계기판의 안전 벨트 경고 같은 차량 표시를 가리지 않게).
     * 근거와 한계는 [OverlayArea.DRIVING_VIEW_WIDTH_FRACTION]에 있다.
     */
    private fun allowedArea(): Rect {
        val u = usableArea()
        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) windowManager.maximumWindowMetrics.bounds.width() else u.right
        val a = OverlayArea.exceptDrivingView(PxRect(u.left, u.top, u.right, u.bottom), screenWidth, dp(MIN_AREA_WIDTH_DP))
        return Rect(a.left, a.top, a.right, a.bottom)
    }

    /** 터치 한 번을 [OverlayGesture]에 넘기고, 판정이 나면 탭은 TOUCH로, 길게 누름은 메뉴로, 끌기는 창 이동으로 옮긴다. */
    @SuppressLint("ClickableViewAccessibility") // 탭은 finish()가 performClick()으로 보낸다(접근성 클릭과 같은 길)
    private fun gestureListener(): View.OnTouchListener = View.OnTouchListener { _, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> allowedArea().let { a ->
                dismissHint()
                clearPressFeedback()
                Log.d(TAG, "오버레이 터치 시작: 손가락=(${e.rawX.toInt()}, ${e.rawY.toInt()}) 창=(${params.x}, ${params.y})")
                val size = checkNotNull(overlaySize)
                if (gesture.down(e.rawX, e.rawY, params.x, params.y, a.left, a.right - size.width, a.top, a.bottom - size.height)) {
                    main.postDelayed(longPress, OverlayGesture.LONG_PRESS_MS)
                    main.postDelayed(pressFeedback, PressFeedback.DELAY_MS)
                }
            }
            MotionEvent.ACTION_MOVE -> gesture.move(e.rawX, e.rawY)?.let { (x, y) ->
                main.removeCallbacks(longPress)
                clearPressFeedback()
                params.x = x
                params.y = y
                applyPosition()
            }
            MotionEvent.ACTION_UP -> {
                main.removeCallbacks(longPress)
                clearPressFeedback()
                finish(gesture.up())
            }
            // 둘째 손가락은 첫 손가락의 좌표를 흔들어 놓으니 이 입력은 더 판정하지 않는다.
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                main.removeCallbacks(longPress)
                clearPressFeedback()
                finish(gesture.cancel())
            }
        }
        true
    }

    private fun finish(result: OverlayGesture.Result) {
        when (result) {
            OverlayGesture.Result.TAP -> {
                Log.d(TAG, "오버레이 제스처: 탭 → TOUCH")
                characterView?.performClick() // 클릭 리스너(onCharacterClick)가 TOUCH를 보낸다
            }
            OverlayGesture.Result.DRAG_END -> {
                Log.d(TAG, "오버레이 제스처: 끌기 끝 → 위치 저장 (${params.x}, ${params.y})")
                savePosition()
            }
            OverlayGesture.Result.NONE -> Unit
        }
    }

    /** 탭 판정과 접근성 클릭(TalkBack 두 번 탭·스위치 접근)이 만나는 곳. 교체 중에는 두 캐릭터가 겹쳐 반응하지 않게 받지 않는다. */
    private fun onCharacterClick() {
        // 메뉴·시트가 열려 있으면 손가락 터치는 바깥 탭으로 닫기만 하고 캐릭터에 닿지 않는다. 접근성 클릭도 같게 TOUCH를 보내지 않는다.
        if (controls.surface != OverlaySurface.Closed) return
        if (characterSwitch.phase == CharacterSwitch.Phase.IDLE) port.touchCharacter()
    }

    private fun savePosition() {
        prefs.edit().putInt(PREF_X, params.x).putInt(PREF_Y, params.y).apply()
    }

    private fun placeFromPrefs(size: OverlaySize) {
        val area = allowedArea()
        params.x = prefs.getInt(PREF_X, (area.left + area.right - size.width) / 2)
            .coerceIn(area.left, maxOf(area.left, area.right - size.width))
        params.y = prefs.getInt(PREF_Y, (area.top + area.bottom - size.height) / 2)
            .coerceIn(area.top, maxOf(area.top, area.bottom - size.height))
    }

    private fun resetPosition() {
        prefs.edit().remove(PREF_X).remove(PREF_Y).apply()
        val size = overlaySize ?: return
        placeFromPrefs(size)
        applyPosition()
        relayoutControls()
    }

    // ---- floating 메뉴와 시트 ----

    private fun openMenu() {
        if (!attached || characterSwitch.phase != CharacterSwitch.Phase.IDLE || !controls.openMenu()) return
        Log.d(TAG, "오버레이 제스처: 길게 누름 → 메뉴 (TOUCH 없음)")
        prefs.edit().putBoolean(PREF_MENU_OPENED, true).apply() // 한 번 열어 봤으니 길게 누름 안내는 더 보이지 않는다
        dismissHint()
        showControls()
    }

    private fun openSheet(kind: OverlaySheet) {
        if (!controls.openSheet(kind)) return
        Log.d(TAG, "오버레이 시트 열림: $kind")
        controlsUi.confirmHide = false
        showControls()
    }

    private fun showControls() {
        if (!ensureControlsWindow()) {
            controls.close()
            return
        }
        relayoutControls()
        controlsUi.surface = controls.surface
    }

    /** X와 바깥 탭. 지금 열린 메뉴나 시트만 닫고 캐릭터 창은 그대로 둔다. */
    private fun closeControls() {
        if (controls.surface != OverlaySurface.Closed) Log.d(TAG, "오버레이 닫기(X·바깥 탭): ${controls.surface} 닫고 캐릭터는 유지")
        controls.close()
        controlsUi.surface = OverlaySurface.Closed
        // 이 창이 터치를 처리하는 도중에 창을 떼지 않는다. 다음 루프에서 뗀다(그 사이 다시 열렸으면 그대로 둔다).
        main.post { if (controls.surface == OverlaySurface.Closed) removeControlsWindow() }
    }

    private fun ensureControlsWindow(): Boolean {
        if (controlsWindow.isShown) return true
        // 안전 영역 전체를 덮는 투명 창이라 바깥 탭을 받아 닫을 수 있다. 시스템 바 영역은 덮지 않는다.
        val area = usableArea()
        val lp = WindowManager.LayoutParams(
            area.width(),
            area.height(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                // WindowManager로 직접 붙이는 창은 하드웨어 가속을 직접 켜야 Compose가 제대로 그린다.
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = area.left
            y = area.top
            title = "VoiceCompanionControlsOverlay"
        }
        return controlsWindow.show(lp) { OverlayControlsContent(controlsUi, port, controlsActions) }
    }

    private fun removeControlsWindow() {
        controlsUi.surface = OverlaySurface.Closed
        controlsUi.menu = emptyMap()
        controlsUi.sheet = null
        controlsUi.confirmHide = false
        controlsWindow.remove()
    }

    // ---- 첫 인사 때 한 번 보이는 '길게 누르면 메뉴' 힌트 ----

    /**
     * 길게 누름이 메뉴의 유일한 입구라서, 메뉴를 한 번 열어 볼 때까지 캐릭터가 나타날 때마다 안내한다([MenuHintPolicy.MAX_SHOWS]번까지).
     * 문구는 목업 기준이고 확정 전이다.
     */
    private fun showMenuHintIfNeeded() {
        val shown = prefs.getInt(PREF_MENU_HINT_COUNT, 0)
        if (!MenuHintPolicy.shouldShow(shown, prefs.getBoolean(PREF_MENU_OPENED, false))) return
        val size = overlaySize ?: return
        val area = allowedArea()
        val anchor = PxRect(params.x, params.y, params.x + size.width, params.y + size.height)
        val safe = PxRect(area.left, area.top, area.right, area.bottom)
        val rect = FloatingMenuLayout.hint(anchor, safe, PxSize(dp(HINT_WIDTH_DP), dp(HINT_HEIGHT_DP)), dp(MENU_GAP_DP))
        val lp = WindowManager.LayoutParams(
            rect.width,
            rect.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rect.left
            y = rect.top
            title = "VoiceCompanionMenuHint"
        }
        if (!hintWindow.show(lp) { MenuHint() }) return
        prefs.edit().putInt(PREF_MENU_HINT_COUNT, shown + 1).apply()
        main.postDelayed(hideHint, HINT_VISIBLE_MS)
    }

    private fun dismissHint() {
        main.removeCallbacks(hideHint)
        hintWindow.remove()
    }

    /** 캐릭터 창 경계를 기준으로 메뉴 버튼과 시트 자리를 다시 계산한다. 컨트롤 창은 안전 영역 왼쪽 위가 원점이다. */
    private fun relayoutControls() {
        val size = overlaySize ?: return
        val area = usableArea() // 컨트롤 창은 시스템 바 안쪽 전체를 덮는다(바깥 탭을 받으려고). 원점은 이 영역 왼쪽 위다.
        val allowed = allowedArea() // 메뉴와 시트는 계기판을 뺀 영역 안에만 둔다.
        val anchor = PxRect(params.x, params.y, params.x + size.width, params.y + size.height).offset(-area.left, -area.top)
        val safe = PxRect(allowed.left - area.left, allowed.top - area.top, allowed.right - area.left, allowed.bottom - area.top)
        controlsUi.menu = FloatingMenuLayout.menu(anchor, safe, PxSize(dp(MENU_PILL_WIDTH_DP), dp(MENU_PILL_HEIGHT_DP)), dp(MENU_CLOSE_DP), dp(MENU_GAP_DP))
        // 시트는 담을 내용만큼만 쓴다(안에서 스크롤). 설정처럼 짧은 시트가 빈 칸으로 길어지지 않게 종류별로 높이를 나눈다.
        val sheetHeightDp = when ((controls.surface as? OverlaySurface.Sheet)?.kind) {
            OverlaySheet.SETTINGS -> SHEET_SETTINGS_HEIGHT_DP
            OverlaySheet.COLLECTION -> SHEET_COLLECTION_HEIGHT_DP
            else -> SHEET_HEIGHT_DP
        }
        controlsUi.sheet = FloatingMenuLayout.sheet(anchor, safe, PxSize(dp(SHEET_WIDTH_DP), dp(sheetHeightDp)), dp(SHEET_GAP_DP))
    }

    // ---- 캐릭터 교체 ----

    /**
     * 시트에서 캐릭터를 골랐다. SWITCH_OUT 뒤에 캐릭터와 음성 팩을 함께 바꾸고 SWITCH_IN으로 들어온다.
     * 팩에 교체 전용 클립·동작이 아직 없어서 SWITCH_OUT·IN은 임시로 창 페이드로 표현한다. 두 캐릭터의 음성은 Runtime이 교체 전에 끊어 겹치지 않는다.
     */
    private fun equip(characterId: String) {
        closeControls()
        if (!characterSwitch.begin(characterId, port.state().activeCharacterId)) return
        Log.d(TAG, "오버레이 교체: SWITCH_OUT → $characterId")
        fadeTo(0f) {
            // OUT 도중 주행이 시작됐으면 lock이 페이드를 끊어 여기까지 오지 않는다. 와도 교체 지점이 null이면 이전 캐릭터를 유지한다.
            characterSwitch.outFinished()?.let {
                Log.d(TAG, "오버레이 교체: 캐릭터·음성 팩 교체 → $it")
                port.equipCharacter(it)
            }
            Log.d(TAG, "오버레이 교체: SWITCH_IN")
            fadeTo(1f) { characterSwitch.inFinished() }
        }
    }

    private fun applyPosition() {
        if (attached) root?.let { windowManager.updateViewLayout(it, params) }
    }

    /** 눌림 표시를 걷는다. 교체 연출이 창 투명도를 쓰는 중이면 그 값을 건드리지 않는다. */
    private fun clearPressFeedback() {
        main.removeCallbacks(pressFeedback)
        if (pressedShown) {
            pressedShown = false
            if (fade == null) setWindowAlpha(PressFeedback.alphaFor(false))
        }
    }

    private fun fadeTo(target: Float, onEnd: () -> Unit) {
        fade?.let {
            it.removeAllListeners()
            it.cancel()
        }
        val animator = ValueAnimator.ofFloat(params.alpha, target).apply {
            duration = SWITCH_FADE_MS
            addUpdateListener { setWindowAlpha(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (fade === animation) {
                        fade = null
                        onEnd()
                    }
                }
            })
        }
        fade = animator
        animator.start()
    }

    private fun setWindowAlpha(alpha: Float) {
        params.alpha = alpha
        if (attached) root?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun dp(value: Int) = (value * density).toInt()

    private fun sizeFor(spec: CharacterOverlaySpec) = OverlaySize(
        (spec.widthDp * density).toInt(),
        (spec.heightDp * density).toInt(),
        // 창 중심 어긋남(offset)은 캐릭터 팩 SDK가 CharacterOverlaySpec에 offsetXDp/offsetYDp를 넣은 버전부터 읽는다. 지금 고정한 버전엔 없어 0이다.
    )

    private fun layoutParams(size: OverlaySize) = WindowManager.LayoutParams(
        size.width,
        size.height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // 이 창은 터치를 받아야 한다. FLAG_NOT_TOUCHABLE을 주면 창 불투명도가 0.8에 고정돼(Android 12+ 최대 가림 불투명도 규칙으로 보임, 에뮬레이터 실측) 캐릭터가 항상 흐려진다.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        title = "VoiceCompanionCharacterOverlay"
    }

    /** 창 크기와, 창 중심이 기준 틀 중심에서 벗어난 거리(오른쪽·아래가 +). 둘 중 하나라도 다르면 창을 새로 만든다. */
    private data class OverlaySize(val width: Int, val height: Int, val offsetX: Int = 0, val offsetY: Int = 0)

    private companion object {
        const val TAG = "VoiceCompanion"
        const val PREF_X = "characterpack.vr.x"
        const val PREF_Y = "characterpack.vr.y"
        const val PREF_MENU_HINT_COUNT = "overlay.menuHint.count"
        const val PREF_MENU_OPENED = "overlay.menu.opened"
        // 캐릭터가 갈 수 있는 영역의 최소 폭. 이보다 좁아지면 계기판 제외를 하지 않는다(작은 화면).
        const val MIN_AREA_WIDTH_DP = 900
        // 캐릭터 팩 SDK의 기준 틀(CharacterOverlaySpec.DEFAULT와 같은 300×400dp). 프리뷰는 이 틀에 맞춰 그린다.
        const val STAGE_WIDTH_DP = 300
        const val STAGE_HEIGHT_DP = 400
        const val SWITCH_FADE_MS = 220L
        const val HINT_VISIBLE_MS = 7000L
        const val HINT_WIDTH_DP = 300
        const val HINT_HEIGHT_DP = 56

        // 메뉴와 시트 크기(dp). 목업(harness/avatar-overlay-mockup-2026-11-12.html)의 비율을 Pleos 2560x1440 화면에 맞췄다.
        const val MENU_PILL_WIDTH_DP = 200
        const val MENU_PILL_HEIGHT_DP = 64
        const val MENU_CLOSE_DP = 72
        const val MENU_GAP_DP = 12
        const val SHEET_WIDTH_DP = 600
        const val SHEET_HEIGHT_DP = 720
        const val SHEET_COLLECTION_HEIGHT_DP = 840
        const val SHEET_SETTINGS_HEIGHT_DP = 460
        const val SHEET_GAP_DP = 24
    }
}

internal enum class OverlayShowResult {
    SHOWING,
    PERMISSION_REQUIRED,
    REJECTED,
}
