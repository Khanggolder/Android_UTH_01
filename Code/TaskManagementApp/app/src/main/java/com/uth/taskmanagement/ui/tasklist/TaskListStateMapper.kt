package com.uth.taskmanagement.ui.tasklist

import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.utils.TaskDueDateFilter
import com.uth.taskmanagement.utils.TaskFilterSortUtils
import com.uth.taskmanagement.utils.TaskPriorityFilter
import com.uth.taskmanagement.utils.TaskSortOption
import com.uth.taskmanagement.utils.TaskStatusFilter

/**
 * Chuyen doi List<TaskEntity> tho (tu TaskRepository - TV3) thanh TaskListUiState
 * (Success / Empty / Error) theo bo loc + sap xep hien tai.
 *
 * TaskListViewModel (TV2) chi can goi TaskListStateMapper.map(...) trong luong
 * combine(tasksFlow, filterState, sortState) { ... } roi post len LiveData/StateFlow.
 */
object TaskListStateMapper {

    fun map(
        allTasks: List<TaskEntity>,
        statusFilter: TaskStatusFilter,
        priorityFilter: TaskPriorityFilter,
        dueDateFilter: TaskDueDateFilter,
        sortOption: TaskSortOption,
        currentTime: Long = System.currentTimeMillis()
    ): TaskListUiState {
        val visibleTasks = TaskFilterSortUtils.filterAndSort(
            tasks = allTasks,
            statusFilter = statusFilter,
            priorityFilter = priorityFilter,
            dueDateFilter = dueDateFilter,
            sortOption = sortOption,
            currentTime = currentTime
        )

        if (visibleTasks.isEmpty()) {
            val hasAnyFilterApplied = statusFilter != TaskStatusFilter.ALL ||
                    priorityFilter != TaskPriorityFilter.ALL ||
                    dueDateFilter != TaskDueDateFilter.ALL
            val isBecauseOfFilter = allTasks.isNotEmpty() && hasAnyFilterApplied

            return TaskListUiState.Empty(
                isBecauseOfFilter = isBecauseOfFilter,
                appliedStatusFilter = statusFilter,
                appliedPriorityFilter = priorityFilter,
                appliedDueDateFilter = dueDateFilter
            )
        }

        val overdueIds = visibleTasks
            .filter { TaskFilterSortUtils.isOverdue(it, currentTime) }
            .map { it.id }
            .toSet()

        return TaskListUiState.Success(
            tasks = visibleTasks,
            overdueTaskIds = overdueIds,
            appliedStatusFilter = statusFilter,
            appliedPriorityFilter = priorityFilter,
            appliedDueDateFilter = dueDateFilter,
            appliedSort = sortOption
        )
    }

    /**
     * Bao boc loi doc du lieu (Room throw exception, IO error, ...) thanh TaskListUiState.Error
     * de UI hien error state thay vi crash.
     */
    fun mapError(throwable: Throwable): TaskListUiState.Error {
        return TaskListUiState.Error(
            message = throwable.message ?: "Unable to load tasks. Please try again."
        )
    }
}
