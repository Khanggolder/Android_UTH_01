package com.uth.taskmanagement.ui.tasklist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> =
        repository.observeAllTasks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun insertTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    /**
     * Xóa task và hủy Alarm tương ứng nếu có reminder.
     */
    fun deleteTask(context: Context, task: TaskEntity) {
        viewModelScope.launch {
            // Hủy Alarm trước khi xóa
            RecurrenceScheduler.cancelAlarm(context, task.id)
            repository.deleteTask(task)
        }
    }

    /**
     * Xóa task theo ID và hủy Alarm tương ứng.
     */
    fun deleteTaskById(context: Context, taskId: Long) {
        viewModelScope.launch {
            RecurrenceScheduler.cancelAlarm(context, taskId)
            repository.deleteTaskById(taskId)
        }
    }

    /**
     * Đánh dấu hoàn thành và hủy Alarm khi [completed] = true.
     */
    fun setTaskCompleted(
        context: Context,
        taskId: Long,
        completed: Boolean
    ) {
        viewModelScope.launch {
            if (completed) {
                // Hủy Alarm khi task hoàn thành
                RecurrenceScheduler.cancelAlarm(context, taskId)
            }
            repository.setTaskCompleted(
                taskId = taskId,
                completed = completed
            )
        }
    }

    fun observeTaskById(taskId: Long) =
        repository.observeTaskById(taskId)
}