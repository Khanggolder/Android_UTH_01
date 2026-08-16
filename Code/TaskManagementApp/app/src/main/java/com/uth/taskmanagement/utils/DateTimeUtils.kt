package com.uth.taskmanagement.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private const val DISPLAY_DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm"
    private const val DISPLAY_DATE_PATTERN = "dd/MM/yyyy"

    fun formatDisplayDateTime(timestampMillis: Long): String {
        return SimpleDateFormat(DISPLAY_DATE_TIME_PATTERN, Locale.getDefault())
            .format(Date(timestampMillis))
    }

    fun formatDisplayDate(timestampMillis: Long): String {
        return SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.getDefault())
            .format(Date(timestampMillis))
    }

    fun isOverdue(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return timestampMillis < nowMillis
    }

    fun startOfDay(timestampMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDay(timestampMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDay(timestampMillis)
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
    }
}