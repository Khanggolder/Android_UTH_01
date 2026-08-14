package com.uth.taskmanagement.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import com.uth.taskmanagement.databinding.FragmentPinSetupBinding
import com.uth.taskmanagement.ui.settings.SettingsViewModel

class PinSetupFragment : Fragment() {

    private var _binding: FragmentPinSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as com.uth.taskmanagement.TaskManagementApp
        com.uth.taskmanagement.ui.settings.SettingsViewModelFactory(
            pinPreferences = app.pinPreferences,
            backupManager = app.backupManager
        )
    }

    private lateinit var keypadController: PinKeypadController

    private var firstEnteredPin: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        setupKeypad()
    }

    private fun setupKeypad() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)

        keypadController = PinKeypadController(dots) { enteredPin ->
            onPinEntered(enteredPin)
        }

        val numberKeys = mapOf(
            binding.key0 to "0", binding.key1 to "1", binding.key2 to "2",
            binding.key3 to "3", binding.key4 to "4", binding.key5 to "5",
            binding.key6 to "6", binding.key7 to "7", binding.key8 to "8",
            binding.key9 to "9"
        )
        keypadController.bindKeys(numberKeys, binding.keyBackspace)
    }

    private fun onPinEntered(pin: String) {
        val first = firstEnteredPin

        if (first == null) {
            firstEnteredPin = pin
            binding.tvTitle.text = "Nhập lại mã PIN"
            binding.tvSubtitle.text = "Xác nhận mã PIN vừa tạo"
            binding.tvError.visibility = View.INVISIBLE

            binding.root.postDelayed({ keypadController.reset() }, 150)
            return
        }

        if (pin == first) {
            viewModel.setupPin(pin) { success ->
                if (success) {
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    showError("Có lỗi xảy ra, vui lòng thử lại")
                    resetToFirstStep()
                }
            }
        } else {
            showError("Mã PIN không khớp, vui lòng thử lại")
            resetToFirstStep()
        }
    }

    private fun resetToFirstStep() {
        firstEnteredPin = null
        binding.tvTitle.text = "Tạo mã PIN"
        binding.tvSubtitle.text = "Nhập 4 chữ số để khóa ứng dụng"
        binding.root.postDelayed({ keypadController.reset() }, 400)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}