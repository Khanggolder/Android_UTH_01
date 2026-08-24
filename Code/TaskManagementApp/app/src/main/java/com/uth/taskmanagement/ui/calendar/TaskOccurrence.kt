package com.uth.taskmanagement.ui.calendar

import com.uth.taskmanagement.data.model.TaskEntity

enum class CalendarEntryType {
    TASK,
    REMINDER
}

data class TaskOccurrence(
    val task: TaskEntity,
    val occurrenceDateTime: Long,
    val entryType: CalendarEntryType = CalendarEntryType.TASK
)
