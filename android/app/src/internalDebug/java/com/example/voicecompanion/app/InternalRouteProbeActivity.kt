package com.example.voicecompanion.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/** Fixed-destination diagnostic for the parked Pleos test emulator. */
class InternalRouteProbeActivity : Activity() {
    private var helper: NaviHelper? = null
    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) { Log.i(TAG, "route search started") }
        override fun onRoutingCompleted() { Log.i(TAG, "route search completed") }
        override fun onRouteCancelled() { Log.i(TAG, "route cancelled") }
        override fun onError(errorCode: NaviErrorCode) { Log.i(TAG, "navigation error: $errorCode") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val search = Button(this).apply { text = "Request test route"; isEnabled = false }
        val cancel = Button(this).apply { text = "Cancel test route"; isEnabled = false }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(search)
            addView(cancel)
        })
        val navi = NaviHelper(this)
        helper = navi
        navi.addListener(listener)
        navi.initialize(
            {
                runOnUiThread {
                    if (helper !== navi) return@runOnUiThread
                    search.isEnabled = true
                    Log.i(TAG, "connected")
                }
            },
            { error -> Log.w(TAG, "connection failed", error) },
        )
        search.setOnClickListener {
            try {
                navi.requestRoute(RouteInfo(
                    latitude = 37.48544722, longitude = 127.03636666,
                    displayLatitude = null, displayLongitude = null,
                    poiName = "42dot", poiId = "36khp37hh8gqcm5nqhe5", poiSubId = "0",
                    address = "서울시 강남구 남부순환로 2621",
                    routeOption = RouteOption.RECOMMENDED,
                ))
                cancel.isEnabled = true
                Log.i(TAG, "test route requested")
            } catch (e: RuntimeException) {
                Log.w(TAG, "route request failed", e)
            }
        }
        cancel.setOnClickListener {
            try {
                navi.cancelRoute()
                cancel.isEnabled = false
                Log.i(TAG, "test route cancellation requested")
            } catch (e: RuntimeException) {
                Log.w(TAG, "route cancellation failed", e)
            }
        }
    }

    override fun onDestroy() {
        val navi = helper
        helper = null
        navi?.removeListener(listener)
        navi?.release()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "VroomiRouteProbe"
    }
}
