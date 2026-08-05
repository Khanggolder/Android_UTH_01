package com.uth.taskmanagement.ui.settings

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.uth.taskmanagement.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as com.uth.taskmanagement.TaskManagementApp
        SettingsViewModelFactory(
            pinPreferences = app.pinPreferences,
            backupManager = app.backupManager
        )
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportTasks(it) { success -> handleResult(success, isExport = true) }
        }
    }

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { confirmAndRestore(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observePinState()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.rowChangePin.setOnClickListener {
        }

        binding.rowExport.setOnClickListener {
            exportLauncher.launch("tasks_backup_${System.currentTimeMillis()}.json")
        }

        binding.rowRestore.setOnClickListener {
            restoreLauncher.launch(arrayOf("application/json"))
        }
    }

    private fun observePinState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPinEnabled.collect { enabled ->
                    updatePinUi(enabled)
                }
            }
        }
    }

    private fun updatePinUi(enabled: Boolean) {
        binding.switchPinLock.setOnCheckedChangeListener(null)
        binding.switchPinLock.isChecked = enabled

        binding.tvPinStatus.text = if (enabled)
            "Đã bật - yêu cầu mã PIN khi mở app"
        else
            "Yêu cầu mã PIN khi mở app"

        binding.rowChangePin.isEnabled = enabled
        binding.rowChangePin.alpha = if (enabled) 1f else 0.4f

        binding.switchPinLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
            } else {
                confirmDisablePin()
            }
        }
    }

    private fun confirmDisablePin() {
        AlertDialog.Builder(requireContext())
            .setTitle("Tắt khóa PIN")
            .setMessage("Bạn có chắc muốn tắt khóa PIN? App sẽ không yêu cầu mã PIN khi mở nữa.")
            .setPositiveButton("Tắt") { _, _ -> viewModel.disablePin() }
            .setNegativeButton("Hủy") { _, _ ->
                binding.switchPinLock.isChecked = true
            }
            .setOnCancelListener {
                binding.switchPinLock.isChecked = true
            }
            .show()
    }

    private fun confirmAndRestore(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Khôi phục dữ liệu")
            .setMessage("Toàn bộ công việc hiện tại sẽ bị thay thế bằng dữ liệu trong file backup. Bạn có chắc chắn muốn tiếp tục?")
            .setPositiveButton("Khôi phục") { _, _ ->
                viewModel.restoreTasks(uri) { success -> handleResult(success, isExport = false) }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun handleResult(success: Boolean, isExport: Boolean) {
        val message = when {
            success && isExport -> "Đã sao lưu thành công"
            success && !isExport -> "Đã khôi phục dữ liệu thành công"
            !success && isExport -> "Sao lưu thất bại, vui lòng thử lại"
            else -> "Khôi phục thất bại, file không hợp lệ"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}