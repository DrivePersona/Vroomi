package com.example.voicecompanion.app

import android.app.Application
import android.os.StrictMode
import com.example.voicecompanion.BuildConfig

/** 앱이 사는 동안 유지할 [CompanionRuntime]의 소유자다. */
class CompanionApp : Application() {
    val runtime by lazy { CompanionRuntime(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build(),
            )
        }
    }
}
