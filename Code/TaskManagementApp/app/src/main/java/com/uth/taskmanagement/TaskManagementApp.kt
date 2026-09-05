package com.uth.taskmanagement

import android.app.Application
import com.uth.taskmanagement.backup.BackupManager
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.repository.AttachmentRepository
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.data.repository.UserRepository
import com.uth.taskmanagement.security.PinPreferences
import com.uth.taskmanagement.backup.ReminderSchedulerImpl
import com.uth.taskmanagement.attachment.AttachmentStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskManagementApp : Application() {

    // Application-scoped coroutine scope – tự huỷ khi process kết thúc
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val database: TaskDatabase by lazy {
        TaskDatabase.getInstance(this)
    }

    val attachmentStorage: AttachmentStorage by lazy {
        AttachmentStorage(this)
    }

    val attachmentRepository: AttachmentRepository by lazy {
        AttachmentRepository(database.attachmentDao(), attachmentStorage)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao(), attachmentRepository)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }

    val pinPreferences: PinPreferences by lazy {
        PinPreferences(this)
    }
    val reminderScheduler: ReminderSchedulerImpl by lazy {
        ReminderSchedulerImpl(this, taskRepository)
    }
    val backupManager: BackupManager by lazy {
        BackupManager(
            taskRepository = taskRepository,
            attachmentRepository = attachmentRepository,
            reminderScheduler = reminderScheduler,
            database = database,
            attachmentStorage = attachmentStorage,
            context = this
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Seed user mặc định khi app khởi động; IGNORE conflict nên idempotent
        applicationScope.launch(Dispatchers.IO) {
            userRepository.ensureDefaultUserExists()
        }
    }
}
