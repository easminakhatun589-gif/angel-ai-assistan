package com.jarvis.assistant.automation.phone

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class JarvisInCallService : InCallService() {

    companion object {
        private const val TAG = "JarvisInCallService"
        var activeCall: Call? = null
            private set
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "SIM Call added: state = ${call.state}")
        activeCall = call
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                Log.d(TAG, "SIM Call state changed: $state")
                when (state) {
                    Call.STATE_RINGING -> {
                        // Incoming call detected
                        val details = call.details
                        val handle = details?.handle?.schemeSpecificPart ?: "Unknown"
                        Log.d(TAG, "Incoming ringing call from: $handle")
                    }
                    Call.STATE_ACTIVE -> {
                        Log.d(TAG, "Call active")
                    }
                    Call.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Call disconnected")
                        if (activeCall == call) {
                            activeCall = null
                        }
                    }
                    else -> {}
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "SIM Call removed")
        if (activeCall == call) {
            activeCall = null
        }
    }
}
