package com.uth.taskmanagement.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDeleteTest {

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
    fun deleteTask_removesOnlySelectedTask() = runBlocking {
        val currentTime = System.currentTimeMillis()

        val task1 = TaskEntity(
            title = "Task cần xóa",
            description = "Task thứ nhất",
            dueDateTime = currentTime + 86_400_000L,
            priority = TaskPriority.HIGH,
            status = TaskStatus.PENDING,
            isCompleted = false,
            reminderTime = null,
            recurrenceType = RecurrenceType.NONE
        )

        val task2 = TaskEntity(
            title = "Task được giữ lại",
            description = "Task thứ hai",
            dueDateTime = currentTime + 172_800_000L,
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.PENDING,
            isCompleted = false,
            reminderTime = null,
            recurrenceType = RecurrenceType.NONE
        )

        val firstId = taskDao.insertTask(task1)
        val secondId = taskDao.insertTask(task2)

        // Xóa task thứ nhất
        taskDao.deleteTaskById(firstId)

        // Truy vấn lại từng task
        val deletedTask = taskDao.getTaskById(firstId)
        val remainingTask = taskDao.getTaskById(secondId)

        // Truy vấn lại toàn bộ danh sách
        val allTasks = taskDao.observeAllTasks().first()

        // Task thứ nhất không còn tồn tại
        assertNull(deletedTask)

        // Task thứ hai vẫn còn nguyên
        assertNotNull(remainingTask)
        assertEquals(secondId, remainingTask?.id)
        assertEquals("Task được giữ lại", remainingTask?.title)

        // Danh sách chỉ còn đúng task thứ hai
        assertEquals(1, allTasks.size)
        assertEquals(secondId, allTasks.first().id)
    }
}