package com.uth.taskmanagement

import android.app.Application
import com.uth.taskmanagement.backup.BackupManager
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.repository.AttachmentRepository
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.data.repository.UserRepository
import com.uth.taskmanagement.security.PinPreferences
import com.uth.taskmanagement.backup.ReminderSchedulerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskManagementApp : Application() {

    // Application-scoped coroutine scope – tự huỷ khi process kết thúc
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val taskRepository: TaskRepository by lazy {
        val db = TaskDatabase.getInstance(this)
        TaskRepository(db.taskDao())
    }

    val userRepository: UserRepository by lazy {
        val db = TaskDatabase.getInstance(this)
        UserRepository(db.userDao())
    }

    val attachmentRepository: AttachmentRepository by lazy {
        val db = TaskDatabase.getInstance(this)
        AttachmentRepository(db.attachmentDao())
    }

    val pinPreferences: PinPreferences by lazy {
        PinPreferences(this)
    }
    val reminderScheduler: ReminderSchedulerImpl by lazy {
        ReminderSchedulerImpl(this)
    }
    val backupManager: BackupManager by lazy {
        BackupManager(taskRepository, attachmentRepository, reminderScheduler, this)
    }

    override fun onCreate() {
        super.onCreate()
        // Seed user mặc định khi app khởi động; IGNORE conflict nên idempotent
        applicationScope.launch(Dispatchers.IO) {
            userRepository.ensureDefaultUserExists()
        }
    }
}
