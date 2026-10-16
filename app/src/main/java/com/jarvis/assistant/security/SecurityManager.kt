package com.jarvis.assistant.security

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import kotlin.system.exitProcess

/**
 * ANGEL AI — SECURITY MODULE
 * Developer       : MD ASLAM
 * Owner           : MD ASLAM
 * Creator Credit  : Created by MD ASLAM
 * Copyright © 2026: MD ASLAM. All Rights Reserved.
 */
object SecurityManager {

    const val DEVELOPER_NAME    = "MD ASLAM"
    const val OWNER_NAME        = "MD ASLAM"
    const val CREATOR_CREDIT    = "Created by MD ASLAM"
    const val COPYRIGHT_NOTICE  = "Copyright © 2026 MD ASLAM. All Rights Reserved."
    const val GITHUB_REPO       = "https://github.com/angelai/assistant"
    const val LICENSE_NAME      = "TAMCL v1.0 — Content creators MUST credit MD ASLAM / ANGEL AI"

    private const val EXPECTED_PACKAGE = "com.jarvis.assistant"

    fun verify(context: Context) {
        checkPackageName(context)
    }

    private fun checkPackageName(context: Context) {
        if (context.packageName != EXPECTED_PACKAGE) {
            android.util.Log.e("SecurityManager", "Clone Detected! Original: $EXPECTED_PACKAGE | Found: ${context.packageName}")
            // Cannot show Dialog from Application context. We log and exit.
            // If called from Activity, a Toast is safer.
            if (context is android.app.Activity) {
                android.widget.Toast.makeText(context, "Unauthorised clone detected. Closing.", android.widget.Toast.LENGTH_LONG).show()
            }
            exitProcess(1)
        }
    }

    @Suppress("unused")
    private fun checkSignature(context: Context) {
        val EXPECTED_SHA256 = "PASTE_YOUR_APK_SHA256_FINGERPRINT_HERE"

        val actual = getApkSignatureSha256(context) ?: run {
            showTamperAlert(context, "Verification Failed", "Unable to verify app authenticity. Closing.")
            return
        }
        if (!actual.equals(EXPECTED_SHA256, ignoreCase = true)) {
            showTamperAlert(
                context,
                "Tampered APK Detected",
                "This APK has been re-signed or modified.\n\n" +
                "$DEVELOPER_NAME only distributes APKs signed with the original certificate.\n\n" +
                "Download the official APK from:\n$GITHUB_REPO"
            )
        }
    }

    private fun getApkSignatureSha256(context: Context): String? {
        return try {
            val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.GET_SIGNATURES
                )
                info.signatures
            }
            val firstSig = sigs?.firstOrNull() ?: return null
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(firstSig.toByteArray())
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun showTamperAlert(context: Context, title: String, message: String) {
        try {
            AlertDialog.Builder(context)
                .setTitle("⚠️ $title")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ -> exitProcess(1) }
                .show()
        } catch (_: Exception) {
            exitProcess(1)
        }
    }
}
