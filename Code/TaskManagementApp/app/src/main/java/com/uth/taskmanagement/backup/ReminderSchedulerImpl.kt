package com.uth.taskmanagement.backup

import android.content.Context
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler

class ReminderSchedulerImpl(
    private val context: Context,
    private val taskRepository: TaskRepository
) : ReminderScheduler {

    override suspend fun cancelAll(taskIds: List<Long>) {
        taskIds.forEach { taskId ->
            RecurrenceScheduler.cancelAlarm(context, taskId)
        }
    }

    override suspend fun scheduleAll(tasks: List<TaskEntity>) {
        tasks.forEach { task ->
            val scheduledTime = RecurrenceScheduler.scheduleReminderForTask(context, task)
            if (scheduledTime != null && scheduledTime != task.reminderTime) {
                taskRepository.updateReminderTime(task.id, scheduledTime)
            }
        }
    }
}
