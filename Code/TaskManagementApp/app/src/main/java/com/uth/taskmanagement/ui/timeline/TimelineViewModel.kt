package com.uth.taskmanagement.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.uth.taskmanagement.data.repository.TaskRepository

class TimelineViewModel(
    repository: TaskRepository
) : ViewModel() {

    val tasks = repository
        .observeAllTasks()
        .asLiveData()
}