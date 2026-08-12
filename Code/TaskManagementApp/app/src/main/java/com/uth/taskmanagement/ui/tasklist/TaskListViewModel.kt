package com.uth.taskmanagement.ui.tasklist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import com.uth.taskmanagement.utils.TaskDueDateFilter
import com.uth.taskmanagement.utils.TaskPriorityFilter
import com.uth.taskmanagement.utils.TaskSortOption
import com.uth.taskmanagement.utils.TaskStatusFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    private val statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    private val priorityFilter = MutableStateFlow(TaskPriorityFilter.ALL)
    private val dueDateFilter = MutableStateFlow(TaskDueDateFilter.ALL)
    private val sortOption = MutableStateFlow(TaskSortOption.DUE_DATE_SOONEST_FIRST)

    val uiState: StateFlow<TaskListUiState> = combine(
        repository.observeAllTasks(),
        statusFilter,
        priorityFilter,
        dueDateFilter,
        sortOption
    ) { allTasks, status, priority, dueDate, sort ->
        TaskListStateMapper.map(
            allTasks = allTasks,
            statusFilter = status,
            priorityFilter = priority,
            dueDateFilter = dueDate,
            sortOption = sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState.Loading
    )

    fun setStatusFilter(filter: TaskStatusFilter) {
        statusFilter.value = filter
    }

    fun setPriorityFilter(filter: TaskPriorityFilter) {
        priorityFilter.value = filter
    }

    fun setDueDateFilter(filter: TaskDueDateFilter) {
        dueDateFilter.value = filter
    }

    fun setSortOption(option: TaskSortOption) {
        sortOption.value = option
    }

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
            RecurrenceScheduler.cancelAlarm(context, task.id)
            repository.deleteTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
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

    fun deleteTaskById(taskId: Long) {
        viewModelScope.launch {
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
                RecurrenceScheduler.cancelAlarm(context, taskId)
            }
            repository.setTaskCompleted(
                taskId = taskId,
                completed = completed
            )
        }
    }

    fun setTaskCompleted(
        taskId: Long,
        completed: Boolean
    ) {
        viewModelScope.launch {
            repository.setTaskCompleted(
                taskId = taskId,
                completed = completed
            )
        }
    }

    fun observeTaskById(taskId: Long) =
        repository.observeTaskById(taskId)
}