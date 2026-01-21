package com.YunYin.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge

class MainActivity : TauriActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        startForegroundServiceIfNeeded()
    }

    /**
     * 应用启动时立刻开启前台服务
     */
    private fun startForegroundServiceIfNeeded() {
        val intent = Intent(this, MusicForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /**
             * Android 8+
             * 必须使用 startForegroundService
             */
            startForegroundService(intent)
        } else {
            /**
             * Android 7 及以下
             */
            startService(intent)
        }
    }
}
