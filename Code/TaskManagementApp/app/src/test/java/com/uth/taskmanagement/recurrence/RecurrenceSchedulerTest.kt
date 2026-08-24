package com.uth.taskmanagement.recurrence

import com.uth.taskmanagement.data.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurrenceSchedulerTest {

    @Test
    fun futureReminder_isKeptUnchanged() {
        val now = timestamp(2026, Calendar.AUGUST, 24)
        val future = timestamp(2026, Calendar.AUGUST, 25)

        assertEquals(
            future,
            RecurrenceScheduler.calculateNextFutureReminderTime(
                reminderTime = future,
                recurrenceType = RecurrenceType.DAILY,
                currentTime = now
            )
        )
    }

    @Test
    fun recurringReminder_skipsAllPastOccurrences() {
        val now = timestamp(2026, Calendar.AUGUST, 24)
        val oldReminder = timestamp(2026, Calendar.AUGUST, 1)

        val next = RecurrenceScheduler.calculateNextFutureReminderTime(
            reminderTime = oldReminder,
            recurrenceType = RecurrenceType.DAILY,
            currentTime = now
        )

        assertTrue(next != null && next > now)
    }

    @Test
    fun expiredOneTimeReminder_hasNoNextOccurrence() {
        val now = timestamp(2026, Calendar.AUGUST, 24)
        val oldReminder = timestamp(2026, Calendar.AUGUST, 23)

        assertNull(
            RecurrenceScheduler.calculateNextFutureReminderTime(
                reminderTime = oldReminder,
                recurrenceType = RecurrenceType.NONE,
                currentTime = now
            )
        )
    }

    private fun timestamp(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 9, 0, 0)
        }.timeInMillis
}
