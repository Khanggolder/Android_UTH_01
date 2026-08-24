package com.uth.taskmanagement.recurrence

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.notification.ReminderReceiver
import java.util.Calendar

object RecurrenceScheduler {

    fun calculateNextReminderTime(currentTime: Long, recurrenceType: RecurrenceType): Long? {
        if (recurrenceType == RecurrenceType.NONE) return null

        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
        when (recurrenceType) {
            RecurrenceType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceType.NONE -> return null
        }
        return calendar.timeInMillis
    }

    fun calculateNextFutureReminderTime(
        reminderTime: Long,
        recurrenceType: RecurrenceType,
        currentTime: Long = System.currentTimeMillis()
    ): Long? {
        if (reminderTime > currentTime) return reminderTime
        if (recurrenceType == RecurrenceType.NONE) return null

        var nextTime = reminderTime
        var attempts = 0
        do {
            val previousTime = nextTime
            nextTime = calculateNextReminderTime(nextTime, recurrenceType) ?: return null
            if (nextTime <= previousTime) return null
            attempts++
        } while (nextTime <= currentTime && attempts < MAX_CATCH_UP_OCCURRENCES)

        return nextTime.takeIf { it > currentTime }
    }

    fun scheduleReminderForTask(
        context: Context,
        task: TaskEntity,
        currentTime: Long = System.currentTimeMillis()
    ): Long? {
        if (task.isCompleted || task.id <= 0L) return null
        val reminderTime = task.reminderTime ?: return null
        val triggerTime = calculateNextFutureReminderTime(
            reminderTime = reminderTime,
            recurrenceType = task.recurrenceType,
            currentTime = currentTime
        ) ?: return null

        scheduleNextAlarm(
            context = context,
            taskId = task.id,
            title = task.title,
            description = task.description,
            triggerTimeMillis = triggerTime
        )
        return triggerTime
    }

    fun scheduleNextAlarm(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        triggerTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createReminderPendingIntent(
            context = context,
            taskId = taskId,
            title = title,
            description = description
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createReminderPendingIntent(
            context = context,
            taskId = taskId,
            title = "",
            description = ""
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createReminderPendingIntent(
        context: Context,
        taskId: Long,
        title: String,
        description: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TASK_DESCRIPTION, description)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val MAX_CATCH_UP_OCCURRENCES = 100_000
}
