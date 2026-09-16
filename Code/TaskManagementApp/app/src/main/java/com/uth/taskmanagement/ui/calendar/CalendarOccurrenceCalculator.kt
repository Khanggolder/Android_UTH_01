package com.uth.taskmanagement.ui.calendar

import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.recurrence.RecurrenceScheduler

object CalendarOccurrenceCalculator {

    fun occurrencesInRange(
        task: TaskEntity,
        rangeStart: Long,
        rangeEnd: Long
    ): List<TaskOccurrence> {
        if (rangeStart > rangeEnd) return emptyList()
        if (task.recurrenceType == RecurrenceType.NONE || task.isCompleted) {
            return if (task.dueDateTime in rangeStart..rangeEnd) {
                listOf(TaskOccurrence(task, task.dueDateTime))
            } else {
                emptyList()
            }
        }

        val occurrences = mutableListOf<TaskOccurrence>()
        var current = task.dueDateTime
        var generated = 0

        while (current <= rangeEnd && generated < MAX_OCCURRENCES) {
            if (current >= rangeStart) occurrences += TaskOccurrence(task, current)
            val next = RecurrenceScheduler.calculateNextReminderTime(
                current,
                task.recurrenceType
            ) ?: break
            if (next <= current) break
            current = next
            generated++
        }

        return occurrences.distinctBy { it.occurrenceDateTime }
    }

    private const val MAX_OCCURRENCES = 100_000
}
