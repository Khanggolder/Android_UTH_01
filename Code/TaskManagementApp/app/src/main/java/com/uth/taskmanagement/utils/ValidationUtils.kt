package com.uth.taskmanagement.utils

object ValidationUtils {
    fun isNotBlank(value: String): Boolean = value.trim().isNotEmpty()

    fun isValidTaskTitle(title: String): Boolean = isNotBlank(title)

    // TODO: Add validation rules for task date, priority, category, and settings forms.
}
