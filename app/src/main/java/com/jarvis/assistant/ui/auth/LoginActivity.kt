package com.jarvis.assistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.databinding.ActivityLoginBinding
import com.jarvis.assistant.auth.AuthManager
import com.jarvis.assistant.ui.home.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)

        val prefs = (application as? JarvisApp)?.preferences
        if (authManager.currentUser != null || prefs?.isGuestMode == true) {
            navigateToMain()
            return
        }

        binding.btnAuthAction.setOnClickListener {
            handleAuthAction()
        }

        binding.tvToggleMode.setOnClickListener {
            toggleAuthMode()
        }

        binding.btnGoogleSignIn.setOnClickListener {
            handleGoogleSignIn()
        }

        binding.tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }

        binding.btnGuestLogin.setOnClickListener {
            handleGuestLogin()
        }

        binding.btnAdminLoginTest.setOnClickListener {
            showAdminLoginTestDialog()
        }
    }

    private fun showAdminLoginTestDialog() {
        val adminEmails = com.jarvis.assistant.security.AdminGuard.PRIMARY_ADMIN_EMAILS.toTypedArray()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("👑 Admin Panel Login Test")
            .setMessage("Select an authorized administrator account to autofill:")
            .setItems(adminEmails) { _, which ->
                val chosen = adminEmails[which]
                binding.etEmail.setText(chosen)
                binding.etPassword.requestFocus()
                Toast.makeText(this, "Admin email set: $chosen\nEnter password or sign in with Google.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleAuthAction() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val name = binding.etName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill email and password", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSignUpMode && name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        val prefs = (application as? JarvisApp)?.preferences
        if (isSignUpMode) {
            authManager.signUpWithEmail(name, email, password,
                onSuccess = {
                    prefs?.isGuestMode = false
                    if (name.isNotBlank()) prefs?.userName = name
                    prefs?.userEmail = email
                    setLoading(false)
                    navigateToMain()
                },
                onError = { error ->
                    setLoading(false)
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            authManager.signInWithEmail(email, password,
                onSuccess = {
                    prefs?.isGuestMode = false
                    prefs?.userEmail = email
                    setLoading(false)
                    navigateToMain()
                },
                onError = { error ->
                    setLoading(false)
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun handleForgotPassword() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        authManager.sendPasswordResetEmail(email,
            onSuccess = {
                setLoading(false)
                Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
            },
            onError = { error ->
                setLoading(false)
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun handleGuestLogin() {
        setLoading(true)
        val prefs = (application as? JarvisApp)?.preferences
        authManager.signInAnonymously(
            onSuccess = {
                prefs?.isGuestMode = true
                if (prefs != null && prefs.userName.isBlank()) {
                    prefs.userName = "Guest Operator"
                }
                setLoading(false)
                Toast.makeText(this, "Welcome, Guest Operator!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            },
            onError = { _ ->
                // Fallback to local guest mode if Firebase Anonymous Auth is unprovisioned or offline
                prefs?.isGuestMode = true
                if (prefs != null && prefs.userName.isBlank()) {
                    prefs.userName = "Guest Operator"
                }
                setLoading(false)
                Toast.makeText(this, "Logged in as Guest Operator", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
        )
    }

    private fun handleGoogleSignIn() {
        setLoading(true)
        val prefs = (application as? JarvisApp)?.preferences
        authManager.signInWithGoogle(
            activity = this,
            scope = lifecycleScope,
            onSuccess = {
                prefs?.isGuestMode = false
                authManager.currentUser?.displayName?.let { if (it.isNotBlank()) prefs?.userName = it }
                authManager.currentUser?.email?.let { if (it.isNotBlank()) prefs?.userEmail = it }
                setLoading(false)
                navigateToMain()
            },
            onError = { error ->
                setLoading(false)
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            },
            onCancelled = {
                setLoading(false)
            }
        )
    }

    private fun toggleAuthMode() {
        isSignUpMode = !isSignUpMode
        if (isSignUpMode) {
            binding.tvWelcome.text = "Join Angel AI"
            binding.tvSubtitle.text = "Create an account to start your journey"
            binding.btnAuthAction.text = "Sign Up"
            binding.tvToggleMode.text = "Already have an account? Login"
            binding.tilName.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.GONE
        } else {
            binding.tvWelcome.text = "Welcome to Angel AI"
            binding.tvSubtitle.text = "Login or sign up to continue your journey"
            binding.btnAuthAction.text = "Login"
            binding.tvToggleMode.text = "Don't have an account? Sign Up"
            binding.tilName.visibility = View.GONE
            binding.tvForgotPassword.visibility = View.VISIBLE
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnAuthAction.isEnabled = !isLoading
        binding.btnGoogleSignIn.isEnabled = !isLoading
        binding.btnGuestLogin.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
