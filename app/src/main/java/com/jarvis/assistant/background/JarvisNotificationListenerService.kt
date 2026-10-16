package com.jarvis.assistant.background

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class JarvisNotificationListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        Log.i("JarvisNotification", "JarvisNotificationListenerService created")
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("JarvisNotification", "TTS language not supported")
            } else {
                isTtsReady = true
            }
        } else {
            Log.e("JarvisNotification", "TTS initialization failed")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        // Ignore our own app notifications
        if (packageName == applicationContext.packageName) return

        val appName = getAppName(packageName)
        Log.i("JarvisNotification", "Notification from $appName ($packageName): Title=$title, Text=$text")

        val announcement = when {
            packageName.contains("whatsapp", ignoreCase = true) -> {
                "WhatsApp message from $title. $text. Do you want to reply?"
            }
            packageName.contains("telecom", ignoreCase = true) || packageName.contains("dialer", ignoreCase = true) || title.contains("call", ignoreCase = true) -> {
                "Incoming call from $title. Do you want to receive or reject?"
            }
            else -> {
                "Notification from $appName. $title: $text"
            }
        }

        speakAnnouncement(announcement)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun speakAnnouncement(text: String) {
        if (isTtsReady && !text.isBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "notification_utterance")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
