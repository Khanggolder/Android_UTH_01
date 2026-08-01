package com.uth.taskmanagement.utils

import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus

/**
 * Bo loc trang thai task.
 * ALL = khong loc, hien tat ca task.
 */
enum class TaskStatusFilter {
    ALL,
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    OVERDUE
}

/**
 * Kieu sap xep danh sach task.
 */
enum class TaskSortOption {
    PRIORITY_HIGH_TO_LOW,
    DUE_DATE_SOONEST_FIRST,
    DUE_DATE_LATEST_FIRST
}

/**
 * Utils thuan (khong phu thuoc Android framework) de loc va sap xep task,
 * dung chung cho TaskListViewModel (TV2) va TaskListFragment (TV2) hien thi.
 */
object TaskFilterSortUtils {

    /**
     * Task duoc coi la qua han khi chua hoan thanh va dueDateTime da qua thoi diem hien tai.
     */
    fun isOverdue(task: TaskEntity, currentTime: Long = System.currentTimeMillis()): Boolean {
        return !task.isCompleted && task.dueDateTime < currentTime
    }

    fun filterByStatus(
        tasks: List<TaskEntity>,
        filter: TaskStatusFilter,
        currentTime: Long = System.currentTimeMillis()
    ): List<TaskEntity> {
        return when (filter) {
            TaskStatusFilter.ALL -> tasks
            TaskStatusFilter.PENDING -> tasks.filter { it.status == TaskStatus.PENDING }
            TaskStatusFilter.IN_PROGRESS -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            TaskStatusFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
            TaskStatusFilter.OVERDUE -> tasks.filter { isOverdue(it, currentTime) }
        }
    }

    fun sort(
        tasks: List<TaskEntity>,
        sortOption: TaskSortOption
    ): List<TaskEntity> {
        return when (sortOption) {
            TaskSortOption.PRIORITY_HIGH_TO_LOW ->
                tasks.sortedWith(
                    compareByDescending<TaskEntity> { it.priority.ordinal }
                        .thenBy { it.dueDateTime }
                )

            TaskSortOption.DUE_DATE_SOONEST_FIRST ->
                tasks.sortedBy { it.dueDateTime }

            TaskSortOption.DUE_DATE_LATEST_FIRST ->
                tasks.sortedByDescending { it.dueDateTime }
        }
    }

    /**
     * Ap dung ca loc va sap xep trong mot buoc, dung truc tiep trong ViewModel.
     */
    fun filterAndSort(
        tasks: List<TaskEntity>,
        statusFilter: TaskStatusFilter,
        sortOption: TaskSortOption,
        currentTime: Long = System.currentTimeMillis()
    ): List<TaskEntity> {
        val filtered = filterByStatus(tasks, statusFilter, currentTime)
        return sort(filtered, sortOption)
    }

    /**
     * Dem so task theo tung muc uu tien, dung cho thong ke/hien thi badge (neu can).
     */
    fun countByPriority(tasks: List<TaskEntity>): Map<TaskPriority, Int> {
        return tasks.groupingBy { it.priority }.eachCount()
    }
}
