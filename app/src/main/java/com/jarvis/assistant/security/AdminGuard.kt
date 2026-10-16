package com.jarvis.assistant.security

import android.app.Activity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminGuard {
    val PRIMARY_ADMIN_EMAILS = setOf(
        "mdaslamlaskar14@gmail.com",
        "easminakhatun589@gmail.com"
    )

    /**
     * Checks if the given email is one of the designated primary super-administrators.
     */
    fun isAuthorizedEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.lowercase().trim()
        return clean in PRIMARY_ADMIN_EMAILS
    }

    /**
     * Checks whether the currently authenticated user is an authorized administrator
     * by verifying their email against the secure Firestore '/admins/{email}' whitelist
     * and checking the primary admin override.
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        val email = user.email?.lowercase()?.trim() ?: return false

        // Primary administrator override
        if (isAuthorizedEmail(email)) {
            return true
        }

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("admins").document(email).get().await()
            doc.exists() && (doc.getBoolean("active") ?: true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Synchronous / Deterministic evaluation for testing and offline authorization.
     */
    fun evaluateAdminStatus(email: String?, isDocInFirestore: Boolean = false): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.lowercase().trim()
        if (isAuthorizedEmail(clean)) return true
        return isDocInFirestore
    }

    /**
     * Guards an administrative navigation route or action.
     * If the user is authorized, executes [onAuthorized].
     * Otherwise, shows a security denial toast and finishes the activity.
     */
    suspend fun enforceAccess(activity: Activity, onAuthorized: () -> Unit) {
        val authorized = isCurrentUserAdmin()
        if (authorized) {
            onAuthorized()
        } else {
            Toast.makeText(
                activity,
                "Security Alert: Unauthorized access attempt blocked.",
                Toast.LENGTH_LONG
            ).show()
            activity.finish()
        }
    }
}
