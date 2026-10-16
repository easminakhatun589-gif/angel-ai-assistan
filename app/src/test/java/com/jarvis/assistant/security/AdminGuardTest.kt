package com.jarvis.assistant.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminGuardTest {

    @Test
    fun primarySuperAdminEmailsAreAuthorized() {
        // Test developer super-admin
        assertTrue(
            "mdaslamlaskar14@gmail.com must be recognized as primary admin",
            AdminGuard.isAuthorizedEmail("mdaslamlaskar14@gmail.com")
        )

        // Test platform owner / active user
        assertTrue(
            "easminakhatun589@gmail.com must be recognized as primary admin",
            AdminGuard.isAuthorizedEmail("easminakhatun589@gmail.com")
        )
    }

    @Test
    fun caseInsensitiveAndWhitespaceHandling() {
        assertTrue(
            "Uppercase email should be handled gracefully",
            AdminGuard.isAuthorizedEmail("MDASLAMLASKAR14@GMAIL.COM")
        )
        assertTrue(
            "Mixed case with surrounding spaces should be authorized",
            AdminGuard.isAuthorizedEmail("  EasminaKhatun589@gmail.com  ")
        )
    }

    @Test
    fun unauthorizedUsersAreRejected() {
        assertFalse(
            "Random user email must NOT be authorized as admin",
            AdminGuard.isAuthorizedEmail("regularuser@example.com")
        )
        assertFalse(
            "Subdomain or spoofed email must be rejected",
            AdminGuard.isAuthorizedEmail("mdaslamlaskar14@gmail.com.fake.com")
        )
        assertFalse(
            "Null email must be rejected",
            AdminGuard.isAuthorizedEmail(null)
        )
        assertFalse(
            "Blank email must be rejected",
            AdminGuard.isAuthorizedEmail("   ")
        )
    }

    @Test
    fun evaluateAdminStatusWithWhitelistDocument() {
        // Primary admin is authorized regardless of firestore document flag
        assertTrue(
            AdminGuard.evaluateAdminStatus("easminakhatun589@gmail.com", isDocInFirestore = false)
        )
        assertTrue(
            AdminGuard.evaluateAdminStatus("mdaslamlaskar14@gmail.com", isDocInFirestore = true)
        )

        // Delegated admin is authorized if present in Firestore whitelist collection
        assertTrue(
            AdminGuard.evaluateAdminStatus("delegated_admin@jarvis.ai", isDocInFirestore = true)
        )

        // Normal user without Firestore whitelist document is blocked
        assertFalse(
            AdminGuard.evaluateAdminStatus("guest@example.com", isDocInFirestore = false)
        )
    }
}
