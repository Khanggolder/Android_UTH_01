package com.uth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("SELECT * FROM tasks ORDER BY dueDateTime ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY dueDateTime ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun observeTaskById(taskId: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query(
        """
        UPDATE tasks
        SET isCompleted = :isCompleted,
            status = :status,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateCompletedState(
        taskId: Long,
        isCompleted: Boolean,
        status: TaskStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDateTime ASC")
    fun observeTasksByStatus(
        status: TaskStatus
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY dueDateTime ASC")
    fun observeTasksByPriority(
        priority: TaskPriority
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY priority DESC, dueDateTime ASC")
    fun observeTasksSortedByPriority(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY dueDateTime ASC")
    fun observeTasksSortedByDueDate(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDateTime < :currentTime
        AND isCompleted = 0
        ORDER BY dueDateTime ASC
        """
    )
    fun observeOverdueTasks(
        currentTime: Long = System.currentTimeMillis()
    ): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE reminderTime IS NOT NULL
        AND isCompleted = 0
        ORDER BY reminderTime ASC
        """
    )
    suspend fun getActiveReminderTasks(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDateTime BETWEEN :startTime AND :endTime
        ORDER BY dueDateTime ASC
        """
    )
    fun observeTasksBetweenDates(
        startTime: Long,
        endTime: Long
    ): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Transaction
    suspend fun replaceAllTasks(tasks: List<TaskEntity>) {
        deleteAllTasks()
        insertTasks(tasks)
    }

    @Query(
        """
        UPDATE tasks
        SET reminderTime = :reminderTime,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateReminderTime(
        taskId: Long,
        reminderTime: Long,
        updatedAt: Long = System.currentTimeMillis()
    )
}
