package com.uth.taskmanagement

import android.app.Application
import com.uth.taskmanagement.backup.BackupManager
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.security.PinPreferences

class TaskManagementApp : Application() {

    val taskRepository: TaskRepository by lazy {
        val db = TaskDatabase.getInstance(this)
        TaskRepository(db.taskDao())
    }

    val pinPreferences: PinPreferences by lazy {
        PinPreferences(this)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(taskRepository, this)
    }
}
