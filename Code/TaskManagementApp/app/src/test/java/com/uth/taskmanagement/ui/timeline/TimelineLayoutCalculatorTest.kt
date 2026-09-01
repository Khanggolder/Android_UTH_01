package com.uth.taskmanagement.ui.timeline

import com.uth.taskmanagement.data.model.TaskEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineLayoutCalculatorTest {

    private val zoneId = ZoneId.of("UTC")
    private val baseDate = LocalDate.of(2026, 1, 1)

    @Test
    fun calculate_emptyTasks_returnsNull() {
        assertNull(TimelineLayoutCalculator.calculate(emptyList(), zoneId))
    }

    @Test
    fun calculate_longRange_capsAt180DaysAndExcludesLaterTasks() {
        val firstTask = task(id = 1L, startDay = 0, dueDay = 1)
        val laterTask = task(id = 2L, startDay = 250, dueDay = 251)

        val layout = TimelineLayoutCalculator.calculate(
            listOf(firstTask, laterTask),
            zoneId
        )!!

        assertEquals(TimelineLayoutCalculator.MAX_TIMELINE_DAYS, layout.totalDays)
        assertEquals(listOf(1L), layout.tasks.map { it.id })
    }

    @Test
    fun calculate_shortRange_includesPaddingWithinLimit() {
        val layout = TimelineLayoutCalculator.calculate(
            listOf(task(id = 1L, startDay = 10, dueDay = 12)),
            zoneId
        )!!

        assertEquals(millis(8), layout.timelineStart)
        assertEquals(millis(17), layout.timelineEnd)
        assertEquals(10, layout.totalDays)
    }

    @Test
    fun calculate_invalidDates_normalizesStartAndDueDate() {
        val createdAt = millis(4)
        val invalidTask = TaskEntity(
            id = 1L,
            title = "Invalid dates",
            startDateTime = 0L,
            dueDateTime = millis(3),
            createdAt = createdAt
        )

        val normalized = TimelineLayoutCalculator.calculate(
            listOf(invalidTask),
            zoneId
        )!!.tasks.single()

        assertEquals(createdAt, normalized.startDateTime)
        assertEquals(createdAt, normalized.dueDateTime)
    }

    @Test
    fun barPosition_taskCrossingLeftBoundary_isClippedToVisibleRange() {
        val position = TimelineLayoutCalculator.barPosition(
            task = task(id = 1L, startDay = -5, dueDay = 3),
            timelineStart = millis(0),
            totalDays = 10,
            zoneId = zoneId
        )!!

        assertEquals(0, position.startDayIndex)
        assertEquals(4, position.durationDays)
    }

    @Test
    fun barPosition_taskOutsideRange_returnsNull() {
        val position = TimelineLayoutCalculator.barPosition(
            task = task(id = 1L, startDay = 20, dueDay = 21),
            timelineStart = millis(0),
            totalDays = 10,
            zoneId = zoneId
        )

        assertNull(position)
    }

    @Test
    fun calculate_visibleTasks_areSortedByStartDate() {
        val layout = TimelineLayoutCalculator.calculate(
            listOf(
                task(id = 2L, startDay = 3, dueDay = 4),
                task(id = 1L, startDay = 1, dueDay = 2)
            ),
            zoneId
        )!!

        assertEquals(listOf(1L, 2L), layout.tasks.map { it.id })
        assertTrue(layout.totalDays <= TimelineLayoutCalculator.MAX_TIMELINE_DAYS)
    }

    private fun task(
        id: Long,
        startDay: Long,
        dueDay: Long
    ) = TaskEntity(
        id = id,
        title = "Task $id",
        startDateTime = millis(startDay),
        dueDateTime = millis(dueDay),
        createdAt = millis(startDay)
    )

    private fun millis(dayOffset: Long): Long = baseDate
        .plusDays(dayOffset)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}
