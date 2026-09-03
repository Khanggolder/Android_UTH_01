package com.uth.taskmanagement.backup

import android.content.Context
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.recurrence.RecurrenceScheduler

class ReminderSchedulerImpl(
    private val context: Context
) : ReminderScheduler {

    override suspend fun cancelAll(taskIds: List<Long>) {
        taskIds.forEach { taskId ->
            RecurrenceScheduler.cancelAlarm(context, taskId)
        }
    }

    override suspend fun scheduleAll(tasks: List<TaskEntity>) {
        tasks.forEach { task ->
            RecurrenceScheduler.scheduleReminderForTask(context, task)
        }
    }
}