package com.uth.taskmanagement.data.repository

import androidx.room.withTransaction
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.local.TaskDao
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val attachmentRepository: AttachmentRepository? = null,
    private val database: TaskDatabase? = null
) {

    fun observeAllTasks(): Flow<List<TaskEntity>> =
        taskDao.observeAllTasks()

    suspend fun getAllTasks(): List<TaskEntity> =
        taskDao.getAllTasks()
    
    fun observeTaskById(taskId: Long): Flow<TaskEntity?> =
        taskDao.observeTaskById(taskId)

    suspend fun getTaskById(taskId: Long): TaskEntity? =
        taskDao.getTaskById(taskId)

    suspend fun insertTask(task: TaskEntity): Long {
        val currentTime = System.currentTimeMillis()

        return taskDao.insertTask(
            task.copy(
                id = 0,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        require(task.id > 0) {
            "Task ID must be greater than 0 when updating."
        }

        taskDao.updateTask(
            task.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateTask(
        task: TaskEntity,
        newAttachments: List<TaskAttachmentEntity>,
        pendingDeleteAttachmentIds: Set<Long>
    ) {
        if (newAttachments.isEmpty() && pendingDeleteAttachmentIds.isEmpty()) {
            updateTask(task)
            return
        }

        require(task.id > 0) { "Task ID must be greater than 0 when updating." }
        val attachments = requireNotNull(attachmentRepository) {
            "Attachment repository is required for attachment changes."
        }
        val roomDatabase = requireNotNull(database) {
            "Task database is required for transactional attachment changes."
        }
        val removals = pendingDeleteAttachmentIds
            .mapNotNull { attachments.getAttachmentById(it) }
            .filter { it.taskId == task.id }
        val removalIds = removals.map { it.id }
        val stagedDeletion = attachments.stageOwnedFiles(removals)

        try {
            roomDatabase.withTransaction {
                taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
                if (newAttachments.isNotEmpty()) attachments.addAttachments(newAttachments)
                attachments.deleteAttachmentRecords(removalIds)
            }
            attachments.commitFileDeletion(stagedDeletion)
        } catch (error: Exception) {
            attachments.rollbackFileDeletion(stagedDeletion)
            throw error
        }
    }

    suspend fun deleteTask(task: TaskEntity) =
        deleteTaskById(task.id)

    suspend fun deleteTaskById(taskId: Long) {
        require(taskId > 0) {
            "Task ID must be greater than 0 when deleting."
        }

        val stagedDeletion = attachmentRepository?.stageOwnedFilesForTask(taskId)
        try {
            taskDao.deleteTaskById(taskId)
            stagedDeletion?.let { attachmentRepository?.commitFileDeletion(it) }
        } catch (error: Exception) {
            stagedDeletion?.let { attachmentRepository?.rollbackFileDeletion(it) }
            throw error
        }
    }

    suspend fun setTaskCompleted(
        taskId: Long,
        completed: Boolean
    ) {
        require(taskId > 0) {
            "Task ID must be greater than 0 when updating completion."
        }

        val status = if (completed) {
            TaskStatus.COMPLETED
        } else {
            TaskStatus.PENDING
        }

        taskDao.updateCompletedState(
            taskId = taskId,
            isCompleted = completed,
            status = status,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun observeTasksByStatus(
        status: TaskStatus
    ): Flow<List<TaskEntity>> =
        taskDao.observeTasksByStatus(status)

    fun observeTasksByPriority(
        priority: TaskPriority
    ): Flow<List<TaskEntity>> =
        taskDao.observeTasksByPriority(priority)

    fun observeTasksSortedByPriority(): Flow<List<TaskEntity>> =
        taskDao.observeTasksSortedByPriority()

    fun observeTasksSortedByDueDate(): Flow<List<TaskEntity>> =
        taskDao.observeTasksSortedByDueDate()

    fun observeOverdueTasks(): Flow<List<TaskEntity>> =
        taskDao.observeOverdueTasks(System.currentTimeMillis())

    fun observeTasksBetweenDates(
        start: Long,
        end: Long
    ): Flow<List<TaskEntity>> =
        taskDao.observeTasksBetweenDates(start, end)

    suspend fun getActiveReminderTasks(): List<TaskEntity> =
        taskDao.getActiveReminderTasks()

    suspend fun replaceAllTasks(tasks: List<TaskEntity>) {
        taskDao.replaceAllTasks(tasks)
    }

    suspend fun updateReminderTime(taskId: Long, reminderTime: Long?) =
        taskDao.updateReminderTime(taskId, reminderTime)
}
