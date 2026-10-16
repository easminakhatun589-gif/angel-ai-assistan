package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.storage.FirebaseStorage
import com.jarvis.assistant.databinding.ActivityPaymentVerificationBinding
import com.jarvis.assistant.util.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await
import java.util.*

class PaymentVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentVerificationBinding
    private lateinit var viewModel: MembershipViewModel
    private var orderId: String? = null
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivScreenshotPreview.setImageURI(it)
            binding.ivScreenshotPreview.visibility = View.VISIBLE
            binding.layoutUploadPlaceholder.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        binding = ActivityPaymentVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.root)

        viewModel = ViewModelProvider(this)[MembershipViewModel::class.java]
        orderId = intent.getStringExtra("ORDER_ID")

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnSelectScreenshot.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSubmitVerification.setOnClickListener {
            val transId = binding.etTransactionId.text.toString().trim()
            if (transId.isEmpty()) {
                Toast.makeText(this, "Please enter Transaction ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please upload a screenshot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadProof(transId)
        }
    }

    private fun uploadProof(transId: String) {
        val currentOrderId = orderId ?: return
        lifecycleScope.launchWhenStarted {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmitVerification.isEnabled = false
            try {
                // Upload image to Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference
                val fileRef = storageRef.child("receipts/${UUID.randomUUID()}.jpg")
                
                val uploadTask = fileRef.putFile(selectedImageUri!!).await()
                val storagePath = fileRef.path

                // Submit to Firestore
                viewModel.submitPayment(currentOrderId, transId, storagePath) {
                    Toast.makeText(this@PaymentVerificationActivity, "Proof submitted successfully. Awaiting verification.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentVerificationActivity, "Failed to upload proof: ${e.message}", Toast.LENGTH_LONG).show()
                // Fallback: allow submission without image if storage fails (optional, but following requirements for "real")
                // Better to show setup required if Storage is missing
                if (e.message?.contains("bucket") == true) {
                    Toast.makeText(this@PaymentVerificationActivity, "Firebase Storage setup required in console.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitVerification.isEnabled = true
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@PaymentVerificationActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }
}
