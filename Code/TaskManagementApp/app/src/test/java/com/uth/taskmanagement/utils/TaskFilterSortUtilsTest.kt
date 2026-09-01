package com.uth.taskmanagement.utils

import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TaskFilterSortUtilsTest {

    @Test
    fun upcomingFilter_excludesCompletedPastTask() {
        val currentTime = timestamp(2026, Calendar.AUGUST, 24, 12)
        val completedPastTask = task(
            dueDateTime = timestamp(2026, Calendar.AUGUST, 20, 9),
            completed = true
        )

        val result = TaskFilterSortUtils.filterByDueDate(
            tasks = listOf(completedPastTask),
            filter = TaskDueDateFilter.UPCOMING,
            currentTime = currentTime
        )

        assertEquals(emptyList<TaskEntity>(), result)
    }

    @Test
    fun upcomingFilter_includesTaskAfterToday() {
        val currentTime = timestamp(2026, Calendar.AUGUST, 24, 12)
        val futureTask = task(
            dueDateTime = timestamp(2026, Calendar.AUGUST, 25, 9)
        )

        val result = TaskFilterSortUtils.filterByDueDate(
            tasks = listOf(futureTask),
            filter = TaskDueDateFilter.UPCOMING,
            currentTime = currentTime
        )

        assertEquals(listOf(futureTask), result)
    }

    private fun task(dueDateTime: Long, completed: Boolean = false) = TaskEntity(
        id = 1,
        title = "Task",
        dueDateTime = dueDateTime,
        status = if (completed) TaskStatus.COMPLETED else TaskStatus.PENDING,
        isCompleted = completed
    )

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}
