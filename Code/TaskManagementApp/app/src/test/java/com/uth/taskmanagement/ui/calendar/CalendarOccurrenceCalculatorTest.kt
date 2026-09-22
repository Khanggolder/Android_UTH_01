package com.uth.taskmanagement.ui.calendar

import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class CalendarOccurrenceCalculatorTest {

    @Test
    fun dailyRecurrence_generatesMultipleDays() {
        val start = millis(2026, Calendar.SEPTEMBER, 1)
        val occurrences = occurrences(start, RecurrenceType.DAILY, 5)

        assertEquals(
            listOf(1, 2, 3, 4, 5),
            occurrences.map { localDate(it.occurrenceDateTime).dayOfMonth }
        )
    }

    @Test
    fun weeklyRecurrenceWithoutReminder_generatesEverySevenDays() {
        val start = millis(2026, Calendar.SEPTEMBER, 1)
        val task = task(start, RecurrenceType.WEEKLY).copy(reminderTime = null)
        val occurrences = CalendarOccurrenceCalculator.occurrencesInRange(
            task,
            start,
            millis(2026, Calendar.SEPTEMBER, 22, 23, 59)
        )

        assertEquals(
            listOf(1, 8, 15, 22),
            occurrences.map { localDate(it.occurrenceDateTime).dayOfMonth }
        )
    }

    @Test
    fun monthlyRecurrence_handlesDifferentMonthLengths() {
        val start = millis(2026, Calendar.JANUARY, 31)
        val end = millis(2026, Calendar.APRIL, 1) - 1
        val occurrences = CalendarOccurrenceCalculator.occurrencesInRange(
            task(start, RecurrenceType.MONTHLY),
            start,
            end
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 28)
            ),
            occurrences.map { localDate(it.occurrenceDateTime) }
        )
    }

    @Test
    fun noRecurrence_returnsOnlyOriginalDueDate() {
        val start = millis(2026, Calendar.SEPTEMBER, 10)
        val occurrences = CalendarOccurrenceCalculator.occurrencesInRange(
            task(start, RecurrenceType.NONE),
            millis(2026, Calendar.SEPTEMBER, 1),
            millis(2026, Calendar.SEPTEMBER, 30, 23, 59)
        )

        assertEquals(listOf(start), occurrences.map { it.occurrenceDateTime })
    }

    private fun occurrences(
        start: Long,
        recurrenceType: RecurrenceType,
        endDay: Int
    ) = CalendarOccurrenceCalculator.occurrencesInRange(
        task(start, recurrenceType),
        start,
        millis(2026, Calendar.SEPTEMBER, endDay, 23, 59)
    )

    private fun task(dueDateTime: Long, recurrenceType: RecurrenceType) = TaskEntity(
        title = "Recurring task",
        dueDateTime = dueDateTime,
        recurrenceType = recurrenceType
    )

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 9,
        minute: Int = 0
    ): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, minute)
    }.timeInMillis

    private fun localDate(time: Long): LocalDate =
        Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
}
