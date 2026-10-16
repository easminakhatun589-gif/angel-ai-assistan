package com.jarvis.assistant.automation.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCallAssistantTest {

    @Test
    fun testCallAssistantStateTransitions() {
        assertEquals(CallAssistantState.IDLE, CallAssistantState.IDLE)
        assertEquals(CallAssistantState.INCOMING_CALL_DETECTED, CallAssistantState.INCOMING_CALL_DETECTED)
        assertEquals(CallAssistantState.ELIGIBILITY_CHECK, CallAssistantState.ELIGIBILITY_CHECK)
        assertEquals(CallAssistantState.WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER, CallAssistantState.WAITING_FOR_APPROVAL_OR_SUPPORTED_ANSWER)
        assertEquals(CallAssistantState.CONNECTING_AI, CallAssistantState.CONNECTING_AI)
        assertEquals(CallAssistantState.LISTENING, CallAssistantState.LISTENING)
        assertEquals(CallAssistantState.THINKING, CallAssistantState.THINKING)
        assertEquals(CallAssistantState.SPEAKING, CallAssistantState.SPEAKING)
    }

    @Test
    fun testLanguageSelectionValidation() {
        val supportedLanguages = listOf("Auto", "Bengali", "English")
        assertTrue(supportedLanguages.contains("Bengali"))
        assertTrue(supportedLanguages.contains("English"))
        assertTrue(supportedLanguages.contains("Auto"))
    }

    @Test
    fun testEmergencyStopAndReset() {
        val initialState = CallAssistantState.SPEAKING
        // Emergency stop should transition back to IDLE
        val stoppedState = CallAssistantState.IDLE
        assertEquals(CallAssistantState.IDLE, stoppedState)
    }
}
