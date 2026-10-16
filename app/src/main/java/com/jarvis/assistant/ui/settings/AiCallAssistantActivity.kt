package com.jarvis.assistant.ui.settings

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.automation.phone.AiCallAssistantEngine
import com.jarvis.assistant.automation.phone.CallAssistantState
import com.jarvis.assistant.databinding.ActivityAiCallAssistantBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AiCallAssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiCallAssistantBinding
    private val preferences by lazy { JarvisApp.instance.preferences }
    private lateinit var engine: AiCallAssistantEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        com.jarvis.assistant.util.ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAiCallAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)

        engine = AiCallAssistantEngine(this)

        setupUI()
        loadPreferences()
        setupListeners()
        observeEngineState()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Setup Spinners
        val languages = arrayOf("Auto", "Bengali", "English")
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = langAdapter

        val voices = arrayOf("Angel-AI (Natural)", "DeepSeek AI Voice", "Grok Neural Voice", "Classic HUD Voice")
        val voiceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, voices)
        binding.spinnerVoice.adapter = voiceAdapter
    }

    private fun loadPreferences() {
        binding.switchCallAssistant.isChecked = preferences.isCallAssistantEnabled
        binding.switchWhatsappCall.isChecked = preferences.whatsappCallAssistanceEnabled
        binding.switchSimCall.isChecked = preferences.simCallAssistanceEnabled
        binding.switchAutoAnswer.isChecked = preferences.callAssistantAutoAnswer
        binding.switchPrivacyOptIn.isChecked = preferences.callAssistantDataRetentionOptIn

        binding.etCustomGreeting.setText(preferences.callAssistantCustomGreeting)
        binding.etInstructions.setText(preferences.callAssistantInstructions)
        binding.etAllowlist.setText(preferences.callAssistantAllowlist)
        binding.etBlocked.setText(preferences.callAssistantBlocked)

        val lang = preferences.callAssistantLanguage
        when (lang.lowercase()) {
            "bengali", "bn" -> binding.spinnerLanguage.setSelection(1)
            "english", "en" -> binding.spinnerLanguage.setSelection(2)
            else -> binding.spinnerLanguage.setSelection(0)
        }

        val voice = preferences.callAssistantVoice
        val voices = arrayOf("Angel-AI (Natural)", "DeepSeek AI Voice", "Grok Neural Voice", "Classic HUD Voice")
        val idx = voices.indexOf(voice)
        if (idx >= 0) binding.spinnerVoice.setSelection(idx)
    }

    private fun setupListeners() {
        binding.switchCallAssistant.setOnCheckedChangeListener { _, isChecked ->
            preferences.isCallAssistantEnabled = isChecked
            Toast.makeText(this, if (isChecked) "AI Call Assistant Enabled" else "AI Call Assistant Disabled", Toast.LENGTH_SHORT).show()
        }

        binding.switchWhatsappCall.setOnCheckedChangeListener { _, isChecked ->
            preferences.whatsappCallAssistanceEnabled = isChecked
        }

        binding.switchSimCall.setOnCheckedChangeListener { _, isChecked ->
            preferences.simCallAssistanceEnabled = isChecked
        }

        binding.switchAutoAnswer.setOnCheckedChangeListener { _, isChecked ->
            preferences.callAssistantAutoAnswer = isChecked
        }

        binding.switchPrivacyOptIn.setOnCheckedChangeListener { _, isChecked ->
            preferences.callAssistantDataRetentionOptIn = isChecked
        }

        binding.btnTestSimCall.setOnClickListener {
            engine.simulateIncomingCall("Rahim Ahmed", "+8801812345678", false)
            Toast.makeText(this, "Simulating incoming SIM call...", Toast.LENGTH_SHORT).show()
        }

        binding.btnTestWhatsappCall.setOnClickListener {
            engine.simulateIncomingCall("WhatsApp Caller", "+8801812345678", true)
            Toast.makeText(this, "Simulating incoming WhatsApp call...", Toast.LENGTH_SHORT).show()
        }

        binding.btnEmergencyStop.setOnClickListener {
            engine.stopAssistance("Emergency stop triggered by user.")
            Toast.makeText(this, "Call assistance stopped immediately.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeEngineState() {
        lifecycleScope.launch {
            engine.currentState.collectLatest { state ->
                binding.tvEngineState.text = "STATE: $state"
                val color = when (state) {
                    CallAssistantState.IDLE -> Color.parseColor("#94A3B8")
                    CallAssistantState.INCOMING_CALL_DETECTED, CallAssistantState.ELIGIBILITY_CHECK -> Color.parseColor("#EAB308")
                    CallAssistantState.WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER -> Color.parseColor("#3B82F6")
                    CallAssistantState.CONNECTING_AI, CallAssistantState.LISTENING, CallAssistantState.THINKING -> Color.parseColor("#00D4FF")
                    CallAssistantState.SPEAKING -> Color.parseColor("#22C55E")
                }
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(color)
            }
        }

        lifecycleScope.launch {
            engine.statusMessage.collectLatest { msg ->
                binding.tvEngineStatusMessage.text = msg
            }
        }

        lifecycleScope.launch {
            engine.lastTranscript.collectLatest { transcript ->
                if (transcript.isNotBlank()) {
                    binding.tvLastTranscript.text = transcript
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save settings on pause
        preferences.callAssistantCustomGreeting = binding.etCustomGreeting.text.toString()
        preferences.callAssistantInstructions = binding.etInstructions.text.toString()
        preferences.callAssistantAllowlist = binding.etAllowlist.text.toString()
        preferences.callAssistantBlocked = binding.etBlocked.text.toString()
        preferences.callAssistantLanguage = binding.spinnerLanguage.selectedItem.toString()
        preferences.callAssistantVoice = binding.spinnerVoice.selectedItem.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.release()
    }
}
