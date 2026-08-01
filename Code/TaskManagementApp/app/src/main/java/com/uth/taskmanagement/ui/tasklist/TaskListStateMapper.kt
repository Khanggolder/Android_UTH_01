package com.uth.taskmanagement.ui.tasklist

import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.utils.TaskFilterSortUtils
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
        sortOption: TaskSortOption,
        currentTime: Long = System.currentTimeMillis()
    ): TaskListUiState {
        val visibleTasks = TaskFilterSortUtils.filterAndSort(
            tasks = allTasks,
            statusFilter = statusFilter,
            sortOption = sortOption,
            currentTime = currentTime
        )

        if (visibleTasks.isEmpty()) {
            val isBecauseOfFilter = allTasks.isNotEmpty() && statusFilter != TaskStatusFilter.ALL
            return TaskListUiState.Empty(
                isBecauseOfFilter = isBecauseOfFilter,
                appliedFilter = statusFilter
            )
        }

        val overdueIds = visibleTasks
            .filter { TaskFilterSortUtils.isOverdue(it, currentTime) }
            .map { it.id }
            .toSet()

        return TaskListUiState.Success(
            tasks = visibleTasks,
            overdueTaskIds = overdueIds,
            appliedFilter = statusFilter,
            appliedSort = sortOption
        )
    }

    /**
     * Bao boc loi doc du lieu (Room throw exception, IO error, ...) thanh TaskListUiState.Error
     * de UI hien error state thay vi crash.
     */
    fun mapError(throwable: Throwable): TaskListUiState.Error {
        return TaskListUiState.Error(
            message = throwable.message ?: "Da xay ra loi khi tai danh sach task."
        )
    }
}
