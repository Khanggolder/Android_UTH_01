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
class RoomInsertQueryTest {

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
    fun insertTask_thenQuery_returnsCorrectData() = runBlocking {
        val currentTime = System.currentTimeMillis()
        val dueTime = currentTime + 86_400_000L
        val reminderTime = currentTime + 3_600_000L

        val inputTask = TaskEntity(
            title = "Kiểm tra Room",
            description = "Test insert và query",
            dueDateTime = dueTime,
            priority = TaskPriority.HIGH,
            status = TaskStatus.PENDING,
            isCompleted = false,
            reminderTime = reminderTime,
            recurrenceType = RecurrenceType.WEEKLY
        )

        val insertedId = taskDao.insertTask(inputTask)

        assertTrue(insertedId > 0)

        val savedTask = taskDao.getTaskById(insertedId)

        assertNotNull(savedTask)
        assertEquals("Kiểm tra Room", savedTask?.title)
        assertEquals("Test insert và query", savedTask?.description)
        assertEquals(dueTime, savedTask?.dueDateTime)
        assertEquals(TaskPriority.HIGH, savedTask?.priority)
        assertEquals(TaskStatus.PENDING, savedTask?.status)
        assertEquals(false, savedTask?.isCompleted)
        assertEquals(reminderTime, savedTask?.reminderTime)
        assertEquals(
            RecurrenceType.WEEKLY,
            savedTask?.recurrenceType
        )
    }
}