package com.uth.taskmanagement.ui.calendar

import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val dotColorRes: Int?
)
