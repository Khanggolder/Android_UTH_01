package com.uth.taskmanagement.ui.taskform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskFormValidatorTest {

    @Test
    fun createTaskWithPastDueDate_isRejected() {
        val now = 10_000L
        val state = validState().copy(
            startDateTime = 1_000L,
            dueDateTime = 9_000L
        )

        assertEquals("Due date must be in the future", TaskFormValidator.validate(state, now))
    }

    @Test
    fun editOverdueTaskWithUnchangedDueDate_isAllowed() {
        val overdueDue = 9_000L
        val state = validState().copy(
            taskId = 1L,
            title = "Updated title",
            startDateTime = 1_000L,
            dueDateTime = overdueDue,
            originalDueDateTime = overdueDue,
            reminderTime = 5_000L,
            originalReminderTime = 5_000L
        )

        assertNull(TaskFormValidator.validate(state, currentTime = 10_000L))
    }

    @Test
    fun editOverdueTaskWithChangedPastDueDate_isRejected() {
        val state = validState().copy(
            taskId = 1L,
            startDateTime = 1_000L,
            dueDateTime = 8_000L,
            originalDueDateTime = 9_000L
        )

        assertEquals(
            "Due date must be in the future",
            TaskFormValidator.validate(state, currentTime = 10_000L)
        )
    }

    private fun validState() = TaskFormState(
        title = "Task",
        description = "Description",
        startDateTime = 11_000L,
        dueDateTime = 12_000L
    )
}
