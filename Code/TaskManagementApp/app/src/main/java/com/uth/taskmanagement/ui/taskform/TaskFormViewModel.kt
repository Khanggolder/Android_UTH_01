package com.uth.taskmanagement.ui.taskform

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
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

    // Timeline
    val startDateTime: Long = System.currentTimeMillis(),
    val dueDateTime: Long = System.currentTimeMillis() + 60 * 60 * 1000L,

    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
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

    private val _formState =
        MutableStateFlow(TaskFormState())

    val formState: StateFlow<TaskFormState> =
        _formState.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // Setters
    // ─────────────────────────────────────────────────────────────

    fun setTitle(value: String) {
        _formState.value =
            _formState.value.copy(
                title = value,
                errorMessage = null
            )
    }

    fun setDescription(value: String) {
        _formState.value =
            _formState.value.copy(
                description = value,
                errorMessage = null
            )
    }

    fun setStartDateTime(millis: Long) {
        _formState.value =
            _formState.value.copy(
                startDateTime = millis,
                errorMessage = null
            )
    }

    fun setDueDateTime(millis: Long) {
        _formState.value =
            _formState.value.copy(
                dueDateTime = millis,
                errorMessage = null
            )
    }

    fun setPriority(priority: TaskPriority) {
        _formState.value =
            _formState.value.copy(
                priority = priority,
                errorMessage = null
            )
    }

    fun setStatus(status: TaskStatus) {
        _formState.value =
            _formState.value.copy(
                status = status,
                errorMessage = null
            )
    }

    fun setReminderTime(millis: Long?) {
        _formState.value =
            _formState.value.copy(
                reminderTime = millis,
                errorMessage = null
            )
    }

    fun setRecurrenceType(type: RecurrenceType) {
        _formState.value =
            _formState.value.copy(
                recurrenceType = type,
                errorMessage = null
            )
    }

    // ─────────────────────────────────────────────────────────────
    // Load task khi Edit
    // ─────────────────────────────────────────────────────────────

    fun loadTask(taskId: Long) {

        if (taskId <= 0L) return

        viewModelScope.launch {

            try {

                val task =
                    repository.getTaskById(taskId)
                        ?: return@launch

                _formState.value =
                    TaskFormState(
                        taskId = task.id,
                        title = task.title,
                        description = task.description,

                        // Timeline
                        startDateTime = task.startDateTime,
                        dueDateTime = task.dueDateTime,

                        priority = task.priority,
                        status = task.status,
                        reminderTime = task.reminderTime,
                        recurrenceType = task.recurrenceType
                    )

            } catch (e: Exception) {

                _formState.value =
                    _formState.value.copy(
                        errorMessage =
                            "Failed to load task: ${e.message}"
                    )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Save Task
    // ─────────────────────────────────────────────────────────────

    fun saveTask() {

        val state = _formState.value

        // ─────────────────────────────────────────────────────────
        // Validation
        // ─────────────────────────────────────────────────────────

        if (state.title.isBlank()) {

            _formState.value =
                state.copy(
                    errorMessage = "Title is required"
                )

            return
        }

        if (state.description.isBlank()) {

            _formState.value =
                state.copy(
                    errorMessage = "Description is required"
                )

            return
        }

        /*
         * Quan trọng cho Project Timeline:
         *
         * Start Date phải nằm trước Due Date.
         */
        if (state.startDateTime >= state.dueDateTime) {

            _formState.value =
                state.copy(
                    errorMessage =
                        "Start date must be before the due date"
                )

            return
        }

        /*
         * Giữ validation hiện tại của project.
         */
        if (state.dueDateTime <= System.currentTimeMillis()) {

            _formState.value =
                state.copy(
                    errorMessage =
                        "Due date must be in the future"
                )

            return
        }

        /*
         * Reminder phải nằm trong tương lai.
         */
        if (
            state.status != TaskStatus.COMPLETED &&
            state.reminderTime != null &&
            state.reminderTime <= System.currentTimeMillis()
        ) {

            _formState.value =
                state.copy(
                    errorMessage =
                        "Reminder time must be in the future"
                )

            return
        }

        /*
         * Reminder không được sau deadline.
         */
        if (
            state.status != TaskStatus.COMPLETED &&
            state.reminderTime != null &&
            state.reminderTime > state.dueDateTime
        ) {

            _formState.value =
                state.copy(
                    errorMessage =
                        "Reminder time cannot be after the due date"
                )

            return
        }

        _formState.value =
            state.copy(
                isLoading = true,
                errorMessage = null
            )

        // ─────────────────────────────────────────────────────────
        // Save database
        // ─────────────────────────────────────────────────────────

        viewModelScope.launch {

            try {

                val context =
                    getApplication<Application>()
                        .applicationContext

                val reminderTime =
                    state.reminderTime

                if (state.taskId <= 0L) {

                    // ─────────────────────────────────────────────
                    // CREATE
                    // ─────────────────────────────────────────────

                    val newTask =
                        TaskEntity(
                            title =
                                state.title.trim(),

                            description =
                                state.description.trim(),

                            startDateTime =
                                state.startDateTime,

                            dueDateTime =
                                state.dueDateTime,

                            priority =
                                state.priority,

                            status =
                                state.status,

                            isCompleted =
                                state.status ==
                                    TaskStatus.COMPLETED,

                            reminderTime =
                                reminderTime,

                            recurrenceType =
                                if (reminderTime != null) {

                                    state.recurrenceType

                                } else {

                                    RecurrenceType.NONE
                                }
                        )

                    val newId =
                        repository.insertTask(
                            newTask
                        )

                    // Schedule reminder
                    if (
                        reminderTime != null &&
                        reminderTime >
                        System.currentTimeMillis() &&
                        state.status !=
                        TaskStatus.COMPLETED
                    ) {

                        RecurrenceScheduler
                            .scheduleNextAlarm(
                                context = context,
                                taskId = newId,
                                title = state.title,
                                description =
                                    state.description,
                                triggerTimeMillis =
                                    reminderTime
                            )
                    }

                } else {

                    // ─────────────────────────────────────────────
                    // UPDATE
                    // ─────────────────────────────────────────────

                    val existing =
                        repository
                            .getTaskById(
                                state.taskId
                            )
                            ?: throw IllegalStateException(
                                "Task not found"
                            )

                    val updatedTask =
                        existing.copy(
                            title =
                                state.title.trim(),

                            description =
                                state.description.trim(),

                            startDateTime =
                                state.startDateTime,

                            dueDateTime =
                                state.dueDateTime,

                            priority =
                                state.priority,

                            status =
                                state.status,

                            isCompleted =
                                state.status ==
                                    TaskStatus.COMPLETED,

                            reminderTime =
                                reminderTime,

                            recurrenceType =
                                if (reminderTime != null) {

                                    state.recurrenceType

                                } else {

                                    RecurrenceType.NONE
                                },

                            updatedAt =
                                System.currentTimeMillis()
                        )

                    repository.updateTask(
                        updatedTask
                    )

                    // Hủy alarm cũ
                    RecurrenceScheduler.cancelAlarm(
                        context,
                        state.taskId
                    )

                    // Schedule reminder mới
                    if (
                        reminderTime != null &&
                        reminderTime >
                        System.currentTimeMillis() &&
                        state.status !=
                        TaskStatus.COMPLETED
                    ) {

                        RecurrenceScheduler
                            .scheduleNextAlarm(
                                context = context,
                                taskId =
                                    state.taskId,
                                title =
                                    state.title,
                                description =
                                    state.description,
                                triggerTimeMillis =
                                    reminderTime
                            )
                    }
                }

                // Save thành công
                _formState.value =
                    _formState.value.copy(
                        isLoading = false,
                        isSaved = true,
                        errorMessage = null
                    )

            } catch (e: Exception) {

                _formState.value =
                    _formState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "Failed to save task: ${e.message}"
                    )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete Task
    // ─────────────────────────────────────────────────────────────

    fun deleteTask() {

        val state =
            _formState.value

        if (state.taskId <= 0L) {
            return
        }

        _formState.value =
            state.copy(
                isLoading = true,
                errorMessage = null
            )

        viewModelScope.launch {

            try {

                val context =
                    getApplication<Application>()
                        .applicationContext

                RecurrenceScheduler.cancelAlarm(
                    context,
                    state.taskId
                )

                repository.deleteTaskById(
                    state.taskId
                )

                _formState.value =
                    _formState.value.copy(
                        isLoading = false,
                        isSaved = true
                    )

            } catch (e: Exception) {

                _formState.value =
                    _formState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "Failed to delete task: ${e.message}"
                    )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Mark Completed
    // ─────────────────────────────────────────────────────────────

    fun markCompleted() {

        val state =
            _formState.value

        if (state.taskId <= 0L) {
            return
        }

        _formState.value =
            state.copy(
                isLoading = true,
                errorMessage = null
            )

        viewModelScope.launch {

            try {

                val context =
                    getApplication<Application>()
                        .applicationContext

                RecurrenceScheduler.cancelAlarm(
                    context,
                    state.taskId
                )

                repository.setTaskCompleted(
                    taskId =
                        state.taskId,
                    completed =
                        true
                )

                _formState.value =
                    _formState.value.copy(
                        status =
                            TaskStatus.COMPLETED,
                        isLoading =
                            false,
                        isSaved =
                            true
                    )

            } catch (e: Exception) {

                _formState.value =
                    _formState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "Failed to complete task: ${e.message}"
                    )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Clear Reminder
    // ─────────────────────────────────────────────────────────────

    fun clearReminder() {

        val state =
            _formState.value

        _formState.value =
            state.copy(
                reminderTime = null,
                recurrenceType =
                    RecurrenceType.NONE,
                errorMessage = null
            )
    }
}