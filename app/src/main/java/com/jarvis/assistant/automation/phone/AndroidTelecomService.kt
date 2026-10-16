package com.jarvis.assistant.automation.phone

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AndroidTelecomService : CallScreeningService() {

    companion object {
        private const val TAG = "AndroidTelecomService"
        var currentSenderInfo: String = ""
        var currentCallDurationSeconds: Int = 0
        private var activeTimerJob: Job? = null
        var listener: ((sender: String, duration: Int) -> Unit)? = null
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val handle = callDetails.handle?.schemeSpecificPart ?: "Unknown Caller"
            val direction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                callDetails.callDirection
            } else {
                // Fallback for API < 29: direction is not easily available in onScreenCall 
                // but we can assume incoming if it's being screened.
                Call.Details.DIRECTION_INCOMING
            }
            
            Log.d(TAG, "onScreenCall: handle=$handle, direction=$direction")

            if (direction == Call.Details.DIRECTION_INCOMING) {
                currentSenderInfo = handle
                currentCallDurationSeconds = 0
                startDurationTimer()

                listener?.invoke(handle, 0)

                val responseBuilder = CallResponse.Builder()
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    responseBuilder.setSilenceCall(false)
                }
                
                respondToCall(callDetails, responseBuilder.build())
            }
        }
    }

    private fun startDurationTimer() {
        activeTimerJob?.cancel()
        activeTimerJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000)
                currentCallDurationSeconds++
                listener?.invoke(currentSenderInfo, currentCallDurationSeconds)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeTimerJob?.cancel()
    }
}
