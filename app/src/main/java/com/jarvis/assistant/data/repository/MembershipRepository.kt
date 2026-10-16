package com.jarvis.assistant.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.Timestamp
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.*
import com.jarvis.assistant.util.OperationType
import com.jarvis.assistant.util.handleFirestoreError
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.*

class MembershipRepository(context: Context) {
    private val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User must be signed in.")

    fun getPlans(): Flow<List<MembershipPlan>> = db.collection("plans")
        .whereEqualTo("active", true)
        .snapshots()
        .map { snapshot ->
            val list = snapshot.toObjects(MembershipPlan::class.java)
            if (list.isEmpty()) getDefaultPlansList() else list
        }
        .catch { e ->
            handleFirestoreError(e as Exception, OperationType.LIST, "plans")
            emit(getDefaultPlansList())
        }

    suspend fun createOrder(planId: String, amountMinorUnits: Long, paymentMethod: String): String {
        val uid = requireUserId()
        val order = Order(
            userId = uid,
            planId = planId,
            amountMinorUnits = amountMinorUnits,
            paymentMethod = paymentMethod,
            paymentStatus = "PENDING"
        )
        val docRef = db.collection("orders").document()
        docRef.set(order).await()
        return docRef.id
    }

    suspend fun submitPayment(orderId: String, transactionRef: String, receiptPath: String) {
        val uid = requireUserId()
        val submission = PaymentSubmission(
            orderId = orderId,
            userId = uid,
            transactionReference = transactionRef,
            receiptStoragePath = receiptPath,
            status = "PENDING"
        )
        db.collection("paymentSubmissions").add(submission).await()
        
        // Update order status to VERIFYING
        db.collection("orders").document(orderId)
            .update("paymentStatus", "VERIFYING").await()
    }

    fun observeCurrentMembership(): Flow<Membership?> {
        val uid = auth.currentUser?.uid ?: return flowOf(null)
        return db.collection("memberships")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .snapshots()
            .map { it.toObjects(Membership::class.java).firstOrNull() }
            .catch { e ->
                handleFirestoreError(e as Exception, OperationType.GET, "memberships")
                throw e
            }
    }

    fun observeAdminSettings(): Flow<AdminSettings?> = db.collection("adminSettings")
        .document("paymentConfig")
        .snapshots()
        .map { it.toObject(AdminSettings::class.java) }

    suspend fun isAdmin(): Boolean {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: return false
        if (com.jarvis.assistant.security.AdminGuard.isAuthorizedEmail(email)) return true
        return try {
            db.collection("admins").document(email).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    fun observePendingSubmissions(): Flow<List<PaymentSubmission>> = db.collection("paymentSubmissions")
        .whereEqualTo("status", "PENDING")
        .snapshots()
        .map { it.toObjects(PaymentSubmission::class.java) }

    suspend fun approveSubmission(submission: PaymentSubmission) {
        val batch = db.batch()
        
        // 1. Update submission
        val subRef = db.collection("paymentSubmissions").document(submission.id)
        batch.update(subRef, mapOf(
            "status" to "APPROVED",
            "reviewedAt" to FieldValue.serverTimestamp(),
            "reviewedBy" to auth.currentUser?.email
        ))

        // 2. Update order
        val orderRef = db.collection("orders").document(submission.orderId)
        batch.update(orderRef, "paymentStatus", "PAID")

        // 3. Activate membership
        val order = db.collection("orders").document(submission.orderId).get().await().toObject(Order::class.java)
        val plan = db.collection("plans").document(order?.planId ?: "").get().await().toObject(MembershipPlan::class.java)
        
        if (order != null && plan != null) {
            val memRef = db.collection("memberships").document(order.userId)
            val calendar = Calendar.getInstance()
            val startedAt = Timestamp.now()
            calendar.time = startedAt.toDate()
            calendar.add(Calendar.MONTH, plan.durationMonths)
            val expiresAt = if (plan.lifetime) null else Timestamp(calendar.time)

            val membership = Membership(
                userId = order.userId,
                planId = plan.id,
                status = "ACTIVE",
                startedAt = startedAt,
                expiresAt = expiresAt,
                lifetime = plan.lifetime,
                sourceOrderId = order.id
            )
            batch.set(memRef, membership)
        }

        batch.commit().await()
    }

    suspend fun rejectSubmission(submission: PaymentSubmission, reason: String) {
        db.collection("paymentSubmissions").document(submission.id)
            .update(mapOf(
                "status" to "REJECTED",
                "rejectionReason" to reason,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "reviewedBy" to auth.currentUser?.email
            )).await()
        
        db.collection("orders").document(submission.orderId)
            .update("paymentStatus", "PENDING").await()
    }

    suspend fun initializeDefaultPlans() {
        val existingPlans = db.collection("plans").limit(1).get().await()
        if (!existingPlans.isEmpty) return // Already initialized

        val plans = getDefaultPlansList()
        val batch = db.batch()
        for (plan in plans) {
            batch.set(db.collection("plans").document(plan.id), plan)
        }
        
        // Also initialize default Admin Settings if missing
        val adminRef = db.collection("adminSettings").document("paymentConfig")
        val adminSnap = adminRef.get().await()
        if (!adminSnap.exists()) {
            batch.set(adminRef, AdminSettings(
                merchantUpiId = "mdaslamlaskar14@okaxis",
                merchantName = "MD ASLAM",
                binancePayEnabled = false,
                binanceMerchantDetails = "Binance Pay ID: 123456789"
            ))
        }

        // Add developers as admin
        for (adminEmail in com.jarvis.assistant.security.AdminGuard.PRIMARY_ADMIN_EMAILS) {
            batch.set(db.collection("admins").document(adminEmail), mapOf("role" to "super_admin", "active" to true))
        }

        batch.commit().await()
    }

    companion object {
        fun getDefaultPlansList(): List<MembershipPlan> = listOf(
            MembershipPlan("monthly", "Starter", 1, 9900, "INR", "Try Angel AI Premium features for one month.", true, false, 
                listOf("Standard AI Limits", "Voice Interaction", "Premium Models", "Priority Support (Basic)")),
            MembershipPlan("quarterly", "Value", 3, 24900, "INR", "Affordable extended access for regular users.", true, false, 
                listOf("Higher AI Limits", "Exclusive Avatars", "Voice Customization", "Priority Support (Silver)")),
            MembershipPlan("half_yearly", "Pro", 6, 39900, "INR", "Long-term premium access.", true, false, 
                listOf("Highest AI Limits", "Custom AI Personas", "Device Automation+", "Priority Support (Gold)")),
            MembershipPlan("yearly", "Ultra", 12, 64900, "INR", "One year of premium access.", true, false, 
                listOf("Unlimited AI Queries*", "All Premium Features", "Beta Access", "Priority Support (Platinum)")),
            MembershipPlan("lifetime", "Ultimate Lifetime", 1200, 99900, "INR", "Lifetime membership for all eligible features.", true, true, 
                listOf("Lifetime VIP Status", "Unlimited Everything", "Custom AI Brain", "Direct Dev Support"))
        )
    }
}
