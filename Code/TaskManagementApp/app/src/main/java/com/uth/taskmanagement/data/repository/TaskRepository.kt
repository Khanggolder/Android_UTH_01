package com.uth.taskmanagement.data.repository

import com.uth.taskmanagement.data.local.TaskDao
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {

    fun observeAllTasks(): Flow<List<TaskEntity>> =
        taskDao.observeAllTasks()

    suspend fun getAllTasks(): List<TaskEntity> =
        taskDao.getAllTasks()
    
    fun observeTaskById(taskId: Long): Flow<TaskEntity?> =
        taskDao.observeTaskById(taskId)

    suspend fun getTaskById(taskId: Long): TaskEntity? =
        taskDao.getTaskById(taskId)

    suspend fun insertTask(task: TaskEntity): Long {
        val currentTime = System.currentTimeMillis()

        return taskDao.insertTask(
            task.copy(
                id = 0,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        require(task.id > 0) {
            "Task ID must be greater than 0 when updating."
        }

        taskDao.updateTask(
            task.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteTask(task: TaskEntity) =
        taskDao.deleteTask(task)

    suspend fun deleteTaskById(taskId: Long) {
        require(taskId > 0) {
            "Task ID must be greater than 0 when deleting."
        }

        taskDao.deleteTaskById(taskId)
    }

    suspend fun setTaskCompleted(
        taskId: Long,
        completed: Boolean
    ) {
        require(taskId > 0) {
            "Task ID must be greater than 0 when updating completion."
        }

        val status = if (completed) {
            TaskStatus.COMPLETED
        } else {
            TaskStatus.PENDING
        }

        taskDao.updateCompletedState(
            taskId = taskId,
            isCompleted = completed,
            status = status,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun observeTasksByStatus(
        status: TaskStatus
    ): Flow<List<TaskEntity>> =
        taskDao.observeTasksByStatus(status)

    fun observeTasksByPriority(
        priority: TaskPriority
    ): Flow<List<TaskEntity>> =
        taskDao.observeTasksByPriority(priority)

    fun observeTasksSortedByPriority(): Flow<List<TaskEntity>> =
        taskDao.observeTasksSortedByPriority()

    fun observeTasksSortedByDueDate(): Flow<List<TaskEntity>> =
        taskDao.observeTasksSortedByDueDate()

    fun observeOverdueTasks(): Flow<List<TaskEntity>> =
        taskDao.observeOverdueTasks(System.currentTimeMillis())

    fun observeTasksBetweenDates(
        start: Long,
        end: Long
    ): Flow<List<TaskEntity>> =
        taskDao.observeTasksBetweenDates(start, end)

    suspend fun getActiveReminderTasks(): List<TaskEntity> =
        taskDao.getActiveReminderTasks()

    suspend fun replaceAllTasks(tasks: List<TaskEntity>) {
        taskDao.replaceAllTasks(tasks)
    }

    suspend fun updateReminderTime(taskId: Long, reminderTime: Long) =
        taskDao.updateReminderTime(taskId, reminderTime)
}
