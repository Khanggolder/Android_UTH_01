package com.uth.taskmanagement.ui.calendar

import com.uth.taskmanagement.data.model.TaskEntity
data class TaskOccurrence(
    val task: TaskEntity,
    val occurrenceDateTime: Long
)