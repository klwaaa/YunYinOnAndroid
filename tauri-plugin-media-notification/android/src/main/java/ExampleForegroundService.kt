package com.plugin.media_notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ExampleForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "example_foreground_channel"
        const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    val title = intent?.getStringExtra("title") ?: "未知歌曲"
    val playing = intent?.getBooleanExtra("playing", false) ?: false

    val notification = buildNotification(
        title,
        playing
    )

    startForeground(NOTIFICATION_ID, notification)

    return START_STICKY
}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "前台服务",
                NotificationManager.IMPORTANCE_LOW // 前台服务通常用 LOW，避免打扰
            ).apply {
                description = "示例前台服务通知渠道"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("前台服务运行中")
            .setContentText("插件正在后台执行任务...")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 替换为你的图标资源
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 使其不可滑动删除
            .build()
    }
}