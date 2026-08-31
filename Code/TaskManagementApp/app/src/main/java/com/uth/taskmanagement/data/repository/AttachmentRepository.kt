package com.uth.taskmanagement.data.repository

import com.uth.taskmanagement.data.local.AttachmentDao
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(
    private val attachmentDao: AttachmentDao
) {

    /**
     * Thêm attachment mới cho một task.
     * @return id được Room tự sinh sau khi insert.
     */
    suspend fun addAttachment(attachment: TaskAttachmentEntity): Long =
        attachmentDao.insert(
            attachment.copy(
                id = 0,
                createdAt = System.currentTimeMillis()
            )
        )

    /**
     * Thêm nhiều attachment cùng lúc (batch insert).
     * @return danh sách id được sinh tương ứng.
     */
    suspend fun addAttachments(attachments: List<TaskAttachmentEntity>): List<Long> {
        val now = System.currentTimeMillis()
        return attachmentDao.insertAll(
            attachments.map { it.copy(id = 0, createdAt = now) }
        )
    }

    suspend fun deleteAttachment(id: Long) =
        attachmentDao.deleteById(id)

    /** Xóa toàn bộ attachment của một task (thường không cần gọi trực tiếp vì CASCADE). */
    suspend fun deleteAllForTask(taskId: Long) =
        attachmentDao.deleteByTaskId(taskId)

    /** Reactive stream – dùng trong ViewModel để observe. */
    fun observeAttachments(taskId: Long): Flow<List<TaskAttachmentEntity>> =
        attachmentDao.observeByTaskId(taskId)

    /** One-shot – dùng khi cần đọc một lần (backup, export…). */
    suspend fun getAttachments(taskId: Long): List<TaskAttachmentEntity> =
        attachmentDao.getByTaskId(taskId)

    suspend fun getAttachmentById(id: Long): TaskAttachmentEntity? =
        attachmentDao.getById(id)
}
