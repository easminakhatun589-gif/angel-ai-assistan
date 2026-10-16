package com.jarvis.assistant.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

class ApkExportService(private val context: Context) {

    data class ExportResult(
        val success: Boolean,
        val exportedFile: File?,
        val splitFiles: List<File>,
        val message: String,
        val hash: String? = null
    )

    fun exportApplication(packageName: String): ExportResult {
        try {
            val pm = context.packageManager
            val appInfo: ApplicationInfo = try {
                pm.getApplicationInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return ExportResult(false, null, emptyList(), "Package not found: $packageName")
            }

            val sourceDir = appInfo.sourceDir
            if (sourceDir.isNullOrBlank()) {
                return ExportResult(false, null, emptyList(), "Source APK path is blank for $packageName")
            }

            val sourceFile = File(sourceDir)
            if (!sourceFile.exists()) {
                return ExportResult(false, null, emptyList(), "Source APK file does not exist at $sourceDir")
            }

            // Secure internal/external export directory
            val exportDir = File(context.getExternalFilesDir(null), "exported_apks").apply {
                if (!exists()) mkdirs()
            }

            val appLabel = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName
            }

            val safeName = appLabel.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val pInfo = pm.getPackageInfo(packageName, 0)
            val versionName = pInfo.versionName ?: "1.0"

            val destFile = File(exportDir, "${safeName}_${versionName}.apk")
            copyFile(sourceFile, destFile)

            if (!destFile.exists() || destFile.length() <= 0) {
                return ExportResult(false, null, emptyList(), "Failed to write exported APK file")
            }

            val hash = calculateSha256(destFile)

            val splitFilesList = mutableListOf<File>()
            val splitDirs = appInfo.splitSourceDirs
            if (!splitDirs.isNullOrEmpty()) {
                for ((index, splitPath) in splitDirs.withIndex()) {
                    val splitSrc = File(splitPath)
                    if (splitSrc.exists()) {
                        val splitDest = File(exportDir, "${safeName}_split_$index.apk")
                        copyFile(splitSrc, splitDest)
                        if (splitDest.exists() && splitDest.length() > 0) {
                            splitFilesList.add(splitDest)
                        }
                    }
                }
            }

            val sizeMb = String.format(Locale.getDefault(), "%.2f MB", destFile.length().toDouble() / (1024 * 1024))
            return ExportResult(
                success = true,
                exportedFile = destFile,
                splitFiles = splitFilesList,
                message = "Successfully exported $appLabel ($sizeMb) to secure directory.",
                hash = hash
            )
        } catch (e: Exception) {
            return ExportResult(false, null, emptyList(), "Export error: ${e.message}")
        }
    }

    fun getSecureUriForFile(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    fun copyFileToUri(source: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(source).use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun calculateSha256(file: File): String? {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }
}
