package com.example.voicecompanion.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

/** Internal debug entry point for a newly installed package that has yet to receive boot broadcasts. */
class InternalRuntimeLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Button(this).apply {
            text = "Start Vroomi runtime"
            setOnClickListener {
                startForegroundService(
                    Intent(this@InternalRuntimeLauncherActivity, CompanionRuntimeService::class.java)
                        .setAction(CompanionRuntimeService.ACTION_BACKGROUND),
                )
                finish()
            }
        })
    }
}
