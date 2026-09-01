package com.uth.taskmanagement.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Nhận sự kiện từ AlarmManager, hiển thị thông báo và tự động đặt lại Alarm nếu Task có lặp
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        if (taskId == -1L) return

        // goAsync() giữ wake lock đủ lâu để coroutine hoàn thành
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = TaskRepository(
                    TaskDatabase.getInstance(context).taskDao()
                )
                val task = repository.getTaskById(taskId) ?: return@launch

                // Nếu task đã hoàn thành thì không lặp
                if (task.isCompleted) return@launch

                val currentReminderTime = task.reminderTime ?: return@launch

                NotificationHelper.showNotification(
                    context = context,
                    notificationId = taskId.toInt(),
                    title = task.title,
                    content = task.description.ifEmpty { "It is time to work on this task!" }
                )

                // Chỉ reschedule nếu có chu kỳ lặp
                if (task.recurrenceType == RecurrenceType.NONE) return@launch

                // Tính thời gian nhắc tiếp theo
                val nextTime = RecurrenceScheduler.calculateNextFutureReminderTime(
                    reminderTime = currentReminderTime,
                    recurrenceType = task.recurrenceType,
                    currentTime = System.currentTimeMillis()
                ) ?: return@launch

                // Cập nhật reminderTime trong DB – không tạo bản ghi mới
                repository.updateReminderTime(taskId, nextTime)

                // Đặt lại Alarm cho lần nhắc tiếp theo
                RecurrenceScheduler.scheduleNextAlarm(
                    context = context,
                    taskId = taskId,
                    title = task.title,
                    description = task.description,
                    triggerTimeMillis = nextTime
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
