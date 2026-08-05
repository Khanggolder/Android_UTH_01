package com.uth.taskmanagement.ui.tasklist

import androidx.lifecycle.ViewModel
import com.uth.taskmanagement.data.repository.TaskRepository

/**
 * ViewModel quản lý danh sách công việc.
 */
class TaskListViewModel(
    private val repository: TaskRepository
) : ViewModel() {
        val tasks = repository.observeAllTasks()
}