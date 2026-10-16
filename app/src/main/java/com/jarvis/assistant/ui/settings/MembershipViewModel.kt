package com.jarvis.assistant.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.model.*
import com.jarvis.assistant.data.repository.MembershipRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MembershipViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MembershipRepository(application)

    private val _plans = MutableStateFlow<List<MembershipPlan>>(emptyList())
    val plans: StateFlow<List<MembershipPlan>> = _plans.asStateFlow()

    private val _currentMembership = MutableStateFlow<Membership?>(null)
    val currentMembership: StateFlow<Membership?> = _currentMembership.asStateFlow()

    private val _adminSettings = MutableStateFlow<AdminSettings?>(null)
    val adminSettings: StateFlow<AdminSettings?> = _adminSettings.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getPlans().collect { _plans.value = it }
            } catch (e: Exception) {
                _error.value = "Failed to load plans"
            } finally {
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            repository.observeCurrentMembership().collect { _currentMembership.value = it }
        }

        viewModelScope.launch {
            repository.observeAdminSettings().collect { _adminSettings.value = it }
        }

        viewModelScope.launch {
            _isAdmin.value = repository.isAdmin()
        }
    }

    fun createOrder(plan: MembershipPlan, paymentMethod: String, onOrderCreated: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val orderId = repository.createOrder(plan.id, plan.priceMinorUnits, paymentMethod)
                onOrderCreated(orderId)
            } catch (e: Exception) {
                _error.value = "Failed to create order"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitPayment(orderId: String, transactionRef: String, receiptPath: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.submitPayment(orderId, transactionRef, receiptPath)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to submit payment"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun initializePlans() {
        viewModelScope.launch {
            repository.initializeDefaultPlans()
        }
    }
}
