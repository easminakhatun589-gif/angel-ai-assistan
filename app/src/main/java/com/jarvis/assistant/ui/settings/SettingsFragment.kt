package com.jarvis.assistant.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.GeminiConstants
import com.jarvis.assistant.databinding.FragmentSettingsBinding
import com.jarvis.assistant.ui.memory.MemoryActivity
import com.jarvis.assistant.ui.permission.PermissionActivity
import com.jarvis.assistant.ui.voice.VoiceEnrollmentActivity
import com.jarvis.assistant.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val preferences by lazy { JarvisApp.instance.preferences }
    private val authManager by lazy { AuthManager(requireContext()) }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            postTestNotification()
        } else {
            Toast.makeText(context, "Notification permission is required to display alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)

        loadUserData()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)
            loadUserData()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) {
            binding.root.visibility = View.VISIBLE
            com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)
            loadUserData()
        } else if (hidden && _binding != null) {
            binding.root.visibility = View.GONE
        }
    }

    private val pickProfileImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            preferences.userProfilePhotoPath = it.toString()
            loadUserData()
            Toast.makeText(requireContext(), "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            preferences.isLocationPermissionGranted = true
            Toast.makeText(requireContext(), "Location permission granted! Country detected: ${preferences.userCountry}", Toast.LENGTH_SHORT).show()
            loadUserData()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCountryFlag(country: String): String {
        return when (country.lowercase().trim()) {
            "india", "in" -> "🇮🇳"
            "united states", "usa", "us" -> "🇺🇸"
            "united kingdom", "uk", "gb" -> "🇬🇧"
            "bangladesh", "bd" -> "🇧🇩"
            "canada", "ca" -> "🇨🇦"
            "australia", "au" -> "🇦🇺"
            "united arab emirates", "uae", "ae" -> "🇦🇪"
            "saudi arabia", "sa" -> "🇸🇦"
            "germany", "de" -> "🇩🇪"
            "france", "fr" -> "🇫🇷"
            "japan", "jp" -> "🇯🇵"
            else -> "🌐"
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        val userName = user?.displayName ?: preferences.userName.ifBlank { "Guest User" }
        binding.tvProfileName.text = userName
        val email = user?.email ?: preferences.userEmail.ifBlank { "Not signed in" }
        val phone = preferences.userPhone.ifBlank { "No phone set" }
        binding.tvProfileSubtitle.text = "$email • $phone"

        if (user?.isAnonymous == true || preferences.isGuestMode || user == null) {
            val phone = preferences.userPhone.ifBlank { "No phone set" }
            binding.tvProfileSubtitle.text = "Guest Session (Local Mode) • $phone • Tap to Sign In"
            binding.tvProfileName.text = preferences.userName.ifBlank { "Guest Operator" }
        }

        // Profile Photo
        val photoPath = preferences.userProfilePhotoPath
        if (!photoPath.isNullOrBlank()) {
            try {
                binding.ivProfileAvatar.setImageURI(Uri.parse(photoPath))
            } catch (_: Exception) {
                binding.ivProfileAvatar.setImageResource(R.drawable.ic_user_profile)
            }
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.ic_user_profile)
        }

        // Country & Flag Badge (if location permission granted or country set)
        val country = preferences.userCountry.ifBlank { "India" }
        if (preferences.isLocationPermissionGranted || country.isNotBlank()) {
            val flag = getCountryFlag(country)
            binding.tvProfileCountryBadge.text = "$flag $country"
            binding.tvProfileCountryBadge.visibility = View.VISIBLE
        } else {
            binding.tvProfileCountryBadge.visibility = View.GONE
        }

        // Owner Voice Profile Status
        val hasVoiceProfile = preferences.hasOwnerVoiceProfile()
        if (hasVoiceProfile) {
            binding.tvOwnerVoiceBadge.text = "ACTIVE"
            binding.tvOwnerVoiceBadge.setTextColor(Color.parseColor("#10B981"))
            binding.tvOwnerVoiceBadge.setBackgroundResource(R.drawable.bg_jarvis_status_badge_green)
            binding.tvOwnerVoiceSubtitle.text = "Voice Profile Active • Only you can speak"

            binding.tvProfileVoiceStatus.text = "VOICE ENROLLED"
            binding.tvProfileVoiceStatus.setTextColor(Color.parseColor("#10B981"))
            binding.chipProfileVoiceStatus.setBackgroundResource(R.drawable.bg_jarvis_status_badge_green)
            binding.ivProfileVoiceBadgeIcon.setImageResource(R.drawable.ic_verified_badge)
            binding.ivProfileVoiceBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
        } else {
            binding.tvOwnerVoiceBadge.text = "SETUP"
            binding.tvOwnerVoiceBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_sapphire))
            binding.tvOwnerVoiceBadge.setBackgroundResource(R.drawable.bg_jarvis_status_badge)
            binding.tvOwnerVoiceSubtitle.text = "Not enrolled • Tap to record voice profile"

            binding.tvProfileVoiceStatus.text = "ENROLL VOICE"
            binding.tvProfileVoiceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_sapphire))
            binding.chipProfileVoiceStatus.setBackgroundResource(R.drawable.bg_jarvis_status_badge)
            binding.ivProfileVoiceBadgeIcon.setImageResource(R.drawable.ic_settings_owner_voice)
            binding.ivProfileVoiceBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.accent_sapphire)
            )
        }

        // Language
        binding.tvCurrentLanguage.text = preferences.language

        // Persona & Voice
        val currentPersona = preferences.personality
        val currentVoice = preferences.voice
        binding.tvCurrentPersona.text = "$currentPersona • Voice: $currentVoice"
        binding.tvProfilePersonaChip.text = currentPersona.uppercase()

        // Silent Execution Mode
        binding.switchSilentMode.isChecked = preferences.isSilentExecutionMode

        // Model
        val modelDisplay = preferences.aiModel.removePrefix("models/")
        binding.tvCurrentModel.text = "$modelDisplay • Live WebSocket"

        // Theme
        binding.tvCurrentTheme.text = when (preferences.appTheme) {
            "dark" -> "Dark Theme"
            "red" -> "Red Theme"
            "sunset" -> "Sunset Theme"
            "gold" -> "Stark Gold HUD"
            "deep_blue" -> "Deep Blue HUD"
            "midnight_purple" -> "Midnight Purple HUD"
            "custom" -> "Custom (${preferences.customThemeColor})"
            else -> "Light Theme"
        }

        // Device Admin Status
        val isAdminActive = com.jarvis.assistant.admin.JarvisDeviceAdminManager.isDeviceAdminActive(requireContext())
        if (isAdminActive) {
            binding.tvDeviceAdminStatus.text = "Active • Device Admin enabled (Tap to lock device)"
            binding.tvDeviceAdminStatus.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvDeviceAdminStatus.text = "Inactive • Tap to activate device administrator"
            binding.tvDeviceAdminStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_sapphire))
        }

        // Admin Portal Status Check
        viewLifecycleOwner.lifecycleScope.launch {
            val isAdmin = com.jarvis.assistant.security.AdminGuard.isCurrentUserAdmin()
            if (isAdded) {
                if (isAdmin) {
                    binding.tvAdminStatusSubtitle.text = "Authorized Admin • Tap to open Control Center"
                    binding.tvAdminStatusSubtitle.setTextColor(Color.parseColor("#10B981"))
                } else {
                    binding.tvAdminStatusSubtitle.text = "Restricted • Tap to test admin credentials"
                    binding.tvAdminStatusSubtitle.setTextColor(Color.parseColor("#94A3B8"))
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Profile Card -> Change Name / Connect Account
        binding.cardProfile.setOnClickListener {
            val user = auth.currentUser
            if (user == null || user.isAnonymous || preferences.isGuestMode) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Account & Profile")
                    .setMessage("You are currently in Guest / Local mode. Connect your Google account to keep all data synchronized, or edit your phone and operator name.")
                    .setPositiveButton("Sign In / Google") { _, _ ->
                        val intent = Intent(requireContext(), com.jarvis.assistant.ui.auth.LoginActivity::class.java)
                        startActivity(intent)
                    }
                    .setNeutralButton("Edit Profile") { _, _ ->
                        showEditProfileDialog()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                showEditProfileDialog()
            }
        }

        binding.rowPremiumMembership.setOnClickListener {
            startActivity(Intent(requireContext(), MembershipActivity::class.java))
        }

        binding.rowAdminPanel.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val isAdmin = com.jarvis.assistant.security.AdminGuard.isCurrentUserAdmin()
                if (isAdmin) {
                    startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                } else {
                    showAdminLoginPrompt()
                }
            }
        }

        // Voice Status Chip on Profile Card -> Opens Voice Enrollment
        binding.chipProfileVoiceStatus.setOnClickListener {
            startActivity(Intent(requireContext(), VoiceEnrollmentActivity::class.java))
        }

        // Section 1: AI & Voice Intelligence
        binding.rowOwnerVoice.setOnClickListener {
            startActivity(Intent(requireContext(), VoiceEnrollmentActivity::class.java))
        }

        binding.rowAiCallAssistant.setOnClickListener {
            startActivity(Intent(requireContext(), AiCallAssistantActivity::class.java))
        }

        binding.rowWhatsAppContacts.setOnClickListener {
            startActivity(Intent(requireContext(), WhatsAppContactSettingsActivity::class.java))
        }

        binding.rowMobileAppsControl.setOnClickListener {
            startActivity(Intent(requireContext(), MobileAppsControlActivity::class.java))
        }

        binding.rowApkManager.setOnClickListener {
            startActivity(Intent(requireContext(), ApkManagerActivity::class.java))
        }


        binding.rowLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.rowPersonaVoice.setOnClickListener {
            startActivity(Intent(requireContext(), PersonaActivity::class.java))
        }

        binding.switchSilentMode.setOnCheckedChangeListener { _, isChecked ->
            preferences.isSilentExecutionMode = isChecked
            val msg = if (isChecked) "Silent Execution Mode Enabled" else "Silent Execution Mode Disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Section 2: System & Intelligence Data
        binding.rowApiConnection.setOnClickListener {
            startActivity(Intent(requireContext(), ApiConnectionActivity::class.java))
        }

        binding.rowMemory.setOnClickListener {
            startActivity(Intent(requireContext(), MemoryActivity::class.java))
        }

        binding.rowPermissions.setOnClickListener {
            startActivity(Intent(requireContext(), PermissionActivity::class.java))
        }

        binding.rowAvatarSettings.setOnClickListener {
            startActivity(Intent(requireContext(), AvatarSettingsActivity::class.java))
        }

        binding.rowVoiceOrbSettings.setOnClickListener {
            startActivity(Intent(requireContext(), VoiceOrbSettingsActivity::class.java))
        }

        binding.rowOverlayAvatars.setOnClickListener {
            startActivity(Intent(requireContext(), OverlayAvatarActivity::class.java))
        }

        binding.rowApplications.setOnClickListener {
            startActivity(Intent(requireContext(), HandsFreeActivity::class.java))
        }

        binding.rowWebsiteControl.setOnClickListener {
            startActivity(Intent(requireContext(), WebsiteControlActivity::class.java))
        }

        // Section 3: Appearance & Security
        binding.rowAppTheme.setOnClickListener {
            ThemeSelectionBottomSheet.newInstance().show(childFragmentManager, ThemeSelectionBottomSheet.TAG)
        }

        binding.rowPrivacyPolicy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyShieldActivity::class.java))
        }

        binding.rowAppIdentity.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("ANGEL AI App Identity")
                .setMessage(com.jarvis.assistant.security.AppIdentityVerifier.report(requireContext()))
                .setPositiveButton("Close", null)
                .show()
        }

        binding.rowDeviceAdmin.setOnClickListener {
            val active = com.jarvis.assistant.admin.JarvisDeviceAdminManager.isDeviceAdminActive(requireContext())
            if (active) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Device Administrator Active")
                    .setMessage("Jarvis Device Administrator is currently active. Would you like to lock the device screen now?")
                    .setPositiveButton("Lock Screen") { _, _ ->
                        com.jarvis.assistant.admin.JarvisDeviceAdminManager.lockDevice(requireContext())
                    }
                    .setNeutralButton("Close", null)
                    .show()
            } else {
                com.jarvis.assistant.admin.JarvisDeviceAdminManager.requestDeviceAdmin(requireActivity())
            }
        }

        binding.rowTestNotification.setOnClickListener {
            handleTestNotification()
        }

        binding.rowLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
                return
            }
        }
        postTestNotification()
    }

    private fun postTestNotification() {
        val context = context ?: return
        val channelId = getString(R.string.default_notification_channel_id)
        val notificationManager = ContextCompat.getSystemService(context, android.app.NotificationManager::class.java)

        // Create channel for newer versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Angel AI Updates",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.jarvis.assistant.ui.home.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Angel AI: System Test")
            .setContentText("Your futuristic assistant is ready. Push notifications are functional!")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)

        notificationManager?.notify(System.currentTimeMillis().toInt(), builder.build())
        Toast.makeText(context, "Test notification triggered!", Toast.LENGTH_SHORT).show()
    }

    private fun showAdminLoginPrompt() {
        val currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "Guest"
        val adminList = com.jarvis.assistant.security.AdminGuard.PRIMARY_ADMIN_EMAILS.joinToString("\n• ")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Admin Panel Restricted")
            .setMessage("Current Account: $currentEmail\n\nAdmin privileges are restricted to registered administrators:\n• $adminList\n\nTo access the Control Center, please sign in with an authorized administrator account.")
            .setPositiveButton("Sign In as Admin") { _, _ ->
                val intent = Intent(requireContext(), com.jarvis.assistant.ui.auth.LoginActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to exit your session?")
            .setPositiveButton("Logout") { _, _ ->
                preferences.isGuestMode = false
                authManager.signOut(lifecycleScope) {
                    val intent = Intent(requireContext(), com.jarvis.assistant.ui.auth.LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = GeminiConstants.SUPPORTED_LANGUAGES.toTypedArray()
        val currentIndex = languages.indexOfFirst { it.equals(preferences.language, ignoreCase = true) }.let { if (it >= 0) it else 0 }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Conversation Language")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val chosen = languages[which]
                preferences.language = chosen
                binding.tvCurrentLanguage.text = chosen
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWakeWordDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Wake Words & Sensitivity")
            .setMessage("• Wake Words: 'Angel', 'Angel AI', 'Jarvis'\n• Acoustic Trigger: Double Clap detection\n• Standby Mode: Low-power local Vosk offline engine\n\nWhen standby listening is active, ANGEL AI awakens instantly upon hearing your voice.")
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun showAppAutomationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Autonomous App Automation")
            .setMessage("ANGEL AI is integrated with your device capabilities:\n\n• WhatsApp: Hands-free messaging & calls\n• YouTube: Search and playback automation\n• Phone Controls: Flashlight, Volume, Brightness, Bluetooth\n• Apps & Tools: Launch, search, and manage apps via voice")
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etDialogEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etDialogPhone)
        val etCountry = dialogView.findViewById<EditText>(R.id.etDialogCountry)
        val btnUploadPhoto = dialogView.findViewById<View>(R.id.btnDialogUploadPhoto)
        val btnDetectLocation = dialogView.findViewById<View>(R.id.btnDialogDetectLocation)

        etName?.setText(preferences.userName)
        etEmail?.setText(preferences.userEmail)
        etPhone?.setText(preferences.userPhone)
        etCountry?.setText(preferences.userCountry)

        btnUploadPhoto?.setOnClickListener {
            pickProfileImageLauncher.launch("image/*")
        }

        btnDetectLocation?.setOnClickListener {
            requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Operator Profile & Identity")
            .setView(dialogView)
            .setPositiveButton("Save Changes") { _, _ ->
                val newName = etName?.text?.toString()?.trim() ?: preferences.userName
                val newEmail = etEmail?.text?.toString()?.trim() ?: ""
                val newPhone = etPhone?.text?.toString()?.trim() ?: ""
                val newCountry = etCountry?.text?.toString()?.trim() ?: "India"
                preferences.userName = newName
                preferences.userEmail = newEmail
                preferences.userPhone = newPhone
                preferences.userCountry = newCountry
                loadUserData()

                // Persist to Firestore and Firebase Auth
                viewLifecycleOwner.lifecycleScope.launch {
                    authManager.syncUserToFirestore(phoneNumber = newPhone, country = newCountry)
                }
                Toast.makeText(requireContext(), "Profile & mobile saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
