package com.YunYin.app

import android.app.Activity
import com.tauri.plugin.TauriPlugin
import com.tauri.plugin.Command
import com.tauri.webview.TauriWebView

class KeepAlivePlugin(private val activity: Activity) : TauriPlugin() {

    companion object {
        fun register(activity: Activity, webview: TauriWebView) {
            val plugin = KeepAlivePlugin(activity)
            plugin.register(webview)
        }
    }

    @Command
    fun keepAlive() {
        // 这里放置保持后台运行的逻辑
        // 示例：启动前台 Service 或者定时任务
    }
}
