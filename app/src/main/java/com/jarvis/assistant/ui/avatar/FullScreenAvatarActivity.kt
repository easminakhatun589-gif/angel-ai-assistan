package com.jarvis.assistant.ui.avatar

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.background.JarvisConversationService
import com.jarvis.assistant.data.model.ConversationState
import com.jarvis.assistant.databinding.ActivityFullScreenAvatarBinding
import com.jarvis.assistant.ui.home.MainActivity
import com.jarvis.assistant.ui.settings.AvatarSettingsActivity
import com.jarvis.assistant.util.ThemeManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FullScreenAvatarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullScreenAvatarBinding
    private val preferences get() = JarvisApp.instance.preferences
    private var avatarHelper: AvatarHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenAvatarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.fullScreenAvatarRoot)

        setupAvatar()
        setupListeners()
        observeConversation()
    }

    private fun setupAvatar() {
        avatarHelper = AvatarHelper(
            webView = binding.avatarFullScreenWebView,
            preferences = preferences,
            onSingleTap = {
                handleMicClick()
            },
            onAvatarReady = {
                avatarHelper?.showStatusPill(true)
                avatarHelper?.setConversationState(JarvisConversationService.conversationState.value)
            }
        )
    }

    private fun setupListeners() {
        binding.btnCloseFullScreen.setOnClickListener { finish() }

        binding.btnOpenAvatarSettings.setOnClickListener {
            startActivity(Intent(this, AvatarSettingsActivity::class.java))
        }

        binding.btnFullScreenResetAngle.setOnClickListener {
            avatarHelper?.resetCamera()
        }

        binding.btnFullScreenMic.setOnClickListener {
            handleMicClick()
        }

        binding.btnFullScreenScreenShare.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
        }
    }

    private fun handleMicClick() {
        if (!JarvisConversationService.isSessionOn.value) {
            val intent = Intent(this, JarvisConversationService::class.java).setAction(JarvisConversationService.ACTION_START_CONVERSATION)
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        } else {
            val intent = Intent(this, JarvisConversationService::class.java).setAction(JarvisConversationService.ACTION_STOP_ALL)
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun observeConversation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    JarvisConversationService.conversationState.collect { state ->
                        avatarHelper?.setConversationState(state)
                        updateStateUI(state)
                    }
                }

                launch {
                    JarvisConversationService.audioLevel.collect { level ->
                        avatarHelper?.pushAudioLevel(level)
                    }
                }

                launch {
                    combine(
                        JarvisConversationService.liveUserTranscript,
                        JarvisConversationService.liveAssistantResponse
                    ) { userText, assistantText ->
                        Pair(userText, assistantText)
                    }.collect { (userText, assistantText) ->
                        if (assistantText.isNotBlank()) {
                            binding.tvSpeakerLabel.text = "ANGEL AI"
                            binding.tvLiveCaptionText.text = assistantText
                            binding.cardLiveCaptions.visibility = View.VISIBLE
                        } else if (userText.isNotBlank()) {
                            binding.tvSpeakerLabel.text = "YOU"
                            binding.tvLiveCaptionText.text = userText
                            binding.cardLiveCaptions.visibility = View.VISIBLE
                        } else {
                            binding.tvSpeakerLabel.text = "ANGEL AI"
                            binding.tvLiveCaptionText.text = "Listening for your voice... Tap the mic or speak anytime."
                        }
                    }
                }
            }
        }
    }

    private fun updateStateUI(state: ConversationState) {
        when (state) {
            ConversationState.LISTENING -> {
                binding.tvFullScreenStatus.text = "LISTENING TO YOU..."
                binding.btnFullScreenMic.animate().scaleX(1.12f).scaleY(1.12f).setDuration(200).start()
            }
            ConversationState.SPEAKING -> {
                binding.tvFullScreenStatus.text = "ANGEL AI SPEAKING"
                binding.btnFullScreenMic.animate().scaleX(1.08f).scaleY(1.08f).setDuration(200).start()
            }
            ConversationState.THINKING -> {
                binding.tvFullScreenStatus.text = "PROCESSING..."
                binding.btnFullScreenMic.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
            ConversationState.IDLE -> {
                binding.tvFullScreenStatus.text = "3D VIRTUAL HUMAN • READY"
                binding.btnFullScreenMic.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
            ConversationState.NOTIFICATION -> {
                binding.tvFullScreenStatus.text = "SYSTEM NOTIFICATION"
                binding.btnFullScreenMic.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
            }
            ConversationState.WAKING -> {
                binding.tvFullScreenStatus.text = "SYSTEM BOOT..."
            }
            ConversationState.SLEEPING -> {
                binding.tvFullScreenStatus.text = "DEEP SLEEP"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        avatarHelper?.onResume()
        avatarHelper?.applyAppearanceFromPreferences()
    }

    override fun onPause() {
        super.onPause()
        avatarHelper?.onPause()
    }

    override fun onDestroy() {
        avatarHelper?.onDestroy()
        super.onDestroy()
    }
}
