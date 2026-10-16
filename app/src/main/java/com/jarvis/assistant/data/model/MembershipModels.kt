package com.jarvis.assistant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MembershipPlan(
    @DocumentId val id: String = "",
    val name: String = "",
    val durationMonths: Int = 0,
    val priceMinorUnits: Long = 0L,
    val currency: String = "INR",
    val description: String = "",
    val active: Boolean = true,
    val lifetime: Boolean = false,
    val benefits: List<String> = emptyList()
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planId: String = "",
    val amountMinorUnits: Long = 0L,
    val currency: String = "INR",
    val paymentMethod: String = "", // UPI, Binance
    val paymentStatus: String = "PENDING", // PENDING, VERIFYING, PAID, CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentSubmission(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val transactionReference: String = "",
    val receiptStoragePath: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    @ServerTimestamp val submittedAt: Timestamp? = null,
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String? = null,
    val rejectionReason: String? = null
)

@IgnoreExtraProperties
data class Membership(
    @DocumentId val id: String = "current",
    val userId: String = "",
    val planId: String = "",
    val status: String = "ACTIVE", // ACTIVE, EXPIRED
    val startedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val lifetime: Boolean = false,
    val sourceOrderId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminSettings(
    val merchantUpiId: String = "",
    val merchantName: String = "",
    val binancePayEnabled: Boolean = false,
    val binanceMerchantDetails: String = ""
)
