package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityApkManagerBinding
import com.jarvis.assistant.util.ApkExportService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApkManagerBinding
    private val appList = mutableListOf<ApkAppItem>()
    private val filteredList = mutableListOf<ApkAppItem>()
    private lateinit var adapter: ApkAppAdapter
    private var isShowingSaved = false

    private var isMultiSelectMode = false
    private val selectedPackages = mutableSetOf<String>()
    private lateinit var exportService: ApkExportService

    private var pendingSaveFile: File? = null

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { saveToUri(it) }
    }

    data class ApkAppItem(
        val name: String,
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val icon: Drawable,
        val sourceDir: String,
        val splitSourceDirs: Array<String>?,
        val isSystemApp: Boolean,
        val isExportedFile: Boolean = false,
        val file: File? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(window.decorView)
        binding = ActivityApkManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exportService = ApkExportService(this)

        setSupportActionBar(binding.toolbarApkManager)
        binding.toolbarApkManager.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupListeners()
        loadInstalledApps()
    }

    private fun setupRecyclerView() {
        adapter = ApkAppAdapter(
            filteredList,
            isMultiSelectMode = { isMultiSelectMode },
            isShowingSaved = { isShowingSaved },
            selectedPackages = selectedPackages,
            onExportClick = { app -> exportApk(app) },
            onShareClick = { app -> if (app.isExportedFile) shareFile(app.file!!, app.name) else shareApkDirectly(app) },
            onInfoClick = { app -> openAppSettings(app) },
            onDownloadClick = { app -> if (app.isExportedFile) initiateDownload(app.file!!) else exportAndDownload(app) },
            onDeleteClick = { app -> if (app.isExportedFile) confirmDelete(app.file!!) },
            onItemClick = { app ->
                if (isMultiSelectMode) {
                    toggleSelection(app.packageName)
                }
            }
        )
        binding.rvApkManager.layoutManager = LinearLayoutManager(this)
        binding.rvApkManager.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnToggleSaved.setOnClickListener {
            if (isShowingSaved) {
                isShowingSaved = false
                binding.btnToggleSaved.text = "My APK Files"
                binding.chipGroupFilters.visibility = View.VISIBLE
                filterApps(binding.etSearchApp.text.toString())
            } else {
                isShowingSaved = true
                binding.btnToggleSaved.text = "Installed Apps"
                binding.chipGroupFilters.visibility = View.GONE
                loadSavedApks()
            }
            exitMultiSelectMode()
        }

        binding.btnMultiSelect.setOnClickListener {
            if (isShowingSaved) {
                Toast.makeText(this, "Multi-select is available for installed apps", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isMultiSelectMode = !isMultiSelectMode
            if (isMultiSelectMode) {
                binding.btnMultiSelect.text = "Cancel"
                binding.layoutMultiSelectBar.visibility = View.VISIBLE
            } else {
                exitMultiSelectMode()
            }
            adapter.notifyDataSetChanged()
        }

        binding.btnSelectAll.setOnClickListener {
            if (selectedPackages.size == filteredList.size) {
                selectedPackages.clear()
                binding.btnSelectAll.text = "Select All"
            } else {
                selectedPackages.clear()
                for (app in filteredList) {
                    selectedPackages.add(app.packageName)
                }
                binding.btnSelectAll.text = "Deselect All"
            }
            updateSelectedCount()
            adapter.notifyDataSetChanged()
        }

        binding.btnExportZip.setOnClickListener {
            exportSelectedAsZip()
        }

        binding.etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isShowingSaved) {
                    filterApps(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipFilterAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isShowingSaved) filterApps(binding.etSearchApp.text.toString())
        }
        binding.chipFilterUser.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isShowingSaved) filterApps(binding.etSearchApp.text.toString())
        }
        binding.chipFilterSystem.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isShowingSaved) filterApps(binding.etSearchApp.text.toString())
        }
        binding.chipFilterSplit.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isShowingSaved) filterApps(binding.etSearchApp.text.toString())
        }
    }

    private fun toggleSelection(packageName: String) {
        if (selectedPackages.contains(packageName)) {
            selectedPackages.remove(packageName)
        } else {
            selectedPackages.add(packageName)
        }
        updateSelectedCount()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectedCount() {
        val count = selectedPackages.size
        binding.tvSelectedCount.text = "$count application${if (count == 1) "" else "s"} selected"
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedPackages.clear()
        binding.btnMultiSelect.text = "Select"
        binding.layoutMultiSelectBar.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    private fun loadInstalledApps() {
        appList.clear()
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (pkgInfo in packages) {
                try {
                    val pInfo = packageManager.getPackageInfo(pkgInfo.packageName, 0)
                    val appName = pkgInfo.loadLabel(packageManager).toString()
                    val icon = pkgInfo.loadIcon(packageManager)
                    val versionName = pInfo.versionName ?: "1.0"
                    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode.toLong()
                    }
                    val isSystem = (pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val sourceDir = pkgInfo.sourceDir ?: ""
                    val splitDirs = pkgInfo.splitSourceDirs

                    appList.add(
                        ApkAppItem(
                            name = appName,
                            packageName = pkgInfo.packageName,
                            versionName = versionName,
                            versionCode = versionCode,
                            icon = icon,
                            sourceDir = sourceDir,
                            splitSourceDirs = splitDirs,
                            isSystemApp = isSystem
                        )
                    )
                } catch (_: Exception) {
                }
            }
            appList.sortBy { it.name.lowercase() }
            filterApps("")
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading apps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterApps(query: String) {
        filteredList.clear()
        val isUser = binding.chipFilterUser.isChecked
        val isSystem = binding.chipFilterSystem.isChecked
        val isSplit = binding.chipFilterSplit.isChecked

        for (app in appList) {
            val matchesType = when {
                isUser -> !app.isSystemApp
                isSystem -> app.isSystemApp
                isSplit -> !app.splitSourceDirs.isNullOrEmpty()
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    app.name.contains(query, true) ||
                    app.packageName.contains(query, true)

            if (matchesType && matchesQuery) {
                filteredList.add(app)
            }
        }
        adapter.notifyDataSetChanged()
        binding.tvAppStats.text = "${filteredList.size} of ${appList.size} applications shown"
    }

    private fun exportSelectedAsZip() {
        if (selectedPackages.isEmpty()) {
            Toast.makeText(this, "Please select at least one application", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val exportDir = File(getExternalFilesDir(null), "exported_apks")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(exportDir, "Selected_APKs_$timeStamp.zip")

            var successCount = 0
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (item in appList) {
                    if (selectedPackages.contains(item.packageName)) {
                        val sourceFile = File(item.sourceDir)
                        if (sourceFile.exists()) {
                            val safeName = item.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val entryName = "${safeName}_${item.versionName}.apk"
                            zos.putNextEntry(ZipEntry(entryName))
                            FileInputStream(sourceFile).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                            successCount++

                            // Include split APKs if any
                            if (!item.splitSourceDirs.isNullOrEmpty()) {
                                for ((idx, splitPath) in item.splitSourceDirs.withIndex()) {
                                    val splitSrc = File(splitPath)
                                    if (splitSrc.exists()) {
                                        zos.putNextEntry(ZipEntry("${safeName}_split_$idx.apk"))
                                        FileInputStream(splitSrc).use { fis ->
                                            fis.copyTo(zos)
                                        }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                    }
                }
                zos.finish()
            }

            if (zipFile.exists() && zipFile.length() > 0) {
                val hash = exportService.calculateSha256(zipFile) ?: "Unknown"
                val sizeMb = String.format(Locale.getDefault(), "%.2f MB", zipFile.length().toDouble() / (1024 * 1024))
                MaterialAlertDialogBuilder(this)
                    .setTitle("ZIP Export Successful")
                    .setMessage("Successfully compressed $successCount applications into ZIP ($sizeMb).\nSHA-256: $hash\nSaved to: ${zipFile.absolutePath}")
                    .setPositiveButton("Share ZIP") { _, _ ->
                        shareFile(zipFile, "Selected APKs ZIP")
                    }
                    .setNeutralButton("Save to Device") { _, _ ->
                        initiateDownload(zipFile)
                    }
                    .setNegativeButton("OK", null)
                    .show()
                exitMultiSelectMode()
            } else {
                Toast.makeText(this, "ZIP export failed: file is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "ZIP export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportApk(app: ApkAppItem) {
        val result = exportService.exportApplication(app.packageName)
        if (result.success && result.exportedFile != null) {
            val sizeMb = String.format(Locale.getDefault(), "%.2f MB", result.exportedFile.length().toDouble() / (1024 * 1024))
            val splitNote = if (result.splitFiles.isNotEmpty()) "\nNote: This app uses Split APKs (${result.splitFiles.size} parts exported)." else ""
            
            MaterialAlertDialogBuilder(this)
                .setTitle("APK Export Successful")
                .setMessage("Successfully exported ${app.name} ($sizeMb).\nSHA-256: ${result.hash ?: "N/A"}\nSaved to: ${result.exportedFile.absolutePath}$splitNote")
                .setPositiveButton("Share") { _, _ ->
                    shareFile(result.exportedFile, app.name)
                }
                .setNeutralButton("Save to Device") { _, _ ->
                    initiateDownload(result.exportedFile)
                }
                .setNegativeButton("OK", null)
                .show()
        } else {
            Toast.makeText(this, "Export failed: ${result.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportAndDownload(app: ApkAppItem) {
        val result = exportService.exportApplication(app.packageName)
        if (result.success && result.exportedFile != null) {
            initiateDownload(result.exportedFile)
        } else {
            Toast.makeText(this, "Export failed: ${result.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initiateDownload(file: File) {
        pendingSaveFile = file
        saveFileLauncher.launch(file.name)
    }

    private fun saveToUri(uri: Uri) {
        val source = pendingSaveFile
        if (source == null || !source.exists()) {
            Toast.makeText(this, "Source file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val success = exportService.copyFileToUri(source, uri)
        if (success) {
            Toast.makeText(this, "File saved successfully to device", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed to save file to device", Toast.LENGTH_LONG).show()
        }
        pendingSaveFile = null
    }

    private fun confirmDelete(file: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (file.delete()) {
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()
                    loadSavedApks()
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareApkDirectly(app: ApkAppItem) {
        val result = exportService.exportApplication(app.packageName)
        if (result.success && result.exportedFile != null) {
            shareFile(result.exportedFile, app.name)
        } else {
            Toast.makeText(this, "Unable to share APK: ${result.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File, appName: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val mimeType = if (file.extension.lowercase() == "zip") "application/zip" else "application/vnd.android.package-archive"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sharing: $appName")
                putExtra(Intent.EXTRA_TEXT, "Here is the exported file ($appName) via Angel AI.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share via WhatsApp or other apps")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "Sharing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppSettings(app: ApkAppItem) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedApks() {
        appList.clear()
        val exportDir = File(getExternalFilesDir(null), "exported_apks")
        if (exportDir.exists()) {
            val files = exportDir.listFiles { file -> 
                val ext = file.extension.lowercase()
                ext == "apk" || ext == "zip"
            }
            if (files != null) {
                files.sortByDescending { it.lastModified() }
                for (file in files) {
                    val sizeMb = String.format(Locale.getDefault(), "%.2f MB", file.length().toDouble() / (1024 * 1024))
                    appList.add(
                        ApkAppItem(
                            name = file.name,
                            packageName = file.absolutePath,
                            versionName = sizeMb,
                            versionCode = file.lastModified(),
                            icon = getDrawable(R.drawable.ic_apk) ?: resources.getDrawable(android.R.drawable.sym_def_app_icon, null),
                            sourceDir = file.absolutePath,
                            splitSourceDirs = null,
                            isSystemApp = false,
                            isExportedFile = true,
                            file = file
                        )
                    )
                }
            }
        }
        filteredList.clear()
        filteredList.addAll(appList)
        adapter.notifyDataSetChanged()
        binding.tvAppStats.text = "${filteredList.size} Saved APK / ZIP Files"
    }

    class ApkAppAdapter(
        private val items: List<ApkAppItem>,
        private val isMultiSelectMode: () -> Boolean,
        private val isShowingSaved: () -> Boolean,
        private val selectedPackages: Set<String>,
        private val onExportClick: (ApkAppItem) -> Unit,
        private val onShareClick: (ApkAppItem) -> Unit,
        private val onInfoClick: (ApkAppItem) -> Unit,
        private val onDownloadClick: (ApkAppItem) -> Unit,
        private val onDeleteClick: (ApkAppItem) -> Unit,
        private val onItemClick: (ApkAppItem) -> Unit
    ) : RecyclerView.Adapter<ApkAppAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
            val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
            val tvName: TextView = view.findViewById(R.id.tvAppName)
            val tvPackage: TextView = view.findViewById(R.id.tvPackageName)
            val tvMeta: TextView = view.findViewById(R.id.tvAppMeta)
            val btnInfo: MaterialButton = view.findViewById(R.id.btnAppInfo)
            val btnShare: MaterialButton = view.findViewById(R.id.btnShareApk)
            val btnExport: MaterialButton = view.findViewById(R.id.btnExportApk)
            val btnDownload: MaterialButton = view.findViewById(R.id.btnDownload)
            val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteApk)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_apk_app, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivIcon.setImageDrawable(item.icon)
            holder.tvName.text = item.name
            holder.tvPackage.text = if (item.isExportedFile) item.file?.absolutePath else item.packageName
            holder.tvMeta.text = if (item.isExportedFile) {
                "Size: ${item.versionName}"
            } else {
                "v${item.versionName} • ${if (item.isSystemApp) "System" else "User"} App${if (!item.splitSourceDirs.isNullOrEmpty()) " • Split APK" else ""}"
            }

            val multiSelect = isMultiSelectMode()
            val showingSaved = isShowingSaved()

            holder.cbSelect.visibility = if (multiSelect && !showingSaved) View.VISIBLE else View.GONE
            holder.cbSelect.isChecked = selectedPackages.contains(item.packageName)

            holder.btnExport.visibility = if (showingSaved) View.GONE else View.VISIBLE
            holder.btnInfo.visibility = if (showingSaved) View.GONE else View.VISIBLE
            holder.btnDownload.visibility = View.VISIBLE
            holder.btnDelete.visibility = if (showingSaved) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                if (multiSelect) onItemClick(item)
                else if (showingSaved) onDownloadClick(item)
                else onInfoClick(item)
            }

            holder.btnExport.setOnClickListener { onExportClick(item) }
            holder.btnShare.setOnClickListener { onShareClick(item) }
            holder.btnInfo.setOnClickListener { onInfoClick(item) }
            holder.btnDownload.setOnClickListener { onDownloadClick(item) }
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
