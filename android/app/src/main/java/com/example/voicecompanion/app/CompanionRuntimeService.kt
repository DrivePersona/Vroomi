package com.example.voicecompanion.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Activity 없이 차량 신호 구독과 오버레이를 유지하는 런타임 소유자다.
 *
 * 포그라운드 서비스여야 한다. 주행 중에는 오버레이 창이 없어 앱이 백그라운드로 밀리고, 일반 서비스는 시스템이
 * 프로세스를 얼린 뒤 유휴 서비스로 정지시켜(실측 am_freeze → am_stop_idle_service) 정차 복귀를 받지 못했다.
 */
class CompanionRuntimeService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() 뒤 5초 안에 호출해야 하므로 런타임 시작보다 먼저 한다.
        startInForeground()
        // 부팅 리시버가 보낸 시작과, 시스템이 sticky 서비스를 되살린 경우(intent == null)를 같은 배경 세션으로 처리한다.
        (application as CompanionApp).runtime.startBackgroundSession()
        Log.i(TAG, "runtime started; action=${intent?.action ?: "sticky restart"}")
        return START_STICKY
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "보이스 컴패니언", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            },
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("보이스 컴패니언")
            .setContentText("차량 상태를 듣고 있어요")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_BACKGROUND = "com.example.voicecompanion.action.START_BACKGROUND"
        private const val TAG = "VoiceCompanionService"
        private const val CHANNEL_ID = "runtime"
        private const val NOTIFICATION_ID = 1
    }
}
