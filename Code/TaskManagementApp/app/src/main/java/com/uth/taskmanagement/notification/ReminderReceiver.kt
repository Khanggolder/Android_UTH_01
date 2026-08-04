package com.uth.taskmanagement.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Nhận sự kiện từ AlarmManager và hiển thị thông báo nhắc việc
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Nhắc việc"
        val description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""

        if (taskId == -1L) return

        NotificationHelper.showNotification(
            context = context,
            notificationId = taskId.toInt(),
            title = title,
            content = description.ifEmpty { "Đã đến lúc thực hiện công việc!" }
        )
    }
}
