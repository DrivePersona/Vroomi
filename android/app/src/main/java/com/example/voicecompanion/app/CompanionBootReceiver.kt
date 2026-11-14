package com.example.voicecompanion.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Android 부팅이 끝나면 백그라운드 세션을 시작한다. Activity는 절대 열지 않는다. */
class CompanionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            context.startForegroundService(
                Intent(context, CompanionRuntimeService::class.java)
                    .setAction(CompanionRuntimeService.ACTION_BACKGROUND),
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "부팅 후 런타임 서비스를 시작하지 못했어요", e)
        }
    }

    private companion object {
        const val TAG = "VoiceCompanionBoot"
    }
}
