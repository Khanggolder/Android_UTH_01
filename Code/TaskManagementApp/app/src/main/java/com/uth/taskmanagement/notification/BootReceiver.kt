package com.uth.taskmanagement.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Khôi phục toàn bộ Reminder sau reboot / thay đổi ngày giờ / múi giờ
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supported = action == Intent.ACTION_BOOT_COMPLETED
                || action == Intent.ACTION_TIME_CHANGED
                || action == "android.intent.action.TIMEZONE_CHANGED"

        if (!supported) return

        val repository = TaskRepository(
            TaskDatabase.getInstance(context).taskDao()
        )

        // goAsync() giữ wake lock đủ lâu để coroutine hoàn thành
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = repository.getActiveReminderTasks()
                val now = System.currentTimeMillis()

                for (task in tasks) {
                    val reminderTime = task.reminderTime ?: continue

                    // Nếu thời gian nhắc đã qua, tính lại theo chu kỳ
                    val nextTime = if (reminderTime > now) {
                        reminderTime
                    } else {
                        RecurrenceScheduler.calculateNextReminderTime(
                            reminderTime,
                            task.recurrenceType
                        ) ?: continue
                    }

                    // Cập nhật DB nếu thời gian đã thay đổi
                    if (nextTime != reminderTime) {
                        repository.updateReminderTime(task.id, nextTime)
                    }

                    // Đặt lại Alarm
                    RecurrenceScheduler.scheduleNextAlarm(
                        context = context,
                        taskId = task.id,
                        title = task.title,
                        description = task.description,
                        triggerTimeMillis = nextTime
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
