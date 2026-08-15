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
    COMPLETED
}

enum class TaskPriorityFilter {
    ALL,
    LOW,
    MEDIUM,
    HIGH
}

enum class TaskDueDateFilter {
    ALL,
    OVERDUE,
    DUE_TODAY,
    UPCOMING
}

/**
 * Kieu sap xep danh sach task.
 */
enum class TaskSortOption {
    DUE_DATE_SOONEST_FIRST,
    DUE_DATE_LATEST_FIRST,
    PRIORITY_HIGH_TO_LOW
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

    fun isDueToday(task: TaskEntity, currentTime: Long  = System.currentTimeMillis()): Boolean {
        val startOfDay = startOfDay(currentTime)
        val endOfDay = startOfDay + DAY_IN_MILLIS
        return task.dueDateTime >= startOfDay && task.dueDateTime < endOfDay
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
        }
    }

    fun filterByPriority(
        tasks: List<TaskEntity>,
        filter: TaskPriorityFilter
    ): List<TaskEntity> {
        return when(filter) {
            TaskPriorityFilter.ALL -> tasks
            TaskPriorityFilter.LOW ->  tasks.filter { it.priority == TaskPriority.LOW }
            TaskPriorityFilter.MEDIUM -> tasks.filter { it.priority == TaskPriority.MEDIUM }
            TaskPriorityFilter.HIGH -> tasks.filter { it.priority == TaskPriority.HIGH }
        }
    }

    fun filterByDueDate(
        tasks: List<TaskEntity>,
        filter: TaskDueDateFilter,
        currentTime: Long = System.currentTimeMillis()
    ): List<TaskEntity> {
        return when (filter) {
            TaskDueDateFilter.ALL -> tasks
            TaskDueDateFilter.OVERDUE -> tasks.filter { isOverdue (it, currentTime) }
            TaskDueDateFilter.DUE_TODAY -> tasks.filter { isDueToday(it, currentTime) }
            TaskDueDateFilter.UPCOMING -> tasks.filter { !isOverdue(it, currentTime) && !isDueToday(it, currentTime) }
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
        priorityFilter: TaskPriorityFilter,
        dueDateFilter: TaskDueDateFilter,
        sortOption: TaskSortOption,
        currentTime: Long = System.currentTimeMillis()
    ): List<TaskEntity> {
        var filtered = filterByStatus(tasks, statusFilter)
        filtered = filterByPriority(filtered, priorityFilter)
        filtered = filterByDueDate(filtered, dueDateFilter, currentTime)
        return sort(filtered, sortOption)
    }

    fun countByPriority(tasks: List<TaskEntity>): Map<TaskPriority, Int> {
        return tasks.groupingBy { it.priority }.eachCount()
    }

    private  const val DAY_IN_MILLIS = 24L * 60 * 60 * 1000

    private  fun startOfDay(timeMillis: Long): Long {
        val  calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE,0)
        calendar.set(java.util.Calendar.SECOND,0)
        return  calendar.timeInMillis
    }
}
