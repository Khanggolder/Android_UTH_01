package com.uth.taskmanagement.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private const val DISPLAY_DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm"

    fun formatDisplayDateTime(timestampMillis: Long): String {
        return SimpleDateFormat(DISPLAY_DATE_TIME_PATTERN, Locale.getDefault())
            .format(Date(timestampMillis))
    }

    // TODO: Add parsing, due-date helpers, and recurrence helpers when task module is implemented.
}
