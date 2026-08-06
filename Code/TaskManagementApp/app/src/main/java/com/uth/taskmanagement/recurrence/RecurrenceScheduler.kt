package com.uth.taskmanagement.recurrence

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.notification.ReminderReceiver
import java.util.Calendar

// Tính thời gian nhắc tiếp theo theo chu kỳ và đặt lại Alarm
object RecurrenceScheduler {

    // Tính thời gian nhắc tiếp theo dựa trên recurrenceType
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

    // Đặt alarm cho lần nhắc tiếp theo
    fun scheduleNextAlarm(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        triggerTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TASK_DESCRIPTION, description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    // Hủy alarm đã đặt cho task
    fun cancelAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
