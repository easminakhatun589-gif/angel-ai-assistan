package com.jarvis.assistant.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.databinding.ActivityCheckoutBinding
import com.jarvis.assistant.util.ThemeManager
import coil.load
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder

class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var viewModel: MembershipViewModel
    private var planId: String? = null
    private var selectedMethod: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.root)

        viewModel = ViewModelProvider(this)[MembershipViewModel::class.java]
        planId = intent.getStringExtra("PLAN_ID")

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            selectedMethod = if (checkedId == binding.rbUpi.id) "UPI" else "Binance"
            updateInstructions()
        }

        binding.btnCopyUpi.setOnClickListener {
            val upiId = binding.tvMerchantDetails.text.toString().split("\n")[0].removePrefix("UPI ID: ")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UPI ID", upiId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "UPI ID Copied", Toast.LENGTH_SHORT).show()
        }

        binding.btnProceedToVerify.setOnClickListener {
            val selectedPlan = viewModel.plans.value.find { it.id == planId } ?: return@setOnClickListener
            viewModel.createOrder(selectedPlan, selectedMethod) { orderId ->
                val intent = Intent(this, PaymentVerificationActivity::class.java)
                intent.putExtra("ORDER_ID", orderId)
                intent.putExtra("PLAN_ID", planId)
                intent.putExtra("METHOD", selectedMethod)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.plans.collectLatest { plans ->
                val plan = plans.find { it.id == planId }
                plan?.let {
                    binding.tvCheckoutPlanName.text = it.name
                    binding.tvCheckoutPrice.text = "₹${it.priceMinorUnits / 100} (${if (it.lifetime) "Lifetime" else "${it.durationMonths} Months"})"
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.adminSettings.collectLatest { settings ->
                settings?.let {
                    if (selectedMethod == "UPI") {
                        binding.tvMerchantDetails.text = "UPI ID: ${it.merchantUpiId}\nMerchant: ${it.merchantName}"
                    } else if (selectedMethod == "Binance") {
                        binding.tvMerchantDetails.text = it.binanceMerchantDetails.ifBlank { "Binance Pay setup required" }
                        if (!it.binancePayEnabled) {
                            binding.rbBinance.isEnabled = false
                            binding.rbBinance.text = "Binance Pay (Setup Required)"
                        }
                    }
                }
            }
        }
    }

    private fun updateInstructions() {
        binding.layoutPaymentInstructions.visibility = View.VISIBLE
        val settings = viewModel.adminSettings.value
        val plan = viewModel.plans.value.find { it.id == planId }
        val amount = (plan?.priceMinorUnits ?: 0L) / 100.0

        if (selectedMethod == "UPI") {
            binding.tvInstructionHeader.text = "UPI Payment"
            val upiId = settings?.merchantUpiId ?: "mdaslamlaskar14@okaxis"
            val merchantName = settings?.merchantName ?: "MD ASLAM"
            binding.tvMerchantDetails.text = "UPI ID: $upiId\nMerchant: $merchantName"
            binding.btnCopyUpi.visibility = View.VISIBLE

            // Generate UPI URI: upi://pay?pa=<id>&pn=<name>&am=<amount>&cu=INR
            val upiUri = "upi://pay?pa=$upiId&pn=${URLEncoder.encode(merchantName, "UTF-8")}&am=$amount&cu=INR"
            val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${URLEncoder.encode(upiUri, "UTF-8")}"
            binding.ivPaymentQr.load(qrUrl)
        } else {
            binding.tvInstructionHeader.text = "Binance Pay"
            val details = settings?.binanceMerchantDetails ?: "Contact admin for Binance ID"
            binding.tvMerchantDetails.text = details
            binding.btnCopyUpi.visibility = View.GONE
            
            // For Binance, if it's just a text ID, show a generic QR or just placeholder
            // In real app, merchant supplies a QR URL in settings
            val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${URLEncoder.encode(details, "UTF-8")}"
            binding.ivPaymentQr.load(qrUrl)
        }
    }
}
