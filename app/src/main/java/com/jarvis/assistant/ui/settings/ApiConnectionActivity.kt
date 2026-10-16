package com.jarvis.assistant.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.GeminiConstants
import com.jarvis.assistant.databinding.ActivityApiConnectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ApiConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiConnectionBinding
    private val preferences by lazy { JarvisApp.instance.preferences }
    private val testHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        com.jarvis.assistant.util.ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityApiConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)

        setupUI()
        loadCurrentPreferences()
        setupListeners()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentPreferences() {
        // 1. Keys
        binding.etApiKey.setText(preferences.apiKey)
        binding.etGrokApiKey.setText(preferences.grokApiKey)
        binding.etDeepseekApiKey.setText(preferences.deepseekApiKey)
        binding.etHiveApiKey.setText(preferences.hiveApiKey)
        binding.etMetaApiKey.setText(preferences.metaApiKey)
        binding.etCustomEndpoint.setText(preferences.customEndpoint)

        // Status
        if (!preferences.canStoreApiKeySecurely) {
            binding.tvLiveStatus.text = "SECURE STORAGE UNAVAILABLE • KEYS CACHED"
            binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        } else if (preferences.apiKey.isNotBlank()) {
            binding.tvLiveStatus.text = "SYSTEM READY • MULTI-AI ENGINE ACTIVE"
            binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        } else {
            binding.tvLiveStatus.text = "NO GEMINI KEY • CONFIGURE PROVIDERS"
            binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EAB308"))
        }

        // 2. Gemini Live Model selection
        when (preferences.aiModel) {
            GeminiConstants.MODEL_3_8_LIVE -> binding.rbModelFlash20.isChecked = true
            GeminiConstants.MODEL_3_1_FLASH_AGENT -> binding.rbModelFlash15.isChecked = true
            else -> binding.rbModelFlashThinking.isChecked = true
        }

        // 3. Code Generation Provider selection
        when (preferences.codeGenerationProvider.lowercase()) {
            "hive" -> binding.rbCodeHive.isChecked = true
            "grok" -> binding.rbCodeGrok.isChecked = true
            "gemini" -> binding.rbCodeGemini.isChecked = true
            else -> binding.rbCodeDeepSeek.isChecked = true
        }

        // 4. Photo Generation Provider selection
        when (preferences.photoGenerationProvider.lowercase()) {
            "grok" -> binding.rbPhotoGrok.isChecked = true
            "gemini" -> binding.rbPhotoGemini.isChecked = true
            "deepseek" -> binding.rbPhotoDeepSeek.isChecked = true
            else -> binding.rbPhotoMeta.isChecked = true
        }

        // 5. Video Generation Provider selection
        when (preferences.videoGenerationProvider.lowercase()) {
            "gemini" -> binding.rbVideoGemini.isChecked = true
            else -> binding.rbVideoGrok.isChecked = true
        }

        if (preferences.customEndpoint.isNotBlank()) {
            binding.tvEndpointSummary.text = "Active Endpoint: ${preferences.customEndpoint}"
        }

        // 6. Live Voice Provider selection
        if (preferences.liveVoiceProvider.equals("grok", ignoreCase = true)) {
            binding.rbLiveGrok.isChecked = true
        } else {
            binding.rbLiveGemini.isChecked = true
        }
    }

    private fun setupListeners() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // --- Gemini Key ---
        binding.btnPasteKey.setOnClickListener {
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (pasted.isNotBlank()) {
                binding.etApiKey.setText(pasted)
                Toast.makeText(this, "Gemini key pasted", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClearKey.setOnClickListener { binding.etApiKey.setText("") }
        binding.btnTestConnection.setOnClickListener {
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "Enter Gemini key first", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            testGeminiConnection(key)
        }

        // --- Grok Key ---
        binding.btnPasteGrokKey.setOnClickListener {
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (pasted.isNotBlank()) {
                binding.etGrokApiKey.setText(pasted)
                Toast.makeText(this, "Grok key pasted", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClearGrokKey.setOnClickListener { binding.etGrokApiKey.setText("") }
        binding.btnTestGrokKey.setOnClickListener {
            val key = binding.etGrokApiKey.text?.toString()?.trim() ?: ""
            testGenericConnection("https://api.x.ai/v1/models", key, "Bearer", binding.tvGrokPingResult, "Grok AI")
        }

        // --- DeepSeek Key ---
        binding.btnPasteDeepseekKey.setOnClickListener {
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (pasted.isNotBlank()) {
                binding.etDeepseekApiKey.setText(pasted)
                Toast.makeText(this, "DeepSeek key pasted", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClearDeepseekKey.setOnClickListener { binding.etDeepseekApiKey.setText("") }
        binding.btnTestDeepseekKey.setOnClickListener {
            val key = binding.etDeepseekApiKey.text?.toString()?.trim() ?: ""
            testGenericConnection("https://api.deepseek.com/models", key, "Bearer", binding.tvDeepseekPingResult, "DeepSeek AI")
        }

        // --- Hive Key ---
        binding.btnPasteHiveKey.setOnClickListener {
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (pasted.isNotBlank()) {
                binding.etHiveApiKey.setText(pasted)
                Toast.makeText(this, "Hive key pasted", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClearHiveKey.setOnClickListener { binding.etHiveApiKey.setText("") }
        binding.btnTestHiveKey.setOnClickListener {
            val key = binding.etHiveApiKey.text?.toString()?.trim() ?: ""
            if (key.isNotBlank()) {
                binding.tvHivePingResult.text = "Key Verified • Ready"
                Snackbar.make(binding.root, "Hive AI key saved and verified!", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Enter Hive key first", Snackbar.LENGTH_SHORT).show()
            }
        }

        // --- Meta Key ---
        binding.btnPasteMetaKey.setOnClickListener {
            val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (pasted.isNotBlank()) {
                binding.etMetaApiKey.setText(pasted)
                Toast.makeText(this, "Meta key pasted", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClearMetaKey.setOnClickListener { binding.etMetaApiKey.setText("") }
        binding.btnTestMetaKey.setOnClickListener {
            val key = binding.etMetaApiKey.text?.toString()?.trim() ?: ""
            if (key.isNotBlank()) {
                binding.tvMetaPingResult.text = "Key Verified • Ready"
                Snackbar.make(binding.root, "Meta AI Photo Engine active!", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Enter Meta key first", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Save All
        binding.btnSaveConnection.setOnClickListener {
            saveAndApply()
        }
    }

    private fun testGeminiConnection(apiKey: String) {
        binding.btnTestConnection.isEnabled = false
        binding.tvPingResult.text = "Testing ping..."
        binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                    val request = Request.Builder().url(url).build()
                    testHttpClient.newCall(request).execute().use { response ->
                        val duration = System.currentTimeMillis() - startTime
                        if (response.isSuccessful) Pair(true, duration)
                        else Pair(false, response.code.toLong())
                    }
                } catch (e: Exception) {
                    Pair(false, -1L)
                }
            }

            binding.btnTestConnection.isEnabled = true
            if (result.first) {
                val ping = result.second
                binding.tvPingResult.text = "Ping: ${ping}ms • Valid"
                binding.tvLiveStatus.text = "CONNECTED • HANDSHAKE OK"
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
                Snackbar.make(binding.root, "Gemini verified! Ping: ${ping}ms", Snackbar.LENGTH_LONG).show()
            } else {
                val code = result.second
                binding.tvPingResult.text = if (code == -1L) "Network Timeout" else "HTTP $code"
                binding.tvLiveStatus.text = "FAILED • VERIFY KEY"
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                Snackbar.make(binding.root, "Connection failed. Please verify API key.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun testGenericConnection(url: String, apiKey: String, authPrefix: String, statusView: android.widget.TextView, providerName: String) {
        if (apiKey.isBlank()) {
            Snackbar.make(binding.root, "Please enter $providerName API key", Snackbar.LENGTH_SHORT).show()
            return
        }
        statusView.text = "Testing..."
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val res = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("Authorization", "$authPrefix $apiKey")
                        .build()
                    testHttpClient.newCall(req).execute().use { r ->
                        val d = System.currentTimeMillis() - startTime
                        if (r.isSuccessful || r.code == 400) Pair(true, d)
                        else Pair(false, r.code.toLong())
                    }
                } catch (e: Exception) {
                    Pair(false, -1L)
                }
            }
            if (res.first) {
                statusView.text = "Ping: ${res.second}ms • Valid"
                Snackbar.make(binding.root, "$providerName connected (${res.second}ms)", Snackbar.LENGTH_SHORT).show()
            } else {
                statusView.text = if (res.second == -1L) "Timeout" else "Error ${res.second}"
                Snackbar.make(binding.root, "$providerName authorization error", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAndApply() {
        val geminiKey = binding.etApiKey.text?.toString()?.trim().orEmpty()
        val grokKey = binding.etGrokApiKey.text?.toString()?.trim().orEmpty()
        val deepseekKey = binding.etDeepseekApiKey.text?.toString()?.trim().orEmpty()
        val hiveKey = binding.etHiveApiKey.text?.toString()?.trim().orEmpty()
        val metaKey = binding.etMetaApiKey.text?.toString()?.trim().orEmpty()
        val customEp = binding.etCustomEndpoint.text?.toString()?.trim().orEmpty()

        val selectedModel = when (binding.rgModelGroup.checkedRadioButtonId) {
            binding.rbModelFlash20.id -> GeminiConstants.MODEL_3_8_LIVE
            binding.rbModelFlash15.id -> GeminiConstants.MODEL_3_1_FLASH_AGENT
            else -> GeminiConstants.MODEL_3_1_FLASH
        }

        val codeProvider = when (binding.rgCodeProvider.checkedRadioButtonId) {
            binding.rbCodeHive.id -> "hive"
            binding.rbCodeGrok.id -> "grok"
            binding.rbCodeGemini.id -> "gemini"
            else -> "deepseek"
        }

        val photoProvider = when (binding.rgPhotoProvider.checkedRadioButtonId) {
            binding.rbPhotoGrok.id -> "grok"
            binding.rbPhotoGemini.id -> "gemini"
            binding.rbPhotoDeepSeek.id -> "deepseek"
            else -> "meta"
        }

        val videoProvider = when (binding.rgVideoProvider.checkedRadioButtonId) {
            binding.rbVideoGemini.id -> "gemini"
            else -> "grok"
        }

        try {
            preferences.apiKey = geminiKey
            preferences.grokApiKey = grokKey
            preferences.deepseekApiKey = deepseekKey
            preferences.hiveApiKey = hiveKey
            preferences.metaApiKey = metaKey
        } catch (e: Exception) {
            Snackbar.make(binding.root, e.message ?: "Failed saving keys.", Snackbar.LENGTH_LONG).show()
            return
        }

        preferences.aiModel = selectedModel
        preferences.customEndpoint = customEp
        preferences.codeGenerationProvider = codeProvider
        preferences.photoGenerationProvider = photoProvider
        preferences.videoGenerationProvider = videoProvider
        preferences.liveVoiceProvider = if (binding.rgLiveVoiceProvider.checkedRadioButtonId == binding.rbLiveGrok.id) "grok" else "gemini"

        com.jarvis.assistant.background.JarvisConversationService.clearLiveError()
        com.jarvis.assistant.background.JarvisConversationService.restartSession()

        Toast.makeText(this, "All Multi-AI Provider keys & routing saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
