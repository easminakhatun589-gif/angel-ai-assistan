package com.jarvis.assistant.ui.settings

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityVoiceOrbSettingsBinding
import kotlinx.coroutines.*

class VoiceOrbSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceOrbSettingsBinding
    private val preferences by lazy { JarvisApp.instance.preferences }
    private var isPreviewLoaded = false

    private val shapes = arrayOf("Sphere", "Energy Orb", "Holographic Ring", "Audio Wave")
    private val colorThemes = arrayOf("Blue", "Purple", "Cyan", "Pink", "Green", "Custom")
    private val animStyles = arrayOf("Smooth", "Dynamic", "Pulsing", "Minimal")
    private val glowIntensities = arrayOf("Low", "Medium", "High")
    private val particleDensities = arrayOf("Off", "Low", "Medium", "High")
    private val orbSizes = arrayOf("Small", "Medium", "Large")
    private val backgroundStyles = arrayOf("Transparent", "Dark Gradient", "Subtle Glow")
    private val micResponses = arrayOf("Low", "Medium", "High")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        com.jarvis.assistant.util.ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceOrbSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)

        setupSpinners()
        loadPreferencesToUI()
        setupPreviewWebView()
        setupClickListeners()
    }

    private fun setupSpinners() {
        binding.spinnerShape.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shapes)
        binding.spinnerColorTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colorThemes)
        binding.spinnerAnimationStyle.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, animStyles)
        binding.spinnerGlowIntensity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, glowIntensities)
        binding.spinnerParticleDensity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, particleDensities)
        binding.spinnerOrbSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, orbSizes)
        binding.spinnerBackground.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, backgroundStyles)
        binding.spinnerMicResponse.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, micResponses)
    }

    private fun loadPreferencesToUI() {
        // Visibility
        when (preferences.orbVisibilityMode) {
            "always" -> binding.rbVisAlways.isChecked = true
            "ai_only" -> binding.rbVisAiOnly.isChecked = true
            "disabled" -> binding.rbVisDisabled.isChecked = true
            else -> binding.rbVisSpeakOrAi.isChecked = true
        }

        // Spinners
        binding.spinnerShape.setSelection(shapes.indexOfFirst { it.equals(preferences.orbShape, true) }.let { if (it >= 0) it else 0 })
        binding.spinnerColorTheme.setSelection(colorThemes.indexOfFirst { it.equals(preferences.orbColorTheme, true) }.let { if (it >= 0) it else 0 })
        binding.spinnerAnimationStyle.setSelection(animStyles.indexOfFirst { it.equals(preferences.orbAnimationStyle, true) }.let { if (it >= 0) it else 0 })
        binding.spinnerGlowIntensity.setSelection(glowIntensities.indexOfFirst { it.equals(preferences.orbGlowIntensity, true) }.let { if (it >= 0) it else 1 })
        binding.spinnerParticleDensity.setSelection(particleDensities.indexOfFirst { it.equals(preferences.orbParticleDensity, true) }.let { if (it >= 0) it else 2 })
        binding.spinnerOrbSize.setSelection(orbSizes.indexOfFirst { it.equals(preferences.orbSize, true) }.let { if (it >= 0) it else 1 })
        binding.spinnerBackground.setSelection(backgroundStyles.indexOfFirst { it.equals(preferences.orbBackground, true) }.let { if (it >= 0) it else 0 })
        binding.spinnerMicResponse.setSelection(micResponses.indexOfFirst { it.equals(preferences.orbMicResponse, true) }.let { if (it >= 0) it else 1 })

        // Behavior Switches
        binding.switchSpeechReactive.isChecked = preferences.orbSpeechReactive
        binding.switchAiPulse.isChecked = preferences.orbAiPulse
        binding.switchThinkingAnim.isChecked = preferences.orbThinkingAnimation
        binding.switchSmoothTransitions.isChecked = preferences.orbSmoothTransitions
        binding.switchTouchRipple.isChecked = preferences.orbTouchRipple
        binding.switchReducedMotion.isChecked = preferences.orbReducedMotion
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupPreviewWebView() {
        binding.orbPreviewWebView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPreviewLoaded = true
                    applySettingsToPreview()
                }
            }

            loadUrl("file:///android_asset/orb.html?embedded=1")
        }
    }

    private fun applySettingsToPreview() {
        if (!isPreviewLoaded) return
        val themeMap = when (binding.spinnerColorTheme.selectedItem.toString().lowercase()) {
            "purple" -> "red"
            "cyan" -> "dark"
            "pink" -> "sunset"
            "green" -> "dark"
            else -> "light"
        }
        binding.orbPreviewWebView.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setTheme('$themeMap');", null)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Voice Orb settings saved successfully.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnResetDefault.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Reset to Default")
                .setMessage("Are you sure you want to reset all Voice Orb & Animation settings to default?")
                .setPositiveButton("Reset") { _, _ ->
                    preferences.resetOrbSettingsToDefault()
                    loadPreferencesToUI()
                    applySettingsToPreview()
                    Toast.makeText(this, "Reset to default settings.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnRestoreLast.setOnClickListener {
            loadPreferencesToUI()
            applySettingsToPreview()
            Toast.makeText(this, "Restored last saved settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        preferences.orbVisibilityMode = when (binding.rgVisibility.checkedRadioButtonId) {
            R.id.rbVisAlways -> "always"
            R.id.rbVisAiOnly -> "ai_only"
            R.id.rbVisDisabled -> "disabled"
            else -> "speak_or_ai"
        }
        preferences.orbShape = binding.spinnerShape.selectedItem.toString()
        preferences.orbColorTheme = binding.spinnerColorTheme.selectedItem.toString()
        preferences.orbAnimationStyle = binding.spinnerAnimationStyle.selectedItem.toString()
        preferences.orbGlowIntensity = binding.spinnerGlowIntensity.selectedItem.toString()
        preferences.orbParticleDensity = binding.spinnerParticleDensity.selectedItem.toString()
        preferences.orbSize = binding.spinnerOrbSize.selectedItem.toString()
        preferences.orbBackground = binding.spinnerBackground.selectedItem.toString()
        preferences.orbSpeechReactive = binding.switchSpeechReactive.isChecked
        preferences.orbMicResponse = binding.spinnerMicResponse.selectedItem.toString()
        preferences.orbAiPulse = binding.switchAiPulse.isChecked
        preferences.orbThinkingAnimation = binding.switchThinkingAnim.isChecked
        preferences.orbSmoothTransitions = binding.switchSmoothTransitions.isChecked
        preferences.orbTouchRipple = binding.switchTouchRipple.isChecked
        preferences.orbReducedMotion = binding.switchReducedMotion.isChecked

        applySettingsToPreview()
    }
}
