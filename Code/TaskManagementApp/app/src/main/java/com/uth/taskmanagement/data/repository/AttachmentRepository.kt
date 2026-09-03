package com.uth.taskmanagement.data.repository

import com.uth.taskmanagement.data.local.AttachmentDao
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(
    private val attachmentDao: AttachmentDao
) {
    suspend fun addAttachment(attachment: TaskAttachmentEntity): Long =
        attachmentDao.insert(
            attachment.copy(
                id = 0,
                createdAt = System.currentTimeMillis()
            )
        )
    suspend fun addAttachments(attachments: List<TaskAttachmentEntity>): List<Long> {
        val now = System.currentTimeMillis()
        return attachmentDao.insertAll(
            attachments.map { it.copy(id = 0, createdAt = now) }
        )
    }

    suspend fun deleteAttachment(id: Long) =
        attachmentDao.deleteById(id)

    suspend fun deleteAllForTask(taskId: Long) =
        attachmentDao.deleteByTaskId(taskId)

    fun observeAttachments(taskId: Long): Flow<List<TaskAttachmentEntity>> =
        attachmentDao.observeByTaskId(taskId)

    suspend fun getAttachments(taskId: Long): List<TaskAttachmentEntity> =
        attachmentDao.getByTaskId(taskId)

    suspend fun getAttachmentById(id: Long): TaskAttachmentEntity? =
        attachmentDao.getById(id)
}
