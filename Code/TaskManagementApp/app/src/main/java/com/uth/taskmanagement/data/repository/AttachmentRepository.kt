package com.uth.taskmanagement.data.repository

import com.uth.taskmanagement.data.local.AttachmentDao
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.attachment.AttachmentStorage
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(
    private val attachmentDao: AttachmentDao,
    private val attachmentStorage: AttachmentStorage
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

    suspend fun deleteAttachment(id: Long) {
        val attachment = attachmentDao.getById(id) ?: return
        val stagedDeletion = attachmentStorage.stageOwnedFilesForDeletion(listOf(attachment))
        try {
            attachmentDao.deleteById(id)
            attachmentStorage.commitDeletion(stagedDeletion)
        } catch (error: Exception) {
            attachmentStorage.rollbackDeletion(stagedDeletion)
            throw error
        }
    }

    suspend fun deleteAllForTask(taskId: Long) {
        val stagedDeletion = stageOwnedFilesForTask(taskId)
        try {
            attachmentDao.deleteByTaskId(taskId)
            commitFileDeletion(stagedDeletion)
        } catch (error: Exception) {
            rollbackFileDeletion(stagedDeletion)
            throw error
        }
    }

    fun observeAttachments(taskId: Long): Flow<List<TaskAttachmentEntity>> =
        attachmentDao.observeByTaskId(taskId)

    suspend fun getAttachments(taskId: Long): List<TaskAttachmentEntity> =
        attachmentDao.getByTaskId(taskId)

    suspend fun getAttachmentById(id: Long): TaskAttachmentEntity? =
        attachmentDao.getById(id)

    suspend fun getAllAttachments(): List<TaskAttachmentEntity> =
        attachmentDao.getAll()

    suspend fun stageOwnedFilesForTask(taskId: Long): AttachmentStorage.StagedDeletion =
        attachmentStorage.stageOwnedFilesForDeletion(attachmentDao.getByTaskId(taskId))

    fun stageOwnedFiles(
        attachments: Collection<TaskAttachmentEntity>
    ): AttachmentStorage.StagedDeletion =
        attachmentStorage.stageOwnedFilesForDeletion(attachments)

    fun commitFileDeletion(stagedDeletion: AttachmentStorage.StagedDeletion) =
        attachmentStorage.commitDeletion(stagedDeletion)

    fun rollbackFileDeletion(stagedDeletion: AttachmentStorage.StagedDeletion) =
        attachmentStorage.rollbackDeletion(stagedDeletion)
}
