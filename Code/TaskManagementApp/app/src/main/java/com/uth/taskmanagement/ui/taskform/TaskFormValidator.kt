package com.uth.taskmanagement.ui.taskform

import com.uth.taskmanagement.data.model.TaskStatus

object TaskFormValidator {

    fun validate(
        state: TaskFormState,
        currentTime: Long = System.currentTimeMillis()
    ): String? {
        if (state.title.isBlank()) return "Title is required"
        if (state.description.isBlank()) return "Description is required"
        if (state.startDateTime >= state.dueDateTime) {
            return "Start date must be before the due date"
        }

        val isCreate = state.taskId <= 0L
        val dueDateChanged = state.originalDueDateTime != state.dueDateTime
        if (state.dueDateTime <= currentTime && (isCreate || dueDateChanged)) {
            return "Due date must be in the future"
        }

        if (state.status != TaskStatus.COMPLETED) {
            val reminderTime = state.reminderTime
            val reminderChanged = reminderTime != state.originalReminderTime
            if (
                reminderTime != null &&
                reminderTime <= currentTime &&
                (isCreate || reminderChanged)
            ) {
                return "Reminder time must be in the future"
            }
            if (reminderTime != null && reminderTime > state.dueDateTime) {
                return "Reminder time cannot be after the due date"
            }
        }

        return null
    }
}
