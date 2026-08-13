package com.uth.taskmanagement.ui.taskform

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskFormState(
    val taskId: Long = -1L,
    val title: String = "",
    val description: String = "",
    val dueDateTime: Long = System.currentTimeMillis() + 60 * 60 * 1000L, // +1h default
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val reminderTime: Long? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class TaskFormViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    // ── Setters gọi từ Fragment khi người dùng nhập liệu ──────────────────

    fun setTitle(value: String) {
        _formState.value = _formState.value.copy(title = value, errorMessage = null)
    }

    fun setDescription(value: String) {
        _formState.value = _formState.value.copy(description = value)
    }

    fun setDueDateTime(millis: Long) {
        _formState.value = _formState.value.copy(dueDateTime = millis)
    }

    fun setPriority(priority: TaskPriority) {
        _formState.value = _formState.value.copy(priority = priority)
    }

    fun setReminderTime(millis: Long?) {
        _formState.value = _formState.value.copy(reminderTime = millis)
    }

    fun setRecurrenceType(type: RecurrenceType) {
        _formState.value = _formState.value.copy(recurrenceType = type)
    }

    // ── Load task khi chỉnh sửa ───────────────────────────────────────────

    fun loadTask(taskId: Long) {
        if (taskId <= 0L) return
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            _formState.value = TaskFormState(
                taskId = task.id,
                title = task.title,
                description = task.description,
                dueDateTime = task.dueDateTime,
                priority = task.priority,
                reminderTime = task.reminderTime,
                recurrenceType = task.recurrenceType
            )
        }
    }

    // ── Lưu Task (insert hoặc update) + schedule/cancel Alarm ────────────

    fun saveTask() {
        val state = _formState.value

        if (state.title.isBlank()) {
            _formState.value = state.copy(errorMessage = "Tiêu đề không được để trống")
            return
        }

        _formState.value = state.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val reminderTime = state.reminderTime

                if (state.taskId <= 0L) {
                    // ── Tạo mới ──────────────────────────────────────────
                    val newTask = TaskEntity(
                        title = state.title,
                        description = state.description,
                        dueDateTime = state.dueDateTime,
                        priority = state.priority,
                        reminderTime = reminderTime,
                        recurrenceType = if (reminderTime != null) state.recurrenceType
                        else RecurrenceType.NONE
                    )
                    val newId = repository.insertTask(newTask)

                    // Đặt Alarm nếu có reminderTime
                    if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                        RecurrenceScheduler.scheduleNextAlarm(
                            context = context,
                            taskId = newId,
                            title = state.title,
                            description = state.description,
                            triggerTimeMillis = reminderTime
                        )
                    }
                } else {
                    // ── Cập nhật ─────────────────────────────────────────
                    val existing = repository.getTaskById(state.taskId) ?: return@launch
                    val updatedTask = existing.copy(
                        title = state.title,
                        description = state.description,
                        dueDateTime = state.dueDateTime,
                        priority = state.priority,
                        reminderTime = reminderTime,
                        recurrenceType = if (reminderTime != null) state.recurrenceType
                        else RecurrenceType.NONE
                    )
                    repository.updateTask(updatedTask)

                    // Hủy Alarm cũ rồi đặt lại
                    RecurrenceScheduler.cancelAlarm(context, state.taskId)
                    if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                        RecurrenceScheduler.scheduleNextAlarm(
                            context = context,
                            taskId = state.taskId,
                            title = state.title,
                            description = state.description,
                            triggerTimeMillis = reminderTime
                        )
                    }
                }

                _formState.value = _formState.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = "Lỗi khi lưu: ${e.message}"
                )
            }
        }
    }

    // ── Hủy reminder (dùng khi xóa reminder trong form) ──────────────────

    fun clearReminder() {
        val state = _formState.value
        if (state.taskId > 0L) {
            val context = getApplication<Application>().applicationContext
            RecurrenceScheduler.cancelAlarm(context, state.taskId)
        }
        _formState.value = state.copy(reminderTime = null, recurrenceType = RecurrenceType.NONE)
    }
}