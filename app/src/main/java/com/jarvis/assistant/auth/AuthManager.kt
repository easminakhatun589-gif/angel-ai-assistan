package com.jarvis.assistant.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)
    private val prefs get() = (context.applicationContext as? JarvisApp)?.preferences

    suspend fun syncUserToFirestore(phoneNumber: String = "", country: String = "") {
        withContext(Dispatchers.IO) {
            val user = auth.currentUser ?: return@withContext
            try {
                val firestore = FirebaseFirestore.getInstance()
                val userRef = firestore.collection("users").document(user.uid)
                val existingDoc = try { userRef.get().await() } catch (_: Exception) { null }

                val name = user.displayName?.ifBlank { null } ?: prefs?.userName?.ifBlank { null } ?: "Operator"
                val email = user.email ?: prefs?.userEmail.orEmpty()
                val phone = phoneNumber.ifBlank { user.phoneNumber ?: prefs?.userPhone.orEmpty() }
                val countryVal = country.ifBlank { prefs?.userCountry.orEmpty().ifBlank { "India" } }

                val userData = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to name,
                    "lastSignIn" to System.currentTimeMillis()
                )
                if (phone.isNotBlank()) {
                    userData["phoneNumber"] = phone
                }
                if (countryVal.isNotBlank()) {
                    userData["country"] = countryVal
                }
                if (existingDoc == null || !existingDoc.exists()) {
                    userData["createdAt"] = System.currentTimeMillis()
                    userData["isBlocked"] = false
                }
                userRef.set(userData, SetOptions.merge()).await()

                // Keep local preferences updated
                prefs?.let { p ->
                    p.isGuestMode = false
                    if (email.isNotBlank()) p.userEmail = email
                    if (name.isNotBlank()) p.userName = name
                    if (phone.isNotBlank()) p.userPhone = phone
                    if (countryVal.isNotBlank()) p.userCountry = countryVal
                }
                Log.d("Auth", "User account successfully synced to Firestore for UID: ${user.uid}")
            } catch (e: Exception) {
                Log.e("Auth", "Failed to sync user record to Firestore", e)
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    syncUserToFirestore()
                }
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Login failed") }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name
                }
                result.user?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            syncUserToFirestore()
                        }
                        onSuccess()
                    } ?: run {
                        CoroutineScope(Dispatchers.IO).launch {
                            syncUserToFirestore()
                        }
                        onSuccess()
                    }
            }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Sign up failed") }
    }

    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to send reset email") }
    }

    fun signInAnonymously(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInAnonymously()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Guest login failed") }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    syncUserToFirestore()
                    onSuccess()
                } else {
                    onError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
        auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }

    val currentUser get() = auth.currentUser
}
