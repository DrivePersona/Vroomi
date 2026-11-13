package com.example.voicecompanion.ui.overlay

import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Activity 없이 WindowManager에 직접 붙이는 Compose 창 하나. 메뉴·시트 창과 힌트 창이 같은 방식으로 쓴다.
 * 붙일 때 수명 소유자를 만들고 뗄 때 정리한다. 창 크기·위치·플래그는 호출하는 쪽이 정한다.
 */
internal class ComposeOverlayWindow(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null

    val isShown: Boolean get() = view != null

    /** 이미 붙어 있으면 그대로 두고 true. 권한이 취소됐거나 창 토큰이 거절되면 false다. */
    fun show(params: WindowManager.LayoutParams, content: @Composable () -> Unit): Boolean {
        if (view != null) return true
        val lifecycleOwner = OverlayLifecycleOwner()
        val composeView = ComposeView(context).apply { setContent(content) }
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        return try {
            windowManager.addView(composeView, params)
            lifecycleOwner.resume()
            view = composeView
            owner = lifecycleOwner
            true
        } catch (_: SecurityException) {
            lifecycleOwner.destroy()
            false
        } catch (_: WindowManager.BadTokenException) {
            lifecycleOwner.destroy()
            false
        }
    }

    fun remove() {
        val attached = view ?: return
        view = null
        try {
            windowManager.removeViewImmediate(attached)
        } catch (_: IllegalArgumentException) {
            // 시스템이 이미 창을 정리한 경우다.
        }
        owner?.destroy()
        owner = null
    }
}
