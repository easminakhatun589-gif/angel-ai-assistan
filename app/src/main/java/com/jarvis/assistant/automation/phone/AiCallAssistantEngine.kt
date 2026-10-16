package com.jarvis.assistant.automation.phone

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.jarvis.assistant.JarvisApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class CallAssistantState {
    IDLE,
    INCOMING_CALL_DETECTED,
    ELIGIBILITY_CHECK,
    WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER,
    CONNECTING_AI,
    LISTENING,
    THINKING,
    SPEAKING
}

class AiCallAssistantEngine(private val context: Context) {
    private val prefs = JarvisApp.instance.preferences
    private val _currentState = MutableStateFlow(CallAssistantState.IDLE)
    val currentState: StateFlow<CallAssistantState> = _currentState

    private val _statusMessage = MutableStateFlow("Call Assistant Idle")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var conversationTimerJob: Job? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langPref = prefs.callAssistantLanguage
                val locale = when (langPref.lowercase()) {
                    "bengali", "bn" -> Locale("bn", "IN")
                    "english", "en" -> Locale.US
                    else -> Locale.getDefault()
                }
                val result = tts?.setLanguage(locale)
                isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
                if (!isTtsReady) {
                    tts?.setLanguage(Locale.US)
                    isTtsReady = true
                }
                tts?.setSpeechRate(prefs.callAssistantSpeakingSpeed)
            }
        }
    }

    fun simulateIncomingCall(callerName: String, callerNumber: String, isWhatsApp: Boolean) {
        if (!prefs.isCallAssistantEnabled) {
            updateState(CallAssistantState.IDLE, "Call assistant is disabled in settings.")
            return
        }

        if (isWhatsApp && !prefs.whatsappCallAssistanceEnabled) {
            updateState(CallAssistantState.IDLE, "WhatsApp call assistance is disabled.")
            return
        }

        if (!isWhatsApp && !prefs.simCallAssistanceEnabled) {
            updateState(CallAssistantState.IDLE, "SIM call assistance is disabled.")
            return
        }

        // Check Allowlist / Blocklist
        val blocked = prefs.callAssistantBlocked.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (blocked.any { callerNumber.contains(it, true) || callerName.contains(it, true) }) {
            updateState(CallAssistantState.IDLE, "Caller $callerName ($callerNumber) is blocked.")
            return
        }

        scope.launch {
            // State 1: INCOMING_CALL_DETECTED
            updateState(CallAssistantState.INCOMING_CALL_DETECTED, "Incoming call detected from $callerName ($callerNumber)")
            delay(800)

            // State 2: ELIGIBILITY_CHECK
            updateState(CallAssistantState.ELIGIBILITY_CHECK, "Checking caller eligibility & allowlist...")
            delay(600)

            val allowlist = prefs.callAssistantAllowlist.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val isAllowed = allowlist.isEmpty() || allowlist.any { callerNumber.contains(it, true) || callerName.contains(it, true) }

            if (!isAllowed) {
                updateState(CallAssistantState.IDLE, "Caller not in allowlist. Call ignored.")
                return@launch
            }

            // State 3: WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER
            val autoAnswer = prefs.callAssistantAutoAnswer
            if (autoAnswer) {
                updateState(CallAssistantState.WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER, "Auto-answering call...")
                delay(800)
                proceedToAiConnection(callerName, isWhatsApp)
            } else {
                updateState(CallAssistantState.WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER, "Waiting for manual approval or supported answer...")
            }
        }
    }

    fun approveIncomingCall(callerName: String, isWhatsApp: Boolean) {
        scope.launch {
            proceedToAiConnection(callerName, isWhatsApp)
        }
    }

    private suspend fun proceedToAiConnection(callerName: String, isWhatsApp: Boolean) {
        // State 4: CONNECTING_AI
        updateState(CallAssistantState.CONNECTING_AI, "Connecting to secure AI call backend...")
        delay(1000)

        // State 5: LISTENING
        updateState(CallAssistantState.LISTENING, "Listening to participant...")
        _lastTranscript.value = "Participant: Hello, is $callerName available?"
        delay(1200)

        // State 6: THINKING
        updateState(CallAssistantState.THINKING, "AI analyzing speech & formulating Bengali/English response...")
        delay(1000)

        // State 7: SPEAKING
        val greeting = prefs.callAssistantCustomGreeting
        val responseText = "Hello! This is Angel AI, an automated AI assistant speaking on behalf of ${prefs.userName.ifBlank { "MD Aslam" }}. $greeting"
        updateState(CallAssistantState.SPEAKING, "Speaking: \"$responseText\"")
        _lastTranscript.value = "AI: $responseText"
        
        speakText(responseText)

        // Start max duration timer
        val maxDurationSec = prefs.callAssistantMaxDurationSeconds
        conversationTimerJob?.cancel()
        conversationTimerJob = scope.launch {
            delay((maxDurationSec * 1000).toLong())
            if (_currentState.value != CallAssistantState.IDLE) {
                stopAssistance("Max conversation duration reached.")
            }
        }
    }

    private fun speakText(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "call_assistant_utterance")
        }
    }

    fun stopAssistance(reason: String = "Assistance ended by user.") {
        conversationTimerJob?.cancel()
        tts?.stop()
        updateState(CallAssistantState.IDLE, reason)
    }

    private fun updateState(newState: CallAssistantState, message: String) {
        _currentState.value = newState
        _statusMessage.value = message
        Log.d("AiCallAssistantEngine", "State: $newState | $message")
    }

    fun release() {
        conversationTimerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
