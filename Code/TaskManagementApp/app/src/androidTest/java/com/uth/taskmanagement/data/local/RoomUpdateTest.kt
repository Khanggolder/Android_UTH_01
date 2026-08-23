package com.uth.taskmanagement.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomUpdateTest {

    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

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
    fun updateTask_keepsIdAndDoesNotChangeOtherTask() = runBlocking {
        val currentTime = System.currentTimeMillis()

        val task1 = TaskEntity(
            title = "Task thứ nhất",
            description = "Dữ liệu trước update",
            dueDateTime = currentTime + 86_400_000L,
            priority = TaskPriority.HIGH,
            status = TaskStatus.PENDING,
            isCompleted = false,
            reminderTime = currentTime + 3_600_000L,
            recurrenceType = RecurrenceType.NONE
        )

        val task2 = TaskEntity(
            title = "Task thứ hai",
            description = "Task không được thay đổi",
            dueDateTime = currentTime + 172_800_000L,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.PENDING,
            isCompleted = false,
            reminderTime = null,
            recurrenceType = RecurrenceType.NONE
        )

        val firstId = taskDao.insertTask(task1)
        val secondId = taskDao.insertTask(task2)

        val firstBeforeUpdate = taskDao.getTaskById(firstId)
        val secondBeforeUpdate = taskDao.getTaskById(secondId)

        assertNotNull(firstBeforeUpdate)
        assertNotNull(secondBeforeUpdate)

        val updatedTask = firstBeforeUpdate!!.copy(
            title = "Task đã cập nhật",
            description = "Dữ liệu sau update",
            priority = TaskPriority.LOW,
            status = TaskStatus.COMPLETED,
            isCompleted = true,
            updatedAt = firstBeforeUpdate.updatedAt + 1_000L
        )

        taskDao.updateTask(updatedTask)

        val firstAfterUpdate = taskDao.getTaskById(firstId)
        val secondAfterUpdate = taskDao.getTaskById(secondId)

        assertNotNull(firstAfterUpdate)
        assertNotNull(secondAfterUpdate)

        // Task thứ nhất giữ nguyên ID
        assertEquals(firstId, firstAfterUpdate?.id)

        // Task thứ nhất được cập nhật đúng
        assertEquals("Task đã cập nhật", firstAfterUpdate?.title)
        assertEquals(
            "Dữ liệu sau update",
            firstAfterUpdate?.description
        )
        assertEquals(TaskPriority.LOW, firstAfterUpdate?.priority)
        assertEquals(TaskStatus.COMPLETED, firstAfterUpdate?.status)
        assertEquals(true, firstAfterUpdate?.isCompleted)

        // updatedAt phải thay đổi
        assertTrue(
            firstAfterUpdate!!.updatedAt >
                firstBeforeUpdate.updatedAt
        )

        // Task thứ hai không bị thay đổi
        assertEquals(secondBeforeUpdate, secondAfterUpdate)
    }
}