package com.jarvis.assistant.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jarvis.assistant.data.model.PaymentSubmission
import com.jarvis.assistant.data.repository.MembershipRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserRecord(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val isBlocked: Boolean = false,
    val createdAt: Long = 0L,
    val lastSignIn: Long = 0L
)

data class AuditLogEntry(
    val id: String = "",
    val adminUid: String = "",
    val action: String = "",
    val target: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val outcome: String = "SUCCESS"
)

data class FeatureFlag(
    val id: String = "",
    val enabled: Boolean = true,
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MembershipRepository(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _pendingSubmissions = MutableStateFlow<List<PaymentSubmission>>(emptyList())
    val pendingSubmissions: StateFlow<List<PaymentSubmission>> = _pendingSubmissions.asStateFlow()

    private val _users = MutableStateFlow<List<UserRecord>>(emptyList())
    val users: StateFlow<List<UserRecord>> = _users.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _featureFlags = MutableStateFlow<List<FeatureFlag>>(emptyList())
    val featureFlags: StateFlow<List<FeatureFlag>> = _featureFlags.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    init {
        observeSubmissions()
        loadUsers()
        loadAuditLogs()
        loadFeatureFlags()
    }

    /**
     * Securely verifies the authenticated user's email against the Firestore whitelist collection 'admins'.
     * Primary administrator: mdaslamlaskar14@gmail.com
     */
    suspend fun verifyAdminWhitelist(): Boolean {
        val user = auth.currentUser ?: return false
        val email = user.email ?: return false

        // Primary hardcoded admin override for robustness
        if (com.jarvis.assistant.security.AdminGuard.isAuthorizedEmail(email)) {
            return true
        }

        return try {
            val docSnapshot = firestore.collection("admins").document(email).get().await()
            docSnapshot.exists() && (docSnapshot.getBoolean("active") ?: true)
        } catch (e: Exception) {
            false
        }
    }

    private fun observeSubmissions() {
        viewModelScope.launch {
            try {
                repository.observePendingSubmissions().collect { _pendingSubmissions.value = it }
            } catch (e: Exception) {
                // handle
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("users").get().await()
                val list = snapshot.documents.map { doc ->
                    UserRecord(
                        uid = doc.id,
                        email = doc.getString("email").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty(),
                        isBlocked = doc.getBoolean("isBlocked") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        lastSignIn = doc.getLong("lastSignIn") ?: 0L
                    )
                }
                _users.value = list
            } catch (e: Exception) {
                _errorMessage.emit("Failed to load users: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setUserBlocked(userId: String, blocked: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("users").document(userId)
                    .update("isBlocked", blocked).await()
                
                logAdminAction("SET_USER_BLOCKED_$blocked", userId)
                _successMessage.emit("User block status updated successfully.")
                loadUsers()
            } catch (e: Exception) {
                _errorMessage.emit("Failed to update user block status: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFeatureFlags() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("featureFlags").get().await()
                val list = snapshot.documents.map { doc ->
                    FeatureFlag(
                        id = doc.id,
                        enabled = doc.getBoolean("enabled") ?: true,
                        description = doc.getString("description").orEmpty(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                }
                _featureFlags.value = list
            } catch (e: Exception) {
                _featureFlags.value = listOf(
                    FeatureFlag("ai_chat", true, "AI Chat Assistant"),
                    FeatureFlag("voice_assistant", true, "Voice Assistant & Wake Word"),
                    FeatureFlag("hud_3d", true, "3D Holographic HUD"),
                    FeatureFlag("apk_manager", true, "APK Manager & Updater"),
                    FeatureFlag("membership", true, "Membership & Payments")
                )
            }
        }
    }

    fun updateFeatureFlag(flagId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "enabled" to enabled,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("featureFlags").document(flagId).set(data).await()
                logAdminAction("UPDATE_FEATURE_FLAG_$flagId", "$enabled")
                _successMessage.emit("Feature $flagId set to $enabled")
                loadFeatureFlags()
            } catch (e: Exception) {
                _errorMessage.emit("Failed to update flag: ${e.localizedMessage}")
            }
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("auditLogs")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                val list = snapshot.documents.map { doc ->
                    AuditLogEntry(
                        id = doc.id,
                        adminUid = doc.getString("adminUid").orEmpty(),
                        action = doc.getString("action").orEmpty(),
                        target = doc.getString("target").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        outcome = doc.getString("outcome").orEmpty()
                    )
                }
                _auditLogs.value = list
            } catch (e: Exception) {
                // ignore if no logs yet
            }
        }
    }

    private fun logAdminAction(action: String, target: String) {
        viewModelScope.launch {
            try {
                val adminUid = auth.currentUser?.uid ?: "unknown"
                val log = mapOf(
                    "adminUid" to adminUid,
                    "action" to action,
                    "target" to target,
                    "timestamp" to System.currentTimeMillis(),
                    "outcome" to "SUCCESS"
                )
                firestore.collection("auditLogs").add(log).await()
                loadAuditLogs()
            } catch (e: Exception) {
                // audit log write fail non-fatal
            }
        }
    }

    fun approve(submission: PaymentSubmission) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.approveSubmission(submission)
                logAdminAction("APPROVE_PAYMENT", submission.id)
                _successMessage.emit("Payment submission approved successfully.")
            } catch (e: Exception) {
                _errorMessage.emit("Approval failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reject(submission: PaymentSubmission, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.rejectSubmission(submission, reason)
                logAdminAction("REJECT_PAYMENT", submission.id)
                _successMessage.emit("Payment submission rejected.")
            } catch (e: Exception) {
                _errorMessage.emit("Rejection failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
