package com.uth.taskmanagement.ui.taskform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.data.repository.TaskRepository
import kotlinx.coroutines.launch

class TaskFormViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    suspend fun getTask(taskId: Long): TaskEntity? {
        return repository.getTaskById(taskId)
    }

    fun saveTask(
        taskId: Long = 0,
        title: String,
        description: String,
        dueDateTime: Long,
        priority: TaskPriority,
        status: TaskStatus,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {

            if (taskId == 0L) {
                val task = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    dueDateTime = dueDateTime,
                    priority = priority,
                    status = status,
                    isCompleted = status == TaskStatus.COMPLETED
                )

                repository.insertTask(task)
            } else {
                val oldTask = repository.getTaskById(taskId)

                if (oldTask != null) {
                    val updatedTask = oldTask.copy(
                        title = title.trim(),
                        description = description.trim(),
                        dueDateTime = dueDateTime,
                        priority = priority,
                        status = status,
                        isCompleted = status == TaskStatus.COMPLETED
                    )

                    repository.updateTask(updatedTask)
                }
            }

            onSuccess()
        }
    }

    fun deleteTask(
        taskId: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteTaskById(taskId)
            onSuccess()
        }
    }

    fun setCompleted(
        taskId: Long,
        completed: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, completed)
            onSuccess()
        }
    }

    fun validate(
        title: String,
        dueDateTime: Long?,
        priority: TaskPriority?,
        status: TaskStatus?
    ): String? {

        if (title.isBlank()) {
            return "Title không được để trống"
        }

        if (dueDateTime == null) {
            return "Vui lòng chọn ngày hết hạn"
        }

        if (dueDateTime <= System.currentTimeMillis()) {
            return "Ngày hết hạn phải lớn hơn thời gian hiện tại"
        }

        if (priority == null) {
            return "Vui lòng chọn priority"
        }

        if (status == null) {
            return "Vui lòng chọn status"
        }

        return null
    }
}