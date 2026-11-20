package com.example.voicecompanion.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

/** Internal debug entry point for a newly installed package that has yet to receive boot broadcasts. */
class InternalRuntimeLauncherActivity : Activity() {
    private var chargeCar: android.car.Car? = null

    private fun probeChargeState() {
        if (chargeCar != null) return
        try {
            val car = android.car.Car.createCar(this)
            chargeCar = car
            val manager = car.getCarManager(android.car.Car.PROPERTY_SERVICE) as android.car.hardware.property.CarPropertyManager
            val registered = manager.registerCallback(object : android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(value: android.car.hardware.CarPropertyValue<*>) {
                    android.util.Log.i("VroomiChargeProbe", "status=${value.status} value=${value.value}")
                }
                override fun onErrorEvent(propertyId: Int, areaId: Int) {
                    android.util.Log.w("VroomiChargeProbe", "callback error: $propertyId/$areaId")
                }
            }, 0x11400f41, 0f)
            android.util.Log.i("VroomiChargeProbe", "registered=$registered")
        } catch (e: RuntimeException) {
            android.util.Log.w("VroomiChargeProbe", "Charge state unavailable", e)
        }
    }

    override fun onDestroy() {
        chargeCar?.disconnect()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("probe_charge_state", false)) {
            setContentView(Button(this).apply {
                text = "Read EV charge state"
                setOnClickListener { probeChargeState() }
            })
            return
        }
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
