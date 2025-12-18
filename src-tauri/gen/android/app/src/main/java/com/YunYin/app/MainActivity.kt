package com.YunYin.app

import android.os.Bundle
import com.tauri.TauriActivity
import com.tauri.webview.TauriWebView
import com.tauri.plugin.PluginRegistry

class MainActivity : TauriActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 状态栏沉浸
    }

    override fun registerPlugins(registry: PluginRegistry, webview: TauriWebView) {
        super.registerPlugins(registry, webview)
        KeepAlivePlugin.register(this, webview)
    }

    override fun onWebViewCreated(webview: TauriWebView) {
        super.onWebViewCreated(webview)
        // 可在此做 WebView 相关操作
    }
}
