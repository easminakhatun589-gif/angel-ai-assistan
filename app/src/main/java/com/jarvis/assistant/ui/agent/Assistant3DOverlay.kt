package com.jarvis.assistant.ui.agent

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import android.widget.FrameLayout
import android.provider.Settings

/**
 * A floating 3D AI Orb overlay that appears when the assistant is replying.
 * Supports displaying over other apps and on the lock screen.
 */
class Assistant3DOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    private val root = FrameLayout(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }

    private val webView = WebView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)
        }
    }

    private val params = WindowManager.LayoutParams(
        dp(160),
        dp(160),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    private var attached = false
    private var isCurrentlySpeaking = false

    init {
        root.addView(webView, FrameLayout.LayoutParams(dp(160), dp(160)))
        webView.loadUrl("https://appassets.androidplatform.net/assets/ai_orb.html")
    }

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        
        if (!attached) {
            try {
                windowManager.addView(root, params)
                attached = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        root.visibility = View.VISIBLE
    }

    fun hide() {
        if (attached) {
            root.visibility = View.GONE
        }
    }

    fun setSpeaking(speaking: Boolean) {
        if (isCurrentlySpeaking == speaking) return
        isCurrentlySpeaking = speaking
        webView.evaluateJavascript("setSpeaking($speaking)", null)
    }

    fun triggerWake() {
        show()
        webView.evaluateJavascript("triggerWake()", null)
    }

    fun triggerSleep() {
        webView.evaluateJavascript("triggerSleep()", null)
        // Optionally hide after a delay if needed, but for "deep sleep" showing a dim orb is better
    }

    fun remove() {
        if (attached) {
            try {
                windowManager.removeView(root)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            attached = false
        }
    }

    private fun dp(px: Int): Int = (px * context.resources.displayMetrics.density).toInt()
}
