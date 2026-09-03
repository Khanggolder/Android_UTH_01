package com.uth.taskmanagement.ui.taskform

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.data.repository.AttachmentRepository

class TaskFormViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository,
    private val attachmentRepository: AttachmentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskFormViewModel::class.java)) {
            return TaskFormViewModel(
                application,
                repository,
                attachmentRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
