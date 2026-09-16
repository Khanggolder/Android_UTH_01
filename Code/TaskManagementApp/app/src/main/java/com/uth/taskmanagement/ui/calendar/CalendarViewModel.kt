package com.uth.taskmanagement.ui.calendar

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.R
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModel(private val repo: TaskRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthDays: StateFlow<List<CalendarDay>> = combine(
        _displayedMonth,
        _selectedDate,
        repo.observeAllTasks()
    ) { month, selected, allTasks ->
        buildMonthGrid(month, selected, allTasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDay: StateFlow<List<TaskOccurrence>> = combine(
        _selectedDate,
        repo.observeAllTasks()
    ) { date, allTasks ->
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        allTasks
            .flatMap { task ->
                buildList {
                    addAll(
                        CalendarOccurrenceCalculator.occurrencesInRange(
                            task,
                            dayStart,
                            dayEnd
                        )
                    )
                    task.reminderTime
                        ?.takeIf { !task.isCompleted && it in dayStart..dayEnd }
                        ?.let { reminderTime ->
                            add(
                                TaskOccurrence(
                                    task = task,
                                    occurrenceDateTime = reminderTime,
                                    entryType = CalendarEntryType.REMINDER
                                )
                            )
                        }
                }
            }
            .sortedBy { it.occurrenceDateTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToPreviousMonth() {
        _displayedMonth.value = _displayedMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _displayedMonth.value = _displayedMonth.value.plusMonths(1)
    }
    fun setTaskCompleted(context: Context, taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            if (completed) {
                RecurrenceScheduler.cancelAlarm(context, taskId)
            }
            repo.setTaskCompleted(taskId = taskId, completed = completed)
            if (!completed) {
                repo.getTaskById(taskId)?.let { task ->
                    val scheduledTime = RecurrenceScheduler.scheduleReminderForTask(
                        context = context,
                        task = task
                    )
                    if (scheduledTime != null && scheduledTime != task.reminderTime) {
                        repo.updateReminderTime(taskId, scheduledTime)
                    }
                }
            }
        }
    }
    private fun buildMonthGrid(
        month: YearMonth,
        selected: LocalDate,
        allTasks: List<TaskEntity>
    ): List<CalendarDay> {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        val firstOfMonth = month.atDay(1)
        val gridStart = firstOfMonth.minusDays(
            (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
        )
        val totalCells = 42 // co dinh 6 hang x 7 cot cho moi thang, tranh lich nhay so hang
        val gridEnd = gridStart.plusDays((totalCells - 1).toLong())
        val rangeStart = gridStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = gridEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val occurrencesByDate = allTasks
            .flatMap { task ->
                CalendarOccurrenceCalculator.occurrencesInRange(task, rangeStart, rangeEnd)
            }
            .groupBy { occurrence ->
                java.time.Instant.ofEpochMilli(occurrence.occurrenceDateTime)
                    .atZone(zone)
                    .toLocalDate()
            }

        return (0 until totalCells).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            val occurrencesOfDay = occurrencesByDate[date].orEmpty()
            CalendarDay(
                date = date,
                isCurrentMonth = YearMonth.from(date) == month,
                isSelected = date == selected,
                dotColorRes = dotColorForDay(occurrencesOfDay, now)
            )
        }
    }

    private fun dotColorForDay(
        occurrencesOfDay: List<TaskOccurrence>,
        currentTime: Long
    ): Int? {
        if (occurrencesOfDay.isEmpty()) return null

        val hasOverdue = occurrencesOfDay.any {
            !it.task.isCompleted && it.occurrenceDateTime < currentTime
        }
        if (hasOverdue) return R.color.timeline_overdue

        val hasDueSoon = occurrencesOfDay.any {
            !it.task.isCompleted &&
                    it.occurrenceDateTime >= currentTime &&
                    it.occurrenceDateTime - currentTime <= SOON_WINDOW_MILLIS
        }
        if (hasDueSoon) return R.color.timeline_pending

        val allCompleted = occurrencesOfDay.all {
            it.task.status == TaskStatus.COMPLETED || it.task.isCompleted
        }
        if (allCompleted) return R.color.timeline_completed

        return R.color.timeline_in_progress
    }

    companion object {
        private const val SOON_WINDOW_MILLIS = 3L * 24 * 60 * 60 * 1000
    }
}

class CalendarViewModelFactory(
    private val repo: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
