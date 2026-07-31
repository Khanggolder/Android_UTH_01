package com.uth.taskmanagement.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String = "",

    val dueDateTime: Long,

    val priority: TaskPriority = TaskPriority.MEDIUM,

    val status: TaskStatus = TaskStatus.PENDING,

    val isCompleted: Boolean = false,

    val reminderTime: Long? = null,

    val recurrenceType: RecurrenceType = RecurrenceType.NONE,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)