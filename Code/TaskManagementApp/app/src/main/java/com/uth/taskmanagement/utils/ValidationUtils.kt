package com.uth.taskmanagement.utils

object ValidationUtils {
    fun isNotBlank(value: String): Boolean = value.trim().isNotEmpty()

    fun isValidTaskTitle(title: String): Boolean = isNotBlank(title)

    fun isValidTaskDescription(description: String): Boolean = isNotBlank(description)

    fun isFutureTimestamp(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return timestampMillis > nowMillis
    }
}