package com.uth.taskmanagement.security

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.uth.taskmanagement.databinding.FragmentPinLoginBinding
import com.uth.taskmanagement.ui.settings.SettingsViewModel
import com.uth.taskmanagement.MainActivity
class PinLoginFragment : Fragment() {

    private var _binding: FragmentPinLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as com.uth.taskmanagement.TaskManagementApp
        com.uth.taskmanagement.ui.settings.SettingsViewModelFactory(
            pinPreferences = app.pinPreferences,
            backupManager = app.backupManager
        )
    }

    private lateinit var keypadController: PinKeypadController

    private var remainingAttempts = MAX_ATTEMPTS
    private var lockoutTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupKeypad()
    }

    private fun setupKeypad() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)

        keypadController = PinKeypadController(dots) { enteredPin ->
            checkPin(enteredPin)
        }

        val numberKeys = mapOf(
            binding.key0 to "0", binding.key1 to "1", binding.key2 to "2",
            binding.key3 to "3", binding.key4 to "4", binding.key5 to "5",
            binding.key6 to "6", binding.key7 to "7", binding.key8 to "8",
            binding.key9 to "9"
        )
        keypadController.bindKeys(numberKeys, binding.keyBackspace)
    }

    private fun checkPin(pin: String) {
        viewModel.verifyPin(pin) { isCorrect ->
            if (isCorrect) {
                onLoginSuccess()
            } else {
                onLoginFailed()
            }
        }
    }

    private fun onLoginSuccess() {
        val activity = requireActivity() as MainActivity
        activity.onPinLoginSuccess()
    }

    private fun onLoginFailed() {
        remainingAttempts--
        _binding?.let { safeBinding ->
            safeBinding.root.postDelayed({
                keypadController.reset()
            }, 150)
        }

        if (remainingAttempts <= 0) {
            startLockout()
        } else {
            showError("Incorrect PIN, $remainingAttempts attempts left")
        }
    }


    private fun startLockout() {
        setKeypadEnabled(false)

        lockoutTimer?.cancel()
        lockoutTimer = object : CountDownTimer(LOCKOUT_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                showError("Too many incorrect attempts. Try again in ${secondsLeft}s")
            }

            override fun onFinish() {
                remainingAttempts = MAX_ATTEMPTS
                setKeypadEnabled(true)
                binding.tvError.visibility = View.INVISIBLE
            }
        }.start()
    }

    private fun setKeypadEnabled(enabled: Boolean) {
        val allKeys = listOf(
            binding.key0, binding.key1, binding.key2, binding.key3, binding.key4,
            binding.key5, binding.key6, binding.key7, binding.key8, binding.key9,
            binding.keyBackspace
        )
        allKeys.forEach { it.isEnabled = enabled }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lockoutTimer?.cancel()
        _binding = null
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }
}