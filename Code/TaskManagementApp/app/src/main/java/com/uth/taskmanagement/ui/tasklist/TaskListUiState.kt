package com.uth.taskmanagement.ui.tasklist

import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.utils.TaskDueDateFilter
import com.uth.taskmanagement.utils.TaskPriorityFilter
import com.uth.taskmanagement.utils.TaskSortOption
import com.uth.taskmanagement.utils.TaskStatusFilter


sealed class TaskListUiState {


    data object Loading : TaskListUiState()


    data class Success(
        val tasks: List<TaskEntity>,
        val overdueTaskIds: Set<Long>,
        val appliedStatusFilter: TaskStatusFilter,
        val appliedPriorityFilter: TaskPriorityFilter,
        val appliedDueDateFilter: TaskDueDateFilter,
        val appliedSort: TaskSortOption
    ) : TaskListUiState()


    data class Empty(
        val isBecauseOfFilter: Boolean,
        val appliedStatusFilter: TaskStatusFilter,
        val appliedPriorityFilter: TaskPriorityFilter,
        val appliedDueDateFilter: TaskDueDateFilter
    ) : TaskListUiState()


    data class Error(
        val message: String
    ) : TaskListUiState()
}
