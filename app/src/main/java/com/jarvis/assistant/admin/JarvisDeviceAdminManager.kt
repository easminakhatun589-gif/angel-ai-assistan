package com.jarvis.assistant.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

object JarvisDeviceAdminManager {
    const val REQUEST_CODE_ENABLE_ADMIN = 1001

    fun getComponentName(context: Context): ComponentName {
        return ComponentName(context, JarvisDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        return dpm?.isAdminActive(getComponentName(context)) == true
    }

    fun requestDeviceAdmin(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(activity))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Jarvis Device Administrator to enable secure screen lock and advanced device security controls."
            )
        }
        activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    fun lockDevice(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val comp = getComponentName(context)
        if (dpm?.isAdminActive(comp) == true) {
            try {
                dpm.lockNow()
                return true
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to lock device: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Device Administrator permission is not active", Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
