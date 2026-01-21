package com.YunYin.app

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * 前台服务：
 * 1. 用来保活应用
 * 2. 非常适合音乐播放、长连接、后台任务
 */
class MusicForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "yunyin_foreground_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * startForegroundService() 之后会回调这里
     * 必须在 5 秒内调用 startForeground()
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("云音正在运行")
            .setContentText("音乐播放服务已启动")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true) // 不可滑动删除
            .build()

        // ⭐ 关键：真正变成前台服务
        startForeground(NOTIFICATION_ID, notification)

        /**
         * START_STICKY：
         * - 服务被系统杀掉
         * - 系统资源允许时自动重启
         * - 非常适合播放器
         */
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 不需要绑定，返回 null
        return null
    }

    /**
     * Android 8+ 必须有通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "云音播放服务",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "用于保持音乐播放和应用存活"

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
