package com.uth.taskmanagement.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            TaskDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveTask() = runBlocking {
        val taskId = taskDao.insertTask(
            TaskEntity(
                title = "Test Room",
                description = "Insert and observe",
                dueDateTime = System.currentTimeMillis() + 60_000
            )
        )

        val tasks = taskDao.observeAllTasks().first()

        assertEquals(1, tasks.size)
        assertEquals(taskId, tasks.first().id)
        assertEquals("Test Room", tasks.first().title)
    }

    @Test
    fun updateTask() = runBlocking {
        val taskId = taskDao.insertTask(
            TaskEntity(
                title = "Old title",
                dueDateTime = System.currentTimeMillis() + 60_000
            )
        )

        val oldTask = taskDao.getTaskById(taskId)
        assertNotNull(oldTask)

        taskDao.updateTask(
            oldTask!!.copy(title = "New title")
        )

        val updatedTask = taskDao.getTaskById(taskId)

        assertEquals("New title", updatedTask?.title)
    }

    @Test
    fun completeTask() = runBlocking {
        val taskId = taskDao.insertTask(
            TaskEntity(
                title = "Complete test",
                dueDateTime = System.currentTimeMillis() + 60_000
            )
        )

        taskDao.updateCompletedState(
            taskId = taskId,
            isCompleted = true,
            status = TaskStatus.COMPLETED
        )

        val task = taskDao.getTaskById(taskId)

        assertTrue(task?.isCompleted == true)
        assertEquals(TaskStatus.COMPLETED, task?.status)
    }

    @Test
    fun deleteTask() = runBlocking {
        val taskId = taskDao.insertTask(
            TaskEntity(
                title = "Delete test",
                dueDateTime = System.currentTimeMillis() + 60_000
            )
        )

        taskDao.deleteTaskById(taskId)

        val task = taskDao.getTaskById(taskId)

        assertFalse(task != null)
    }
}