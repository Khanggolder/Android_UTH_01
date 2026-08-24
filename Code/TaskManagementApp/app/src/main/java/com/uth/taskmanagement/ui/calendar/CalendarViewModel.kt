package com.uth.taskmanagement.ui.calendar

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class CalendarViewModel(private val repo: TaskRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

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
