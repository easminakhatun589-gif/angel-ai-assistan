package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.jarvis.assistant.databinding.FragmentAdminDashboardBinding
import com.jarvis.assistant.security.AdminGuard
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {
    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkAdminPrivileges()
    }

    private fun checkAdminPrivileges() {
        binding.progressAdminCheck.visibility = View.VISIBLE
        binding.layoutAdminControls.visibility = View.GONE
        binding.layoutUnauthorized.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val isAdmin = AdminGuard.isCurrentUserAdmin()
            if (!isAdded) return@launch

            binding.progressAdminCheck.visibility = View.GONE

            if (isAdmin) {
                binding.layoutAdminControls.visibility = View.VISIBLE
                val email = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
                binding.tvAdminEmail.text = "Authenticated as: $email"

                binding.btnOpenFullDashboard.setOnClickListener {
                    startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                }
                binding.btnManageUsers.setOnClickListener {
                    startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                }
                binding.btnManageFeatures.setOnClickListener {
                    startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                }
                binding.btnAuditLogs.setOnClickListener {
                    startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                }
            } else {
                binding.layoutUnauthorized.visibility = View.VISIBLE
                binding.btnExitAdmin.setOnClickListener {
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
