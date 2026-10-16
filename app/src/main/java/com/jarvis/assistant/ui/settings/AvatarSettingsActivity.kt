package com.jarvis.assistant.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityAvatarSettingsBinding
import com.jarvis.assistant.ui.avatar.AvatarHelper
import com.jarvis.assistant.ui.avatar.FullScreenAvatarActivity
import com.jarvis.assistant.util.ThemeManager
import java.io.File
import java.io.FileOutputStream

class AvatarSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvatarSettingsBinding
    private val preferences get() = JarvisApp.instance.preferences
    private var avatarHelper: AvatarHelper? = null

    // Staging changes
    private var stagedModel: String = "angel_human"
    private var stagedHairStyle: String = "sleek"
    private var stagedHairColor: String = "#d4af37"
    private var stagedEyeColor: String = "#00d4ff"
    private var stagedSkinTone: String = "fair"
    private var stagedClothingStyle: String = "cyber_suit"
    private var stagedClothingColor: String = "#0f172a"
    private var stagedEnvironment: String = "cyber_studio"
    private var stagedQuality: String = "high"
    private var stagedDisplayMode: String = "home_stage"
    private var stagedSpeechSpeed: Float = 1.0f
    private var stagedSelectedVoice: String = "Puck"

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleCustomImageSelected(it) }
    }

    private val pickModelLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleCustom3DModelSelected(it) }
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleCustomVideoSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.avatarSettingsRoot)

        binding.btnBack.setOnClickListener { finish() }

        loadInitialPreferences()
        setupAvatarPreview()
        setupAppearanceControls()
        setupDisplayControls()
        setupVoiceAndExpressionControls()
        setupCustomImageControls()
        setupCustom3DControls()
        setupCustomVideoControls()
        setup3DAnimationControls()
        setupActionButtons()
    }

    private fun loadInitialPreferences() {
        stagedModel = preferences.avatarModel
        stagedHairStyle = preferences.avatarHairStyle
        stagedHairColor = preferences.avatarHairColor
        stagedEyeColor = preferences.avatarEyeColor
        stagedSkinTone = preferences.avatarSkinTone
        stagedClothingStyle = preferences.avatarClothingStyle
        stagedClothingColor = preferences.avatarClothingColor
        stagedEnvironment = preferences.avatarEnvironment
        stagedQuality = preferences.avatarGraphicsQuality
        stagedDisplayMode = preferences.avatarDisplayMode
        stagedSpeechSpeed = preferences.avatarSpeechSpeed
        stagedSelectedVoice = preferences.voice
    }

    private fun setupAvatarPreview() {
        avatarHelper = AvatarHelper(
            webView = binding.avatarPreviewWebView,
            preferences = preferences,
            onAvatarReady = {
                pushStagedAppearanceToPreview()
            }
        )

        binding.btnResetCamera.setOnClickListener {
            avatarHelper?.resetCamera()
        }

        binding.btnFullScreenPreview.setOnClickListener {
            startActivity(Intent(this, FullScreenAvatarActivity::class.java))
        }

        // Expression Buttons
        binding.btnExprNeutral.setOnClickListener {
            avatarHelper?.setState("idle")
            avatarHelper?.setExpression("neutral")
            binding.tvPreviewStateLabel.text = "EXPRESSION • NEUTRAL"
        }
        binding.btnExprSmile.setOnClickListener {
            avatarHelper?.setState("idle")
            avatarHelper?.setExpression("happy")
            binding.tvPreviewStateLabel.text = "EXPRESSION • SMILE"
        }
        binding.btnExprListening.setOnClickListener {
            avatarHelper?.setState("listening")
            avatarHelper?.setExpression("listening")
            binding.tvPreviewStateLabel.text = "EXPRESSION • LISTENING"
        }
        binding.btnExprThinking.setOnClickListener {
            avatarHelper?.setState("thinking")
            avatarHelper?.setExpression("thinking")
            binding.tvPreviewStateLabel.text = "EXPRESSION • THINKING"
        }
        binding.btnExprSpeaking.setOnClickListener {
            avatarHelper?.setState("speaking")
            avatarHelper?.setExpression("speaking")
            avatarHelper?.pushAudioLevel(0.85f, 0.4f, 0.6f)
            binding.tvPreviewStateLabel.text = "EXPRESSION • SPEAKING"
        }
    }

    private fun setupAppearanceControls() {
        // Model toggle
        when (stagedModel) {
            "angel_cyber" -> binding.toggleAvatarModel.check(R.id.btnModelCyber)
            "angel_nova" -> binding.toggleAvatarModel.check(R.id.btnModelNova)
            "custom_image" -> binding.toggleAvatarModel.check(R.id.btnModelCustomImage)
            "custom_3d" -> binding.toggleAvatarModel.check(R.id.btnModelImported3D)
            else -> binding.toggleAvatarModel.check(R.id.btnModelHuman)
        }

        binding.toggleAvatarModel.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            stagedModel = when (checkedId) {
                R.id.btnModelCyber -> "angel_cyber"
                R.id.btnModelNova -> "angel_nova"
                R.id.btnModelCustomImage -> "custom_image"
                R.id.btnModelImported3D -> "custom_3d"
                else -> "angel_human"
            }
            pushStagedAppearanceToPreview()
        }

        // Hairstyle toggle
        when (stagedHairStyle) {
            "ponytail" -> binding.toggleHairStyle.check(R.id.btnHairPonytail)
            "cyber_crop" -> binding.toggleHairStyle.check(R.id.btnHairCrop)
            "wavy" -> binding.toggleHairStyle.check(R.id.btnHairWavy)
            else -> binding.toggleHairStyle.check(R.id.btnHairSleek)
        }

        binding.toggleHairStyle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            stagedHairStyle = when (checkedId) {
                R.id.btnHairPonytail -> "ponytail"
                R.id.btnHairCrop -> "cyber_crop"
                R.id.btnHairWavy -> "wavy"
                else -> "sleek"
            }
            pushStagedAppearanceToPreview()
        }

        // Hair color swatches
        binding.swatchHairGold.setOnClickListener { stagedHairColor = "#D4AF37"; pushStagedAppearanceToPreview() }
        binding.swatchHairBlack.setOnClickListener { stagedHairColor = "#111827"; pushStagedAppearanceToPreview() }
        binding.swatchHairCyan.setOnClickListener { stagedHairColor = "#38BDF8"; pushStagedAppearanceToPreview() }
        binding.swatchHairAuburn.setOnClickListener { stagedHairColor = "#B45309"; pushStagedAppearanceToPreview() }
        binding.swatchHairSilver.setOnClickListener { stagedHairColor = "#E2E8F0"; pushStagedAppearanceToPreview() }

        // Eye color swatches
        binding.swatchEyeCyan.setOnClickListener { stagedEyeColor = "#00D4FF"; pushStagedAppearanceToPreview() }
        binding.swatchEyeGreen.setOnClickListener { stagedEyeColor = "#10B981"; pushStagedAppearanceToPreview() }
        binding.swatchEyePurple.setOnClickListener { stagedEyeColor = "#8B5CF6"; pushStagedAppearanceToPreview() }
        binding.swatchEyeHazel.setOnClickListener { stagedEyeColor = "#F59E0B"; pushStagedAppearanceToPreview() }
        binding.swatchEyeBlue.setOnClickListener { stagedEyeColor = "#3B82F6"; pushStagedAppearanceToPreview() }

        // Skin tone swatches
        binding.swatchSkinFair.setOnClickListener { stagedSkinTone = "fair"; pushStagedAppearanceToPreview() }
        binding.swatchSkinOlive.setOnClickListener { stagedSkinTone = "olive"; pushStagedAppearanceToPreview() }
        binding.swatchSkinBronze.setOnClickListener { stagedSkinTone = "bronze"; pushStagedAppearanceToPreview() }
        binding.swatchSkinPorcelain.setOnClickListener { stagedSkinTone = "porcelain"; pushStagedAppearanceToPreview() }
        binding.swatchSkinDeep.setOnClickListener { stagedSkinTone = "deep"; pushStagedAppearanceToPreview() }

        // Clothing style toggle
        when (stagedClothingStyle) {
            "formal_blazer" -> binding.toggleClothingStyle.check(R.id.btnClothBlazer)
            "casual_hoodie" -> binding.toggleClothingStyle.check(R.id.btnClothHoodie)
            "futuristic_tunic" -> binding.toggleClothingStyle.check(R.id.btnClothTunic)
            else -> binding.toggleClothingStyle.check(R.id.btnClothCyberSuit)
        }

        binding.toggleClothingStyle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            stagedClothingStyle = when (checkedId) {
                R.id.btnClothBlazer -> "formal_blazer"
                R.id.btnClothHoodie -> "casual_hoodie"
                R.id.btnClothTunic -> "futuristic_tunic"
                else -> "cyber_suit"
            }
            pushStagedAppearanceToPreview()
        }

        // Environment toggle
        when (stagedEnvironment) {
            "sunset" -> binding.toggleEnvironment.check(R.id.btnEnvSunset)
            "nebula" -> binding.toggleEnvironment.check(R.id.btnEnvNebula)
            "minimal_dark" -> binding.toggleEnvironment.check(R.id.btnEnvDark)
            "transparent" -> binding.toggleEnvironment.check(R.id.btnEnvTransparent)
            else -> binding.toggleEnvironment.check(R.id.btnEnvCyberStudio)
        }

        binding.toggleEnvironment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            stagedEnvironment = when (checkedId) {
                R.id.btnEnvSunset -> "sunset"
                R.id.btnEnvNebula -> "nebula"
                R.id.btnEnvDark -> "minimal_dark"
                R.id.btnEnvTransparent -> "transparent"
                else -> "cyber_studio"
            }
            pushStagedAppearanceToPreview()
        }
    }

    private fun setupDisplayControls() {
        // Display mode
        when (stagedDisplayMode) {
            "fullscreen" -> binding.rbDisplayFullScreen.isChecked = true
            "compact_floating" -> binding.rbDisplayFloating.isChecked = true
            "orb" -> binding.rbDisplayOrb.isChecked = true
            else -> binding.rbDisplayHomeStage.isChecked = true
        }

        binding.rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            stagedDisplayMode = when (checkedId) {
                R.id.rbDisplayFullScreen -> "fullscreen"
                R.id.rbDisplayFloating -> "compact_floating"
                R.id.rbDisplayOrb -> "orb"
                else -> "home_stage"
            }
        }

        // Quality toggle
        when (stagedQuality) {
            "low" -> binding.toggleQuality.check(R.id.btnQualityLow)
            "medium" -> binding.toggleQuality.check(R.id.btnQualityMedium)
            else -> binding.toggleQuality.check(R.id.btnQualityHigh)
        }

        binding.toggleQuality.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            stagedQuality = when (checkedId) {
                R.id.btnQualityLow -> "low"
                R.id.btnQualityMedium -> "medium"
                else -> "high"
            }
            avatarHelper?.setQuality(stagedQuality)
        }

        binding.switchAnimations.isChecked = preferences.isAvatarAnimationsEnabled
        binding.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            pushStagedAppearanceToPreview()
        }

        binding.switchVfx.isChecked = preferences.isAvatarVfxEnabled
        binding.switchVfx.setOnCheckedChangeListener { _, isChecked ->
            pushStagedAppearanceToPreview()
        }
    }

    private fun setupVoiceAndExpressionControls() {
        val voices = listOf("Puck", "Charon", "Aoede", "Fenrir", "Kore", "Leda")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, voices)
        binding.spinnerVoice.adapter = adapter
        val idx = voices.indexOf(preferences.voice).coerceAtLeast(0)
        binding.spinnerVoice.setSelection(idx)

        binding.tvSpeechSpeedValue.text = String.format(java.util.Locale.US, "%.1fx", stagedSpeechSpeed)
        binding.sliderSpeechSpeed.value = stagedSpeechSpeed
        binding.sliderSpeechSpeed.addOnChangeListener { _, value, _ ->
            stagedSpeechSpeed = value
            binding.tvSpeechSpeedValue.text = String.format(java.util.Locale.US, "%.1fx", value)
        }

        binding.switchLipSync.isChecked = preferences.isAvatarLipSyncEnabled
        binding.switchIdleAnim.isChecked = preferences.isAvatarIdleAnimEnabled
    }

    private fun setupCustomImageControls() {
        val existingPath = preferences.avatarCustomImagePath
        if (!existingPath.isNullOrBlank()) {
            val file = File(existingPath)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                binding.ivCustomImageThumbnail.setImageBitmap(bmp)
            }
        }

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnClearImage.setOnClickListener {
            preferences.avatarCustomImagePath = null
            binding.ivCustomImageThumbnail.setImageResource(R.drawable.ic_nav_chat)
            stagedModel = "angel_human"
            binding.toggleAvatarModel.check(R.id.btnModelHuman)
            pushStagedAppearanceToPreview()
            Snackbar.make(binding.root, "Custom portrait removed.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun handleCustomImageSelected(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val targetFile = File(filesDir, "avatar_custom_portrait.png")
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }

                if (targetFile.exists() && targetFile.length() > 0) {
                    val bmp = BitmapFactory.decodeFile(targetFile.absolutePath)
                    binding.ivCustomImageThumbnail.setImageBitmap(bmp)
                    preferences.avatarCustomImagePath = targetFile.absolutePath
                    stagedModel = "custom_image"
                    binding.toggleAvatarModel.check(R.id.btnModelCustomImage)
                    avatarHelper?.loadCustomImageFromFile(targetFile)
                    Snackbar.make(binding.root, "Custom portrait applied successfully!", Snackbar.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustom3DControls() {
        binding.btnImport3DModel.setOnClickListener {
            pickModelLauncher.launch("*/*")
        }

        binding.btnReset3DModel.setOnClickListener {
            preferences.avatarCustom3DPath = null
            stagedModel = "angel_human"
            binding.toggleAvatarModel.check(R.id.btnModelHuman)
            pushStagedAppearanceToPreview()
            Snackbar.make(binding.root, "Custom 3D model reset to default.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun handleCustom3DModelSelected(uri: Uri) {
        try {
            val fileName = uri.lastPathSegment ?: "model.glb"
            if (!fileName.endsWith(".glb", ignoreCase = true) && !fileName.endsWith(".gltf", ignoreCase = true)) {
                Toast.makeText(this, "Please select a valid .glb or .gltf 3D model.", Toast.LENGTH_LONG).show()
                return
            }

            contentResolver.openInputStream(uri)?.use { input ->
                val targetFile = File(filesDir, "custom_avatar.glb")
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }

                if (targetFile.exists() && targetFile.length() > 0) {
                    preferences.avatarCustom3DPath = targetFile.absolutePath
                    stagedModel = "custom_3d"
                    binding.toggleAvatarModel.check(R.id.btnModelImported3D)
                    Snackbar.make(binding.root, "3D Model imported successfully!", Snackbar.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to import 3D model: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomVideoControls() {
        val existingVideo = preferences.avatarCustomVideoPath
        if (!existingVideo.isNullOrBlank()) {
            val file = File(existingVideo)
            if (file.exists()) {
                binding.tvVideoStatus.text = "Loaded: ${file.name}"
            }
        }

        binding.btnPickVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        binding.btnClearVideo.setOnClickListener {
            preferences.avatarCustomVideoPath = null
            binding.tvVideoStatus.text = "No video selected"
            Snackbar.make(binding.root, "Custom video avatar cleared.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun handleCustomVideoSelected(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val targetFile = File(filesDir, "avatar_custom_loop.mp4")
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }

                if (targetFile.exists() && targetFile.length() > 0) {
                    preferences.avatarCustomVideoPath = targetFile.absolutePath
                    binding.tvVideoStatus.text = "Loaded: ${targetFile.name}"
                    avatarHelper?.playAnimation("dance")
                    Snackbar.make(binding.root, "Custom video avatar/loop applied!", Snackbar.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setup3DAnimationControls() {
        binding.btnAnimDance.setOnClickListener {
            avatarHelper?.playAnimation("dance")
            binding.tvPreviewStateLabel.text = "3D ANIMATION • DANCE GROOVE"
            Snackbar.make(binding.root, "Playing 3D Dance Motion", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnAnimCombat.setOnClickListener {
            avatarHelper?.playAnimation("combat")
            binding.tvPreviewStateLabel.text = "3D ANIMATION • COMBAT STANCE"
            Snackbar.make(binding.root, "Activating 3D Combat Stance", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnAnimOrbit.setOnClickListener {
            avatarHelper?.playAnimation("orbit")
            binding.tvPreviewStateLabel.text = "3D ANIMATION • HOLOGRAM SPIN"
            Snackbar.make(binding.root, "Triggering 3D Holographic Spin", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnAnimVictory.setOnClickListener {
            avatarHelper?.playAnimation("victory")
            binding.tvPreviewStateLabel.text = "3D ANIMATION • VICTORY SALUTE"
            Snackbar.make(binding.root, "Executing 3D Victory Salute", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnAnimBow.setOnClickListener {
            avatarHelper?.playAnimation("bow")
            binding.tvPreviewStateLabel.text = "3D ANIMATION • CYBER BOW"
            Snackbar.make(binding.root, "Executing 3D Cyber Bow", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnAnimQuantum.setOnClickListener {
            avatarHelper?.playAnimation("quantum")
            binding.tvPreviewStateLabel.text = "3D ANIMATION • QUANTUM PULSE"
            Snackbar.make(binding.root, "Activating 3D Quantum Pulse", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons() {
        binding.btnSaveChanges.setOnClickListener {
            saveChanges()
        }

        binding.btnResetDefault.setOnClickListener {
            resetDefaults()
        }

        binding.btnCancelChanges.setOnClickListener {
            finish()
        }
    }

    private fun saveChanges() {
        preferences.avatarModel = stagedModel
        preferences.avatarHairStyle = stagedHairStyle
        preferences.avatarHairColor = stagedHairColor
        preferences.avatarEyeColor = stagedEyeColor
        preferences.avatarSkinTone = stagedSkinTone
        preferences.avatarClothingStyle = stagedClothingStyle
        preferences.avatarClothingColor = stagedClothingColor
        preferences.avatarEnvironment = stagedEnvironment
        preferences.avatarGraphicsQuality = stagedQuality
        preferences.avatarDisplayMode = stagedDisplayMode
        preferences.avatarSpeechSpeed = stagedSpeechSpeed
        preferences.isAvatarAnimationsEnabled = binding.switchAnimations.isChecked
        preferences.isAvatarVfxEnabled = binding.switchVfx.isChecked
        preferences.isAvatarLipSyncEnabled = binding.switchLipSync.isChecked
        preferences.isAvatarIdleAnimEnabled = binding.switchIdleAnim.isChecked

        val selectedVoice = binding.spinnerVoice.selectedItem?.toString()
        if (!selectedVoice.isNullOrBlank()) {
            preferences.voice = selectedVoice
        }

        Toast.makeText(this, "Avatar settings saved successfully!", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun resetDefaults() {
        stagedModel = "angel_human"
        stagedHairStyle = "sleek"
        stagedHairColor = "#D4AF37"
        stagedEyeColor = "#00D4FF"
        stagedSkinTone = "fair"
        stagedClothingStyle = "cyber_suit"
        stagedClothingColor = "#0F172A"
        stagedEnvironment = "cyber_studio"
        stagedQuality = "high"
        stagedDisplayMode = "home_stage"
        stagedSpeechSpeed = 1.0f

        binding.toggleAvatarModel.check(R.id.btnModelHuman)
        binding.toggleHairStyle.check(R.id.btnHairSleek)
        binding.toggleClothingStyle.check(R.id.btnClothCyberSuit)
        binding.toggleEnvironment.check(R.id.btnEnvCyberStudio)
        binding.toggleQuality.check(R.id.btnQualityHigh)
        binding.rbDisplayHomeStage.isChecked = true
        binding.switchAnimations.isChecked = true
        binding.switchVfx.isChecked = true
        binding.switchLipSync.isChecked = true
        binding.switchIdleAnim.isChecked = true
        binding.sliderSpeechSpeed.value = 1.0f

        pushStagedAppearanceToPreview()
        Snackbar.make(binding.root, "Reset to default avatar settings.", Snackbar.LENGTH_SHORT).show()
    }

    private fun pushStagedAppearanceToPreview() {
        val customImagePath = preferences.avatarCustomImagePath
        if (stagedModel == "custom_image" && !customImagePath.isNullOrBlank()) {
            val file = File(customImagePath)
            if (file.exists()) {
                avatarHelper?.loadCustomImageFromFile(file)
                return
            }
        }

        try {
            val cfg = org.json.JSONObject().apply {
                put("model", stagedModel)
                put("gender", preferences.avatarGender)
                put("hairStyle", stagedHairStyle)
                put("hairColor", parseHex(stagedHairColor, 0xd4af37))
                put("eyeColor", parseHex(stagedEyeColor, 0x00d4ff))
                put("skinTone", stagedSkinTone)
                put("clothingStyle", stagedClothingStyle)
                put("clothingColor", parseHex(stagedClothingColor, 0x0f172a))
                put("clothingAccent", parseHex(preferences.appTheme, 0x00d4ff))
                put("environment", stagedEnvironment)
                put("vfxEnabled", binding.switchVfx.isChecked)
                put("quality", stagedQuality)
                put("animationsEnabled", binding.switchAnimations.isChecked)
                put("idleAnimEnabled", binding.switchIdleAnim.isChecked)
                put("lipSyncEnabled", binding.switchLipSync.isChecked)
            }

            binding.avatarPreviewWebView.post {
                binding.avatarPreviewWebView.evaluateJavascript(
                    "window.AngelAvatar && window.AngelAvatar.setAppearance($cfg);",
                    null
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun parseHex(hex: String, fallback: Int): Int {
        return try {
            hex.replace("#", "").trim().toInt(16)
        } catch (e: Exception) {
            fallback
        }
    }

    override fun onResume() {
        super.onResume()
        avatarHelper?.onResume()
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
