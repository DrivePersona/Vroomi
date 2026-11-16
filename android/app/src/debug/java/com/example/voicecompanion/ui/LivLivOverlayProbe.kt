package com.example.voicecompanion.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.example.voicecompanion.ui.overlay.OverlayDrag
import java.io.FileNotFoundException

/**
 * 디버그 전용 스파이크: 오버레이 창 안 WebView에서 `.livliv` 모델과 확정 매핑(`livliv-spike/mapping.js`)이 도는지 본다.
 * 제품 코드가 아니다. 모델·음성 클립은 저장소에 없고(라이선스·공개 경계) 로컬에서 에셋 폴더에 넣어야 한다.
 */
@SuppressLint("StaticFieldLeak", "SetJavaScriptEnabled", "ClickableViewAccessibility")
internal object LivLivOverlayProbe {
    private const val TAG = "LivLivProbe"
    private var root: FrameLayout? = null
    private var web: WebView? = null
    private var shownAt = 0L

    private class Bridge {
        @JavascriptInterface
        fun emit(e: String, v: String) {
            Log.i(TAG, "$e $v (+${SystemClock.elapsedRealtime() - shownAt}ms)")
        }
    }

    fun show(app: Context, persona: String) {
        if (root != null) {
            Log.i(TAG, "overlay already shown")
            return
        }
        val wm = app.getSystemService(WindowManager::class.java)
        val d = app.resources.displayMetrics.density
        val w = (320 * d).toInt()
        val h = (430 * d).toInt()
        shownAt = SystemClock.elapsedRealtime()
        val webView = WebView(app).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = false // 시험용 WebAudio 클립 재생
            addJavascriptInterface(Bridge(), "PleosProbe")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, r: WebResourceRequest): WebResourceResponse? {
                    val u = r.url
                    if (u.scheme != "https" || u.host != "appassets.androidplatform.net") return null
                    val path = u.path?.removePrefix("/assets/") ?: return null
                    if (!path.startsWith("livliv-spike/") || path.contains("..")) return null
                    val type = when (path.substringAfterLast('.')) {
                        "html" -> "text/html"
                        "js" -> "text/javascript"
                        "json" -> "application/json"
                        "webp" -> "image/webp"
                        "wav" -> "audio/wav"
                        else -> "application/octet-stream"
                    }
                    return try {
                        WebResourceResponse(type, "UTF-8", app.assets.open(path))
                    } catch (_: FileNotFoundException) {
                        Log.e(TAG, "Missing asset: $path")
                        WebResourceResponse("text/plain", "UTF-8", 404, "Not found", emptyMap(), "".byteInputStream())
                    }
                }
            }
        }
        val touch = View(app)
        val frame = FrameLayout(app).apply {
            addView(webView, FrameLayout.LayoutParams(w, h))
            addView(touch, FrameLayout.LayoutParams(w, h))
        }
        val lp = WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 1100
            y = 300
            title = "VoiceCompanionCharacterOverlay"
        }
        val drag = OverlayDrag(ViewConfiguration.get(app).scaledTouchSlop * 2)
        touch.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> drag.down(e.rawX, e.rawY, lp.x, lp.y, 0, 4000, 0, 4000)
                MotionEvent.ACTION_MOVE -> drag.move(e.rawX, e.rawY)?.let { (x, y) ->
                    lp.x = x
                    lp.y = y
                    wm.updateViewLayout(frame, lp)
                }
                MotionEvent.ACTION_UP -> Log.i(TAG, if (drag.dragging) "touch: drag end x=${lp.x} y=${lp.y}" else "touch: TAP")
            }
            true
        }
        wm.addView(frame, lp)
        root = frame
        web = webView
        webView.loadUrl("https://appassets.androidplatform.net/assets/livliv-spike/index.html?persona=$persona&t=1")
        Log.i(TAG, "overlay show requested persona=$persona")
    }

    /** 이벤트 하나를 매핑에 넣는다(음성 클립 재생 + 동작). [event]는 BRAKE·TURN·MODE·TIRE·TOUCH·WARNING만 받는다. */
    fun event(event: String) {
        if (event !in setOf("BRAKE", "TURN", "MODE", "TIRE", "TOUCH", "WARNING")) {
            Log.w(TAG, "unknown event $event")
            return
        }
        web?.evaluateJavascript("window.probeEvent && window.probeEvent('$event')", null)
        Log.i(TAG, "event sent $event")
    }

    /** 지금까지의 머리 각도 범위와 표정·idle 상태를 로그로 낸다. */
    fun report() {
        web?.evaluateJavascript("window.probeReport && window.probeReport()", null)
    }

    /** 창은 남기고 재생만 멈춘다(WebView를 살려 두는 안). */
    fun park() {
        web?.evaluateJavascript("window.livlivPlayer.pause()", null)
        web?.onPause()
        web?.pauseTimers()
        root?.visibility = View.INVISIBLE
        Log.i(TAG, "overlay parked")
    }

    fun unpark() {
        shownAt = SystemClock.elapsedRealtime()
        root?.visibility = View.VISIBLE
        web?.resumeTimers()
        web?.onResume()
        web?.evaluateJavascript("window.probeResume(); window.livlivPlayer.play()", null)
        Log.i(TAG, "overlay unparked")
    }
}
