package com.uth.taskmanagement.data.local

import androidx.room.TypeConverter
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus

class Converters {

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String {
        return priority.name
    }

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority {
        return TaskPriority.valueOf(value)
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus {
        return TaskStatus.valueOf(value)
    }

    @TypeConverter
    fun fromRecurrenceType(recurrenceType: RecurrenceType): String {
        return recurrenceType.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return RecurrenceType.valueOf(value)
    }
}