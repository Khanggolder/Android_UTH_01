package com.uth.taskmanagement.ui.calendar

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.R
import com.uth.taskmanagement.data.model.RecurrenceType
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
import java.util.Calendar

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
                    findOccurrenceInRange(task, dayStart, dayEnd)?.let(::add)
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
    private fun findOccurrenceInRange(
        task: TaskEntity,
        rangeStart: Long,
        rangeEnd: Long
    ): TaskOccurrence? {
        if (task.recurrenceType == RecurrenceType.NONE || task.isCompleted) {
            return if (task.dueDateTime in rangeStart..rangeEnd) {
                TaskOccurrence(task, task.dueDateTime)
            } else null
        }

        var current = task.dueDateTime
        var safeGuard = 0

        while (current < rangeStart && safeGuard < 1000) {
            current = nextOccurrence(current, task.recurrenceType)
            safeGuard++
        }

        return if (current in rangeStart..rangeEnd) {
            TaskOccurrence(task, current)
        } else null
    }

    private fun buildMonthGrid(
        month: YearMonth,
        selected: LocalDate,
        allTasks: List<TaskEntity>
    ): List<CalendarDay> {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        val tasksByDate: Map<LocalDate, List<TaskEntity>> = allTasks.groupBy { task ->
            java.time.Instant.ofEpochMilli(task.dueDateTime).atZone(zone).toLocalDate()
        }

        val firstOfMonth = month.atDay(1)
        val gridStart = firstOfMonth.minusDays(
            (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
        )
        val totalCells = 42 // co dinh 6 hang x 7 cot cho moi thang, tranh lich nhay so hang

        return (0 until totalCells).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            val tasksOfDay = tasksByDate[date].orEmpty()
            CalendarDay(
                date = date,
                isCurrentMonth = YearMonth.from(date) == month,
                isSelected = date == selected,
                dotColorRes = dotColorForDay(tasksOfDay, now)
            )
        }
    }

    private fun dotColorForDay(tasksOfDay: List<TaskEntity>, currentTime: Long): Int? {
        if (tasksOfDay.isEmpty()) return null

        val hasOverdue = tasksOfDay.any { !it.isCompleted && it.dueDateTime < currentTime }
        if (hasOverdue) return R.color.timeline_overdue

        val hasDueSoon = tasksOfDay.any {
            !it.isCompleted &&
                    it.dueDateTime >= currentTime &&
                    it.dueDateTime - currentTime <= SOON_WINDOW_MILLIS
        }
        if (hasDueSoon) return R.color.timeline_pending

        val allCompleted = tasksOfDay.all { it.status == TaskStatus.COMPLETED || it.isCompleted }
        if (allCompleted) return R.color.timeline_completed

        return R.color.timeline_in_progress
    }

    private fun nextOccurrence(current: Long, type: RecurrenceType): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        when (type) {
            RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            RecurrenceType.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
            RecurrenceType.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceType.NONE -> {}
        }
        return cal.timeInMillis
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
