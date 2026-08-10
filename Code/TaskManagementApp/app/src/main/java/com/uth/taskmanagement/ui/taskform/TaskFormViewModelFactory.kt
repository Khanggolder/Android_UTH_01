package com.uth.taskmanagement.ui.taskform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uth.taskmanagement.data.repository.TaskRepository

class TaskFormViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskFormViewModel::class.java)) {
            return TaskFormViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}