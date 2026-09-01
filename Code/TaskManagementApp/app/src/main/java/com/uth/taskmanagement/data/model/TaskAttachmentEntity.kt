package com.uth.taskmanagement.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_attachments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class TaskAttachmentEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID của Task sở hữu attachment này. */
    val taskId: Long,

    /** Tên file hiển thị cho người dùng. */
    val fileName: String,

    /** Android content URI hoặc file URI trỏ đến file thực. */
    val uri: String,

    /** MIME type, ví dụ: "image/jpeg", "application/pdf". */
    @ColumnInfo(defaultValue = "''")
    val mimeType: String = "",

    /** Kích thước file tính bằng bytes. */
    @ColumnInfo(defaultValue = "0")
    val sizeBytes: Long = 0L,

    /** Thời điểm attachment được thêm vào (epoch millis). */
    val createdAt: Long = System.currentTimeMillis()
)
