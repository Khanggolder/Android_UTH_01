package com.uth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    /** insertAttachment — thêm 1 attachment, trả về id được sinh. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: TaskAttachmentEntity): Long

    /** insertAttachments — thêm nhiều attachment cùng lúc (batch). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(attachments: List<TaskAttachmentEntity>): List<Long>

    /** deleteAttachment — xóa attachment theo id. */
    @Query("DELETE FROM task_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** deleteAttachmentsByTaskId — xóa toàn bộ attachment của một task. */
    @Query("DELETE FROM task_attachments WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    /** observeAttachmentsByTaskId — Flow tự cập nhật khi danh sách thay đổi. */
    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeByTaskId(taskId: Long): Flow<List<TaskAttachmentEntity>>

    /** getAttachmentsByTaskId — one-shot, dùng khi chỉ cần đọc một lần. */
    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTaskId(taskId: Long): List<TaskAttachmentEntity>

    /** getAttachmentById — lấy 1 attachment theo id. */
    @Query("SELECT * FROM task_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskAttachmentEntity?
}
