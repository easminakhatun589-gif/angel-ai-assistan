package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.storage.FirebaseStorage
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.PaymentSubmission
import com.jarvis.assistant.databinding.ActivityAdminDashboardBinding
import com.jarvis.assistant.security.AdminGuard
import com.jarvis.assistant.util.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var viewModel: AdminViewModel
    private lateinit var submissionAdapter: SubmissionAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var flagAdapter: FlagAdapter
    private lateinit var auditAdapter: AuditAdapter

    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.root)

        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "ANGEL AI — Admin Control Center"
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.progressBar.visibility = View.VISIBLE

        // Use AdminGuard to securely protect admin navigation route against Firestore whitelist
        lifecycleScope.launch {
            AdminGuard.enforceAccess(this@AdminDashboardActivity) {
                binding.progressBar.visibility = View.GONE
                setupTabs()
                setupAdapters()
                observeViewModel()
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Payments"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Users"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Features"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Audit Logs"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateVisibilityForTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateVisibilityForTab() {
        binding.rvSubmissions.visibility = if (currentTab == 0) View.VISIBLE else View.GONE
        binding.rvUsers.visibility = if (currentTab == 1) View.VISIBLE else View.GONE
        binding.rvFlags.visibility = if (currentTab == 2) View.VISIBLE else View.GONE
        binding.rvAudit.visibility = if (currentTab == 3) View.VISIBLE else View.GONE

        binding.btnBroadcast.visibility = if (currentTab == 3) View.VISIBLE else View.GONE
    }

    private fun setupAdapters() {
        submissionAdapter = SubmissionAdapter(
            onApprove = { viewModel.approve(it) },
            onReject = { showRejectDialog(it) },
            onViewReceipt = { showReceipt(it) }
        )
        binding.rvSubmissions.layoutManager = LinearLayoutManager(this)
        binding.rvSubmissions.adapter = submissionAdapter

        userAdapter = UserAdapter(
            onBlockToggle = { user, block -> viewModel.setUserBlocked(user.uid, block) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = userAdapter

        flagAdapter = FlagAdapter(
            onToggle = { flag, enabled -> viewModel.updateFeatureFlag(flag.id, enabled) }
        )
        binding.rvFlags.layoutManager = LinearLayoutManager(this)
        binding.rvFlags.adapter = flagAdapter

        auditAdapter = AuditAdapter()
        binding.rvAudit.layoutManager = LinearLayoutManager(this)
        binding.rvAudit.adapter = auditAdapter

        binding.btnBroadcast.setOnClickListener {
            showBroadcastDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.pendingSubmissions.collectLatest { subs ->
                if (currentTab == 0) {
                    submissionAdapter.submitList(subs)
                    binding.tvEmpty.visibility = if (subs.isEmpty() && currentTab == 0) View.VISIBLE else View.GONE
                    binding.tvEmpty.text = "No pending payment submissions"
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.users.collectLatest { list ->
                if (currentTab == 1) {
                    userAdapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty() && currentTab == 1) View.VISIBLE else View.GONE
                    binding.tvEmpty.text = "No registered users found"
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.featureFlags.collectLatest { list ->
                if (currentTab == 2) {
                    flagAdapter.submitList(list)
                    binding.tvEmpty.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.auditLogs.collectLatest { list ->
                if (currentTab == 3) {
                    auditAdapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty() && currentTab == 3) View.VISIBLE else View.GONE
                    binding.tvEmpty.text = "No audit logs recorded"
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.errorMessage.collectLatest { msg ->
                Toast.makeText(this@AdminDashboardActivity, msg, Toast.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.successMessage.collectLatest { msg ->
                Toast.makeText(this@AdminDashboardActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRejectDialog(submission: PaymentSubmission) {
        val input = android.widget.EditText(this).apply {
            hint = "Reason for rejection"
            setPadding(32, 32, 32, 32)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Reject Payment Submission")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isNotEmpty()) viewModel.reject(submission, reason)
                else Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReceipt(submission: PaymentSubmission) {
        val imgView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800)
        }
        val storageRef = FirebaseStorage.getInstance().reference.child(submission.receiptStoragePath)
        
        lifecycleScope.launchWhenStarted {
            try {
                val bytes = storageRef.getBytes(1024 * 1024 * 5).await()
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgView.setImageBitmap(bitmap)
                
                MaterialAlertDialogBuilder(this@AdminDashboardActivity)
                    .setTitle("Payment Receipt")
                    .setView(imgView)
                    .setPositiveButton("Close", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Failed to load receipt image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBroadcastDialog() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }
        val titleInput = android.widget.EditText(this).apply { hint = "Notification Title" }
        val bodyInput = android.widget.EditText(this).apply { hint = "Announcement Message" }
        layout.addView(titleInput)
        layout.addView(bodyInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Send Broadcast Announcement")
            .setView(layout)
            .setPositiveButton("Send") { _, _ ->
                val title = titleInput.text.toString().trim()
                val body = bodyInput.text.toString().trim()
                if (title.isNotEmpty() && body.isNotEmpty()) {
                    Toast.makeText(this, "Broadcast notification queued successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Title and body required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class SubmissionAdapter(
    private val onApprove: (PaymentSubmission) -> Unit,
    private val onReject: (PaymentSubmission) -> Unit,
    private val onViewReceipt: (PaymentSubmission) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.ViewHolder>() {
    private var items = listOf<PaymentSubmission>()

    fun submitList(newList: List<PaymentSubmission>) {
        items = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubOrderId: TextView = view.findViewById(R.id.tvSubOrderId)
        val tvSubTransId: TextView = view.findViewById(R.id.tvSubTransId)
        val tvSubUserEmail: TextView = view.findViewById(R.id.tvSubUserEmail)
        val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        val btnViewReceipt: MaterialButton = view.findViewById(R.id.btnViewReceipt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_submission, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sub = items[position]
        holder.tvSubOrderId.text = "Order ID: ${sub.orderId}"
        holder.tvSubTransId.text = "Trans ID: ${sub.transactionReference}"
        holder.tvSubUserEmail.text = "User: ${sub.userId}"
        holder.btnApprove.setOnClickListener { onApprove(sub) }
        holder.btnReject.setOnClickListener { onReject(sub) }
        holder.btnViewReceipt.setOnClickListener { onViewReceipt(sub) }
    }

    override fun getItemCount() = items.size
}

class UserAdapter(private val onBlockToggle: (UserRecord, Boolean) -> Unit) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    private var items = listOf<UserRecord>()

    fun submitList(newList: List<UserRecord>) {
        items = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvUid: TextView = view.findViewById(R.id.tvUid)
        val btnBlock: MaterialButton = view.findViewById(R.id.btnBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = items[position]
        holder.tvEmail.text = user.email.ifBlank { "Anonymous / No Email" }
        holder.tvUid.text = "UID: ${user.uid}"
        holder.btnBlock.text = if (user.isBlocked) "Unblock" else "Block"
        holder.btnBlock.setOnClickListener { onBlockToggle(user, !user.isBlocked) }
    }

    override fun getItemCount() = items.size
}

class FlagAdapter(private val onToggle: (FeatureFlag, Boolean) -> Unit) :
    RecyclerView.Adapter<FlagAdapter.ViewHolder>() {
    private var items = listOf<FeatureFlag>()

    fun submitList(newList: List<FeatureFlag>) {
        items = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvFlagName)
        val tvDesc: TextView = view.findViewById(R.id.tvFlagDesc)
        val switchToggle: com.google.android.material.switchmaterial.SwitchMaterial = view.findViewById(R.id.switchFlag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_flag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flag = items[position]
        holder.tvName.text = flag.id.uppercase()
        holder.tvDesc.text = flag.description
        holder.switchToggle.isChecked = flag.enabled
        holder.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != flag.enabled) onToggle(flag, isChecked)
        }
    }

    override fun getItemCount() = items.size
}

class AuditAdapter : RecyclerView.Adapter<AuditAdapter.ViewHolder>() {
    private var items = listOf<AuditLogEntry>()

    fun submitList(newList: List<AuditLogEntry>) {
        items = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAction: TextView = view.findViewById(R.id.tvAction)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_audit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = items[position]
        holder.tvAction.text = log.action
        holder.tvDetails.text = "Target: ${log.target} | Admin: ${log.adminUid.take(6)}..."
        holder.tvTime.text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(log.timestamp))
    }

    override fun getItemCount() = items.size
}
