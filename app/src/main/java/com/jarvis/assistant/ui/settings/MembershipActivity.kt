package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.MembershipPlan
import com.jarvis.assistant.databinding.ActivityMembershipBinding
import com.jarvis.assistant.util.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class MembershipActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMembershipBinding
    private lateinit var viewModel: MembershipViewModel
    private lateinit var adapter: MembershipPlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        binding = ActivityMembershipBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.root)

        viewModel = ViewModelProvider(this)[MembershipViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupFaq()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MembershipPlanAdapter(
            onSelect = { plan ->
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("PLAN_ID", plan.id)
                startActivity(intent)
            },
            onPreview = { plan ->
                showPlanPreview(plan)
            }
        )
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.plans.collectLatest { plans ->
                if (plans.isEmpty()) {
                    viewModel.initializePlans() // Auto-init if empty for demo/setup
                } else {
                    adapter.submitList(plans)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.currentMembership.collectLatest { membership ->
                if (membership != null) {
                    binding.tvCurrentPlanName.text = "Premium - ${membership.planId.capitalize()}"
                    val dateStr = membership.expiresAt?.toDate()?.let { 
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                    } ?: "Lifetime"
                    binding.tvMembershipExpiry.text = if (membership.lifetime) "Unlimited Lifetime Access" else "Expires: $dateStr"
                    binding.cardCurrentStatus.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")))
                } else {
                    binding.tvCurrentPlanName.text = "Free Version"
                    binding.tvMembershipExpiry.text = "Limited access to features"
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isAdmin.collectLatest { isAdmin ->
                binding.btnAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
            }
        }

        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
        }

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@MembershipActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupFaq() {
        val faqs = listOf(
            "What is Angel AI Premium?" to "Premium gives you access to advanced AI models, faster response times, and exclusive features like custom avatars and device automation.",
            "Can I cancel anytime?" to "Subscriptions are one-time payments for a fixed duration. You won't be charged automatically after the period ends.",
            "How long does verification take?" to "Manual payment verification usually takes 1-6 hours. Once approved, your premium features are activated instantly.",
            "Is the Lifetime plan really lifetime?" to "Yes! The Ultimate Lifetime plan provides access to all current and future premium features for as long as Angel AI is operational."
        )

        for ((q, a) in faqs) {
            val faqView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, binding.layoutFaq, false)
            faqView.findViewById<TextView>(android.R.id.text1).apply {
                text = q
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(0, 16, 0, 4)
            }
            faqView.findViewById<TextView>(android.R.id.text2).apply {
                text = a
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 12f
                setPadding(0, 0, 0, 16)
            }
            binding.layoutFaq.addView(faqView)
        }
    }

    private fun showPlanPreview(plan: MembershipPlan) {
        val message = """
            Plan: ${plan.name}
            Price: ₹${plan.priceMinorUnits / 100}
            Duration: ${if (plan.lifetime) "Lifetime" else "${plan.durationMonths} Months"}
            
            Benefits:
            ${plan.benefits.joinToString("\n") { "• $it" }}
            
            Terms:
            • Non-refundable once activated
            • Secure UPI/Binance payment
            • Manual verification required
        """.trimIndent()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("${plan.name} Plan Preview")
            .setMessage(message)
            .setPositiveButton("Select Plan") { _, _ ->
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("PLAN_ID", plan.id)
                startActivity(intent)
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private class MembershipPlanAdapter(
        private val onSelect: (MembershipPlan) -> Unit,
        private val onPreview: (MembershipPlan) -> Unit
    ) : RecyclerView.Adapter<MembershipPlanAdapter.ViewHolder>() {

        private var plans = emptyList<MembershipPlan>()

        fun submitList(newPlans: List<MembershipPlan>) {
            plans = newPlans
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPlanName)
            val tvPrice: TextView = view.findViewById(R.id.tvPlanPrice)
            val tvDesc: TextView = view.findViewById(R.id.tvPlanDescription)
            val tvPriceSub: TextView = view.findViewById(R.id.tvPriceSub)
            val layoutBenefits: LinearLayout = view.findViewById(R.id.layoutBenefits)
            val btnSelect: MaterialButton = view.findViewById(R.id.btnSelectPlan)
            val btnPreview: MaterialButton = view.findViewById(R.id.btnPreviewPlan)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_membership_plan, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val plan = plans[position]
            holder.tvName.text = plan.name
            holder.tvPrice.text = "₹${plan.priceMinorUnits / 100}"
            holder.tvDesc.text = plan.description
            holder.tvPriceSub.text = if (plan.lifetime) "One-time" else "/ ${plan.durationMonths} month${if (plan.durationMonths > 1) "s" else ""}"

            holder.layoutBenefits.removeAllViews()
            for (benefit in plan.benefits.take(3)) {
                val tv = TextView(holder.itemView.context).apply {
                    text = "✓ $benefit"
                    setTextColor(Color.parseColor("#10B981"))
                    textSize = 11f
                    setPadding(0, 4, 0, 4)
                }
                holder.layoutBenefits.addView(tv)
            }

            holder.btnSelect.setOnClickListener { onSelect(plan) }
            holder.btnPreview.setOnClickListener { onPreview(plan) }
        }

        override fun getItemCount() = plans.size
    }
}
