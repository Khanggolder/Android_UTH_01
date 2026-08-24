package com.uth.taskmanagement.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.uth.taskmanagement.databinding.FragmentSettingsBinding
import com.uth.taskmanagement.R
import com.uth.taskmanagement.security.PinSetupFragment
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        showMessage(
            if (granted) "Notification permission granted"
            else "Notification permission denied"
        )
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
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PinSetupFragment())
                .addToBackStack("pin_setup")
                .commit()
        }
        binding.rowExport.setOnClickListener {
            exportLauncher.launch("tasks_backup_${System.currentTimeMillis()}.json")
        }
        binding.rowRestore.setOnClickListener {
            restoreLauncher.launch(arrayOf("application/json"))
        }
        binding.rowNotificationPermission.setOnClickListener {
            handleNotificationPermission()
        }
    }

    private fun handleNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                showMessage("Notifications are enabled")
            } else {
                openNotificationSettings()
            }
            return
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                showMessage("Notification permission already granted")
            }
            shouldShowRequestPermissionRationale(permission) -> {
                notificationPermissionLauncher.launch(permission)
            }
            else -> openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
        )
    }

    private fun observePinState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPinEnabled.collect { enabled -> updatePinUi(enabled) }
            }
        }
    }

    private fun updatePinUi(enabled: Boolean) {
        binding.switchPinLock.setOnCheckedChangeListener(null)
        binding.switchPinLock.isChecked = enabled
        binding.tvPinStatus.text = if (enabled) {
            "Enabled - PIN required when opening the app"
        } else {
            "Require a PIN when opening the app"
        }
        binding.switchPinLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, PinSetupFragment())
                    .addToBackStack("pin_setup")
                    .commit()
            } else {
                confirmDisablePin()
            }
        }
    }

    private fun confirmDisablePin() {
        AlertDialog.Builder(requireContext())
            .setTitle("Disable PIN lock")
            .setMessage("Disable PIN protection for this demo app?")
            .setPositiveButton("Disable") { _, _ -> viewModel.disablePin() }
            .setNegativeButton("Cancel") { _, _ -> binding.switchPinLock.isChecked = true }
            .setOnCancelListener { binding.switchPinLock.isChecked = true }
            .show()
    }

    private fun confirmAndRestore(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Restore data")
            .setMessage("Current tasks will be replaced by the selected backup file. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                viewModel.restoreTasks(uri) { success -> handleResult(success, isExport = false) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleResult(success: Boolean, isExport: Boolean) {
        val message = when {
            success && isExport -> "Backup exported successfully"
            success && !isExport -> "Backup restored successfully"
            !success && isExport -> "Backup export failed"
            else -> "Restore failed. Please check the JSON file."
        }
        showMessage(message)
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
