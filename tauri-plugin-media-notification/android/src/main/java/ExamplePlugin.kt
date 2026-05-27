package com.plugin.media_notification

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin

// =========================
// 参数
// =========================
@InvokeArg
class StartNotificationArgs {

    var songTitle: String? = null

    var isPlaying: Boolean = false
}

// =========================
// 插件主体
// =========================
@TauriPlugin
class ExamplePlugin(
    private val activity: Activity
) : Plugin(activity) {

    companion object {

        // action
        const val ACTION_PREV =
            "com.plugin.media_notification.PREV"

        const val ACTION_NEXT =
            "com.plugin.media_notification.NEXT"

        const val ACTION_PLAY_PAUSE =
            "com.plugin.media_notification.PLAY_PAUSE"

        const val ACTION_UPDATE =
            "com.plugin.media_notification.UPDATE"

        // 插件实例
        var instance: ExamplePlugin? = null
    }

    // 保存 receiver
    private lateinit var notificationReceiver: NotificationReceiver

    // =========================
    // 插件加载
    // =========================
    override fun load(webView: WebView) {

        super.load(webView)

        instance = this

        // 创建 receiver
        notificationReceiver =
            NotificationReceiver()

        // 注册广播
        val filter =
            IntentFilter().apply {

                addAction(ACTION_PREV)

                addAction(ACTION_NEXT)

                addAction(ACTION_PLAY_PAUSE)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            activity.registerReceiver(
                notificationReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            activity.registerReceiver(
                notificationReceiver,
                filter
            )
        }
    }

    // =========================
    // 插件销毁
    // =========================
    override fun onDestroy() {

        try {

            activity.unregisterReceiver(
                notificationReceiver
            )

        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    // =========================
    // 前端调用
    // =========================
    @Command
    fun startNotification(
        invoke: Invoke
    ) {

        val args =
            invoke.parseArgs(
                StartNotificationArgs::class.java
            )

        val songTitle =
            args.songTitle ?: "未知歌曲"

        val isPlaying =
            args.isPlaying

        // Android 13 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1001
                )

                invoke.reject(
                    "POST_NOTIFICATIONS permission denied"
                )

                return
            }
        }

        // 启动前台服务
        val intent =
            Intent(
                activity,
                ExampleForegroundService::class.java
            ).apply {

                action = ACTION_UPDATE

                putExtra(
                    "songTitle",
                    songTitle
                )

                putExtra(
                    "isPlaying",
                    isPlaying
                )
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            activity.startForegroundService(
                intent
            )

        } else {

            activity.startService(intent)
        }

        val ret = JSObject()

        ret.put("success", true)

        invoke.resolve(ret)
    }
}

// =========================
// 通知按钮 Receiver
// =========================
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        when (intent?.action) {

            ExamplePlugin.ACTION_PREV -> {

                ExamplePlugin.instance?.trigger(
                    "previous",
                    JSObject()
                )
            }

            ExamplePlugin.ACTION_NEXT -> {

                ExamplePlugin.instance?.trigger(
                    "next",
                    JSObject()
                )
            }

            ExamplePlugin.ACTION_PLAY_PAUSE -> {

                ExamplePlugin.instance?.trigger(
                    "playpause",
                    JSObject()
                )
            }
        }
    }
}

// =========================
// 前台服务
// =========================
class ExampleForegroundService : Service() {

    companion object {

        const val CHANNEL_ID =
            "media_notification_channel"

        const val NOTIFICATION_ID =
            1001
    }

    private var currentSongTitle =
        "未知歌曲"

    private var isPlaying =
        false

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (
            intent?.action
            == ExamplePlugin.ACTION_UPDATE
        ) {

            currentSongTitle =
                intent.getStringExtra(
                    "songTitle"
                ) ?: currentSongTitle

            isPlaying =
                intent.getBooleanExtra(
                    "isPlaying",
                    isPlaying
                )
        }

        val notification =
            buildNotification(
                currentSongTitle,
                isPlaying
            )

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        return START_STICKY
    }

    // =========================
    // 创建通知渠道
    // =========================
    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                        CHANNEL_ID,
                        "音乐播放控制",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        // 禁用应用图标的通知角标（红点）
                        setShowBadge(false)
                    }

            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                channel
            )
        }
    }

    // =========================
    // 创建通知
    // =========================
    private fun buildNotification(
        songTitle: String,
        isPlaying: Boolean
    ): Notification {

        val playPauseIcon =
            if (isPlaying)
                android.R.drawable.ic_media_pause
            else
                android.R.drawable.ic_media_play

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )

            .setSmallIcon(
                android.R.drawable.ic_media_play
            )

            .setContentTitle(
                songTitle
            )

            .setContentText(
                if (isPlaying)
                    "正在播放"
                else
                    "已暂停"
            )

            .setOnlyAlertOnce(true)

            .setOngoing(true)

            // 上一曲
            .addAction(
                android.R.drawable.ic_media_previous,
                "上一曲",
                createPendingIntent(
                    ExamplePlugin.ACTION_PREV
                )
            )

            // 播放暂停
            .addAction(
                playPauseIcon,
                "播放暂停",
                createPendingIntent(
                    ExamplePlugin.ACTION_PLAY_PAUSE
                )
            )

            // 下一曲
            .addAction(
                android.R.drawable.ic_media_next,
                "下一曲",
                createPendingIntent(
                    ExamplePlugin.ACTION_NEXT
                )
            )

            .setStyle(
                MediaStyle()
            )

            .build()
    }

    // =========================
    // 创建 PendingIntent
    // =========================
    private fun createPendingIntent(
        action: String
    ): PendingIntent {

        val intent =
            Intent(
                this,
                NotificationReceiver::class.java
            ).apply {

                this.action = action
            }

        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else
                        0

        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            flags
        )
    }
}