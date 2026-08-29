package com.uth.taskmanagement

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TC-39 - Database integrity
 * Test tự động: tạo 20 task, update 10 task, xóa 5 task, kiểm tra dữ liệu
 * còn lại chính xác - không ID trùng, task update đúng, task xóa biến mất,
 * task khác không bị ảnh hưởng.
 *
 * Dùng in-memory Database (không đụng vào dữ liệu thật trên máy), tự hủy
 * sau khi test xong.
 */
@RunWith(AndroidJUnit4::class)
class TaskRepositoryIntegrityTest {

    private lateinit var db: TaskDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepository(db.taskDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testDatabaseIntegrityAfterInsertUpdateDelete() = runBlocking {
        // Bước 1: Tạo 20 task
        val insertedIds = mutableListOf<Long>()
        for (i in 1..20) {
            val task = TaskEntity(
                title = "Task $i",
                description = "Description $i",
                dueDateTime = System.currentTimeMillis() + i * 60000L,
                priority = TaskPriority.MEDIUM
            )
            val id = repository.insertTask(task)
            insertedIds.add(id)
        }
        assertEquals("Phải tạo đúng 20 task", 20, insertedIds.size)

        // Bước 2: Update 10 task đầu tiên - đổi title để phân biệt
        val idsToUpdate = insertedIds.take(10)
        for (id in idsToUpdate) {
            val task = repository.getTaskById(id)
            requireNotNull(task) { "Task $id phải tồn tại trước khi update" }
            repository.updateTask(task.copy(title = "Updated Task $id", priority = TaskPriority.HIGH))
        }

        // Bước 3: Xóa 5 task cuối cùng (5 task không nằm trong nhóm update)
        val idsToDelete = insertedIds.takeLast(5)
        for (id in idsToDelete) {
            repository.deleteTaskById(id)
        }

        // Bước 4: Query lại toàn bộ, kiểm tra
        val allTasks = repository.getAllTasks()

        // Còn đúng 15 task (20 - 5 xóa)
        assertEquals("Phải còn đúng 15 task sau khi xóa 5", 15, allTasks.size)

        // Không có ID nào trùng lặp
        val uniqueIds = allTasks.map { it.id }.toSet()
        assertEquals("Không được có ID trùng lặp", allTasks.size, uniqueIds.size)

        // 10 task đã update phải có title mới, priority mới
        for (id in idsToUpdate) {
            val updated = allTasks.find { it.id == id }
            requireNotNull(updated) { "Task $id đã update phải còn tồn tại" }
            assertEquals("Updated Task $id", updated.title)
            assertEquals(TaskPriority.HIGH, updated.priority)
        }

        // 5 task đã xóa không còn xuất hiện
        for (id in idsToDelete) {
            val deleted = allTasks.find { it.id == id }
            assertNull("Task $id phải đã bị xóa, không còn trong danh sách", deleted)
        }

        // 5 task còn lại (không update, không xóa) phải giữ nguyên dữ liệu gốc
        val untouchedIds = insertedIds.drop(10).dropLast(5)
        assertEquals("Phải có đúng 5 task không bị đụng tới", 5, untouchedIds.size)
        for (id in untouchedIds) {
            val task = allTasks.find { it.id == id }
            requireNotNull(task) { "Task $id không update/không xóa phải còn nguyên" }
            assertTrue(
                "Title của task $id không update phải giữ nguyên dạng 'Task X'",
                task.title.startsWith("Task ") && !task.title.startsWith("Updated")
            )
            assertEquals(TaskPriority.MEDIUM, task.priority)
        }
    }
}