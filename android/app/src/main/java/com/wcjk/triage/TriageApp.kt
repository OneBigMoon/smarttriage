package com.wcjk.triage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TriageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("triage", "分诊系统", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
    companion object { lateinit var instance: TriageApp; private set }
}
