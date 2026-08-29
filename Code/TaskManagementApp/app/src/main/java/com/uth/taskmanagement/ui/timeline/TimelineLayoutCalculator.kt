package com.uth.taskmanagement.ui.timeline

import com.uth.taskmanagement.data.model.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal object TimelineLayoutCalculator {

    const val DAY_WIDTH_DP = 80
    const val MAX_TIMELINE_DAYS = 180
    private const val EXTRA_DAYS_BEFORE = 2L
    private const val EXTRA_DAYS_AFTER = 5L

    data class Layout(
        val timelineStart: Long,
        val timelineEnd: Long,
        val totalDays: Int,
        val tasks: List<TaskEntity>
    )

    data class BarPosition(
        val startDayIndex: Int,
        val durationDays: Int
    )

    fun calculate(
        tasks: List<TaskEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Layout? {
        if (tasks.isEmpty()) return null

        val normalizedTasks = tasks.map(::normalizeTask)
        val earliestStart = normalizedTasks.minOf { it.startDateTime }
        val latestEnd = normalizedTasks.maxOf { it.dueDateTime }

        val timelineStart = addDays(
            startOfDay(earliestStart, zoneId),
            -EXTRA_DAYS_BEFORE,
            zoneId
        )
        val naturalEnd = addDays(
            startOfDay(latestEnd, zoneId),
            EXTRA_DAYS_AFTER,
            zoneId
        )
        val maximumEnd = addDays(
            timelineStart,
            (MAX_TIMELINE_DAYS - 1).toLong(),
            zoneId
        )
        val timelineEnd = minOf(naturalEnd, maximumEnd)
        val endExclusive = addDays(timelineEnd, 1L, zoneId)

        val visibleTasks = normalizedTasks
            .filter { task ->
                task.startDateTime < endExclusive &&
                    task.dueDateTime >= timelineStart
            }
            .sortedBy { it.startDateTime }

        return Layout(
            timelineStart = timelineStart,
            timelineEnd = timelineEnd,
            totalDays = daysBetween(timelineStart, timelineEnd, zoneId) + 1,
            tasks = visibleTasks
        )
    }

    fun barPosition(
        task: TaskEntity,
        timelineStart: Long,
        totalDays: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BarPosition? {
        if (totalDays <= 0) return null

        val rangeStart = toLocalDate(timelineStart, zoneId)
        val rangeEnd = rangeStart.plusDays((totalDays - 1).toLong())
        val taskStart = toLocalDate(task.startDateTime, zoneId)
        val taskEnd = toLocalDate(task.dueDateTime, zoneId)

        if (taskStart > rangeEnd || taskEnd < rangeStart) return null

        val visibleStart = if (taskStart < rangeStart) rangeStart else taskStart
        val visibleEnd = if (taskEnd > rangeEnd) rangeEnd else taskEnd
        val startIndex = ChronoUnit.DAYS.between(rangeStart, visibleStart).toInt()
        val duration = ChronoUnit.DAYS.between(visibleStart, visibleEnd).toInt() + 1

        return BarPosition(
            startDayIndex = startIndex,
            durationDays = duration
        )
    }

    fun startOfDay(
        millis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = toLocalDate(millis, zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    fun addDays(
        millis: Long,
        days: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = toLocalDate(millis, zoneId)
        .plusDays(days)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    private fun normalizeTask(task: TaskEntity): TaskEntity {
        val safeStart = when {
            task.startDateTime > 0L -> task.startDateTime
            task.createdAt > 0L -> task.createdAt
            else -> task.dueDateTime
        }
        val safeDue = maxOf(task.dueDateTime, safeStart)

        return task.copy(
            startDateTime = safeStart,
            dueDateTime = safeDue
        )
    }

    private fun daysBetween(
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId
    ): Int = ChronoUnit.DAYS.between(
        toLocalDate(startMillis, zoneId),
        toLocalDate(endMillis, zoneId)
    ).toInt()

    private fun toLocalDate(millis: Long, zoneId: ZoneId) =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
}
