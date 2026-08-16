package com.uth.taskmanagement.core

object AppConstants {
    const val DATABASE_NAME = "task_management.db"
    const val PIN_DATASTORE_NAME = "pin_prefs"

    const val NOTIFICATION_CHANNEL_ID = "task_reminder_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Task reminder"

    const val BACKUP_FILE_PREFIX = "tasks_backup"
    const val BACKUP_FILE_EXTENSION = ".json"

    const val DEFAULT_PIN_LENGTH = 4
    const val DEFAULT_LOCKOUT_SECONDS = 30
}