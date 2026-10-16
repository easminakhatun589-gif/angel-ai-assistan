package com.jarvis.assistant.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jarvis.assistant.data.model.ChatTurn
import com.jarvis.assistant.data.model.GeminiConstants

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val secureApiPrefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            ENCRYPTED_API_PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Secure API-key storage could not be initialized", e)
        null
    }
    private val gson = Gson()

    init {
        migrateApiKeyToEncryptedStorage()
        val current = prefs.getString(KEY_MODEL, null)
        if (current == null || current !in GeminiConstants.SUPPORTED_MODELS) {
            prefs.edit().putString(KEY_MODEL, GeminiConstants.DEFAULT_MODEL).apply()
        }
    }

    companion object {
        private const val PREF_NAME = "jarvis_prefs"
        private const val ENCRYPTED_API_PREF_NAME = "jarvis_secure_api_prefs"
        private const val TAG = "AppPreferences"
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_MODEL = "key_model"
        private const val KEY_VOICE = "key_voice"
        private const val KEY_PERSONALITY = "key_personality"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_MIC_MUTED = "key_mic_muted"
        private const val KEY_CHAT_HISTORY = "key_chat_history"
        private const val KEY_FAVORITE_SONG = "key_favorite_song"
        private const val KEY_FAVORITE_APP = "key_favorite_app"
        private const val KEY_SONG_COUNTS = "key_song_counts"
        private const val KEY_USER_BIRTHDAY = "key_user_birthday"
        private const val KEY_USER_LOCATION = "key_user_location"
        private const val KEY_USER_AGE = "key_user_age"
        private const val KEY_DAILY_ROUTINE = "key_daily_routine"
        private const val KEY_OWNER_VOICE_ENABLED = "key_owner_voice_enabled"
        private const val KEY_OWNER_VOICE_PROFILE = "key_owner_voice_profile"
        private const val KEY_OWNER_VOICE_SAMPLES = "key_owner_voice_samples"
        private const val KEY_OWNER_VOICE_THRESHOLD = "key_owner_voice_threshold"
        private const val KEY_OWNER_VOICE_SAMPLE_COUNT = "key_owner_voice_sample_count"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_SILENT_EXECUTION_MODE = "key_silent_execution_mode"
        private const val KEY_TASK_OVERLAY_ENABLED = "key_task_overlay_enabled"
        private const val KEY_TASK_OVERLAY_AVATAR = "key_task_overlay_avatar"
        private const val KEY_TASK_OVERLAY_MODE = "key_task_overlay_mode"
        private const val KEY_TASK_OVERLAY_EDGE = "key_task_overlay_edge"
        private const val KEY_TASK_OVERLAY_FREE_POSITION = "key_task_overlay_free_position"
        private const val KEY_TASK_OVERLAY_X = "key_task_overlay_x"
        private const val KEY_TASK_OVERLAY_Y = "key_task_overlay_y"
        private const val KEY_APP_THEME = "key_app_theme"
        private const val KEY_CUSTOM_THEME_COLOR = "key_custom_theme_color"
        private const val KEY_ORB_ANIMATION = "key_orb_animation"
        private const val KEY_ORB_SPEED = "key_orb_speed"
        private const val KEY_ORB_GLOW = "key_orb_glow"
        private const val KEY_ORB_SCALE = "key_orb_scale"
        private const val KEY_ORB_HUE = "key_orb_hue"
        private const val KEY_CUSTOM_ENDPOINT = "key_custom_endpoint"
        private const val KEY_GROK_API_KEY = "key_grok_api_key"
        private const val KEY_DEEPSEEK_API_KEY = "key_deepseek_api_key"
        private const val KEY_HIVE_API_KEY = "key_hive_api_key"
        private const val KEY_META_API_KEY = "key_meta_api_key"
        private const val KEY_CODE_PROVIDER = "key_code_provider"
        private const val KEY_PHOTO_PROVIDER = "key_photo_provider"
        private const val KEY_VIDEO_PROVIDER = "key_video_provider"
        private const val KEY_LIVE_VOICE_PROVIDER = "key_live_voice_provider"

        // AI Call Assistant Preferences
        private const val KEY_CALL_ASSISTANT_ENABLED = "key_call_assistant_enabled"
        private const val KEY_WHATSAPP_CALL_ENABLED = "key_whatsapp_call_enabled"
        private const val KEY_SIM_CALL_ENABLED = "key_sim_call_enabled"
        private const val KEY_CALL_ASSISTANT_LANGUAGE = "key_call_assistant_language"
        private const val KEY_CALL_ASSISTANT_VOICE = "key_call_assistant_voice"
        private const val KEY_CALL_ASSISTANT_SPEED = "key_call_assistant_speed"
        private const val KEY_CALL_ASSISTANT_GREETING = "key_call_assistant_greeting"
        private const val KEY_CALL_ASSISTANT_INSTRUCTIONS = "key_call_assistant_instructions"
        private const val KEY_CALL_ASSISTANT_AUTO_ANSWER = "key_call_assistant_auto_answer"
        private const val KEY_CALL_ASSISTANT_MODE = "key_call_assistant_mode"
        private const val KEY_CALL_ASSISTANT_ALLOWLIST = "key_call_assistant_allowlist"
        private const val KEY_CALL_ASSISTANT_BLOCKED = "key_call_assistant_blocked"
        private const val KEY_CALL_ASSISTANT_MAX_DURATION = "key_call_assistant_max_duration"
        private const val KEY_CALL_ASSISTANT_PRIVACY_OPTIN = "key_call_assistant_privacy_optin"

        // Avatar System
        private const val KEY_AVATAR_MODEL = "key_avatar_model"
        private const val KEY_AVATAR_GENDER = "key_avatar_gender"
        private const val KEY_AVATAR_HAIR_STYLE = "key_avatar_hair_style"
        private const val KEY_AVATAR_HAIR_COLOR = "key_avatar_hair_color"
        private const val KEY_AVATAR_EYE_COLOR = "key_avatar_eye_color"
        private const val KEY_AVATAR_SKIN_TONE = "key_avatar_skin_tone"
        private const val KEY_AVATAR_CLOTHING_STYLE = "key_avatar_clothing_style"
        private const val KEY_AVATAR_CLOTHING_COLOR = "key_avatar_clothing_color"
        private const val KEY_AVATAR_ENVIRONMENT = "key_avatar_environment"
        private const val KEY_AVATAR_LIGHTING = "key_avatar_lighting"
        private const val KEY_AVATAR_VFX_ENABLED = "key_avatar_vfx_enabled"
        private const val KEY_AVATAR_DISPLAY_MODE = "key_avatar_display_mode"
        private const val KEY_AVATAR_GRAPHICS_QUALITY = "key_avatar_graphics_quality"
        private const val KEY_AVATAR_ANIMATIONS_ENABLED = "key_avatar_animations_enabled"
        private const val KEY_AVATAR_IDLE_ANIM_ENABLED = "key_avatar_idle_anim_enabled"
        private const val KEY_AVATAR_LIPSYNC_ENABLED = "key_avatar_lipsync_enabled"
        private const val KEY_AVATAR_DEFAULT_EXPRESSION = "key_avatar_default_expression"
        private const val KEY_AVATAR_SPEECH_SPEED = "key_avatar_speech_speed"
        private const val KEY_AVATAR_SPEECH_PITCH = "key_avatar_speech_pitch"
        private const val KEY_AVATAR_CUSTOM_IMAGE_PATH = "key_avatar_custom_image_path"
        private const val KEY_AVATAR_CUSTOM_3D_PATH = "key_avatar_custom_3d_path"
        private const val KEY_AVATAR_CUSTOM_VIDEO_PATH = "key_avatar_custom_video_path"

        // Voice Orb & Animation Preferences
        private const val KEY_ORB_VISIBILITY_MODE = "key_orb_visibility_mode"
        private const val KEY_ORB_SHAPE = "key_orb_shape"
        private const val KEY_ORB_COLOR_THEME = "key_orb_color_theme"
        private const val KEY_ORB_ANIM_STYLE = "key_orb_anim_style"
        private const val KEY_ORB_GLOW_INTENSITY = "key_orb_glow_intensity"
        private const val KEY_ORB_PARTICLE_DENSITY = "key_orb_particle_density"
        private const val KEY_ORB_SIZE = "key_orb_size"
        private const val KEY_ORB_BACKGROUND = "key_orb_background"
        private const val KEY_ORB_SPEECH_REACTIVE = "key_orb_speech_reactive"
        private const val KEY_ORB_MIC_RESPONSE = "key_orb_mic_response"
        private const val KEY_ORB_AI_PULSE = "key_orb_ai_pulse"
        private const val KEY_ORB_THINKING_ANIM = "key_orb_thinking_anim"
        private const val KEY_ORB_FADE_DELAY = "key_orb_fade_delay"
        private const val KEY_ORB_SMOOTH_TRANSITIONS = "key_orb_smooth_transitions"
        private const val KEY_ORB_TOUCH_RIPPLE = "key_orb_touch_ripple"
        private const val KEY_ORB_REDUCED_MOTION = "key_orb_reduced_motion"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_PHONE = "key_user_phone"
        private const val KEY_USER_COUNTRY = "key_user_country"
        private const val KEY_USER_PROFILE_PHOTO_PATH = "key_user_profile_photo_path"
        private const val KEY_LOCATION_PERMISSION_GRANTED = "key_location_permission_granted"
        private const val KEY_FCM_TOPIC_SUBSCRIBED = "key_fcm_topic_subscribed"
        private const val KEY_FCM_RETRY_COUNT = "key_fcm_retry_count"
        private const val KEY_FCM_LAST_ATTEMPT_TIME = "key_fcm_last_attempt_time"
        private const val KEY_FCM_LAST_ERROR = "key_fcm_last_error"
        private const val KEY_IS_GUEST_MODE = "key_is_guest_mode"
    }

    var isFcmTopicSubscribed: Boolean
        get() = prefs.getBoolean(KEY_FCM_TOPIC_SUBSCRIBED, false)
        set(value) { prefs.edit().putBoolean(KEY_FCM_TOPIC_SUBSCRIBED, value).commit() }

    var fcmRetryCount: Int
        get() = prefs.getInt(KEY_FCM_RETRY_COUNT, 0)
        set(value) { prefs.edit().putInt(KEY_FCM_RETRY_COUNT, value).commit() }

    var lastFcmAttemptTime: Long
        get() = prefs.getLong(KEY_FCM_LAST_ATTEMPT_TIME, 0L)
        set(value) { prefs.edit().putLong(KEY_FCM_LAST_ATTEMPT_TIME, value).commit() }

    var fcmLastError: String
        get() = prefs.getString(KEY_FCM_LAST_ERROR, "") ?: ""
        set(value) { prefs.edit().putString(KEY_FCM_LAST_ERROR, value).commit() }

    var isGuestMode: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST_MODE, false)
        set(value) { prefs.edit().putBoolean(KEY_IS_GUEST_MODE, value).commit() }

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userCountry: String
        get() = prefs.getString(KEY_USER_COUNTRY, "India") ?: "India"
        set(value) = prefs.edit().putString(KEY_USER_COUNTRY, value).apply()

    var userProfilePhotoPath: String?
        get() = prefs.getString(KEY_USER_PROFILE_PHOTO_PATH, null)
        set(value) = prefs.edit().putString(KEY_USER_PROFILE_PHOTO_PATH, value).apply()

    var isLocationPermissionGranted: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_PERMISSION_GRANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCATION_PERMISSION_GRANTED, value).apply()

    var isOwnerVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_OWNER_VOICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OWNER_VOICE_ENABLED, value).apply()

    var ownerVoiceThreshold: Float
        get() {
            val stored = prefs.getFloat(KEY_OWNER_VOICE_THRESHOLD, 0.70f)
            // Auto-migrate older builds that saved overly strict 0.50f threshold
            return if (stored < 0.60f) 0.70f else stored
        }
        set(value) = prefs.edit().putFloat(KEY_OWNER_VOICE_THRESHOLD, value).apply()

    var ownerVoiceSampleCount: Int
        get() = prefs.getInt(KEY_OWNER_VOICE_SAMPLE_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_OWNER_VOICE_SAMPLE_COUNT, value).apply()

    fun hasOwnerVoiceProfile(): Boolean {
        val str = prefs.getString(KEY_OWNER_VOICE_PROFILE, null)
        return !str.isNullOrBlank()
    }

    fun getOwnerVoiceProfile(): FloatArray? {
        val json = prefs.getString(KEY_OWNER_VOICE_PROFILE, null) ?: return null
        return try {
            val arr = org.json.JSONArray(json)
            FloatArray(arr.length()) { i -> arr.getDouble(i).toFloat() }
        } catch (e: Exception) {
            null
        }
    }

    fun getOwnerVoiceSamples(): List<FloatArray> {
        val json = prefs.getString(KEY_OWNER_VOICE_SAMPLES, null) ?: return emptyList()
        return try {
            val root = org.json.JSONArray(json)
            val list = mutableListOf<FloatArray>()
            for (i in 0 until root.length()) {
                val arr = root.getJSONArray(i)
                val vec = FloatArray(arr.length()) { j -> arr.getDouble(j).toFloat() }
                if (vec.isNotEmpty()) list.add(vec)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOwnerVoiceProfile(masterProfile: FloatArray, sampleVectors: List<FloatArray>) {
        val masterArr = org.json.JSONArray()
        for (v in masterProfile) {
            masterArr.put(v.toDouble())
        }

        val samplesArr = org.json.JSONArray()
        for (vec in sampleVectors) {
            val arr = org.json.JSONArray()
            for (v in vec) {
                arr.put(v.toDouble())
            }
            samplesArr.put(arr)
        }

        prefs.edit()
            .putString(KEY_OWNER_VOICE_PROFILE, masterArr.toString())
            .putString(KEY_OWNER_VOICE_SAMPLES, samplesArr.toString())
            .putInt(KEY_OWNER_VOICE_SAMPLE_COUNT, sampleVectors.size)
            .putBoolean(KEY_OWNER_VOICE_ENABLED, true)
            .apply()
    }

    fun saveOwnerVoiceProfile(profile: FloatArray, sampleCount: Int) {
        val arr = org.json.JSONArray()
        for (v in profile) {
            arr.put(v.toDouble())
        }
        prefs.edit()
            .putString(KEY_OWNER_VOICE_PROFILE, arr.toString())
            .putInt(KEY_OWNER_VOICE_SAMPLE_COUNT, sampleCount)
            .putBoolean(KEY_OWNER_VOICE_ENABLED, true)
            .apply()
    }

    fun clearOwnerVoiceProfile() {
        prefs.edit()
            .remove(KEY_OWNER_VOICE_PROFILE)
            .remove(KEY_OWNER_VOICE_SAMPLES)
            .remove(KEY_OWNER_VOICE_SAMPLE_COUNT)
            .putBoolean(KEY_OWNER_VOICE_ENABLED, false)
            .apply()
    }

    var apiKey: String
        get() {
            val savedKey = secureApiPrefs?.getString(KEY_API_KEY, "")?.trim().orEmpty()
            if (savedKey.isNotEmpty()) return savedKey
            return com.jarvis.assistant.BuildConfig.GEMINI_API_KEY.trim()
        }
        set(value) {
            val secure = checkNotNull(secureApiPrefs) { "Secure API-key storage is unavailable on this device." }
            val clean = value.trim()
            val saved = if (clean.isEmpty()) secure.edit().remove(KEY_API_KEY).commit()
            else secure.edit().putString(KEY_API_KEY, clean).commit()
            check(saved) { "Could not save API key to encrypted storage." }
        }

    var grokApiKey: String
        get() {
            val savedKey = secureApiPrefs?.getString(KEY_GROK_API_KEY, "")?.trim().orEmpty()
            if (savedKey.isNotEmpty()) return savedKey
            return com.jarvis.assistant.BuildConfig.GROK_API_KEY.trim()
        }
        set(value) {
            val secure = secureApiPrefs ?: prefs
            val clean = value.trim()
            if (clean.isEmpty()) secure.edit().remove(KEY_GROK_API_KEY).apply()
            else secure.edit().putString(KEY_GROK_API_KEY, clean).apply()
        }

    var deepseekApiKey: String
        get() {
            val savedKey = secureApiPrefs?.getString(KEY_DEEPSEEK_API_KEY, "")?.trim().orEmpty()
            if (savedKey.isNotEmpty()) return savedKey
            return com.jarvis.assistant.BuildConfig.DEEPSEEK_API_KEY.trim()
        }
        set(value) {
            val secure = secureApiPrefs ?: prefs
            val clean = value.trim()
            if (clean.isEmpty()) secure.edit().remove(KEY_DEEPSEEK_API_KEY).apply()
            else secure.edit().putString(KEY_DEEPSEEK_API_KEY, clean).apply()
        }

    var hiveApiKey: String
        get() {
            val savedKey = secureApiPrefs?.getString(KEY_HIVE_API_KEY, "")?.trim().orEmpty()
            if (savedKey.isNotEmpty()) return savedKey
            return com.jarvis.assistant.BuildConfig.HIVE_API_KEY.trim()
        }
        set(value) {
            val secure = secureApiPrefs ?: prefs
            val clean = value.trim()
            if (clean.isEmpty()) secure.edit().remove(KEY_HIVE_API_KEY).apply()
            else secure.edit().putString(KEY_HIVE_API_KEY, clean).apply()
        }

    var metaApiKey: String
        get() {
            val savedKey = secureApiPrefs?.getString(KEY_META_API_KEY, "")?.trim().orEmpty()
            if (savedKey.isNotEmpty()) return savedKey
            return com.jarvis.assistant.BuildConfig.META_API_KEY.trim()
        }
        set(value) {
            val secure = secureApiPrefs ?: prefs
            val clean = value.trim()
            if (clean.isEmpty()) secure.edit().remove(KEY_META_API_KEY).apply()
            else secure.edit().putString(KEY_META_API_KEY, clean).apply()
        }

    var codeGenerationProvider: String
        get() = prefs.getString(KEY_CODE_PROVIDER, "deepseek") ?: "deepseek"
        set(value) = prefs.edit().putString(KEY_CODE_PROVIDER, value).apply()

    var photoGenerationProvider: String
        get() = prefs.getString(KEY_PHOTO_PROVIDER, "meta") ?: "meta"
        set(value) = prefs.edit().putString(KEY_PHOTO_PROVIDER, value).apply()

    var videoGenerationProvider: String
        get() = prefs.getString(KEY_VIDEO_PROVIDER, "grok") ?: "grok"
        set(value) = prefs.edit().putString(KEY_VIDEO_PROVIDER, value).apply()

    val canStoreApiKeySecurely: Boolean get() = secureApiPrefs != null

    private fun migrateApiKeyToEncryptedStorage() {
        val legacyKey = prefs.getString(KEY_API_KEY, null)?.trim().orEmpty()
        if (legacyKey.isEmpty()) return
        val secure = secureApiPrefs ?: run {
            prefs.edit().remove(KEY_API_KEY).commit()
            return
        }
        try {
            val existing = secure.getString(KEY_API_KEY, null)
            if (existing.isNullOrEmpty()) {
                check(secure.edit().putString(KEY_API_KEY, legacyKey).commit())
            }
            if (!secure.getString(KEY_API_KEY, null).isNullOrEmpty()) prefs.edit().remove(KEY_API_KEY).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Legacy API-key migration to encrypted storage failed", e)
        }
    }

    var aiModel: String
        get() {
            val stored = prefs.getString(KEY_MODEL, GeminiConstants.DEFAULT_MODEL) ?: GeminiConstants.DEFAULT_MODEL
            if (stored !in GeminiConstants.SUPPORTED_MODELS) {
                prefs.edit().putString(KEY_MODEL, GeminiConstants.DEFAULT_MODEL).apply()
                return GeminiConstants.DEFAULT_MODEL
            }
            return stored
        }
        set(value) {
            val toSave = if (value in GeminiConstants.SUPPORTED_MODELS) value else GeminiConstants.DEFAULT_MODEL
            prefs.edit().putString(KEY_MODEL, toSave).apply()
        }

    var language: String
        get() {
            val stored = prefs.getString(KEY_LANGUAGE, GeminiConstants.DEFAULT_LANGUAGE) ?: GeminiConstants.DEFAULT_LANGUAGE
            return if (stored.contains("Auto", ignoreCase = true)) "Auto" else stored
        }
        set(value) {
            val clean = if (value.contains("Auto", ignoreCase = true)) "Auto" else value.trim()
            prefs.edit().putString(KEY_LANGUAGE, clean).apply()
        }

    var voice: String
        get() = prefs.getString(KEY_VOICE, GeminiConstants.DEFAULT_VOICE) ?: GeminiConstants.DEFAULT_VOICE
        set(value) = prefs.edit().putString(KEY_VOICE, value).apply()

    var personality: String
        get() = prefs.getString(KEY_PERSONALITY, GeminiConstants.PERSONALITY_ASSISTANT) ?: GeminiConstants.PERSONALITY_ASSISTANT
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var userName: String
        get() {
            val name = prefs.getString(KEY_USER_NAME, "") ?: ""
            return if (name.isBlank() || name == "Salaam" || name == "TechGPT X") "MD ASLAM" else name
        }
        set(value) {
            prefs.edit().putString(KEY_USER_NAME, value.trim()).commit()
        }

    var appTheme: String
        get() {
            val stored = prefs.getString(KEY_APP_THEME, "sunset") ?: "sunset"
            return when (stored) {
                "light", "pearl_white" -> "light"
                "dark", "midnight_onyx", "cyberpunk_neon", "emerald_obsidian" -> "dark"
                "red" -> "red"
                "sunset", "rose_gold" -> "sunset"
                "custom" -> "custom"
                else -> "light"
            }
        }
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var customThemeColor: String
        get() = prefs.getString(KEY_CUSTOM_THEME_COLOR, "#FFFFFF") ?: "#FFFFFF"
        set(value) = prefs.edit().putString(KEY_CUSTOM_THEME_COLOR, value.trim()).apply()

    var orbAnimation: String
        get() {
            val valid = listOf("holographic_plasma", "cosmic_pulsar", "quantum_core", "cyber_ring", "siri_glow")
            val stored = prefs.getString(KEY_ORB_ANIMATION, "holographic_plasma") ?: "holographic_plasma"
            return if (stored in valid) stored else "holographic_plasma"
        }
        set(value) = prefs.edit().putString(KEY_ORB_ANIMATION, value).apply()

    var orbSpeed: Float
        get() = prefs.getFloat(KEY_ORB_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ORB_SPEED, value).apply()

    var orbGlow: Float
        get() = prefs.getFloat(KEY_ORB_GLOW, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ORB_GLOW, value).apply()

    var orbScale: Float
        get() = prefs.getFloat(KEY_ORB_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ORB_SCALE, value).apply()

    var orbHue: Float
        get() = prefs.getFloat(KEY_ORB_HUE, -1f)
        set(value) = prefs.edit().putFloat(KEY_ORB_HUE, value).apply()

    var customEndpoint: String
        get() = prefs.getString(KEY_CUSTOM_ENDPOINT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_ENDPOINT, value.trim()).apply()

    var userBirthday: String
        get() = prefs.getString(KEY_USER_BIRTHDAY, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER_BIRTHDAY, value.trim()).commit()
        }

    var userLocation: String
        get() = prefs.getString(KEY_USER_LOCATION, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER_LOCATION, value.trim()).commit()
        }

    var userAge: String
        get() = prefs.getString(KEY_USER_AGE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER_AGE, value.trim()).commit()
        }

    var dailyRoutine: String
        get() = prefs.getString(KEY_DAILY_ROUTINE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_DAILY_ROUTINE, value.trim()).commit()
        }

    var favoriteSong: String
        get() = prefs.getString(KEY_FAVORITE_SONG, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FAVORITE_SONG, value.trim()).commit()
        }

    var favoriteApp: String
        get() = prefs.getString(KEY_FAVORITE_APP, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FAVORITE_APP, value.trim()).commit()
        }

    var isMicMuted: Boolean
        get() = prefs.getBoolean(KEY_MIC_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MIC_MUTED, value).apply()

    var isSilentExecutionMode: Boolean
        get() = prefs.getBoolean(KEY_SILENT_EXECUTION_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_SILENT_EXECUTION_MODE, value).apply()

    var isTaskOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_TASK_OVERLAY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TASK_OVERLAY_ENABLED, value).apply()

    var taskOverlayAvatar: String
        get() = prefs.getString(KEY_TASK_OVERLAY_AVATAR, "ghost") ?: "ghost"
        set(value) {
            val available = listOf("alien", "blob", "cat", "circle", "cloud", "droid", "drop", "flower", "ghost", "hexagon", "mech", "pebble", "pill", "puddle", "square", "star", "triangle")
            prefs.edit().putString(KEY_TASK_OVERLAY_AVATAR, value.takeIf { it in available } ?: "ghost").apply()
        }

    /** always keeps the avatar visible while the service is alive; working limits it to active tasks. */
    var taskOverlayMode: String
        get() = prefs.getString(KEY_TASK_OVERLAY_MODE, "always") ?: "always"
        set(value) = prefs.edit().putString(KEY_TASK_OVERLAY_MODE, if (value == "working") "working" else "always").apply()

    /** Preferred dock edge. Dragging the overlay updates this to the closest edge. */
    var taskOverlayEdge: String
        get() = prefs.getString(KEY_TASK_OVERLAY_EDGE, "right") ?: "right"
        set(value) = prefs.edit().putString(KEY_TASK_OVERLAY_EDGE, value.takeIf { it in setOf("left", "right", "top", "bottom") } ?: "right").apply()

    var taskOverlayFreePosition: Boolean
        get() = prefs.getBoolean(KEY_TASK_OVERLAY_FREE_POSITION, false)
        set(value) = prefs.edit().putBoolean(KEY_TASK_OVERLAY_FREE_POSITION, value).apply()

    var taskOverlayX: Int
        get() = prefs.getInt(KEY_TASK_OVERLAY_X, -1)
        set(value) = prefs.edit().putInt(KEY_TASK_OVERLAY_X, value.coerceAtLeast(0)).apply()

    var taskOverlayY: Int
        get() = prefs.getInt(KEY_TASK_OVERLAY_Y, -1)
        set(value) = prefs.edit().putInt(KEY_TASK_OVERLAY_Y, value.coerceAtLeast(0)).apply()

    fun getSongPlayCounts(): MutableMap<String, Int> {
        val json = prefs.getString(KEY_SONG_COUNTS, null) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, Int>>() {}.type
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun recordSongPlay(rawSong: String): String {
        val cleanSong = rawSong.trim()
        if (cleanSong.isBlank()) return favoriteSong

        val counts = getSongPlayCounts()
        val currentCount = counts.getOrDefault(cleanSong.lowercase(), 0) + 1
        counts[cleanSong.lowercase()] = currentCount

        // Update stored JSON
        prefs.edit().putString(KEY_SONG_COUNTS, gson.toJson(counts)).apply()

        // Auto-promote to favorite song if played at least 2 times, or if favoriteSong is empty
        val currentFav = favoriteSong
        if (currentFav.isBlank() || currentCount >= 2) {
            // Find most played song
            val topSong = counts.maxByOrNull { it.value }?.key
            if (!topSong.isNullOrBlank()) {
                favoriteSong = cleanSong
            }
        }
        return favoriteSong
    }

    fun loadChatHistory(): List<ChatTurn> {
        val json = prefs.getString(KEY_CHAT_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatTurn>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChatHistory(history: List<ChatTurn>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_CHAT_HISTORY, json).apply()
    }

    fun clearChatHistory() {
        prefs.edit().remove(KEY_CHAT_HISTORY).apply()
    }

    // ----------------------------------------------------
    // ANGEL AI 3D AVATAR SYSTEM PREFERENCES
    // ----------------------------------------------------
    var avatarModel: String
        get() = prefs.getString(KEY_AVATAR_MODEL, "angel_human") ?: "angel_human"
        set(value) = prefs.edit().putString(KEY_AVATAR_MODEL, value).apply()

    var avatarGender: String
        get() = prefs.getString(KEY_AVATAR_GENDER, "female") ?: "female"
        set(value) = prefs.edit().putString(KEY_AVATAR_GENDER, value).apply()

    var avatarHairStyle: String
        get() = prefs.getString(KEY_AVATAR_HAIR_STYLE, "sleek") ?: "sleek"
        set(value) = prefs.edit().putString(KEY_AVATAR_HAIR_STYLE, value).apply()

    var avatarHairColor: String
        get() = prefs.getString(KEY_AVATAR_HAIR_COLOR, "#d4af37") ?: "#d4af37"
        set(value) = prefs.edit().putString(KEY_AVATAR_HAIR_COLOR, value).apply()

    var avatarEyeColor: String
        get() = prefs.getString(KEY_AVATAR_EYE_COLOR, "#00d4ff") ?: "#00d4ff"
        set(value) = prefs.edit().putString(KEY_AVATAR_EYE_COLOR, value).apply()

    var avatarSkinTone: String
        get() = prefs.getString(KEY_AVATAR_SKIN_TONE, "fair") ?: "fair"
        set(value) = prefs.edit().putString(KEY_AVATAR_SKIN_TONE, value).apply()

    var avatarClothingStyle: String
        get() = prefs.getString(KEY_AVATAR_CLOTHING_STYLE, "cyber_suit") ?: "cyber_suit"
        set(value) = prefs.edit().putString(KEY_AVATAR_CLOTHING_STYLE, value).apply()

    var avatarClothingColor: String
        get() = prefs.getString(KEY_AVATAR_CLOTHING_COLOR, "#0f172a") ?: "#0f172a"
        set(value) = prefs.edit().putString(KEY_AVATAR_CLOTHING_COLOR, value).apply()

    var avatarEnvironment: String
        get() = prefs.getString(KEY_AVATAR_ENVIRONMENT, "cyber_studio") ?: "cyber_studio"
        set(value) = prefs.edit().putString(KEY_AVATAR_ENVIRONMENT, value).apply()

    var avatarLighting: String
        get() = prefs.getString(KEY_AVATAR_LIGHTING, "studio") ?: "studio"
        set(value) = prefs.edit().putString(KEY_AVATAR_LIGHTING, value).apply()

    var isAvatarVfxEnabled: Boolean
        get() = prefs.getBoolean(KEY_AVATAR_VFX_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AVATAR_VFX_ENABLED, value).apply()

    var avatarDisplayMode: String
        get() = prefs.getString(KEY_AVATAR_DISPLAY_MODE, "home_stage") ?: "home_stage"
        set(value) = prefs.edit().putString(KEY_AVATAR_DISPLAY_MODE, value).apply()

    var avatarGraphicsQuality: String
        get() = prefs.getString(KEY_AVATAR_GRAPHICS_QUALITY, "high") ?: "high"
        set(value) = prefs.edit().putString(KEY_AVATAR_GRAPHICS_QUALITY, value).apply()

    var isAvatarAnimationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AVATAR_ANIMATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AVATAR_ANIMATIONS_ENABLED, value).apply()

    var isAvatarIdleAnimEnabled: Boolean
        get() = prefs.getBoolean(KEY_AVATAR_IDLE_ANIM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AVATAR_IDLE_ANIM_ENABLED, value).apply()

    var isAvatarLipSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AVATAR_LIPSYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AVATAR_LIPSYNC_ENABLED, value).apply()

    var avatarDefaultExpression: String
        get() = prefs.getString(KEY_AVATAR_DEFAULT_EXPRESSION, "neutral") ?: "neutral"
        set(value) = prefs.edit().putString(KEY_AVATAR_DEFAULT_EXPRESSION, value).apply()

    var avatarSpeechSpeed: Float
        get() = prefs.getFloat(KEY_AVATAR_SPEECH_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_AVATAR_SPEECH_SPEED, value).apply()

    var avatarSpeechPitch: Float
        get() = prefs.getFloat(KEY_AVATAR_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_AVATAR_SPEECH_PITCH, value).apply()

    var avatarCustomImagePath: String?
        get() = prefs.getString(KEY_AVATAR_CUSTOM_IMAGE_PATH, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_CUSTOM_IMAGE_PATH, value).apply()

    var avatarCustom3DPath: String?
        get() = prefs.getString(KEY_AVATAR_CUSTOM_3D_PATH, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_CUSTOM_3D_PATH, value).apply()

    var avatarCustomVideoPath: String?
        get() = prefs.getString(KEY_AVATAR_CUSTOM_VIDEO_PATH, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_CUSTOM_VIDEO_PATH, value).apply()

    var orbVisibilityMode: String
        get() = prefs.getString(KEY_ORB_VISIBILITY_MODE, "speak_or_ai") ?: "speak_or_ai"
        set(value) = prefs.edit().putString(KEY_ORB_VISIBILITY_MODE, value).apply()

    var orbShape: String
        get() = prefs.getString(KEY_ORB_SHAPE, "sphere") ?: "sphere"
        set(value) = prefs.edit().putString(KEY_ORB_SHAPE, value).apply()

    var orbColorTheme: String
        get() = prefs.getString(KEY_ORB_COLOR_THEME, "blue") ?: "blue"
        set(value) = prefs.edit().putString(KEY_ORB_COLOR_THEME, value).apply()

    var orbAnimationStyle: String
        get() = prefs.getString(KEY_ORB_ANIM_STYLE, "smooth") ?: "smooth"
        set(value) = prefs.edit().putString(KEY_ORB_ANIM_STYLE, value).apply()

    var orbGlowIntensity: String
        get() = prefs.getString(KEY_ORB_GLOW_INTENSITY, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_ORB_GLOW_INTENSITY, value).apply()

    var orbParticleDensity: String
        get() = prefs.getString(KEY_ORB_PARTICLE_DENSITY, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_ORB_PARTICLE_DENSITY, value).apply()

    var orbSize: String
        get() = prefs.getString(KEY_ORB_SIZE, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_ORB_SIZE, value).apply()

    var orbBackground: String
        get() = prefs.getString(KEY_ORB_BACKGROUND, "transparent") ?: "transparent"
        set(value) = prefs.edit().putString(KEY_ORB_BACKGROUND, value).apply()

    var orbSpeechReactive: Boolean
        get() = prefs.getBoolean(KEY_ORB_SPEECH_REACTIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_ORB_SPEECH_REACTIVE, value).apply()

    var orbMicResponse: String
        get() = prefs.getString(KEY_ORB_MIC_RESPONSE, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_ORB_MIC_RESPONSE, value).apply()

    var orbAiPulse: Boolean
        get() = prefs.getBoolean(KEY_ORB_AI_PULSE, true)
        set(value) = prefs.edit().putBoolean(KEY_ORB_AI_PULSE, value).apply()

    var orbThinkingAnimation: Boolean
        get() = prefs.getBoolean(KEY_ORB_THINKING_ANIM, true)
        set(value) = prefs.edit().putBoolean(KEY_ORB_THINKING_ANIM, value).apply()

    var orbFadeDelay: Int
        get() = prefs.getInt(KEY_ORB_FADE_DELAY, 3000)
        set(value) = prefs.edit().putInt(KEY_ORB_FADE_DELAY, value).apply()

    var orbSmoothTransitions: Boolean
        get() = prefs.getBoolean(KEY_ORB_SMOOTH_TRANSITIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_ORB_SMOOTH_TRANSITIONS, value).apply()

    var orbTouchRipple: Boolean
        get() = prefs.getBoolean(KEY_ORB_TOUCH_RIPPLE, true)
        set(value) = prefs.edit().putBoolean(KEY_ORB_TOUCH_RIPPLE, value).apply()

    var orbReducedMotion: Boolean
        get() = prefs.getBoolean(KEY_ORB_REDUCED_MOTION, false)
        set(value) = prefs.edit().putBoolean(KEY_ORB_REDUCED_MOTION, value).apply()

    var isCallAssistantEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_ASSISTANT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_ASSISTANT_ENABLED, value).apply()

    var whatsappCallAssistanceEnabled: Boolean
        get() = prefs.getBoolean(KEY_WHATSAPP_CALL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WHATSAPP_CALL_ENABLED, value).apply()

    var simCallAssistanceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SIM_CALL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SIM_CALL_ENABLED, value).apply()

    var callAssistantLanguage: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_LANGUAGE, "Auto") ?: "Auto"
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_LANGUAGE, value).apply()

    var callAssistantVoice: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_VOICE, "Angel-AI (Natural)") ?: "Angel-AI (Natural)"
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_VOICE, value).apply()

    var callAssistantSpeakingSpeed: Float
        get() = prefs.getFloat(KEY_CALL_ASSISTANT_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_CALL_ASSISTANT_SPEED, value).apply()

    var callAssistantCustomGreeting: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_GREETING, "Hello, I am Angel AI, the assistant for MD Aslam. How can I assist you?") ?: "Hello, I am Angel AI, the assistant for MD Aslam. How can I assist you?"
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_GREETING, value).apply()

    var callAssistantInstructions: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_INSTRUCTIONS, "Be polite, concise, and professional. Identify yourself as an AI assistant.") ?: "Be polite, concise, and professional. Identify yourself as an AI assistant."
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_INSTRUCTIONS, value).apply()

    var callAssistantAutoAnswer: Boolean
        get() = prefs.getBoolean(KEY_CALL_ASSISTANT_AUTO_ANSWER, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_ASSISTANT_AUTO_ANSWER, value).apply()

    var callAssistantMode: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_MODE, "manual_and_ai") ?: "manual_and_ai"
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_MODE, value).apply()

    var callAssistantAllowlist: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_ALLOWLIST, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_ALLOWLIST, value).apply()

    var callAssistantBlocked: String
        get() = prefs.getString(KEY_CALL_ASSISTANT_BLOCKED, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CALL_ASSISTANT_BLOCKED, value).apply()

    var callAssistantMaxDurationSeconds: Int
        get() = prefs.getInt(KEY_CALL_ASSISTANT_MAX_DURATION, 120)
        set(value) = prefs.edit().putInt(KEY_CALL_ASSISTANT_MAX_DURATION, value).apply()

    var callAssistantDataRetentionOptIn: Boolean
        get() = prefs.getBoolean(KEY_CALL_ASSISTANT_PRIVACY_OPTIN, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_ASSISTANT_PRIVACY_OPTIN, value).apply()

    var liveVoiceProvider: String
        get() = prefs.getString(KEY_LIVE_VOICE_PROVIDER, "gemini") ?: "gemini"
        set(value) = prefs.edit().putString(KEY_LIVE_VOICE_PROVIDER, value).apply()

    fun resetOrbSettingsToDefault() {
        prefs.edit()
            .remove(KEY_ORB_VISIBILITY_MODE)
            .remove(KEY_ORB_SHAPE)
            .remove(KEY_ORB_COLOR_THEME)
            .remove(KEY_ORB_ANIM_STYLE)
            .remove(KEY_ORB_GLOW_INTENSITY)
            .remove(KEY_ORB_PARTICLE_DENSITY)
            .remove(KEY_ORB_SIZE)
            .remove(KEY_ORB_BACKGROUND)
            .remove(KEY_ORB_SPEECH_REACTIVE)
            .remove(KEY_ORB_MIC_RESPONSE)
            .remove(KEY_ORB_AI_PULSE)
            .remove(KEY_ORB_THINKING_ANIM)
            .remove(KEY_ORB_FADE_DELAY)
            .remove(KEY_ORB_SMOOTH_TRANSITIONS)
            .remove(KEY_ORB_TOUCH_RIPPLE)
            .remove(KEY_ORB_REDUCED_MOTION)
            .apply()
    }
}
