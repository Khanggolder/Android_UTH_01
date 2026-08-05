package com.uth.taskmanagement.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.backup.BackupManager
import com.uth.taskmanagement.security.PinPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val pinPreferences: PinPreferences,
    private val backupManager: BackupManager
) : ViewModel() {

    val isPinEnabled: StateFlow<Boolean> = pinPreferences.isPinEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    fun setupPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { pinPreferences.setupPin(pin) }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            pinPreferences.disablePin()
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isCorrect = pinPreferences.verifyPin(pin)
            onResult(isCorrect)
        }
    }

    fun exportTasks(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            backupManager.exportTasks(uri)
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    fun restoreTasks(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            backupManager.restoreTasks(uri)
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }
}

class SettingsViewModelFactory(
    private val pinPreferences: PinPreferences,
    private val backupManager: BackupManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(pinPreferences, backupManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}