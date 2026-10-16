package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityMobileAppsControlBinding

class MobileAppsControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMobileAppsControlBinding
    private val appList = mutableListOf<AppItem>()
    private val filteredList = mutableListOf<AppItem>()
    private lateinit var appAdapter: AppAdapter

    data class AppItem(
        val name: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable,
        val isSystemApp: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(window.decorView)
        binding = ActivityMobileAppsControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMobileApps)
        binding.toolbarMobileApps.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupListeners()
        loadInstalledApps()
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            filteredList,
            onLaunchClick = { app ->
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    try {
                        startActivity(launchIntent)
                        Toast.makeText(this, "Launching ${app.name}...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unable to launch ${app.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "${app.name} has no launch intent", Toast.LENGTH_SHORT).show()
                }
            },
            onInfoClick = { app ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open settings for ${app.name}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvInstalledApps.layoutManager = LinearLayoutManager(this)
        binding.rvInstalledApps.adapter = appAdapter
    }

    private fun setupListeners() {
        binding.btnRefreshApps.setOnClickListener {
            loadInstalledApps()
        }

        binding.etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipFilterAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.chipFilterUser.isChecked = false
                binding.chipFilterSystem.isChecked = false
                filterApps(binding.etSearchApp.text.toString())
            }
        }

        binding.chipFilterUser.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.chipFilterAll.isChecked = false
                binding.chipFilterSystem.isChecked = false
                filterUserApps()
            }
        }

        binding.chipFilterSystem.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.chipFilterAll.isChecked = false
                binding.chipFilterUser.isChecked = false
                filterSystemApps()
            }
        }
    }

    private fun loadInstalledApps() {
        appList.clear()
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (packageInfo in packages) {
                val appName = packageInfo.loadLabel(packageManager).toString()
                val icon = packageInfo.loadIcon(packageManager)
                val isSystem = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                appList.add(AppItem(appName, packageInfo.packageName, icon, isSystem))
            }
            appList.sortBy { it.name.lowercase() }
            filterApps(binding.etSearchApp.text.toString())
            binding.tvAppStats.text = "${appList.size} Installed Apps Detected"
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading installed apps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterApps(query: String) {
        filteredList.clear()
        val isUserChecked = binding.chipFilterUser.isChecked
        val isSystemChecked = binding.chipFilterSystem.isChecked

        for (app in appList) {
            val matchesType = when {
                isUserChecked -> !app.isSystemApp
                isSystemChecked -> app.isSystemApp
                else -> true
            }
            val matchesQuery = query.isBlank() || app.name.contains(query, true) || app.packageName.contains(query, true)
            if (matchesType && matchesQuery) {
                filteredList.add(app)
            }
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun filterUserApps() {
        filteredList.clear()
        val query = binding.etSearchApp.text.toString()
        for (app in appList) {
            if (!app.isSystemApp && (query.isBlank() || app.name.contains(query, true) || app.packageName.contains(query, true))) {
                filteredList.add(app)
            }
        }
        appAdapter.notifyDataSetChanged()
    }

    private fun filterSystemApps() {
        filteredList.clear()
        val query = binding.etSearchApp.text.toString()
        for (app in appList) {
            if (app.isSystemApp && (query.isBlank() || app.name.contains(query, true) || app.packageName.contains(query, true))) {
                filteredList.add(app)
            }
        }
        appAdapter.notifyDataSetChanged()
    }

    class AppAdapter(
        private val apps: List<AppItem>,
        private val onLaunchClick: (AppItem) -> Unit,
        private val onInfoClick: (AppItem) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

        class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
            val tvAppName: TextView = view.findViewById(R.id.tvAppName)
            val tvAppPackage: TextView = view.findViewById(R.id.tvAppPackage)
            val tvAppTypeBadge: TextView = view.findViewById(R.id.tvAppTypeBadge)
            val btnAppInfo: MaterialButton = view.findViewById(R.id.btnAppInfo)
            val btnLaunchApp: MaterialButton = view.findViewById(R.id.btnLaunchApp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_installed_app, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            holder.tvAppName.text = app.name
            holder.tvAppPackage.text = app.packageName
            holder.ivAppIcon.setImageDrawable(app.icon)

            if (app.isSystemApp) {
                holder.tvAppTypeBadge.text = "SYSTEM"
                holder.tvAppTypeBadge.setTextColor(android.graphics.Color.parseColor("#B0BEC5"))
            } else {
                holder.tvAppTypeBadge.text = "USER"
                holder.tvAppTypeBadge.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
            }

            holder.btnLaunchApp.setOnClickListener { onLaunchClick(app) }
            holder.btnAppInfo.setOnClickListener { onInfoClick(app) }
        }

        override fun getItemCount() = apps.size
    }
}
