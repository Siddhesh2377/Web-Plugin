package com.mp.web_automation

import android.app.Application
import android.webkit.WebView

class WebAutomationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
