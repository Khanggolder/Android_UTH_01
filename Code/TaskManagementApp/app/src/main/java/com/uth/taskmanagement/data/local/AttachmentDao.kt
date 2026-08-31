package com.uth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: TaskAttachmentEntity): Long

    @Query("DELETE FROM task_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM task_attachments WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    /** Reactive – tự cập nhật khi danh sách attachment thay đổi. */
    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeByTaskId(taskId: Long): Flow<List<TaskAttachmentEntity>>

    /** One-shot – dùng khi chỉ cần đọc một lần. */
    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTaskId(taskId: Long): List<TaskAttachmentEntity>

    @Query("SELECT * FROM task_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskAttachmentEntity?
}
