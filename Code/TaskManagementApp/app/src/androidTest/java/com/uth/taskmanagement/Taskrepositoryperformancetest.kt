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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * TC-40 - Room stress/performance
 * Insert 500 task giả, đo thời gian insert + query, kiểm tra không crash/ANR.
 * Ghi số liệu THẬT (thời gian, RAM nếu lấy được), không tự đặt ngưỡng Pass giả -
 * test này chỉ báo cáo con số, không tự assert "phải nhanh hơn X giây".
 */
@RunWith(AndroidJUnit4::class)
class TaskRepositoryPerformanceTest {

    private lateinit var db: TaskDatabase
    private lateinit var repository: TaskRepository
    private val TAG = "TC-40-Performance"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Dùng Room thật trên disk (không phải in-memory) để đo hiệu năng
        // sát với thực tế sử dụng của app, không dùng DB có sẵn (tránh lẫn dữ liệu cũ)
        db = Room.databaseBuilder(context, TaskDatabase::class.java, "perf_test_db")
            .fallbackToDestructiveMigration(true)
            .build()
        repository = TaskRepository(db.taskDao())
    }

    @After
    fun teardown() {
        db.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("perf_test_db")
    }

    private fun getMemoryUsageMb(): Long {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024 * 1024)
    }

    @Test
    fun testInsertAndQuery500Tasks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Log.i(TAG, "Device model: ${android.os.Build.MODEL}")
        Log.i(TAG, "Android version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")

        // Trạng thái trước khi test
        val tasksBefore = repository.getAllTasks().size
        val ramBefore = getMemoryUsageMb()
        Log.i(TAG, "BEFORE: tasks=$tasksBefore, RAM=${ramBefore}MB")

        // Insert 500 task, đo thời gian
        val insertStart = System.currentTimeMillis()
        for (i in 1..500) {
            val task = TaskEntity(
                title = "Stress Task $i",
                description = "Generated for performance test",
                dueDateTime = System.currentTimeMillis() + i * 60000L,
                priority = TaskPriority.entries[i % 3]
            )
            repository.insertTask(task)
        }
        val insertEnd = System.currentTimeMillis()
        val insertDurationMs = insertEnd - insertStart

        // Query lại toàn bộ, đo thời gian
        val queryStart = System.currentTimeMillis()
        val allTasks = repository.getAllTasks()
        val queryEnd = System.currentTimeMillis()
        val queryDurationMs = queryEnd - queryStart

        val ramAfter = getMemoryUsageMb()

        Log.i(TAG, "AFTER: tasks=${allTasks.size}")
        Log.i(TAG, "Insert time: ${insertDurationMs}ms (${insertDurationMs / 1000.0}s)")
        Log.i(TAG, "Query time: ${queryDurationMs}ms (${queryDurationMs / 1000.0}s)")
        Log.i(TAG, "RAM before: ${ramBefore}MB, RAM after: ${ramAfter}MB")
        Log.i(TAG, "App crash: No (test completed)")

        // Chỉ assert tính đúng đắn dữ liệu (không assert về tốc độ / ngưỡng thời gian giả định)
        assertEquals("Phải insert đủ 500 task", tasksBefore + 500, allTasks.size)
    }
}