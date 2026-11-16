package com.example.voicecompanion.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 디버그 전용 스파이크 제어. Activity/task를 만들지 않는다(Pleos가 task를 지우면 프로세스를 죽인다).
 * `adb shell am broadcast --user 10 -n <패키지>/com.example.voicecompanion.ui.LivLivProbeReceiver --es overlay show --es persona seori`
 * overlay 값: show(persona seori|gomji|pin)·event(--es name BRAKE 등)·report·park·unpark.
 */
class LivLivProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.getStringExtra("overlay")) {
            "show" -> LivLivOverlayProbe.show(app, intent.getStringExtra("persona") ?: "seori")
            "event" -> LivLivOverlayProbe.event(intent.getStringExtra("name") ?: "")
            "report" -> LivLivOverlayProbe.report()
            "park" -> LivLivOverlayProbe.park()
            "unpark" -> LivLivOverlayProbe.unpark()
        }
    }
}
