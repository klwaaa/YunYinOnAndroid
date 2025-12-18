package com.YunYin.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class KeepAliveService : Service() {

    private val CHANNEL_ID = "KeepAliveChannel"

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KeepAlive Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification: Notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("YunYin")
                .setContentText("后台播放中")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()

            startForeground(1, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
