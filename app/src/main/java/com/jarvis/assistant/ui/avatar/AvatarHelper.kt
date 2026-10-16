package com.jarvis.assistant.ui.avatar

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jarvis.assistant.data.model.ConversationState
import com.jarvis.assistant.data.preferences.AppPreferences
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Controller and bridge for the Realistic 3D Virtual Human Avatar WebView.
 */
class AvatarHelper(
    private val webView: WebView,
    private val preferences: AppPreferences,
    private val onSingleTap: (() -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onAvatarReady: (() -> Unit)? = null
) {
    var isReady: Boolean = false
        private set

    private var pendingState: String? = null
    private var lastAudioPushTime = 0L

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setupWebView() {
        webView.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            addJavascriptInterface(object {
                @JavascriptInterface
                fun onAvatarReady() {
                    post {
                        isReady = true
                        applyAppearanceFromPreferences()
                        pendingState?.let { setState(it) }
                        onAvatarReady?.invoke()
                    }
                }
            }, "AvatarBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isReady = true
                    applyAppearanceFromPreferences()
                    pendingState?.let { setState(it) }
                    onAvatarReady?.invoke()
                }
            }

            // Gesture handling for tap-to-talk while allowing touch drag for 3D rotation
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onSingleTap?.invoke()
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    onLongPress?.invoke()
                }
            })

            setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
                v.onTouchEvent(event)
            }

            loadUrl("file:///android_asset/avatar.html")
        }
    }

    fun setState(state: String) {
        pendingState = state
        if (!isReady) return
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.setState('$state');",
                null
            )
        }
    }

    fun setConversationState(convState: ConversationState) {
        val stateStr = when (convState) {
            ConversationState.LISTENING -> "listening"
            ConversationState.SPEAKING -> "speaking"
            ConversationState.THINKING -> "thinking"
            ConversationState.IDLE -> "idle"
            ConversationState.NOTIFICATION -> "notification"
            ConversationState.WAKING -> "waking"
            ConversationState.SLEEPING -> "sleeping"
        }
        setState(stateStr)
    }

    fun setExpression(expression: String) {
        if (!isReady) return
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.setExpression('$expression');",
                null
            )
        }
    }

    fun pushAudioLevel(amplitude: Float, bass: Float = 0f, high: Float = 0f) {
        if (!isReady) return
        val now = System.currentTimeMillis()
        if (now - lastAudioPushTime < 33L) return // ~30 fps throttle
        lastAudioPushTime = now
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.setAudioLevel($amplitude, $bass, $high);",
                null
            )
        }
    }

    fun applyAppearanceFromPreferences() {
        if (!isReady) return

        val customImagePath = preferences.avatarCustomImagePath
        if (preferences.avatarModel == "custom_image" && !customImagePath.isNullOrBlank()) {
            loadCustomImageFromFile(File(customImagePath))
            return
        }

        try {
            val cfg = JSONObject().apply {
                put("model", preferences.avatarModel)
                put("gender", preferences.avatarGender)
                put("hairStyle", preferences.avatarHairStyle)
                put("hairColor", parseHexColor(preferences.avatarHairColor, 0xd4af37))
                put("eyeColor", parseHexColor(preferences.avatarEyeColor, 0x00d4ff))
                put("skinTone", preferences.avatarSkinTone)
                put("clothingStyle", preferences.avatarClothingStyle)
                put("clothingColor", parseHexColor(preferences.avatarClothingColor, 0x0f172a))
                put("clothingAccent", parseHexColor(preferences.appTheme, 0x00d4ff))
                put("environment", preferences.avatarEnvironment)
                put("lighting", preferences.avatarLighting)
                put("vfxEnabled", preferences.isAvatarVfxEnabled)
                put("quality", preferences.avatarGraphicsQuality)
                put("animationsEnabled", preferences.isAvatarAnimationsEnabled)
                put("idleAnimEnabled", preferences.isAvatarIdleAnimEnabled)
                put("lipSyncEnabled", preferences.isAvatarLipSyncEnabled)
                put("defaultExpression", preferences.avatarDefaultExpression)
            }

            webView.post {
                webView.evaluateJavascript(
                    "window.AngelAvatar && window.AngelAvatar.setAppearance($cfg);",
                    null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AvatarHelper", "Error applying avatar appearance: ${e.message}", e)
        }
    }

    fun setQuality(quality: String) {
        if (!isReady) return
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.setQuality('$quality');",
                null
            )
        }
    }

    fun resetCamera() {
        if (!isReady) return
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.resetCamera();",
                null
            )
        }
    }

    fun playAnimation(animationName: String) {
        if (!isReady) return
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.playAnimation('$animationName');",
                null
            )
        }
    }

    fun showStatusPill(show: Boolean) {
        if (!isReady) return
        webView.post {
            webView.evaluateJavascript(
                "window.AngelAvatar && window.AngelAvatar.showStatusPill($show);",
                null
            )
        }
    }

    fun loadCustomImageFromFile(file: File) {
        if (!file.exists()) return
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64"
            webView.post {
                webView.evaluateJavascript(
                    "window.AngelAvatar && window.AngelAvatar.loadCustomImage('$dataUri');",
                    null
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AvatarHelper", "Error loading custom portrait: ${e.message}", e)
        }
    }

    private fun parseHexColor(colorStr: String, fallback: Int): Int {
        return try {
            val clean = colorStr.replace("#", "").trim()
            clean.toInt(16)
        } catch (e: Exception) {
            fallback
        }
    }

    fun onResume() {
        webView.onResume()
        webView.resumeTimers()
    }

    fun onPause() {
        webView.onPause()
        webView.pauseTimers()
    }

    fun onDestroy() {
        try {
            webView.removeJavascriptInterface("AvatarBridge")
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {
            // Safe cleanup
        }
    }
}
