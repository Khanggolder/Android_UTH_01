package com.uth.taskmanagement.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TaskDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private var database: SupportSQLiteDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .deleteDatabase(TEST_DATABASE)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MIGRATION 1 → 2
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun migrate1To2_backfillsAndClampsStartDateTime() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            insertVersionOneTask(
                id = 1L,
                title = "Created before due date",
                createdAt = 100L,
                dueDateTime = 200L
            )
            insertVersionOneTask(
                id = 2L,
                title = "Created after due date",
                createdAt = 300L,
                dueDateTime = 200L
            )
            close()
        }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            TaskDatabase.MIGRATION_1_2
        )

        database!!.query(
            "SELECT id, startDateTime, dueDateTime FROM tasks ORDER BY id"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals(100L, cursor.getLong(1))
            assertEquals(200L, cursor.getLong(2))

            cursor.moveToNext()
            assertEquals(2L, cursor.getLong(0))
            assertEquals(200L, cursor.getLong(1))
            assertEquals(200L, cursor.getLong(2))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MIGRATION 2 → 3
    // ─────────────────────────────────────────────────────────────────────────

    /** 1. Task cũ vẫn tồn tại sau migration. */
    @Test
    fun migration2To3_tasksPreserved() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            insertVersionTwoTask(id = 1L, title = "Old Task")
            close()
        }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true,
            TaskDatabase.MIGRATION_2_3
        )

        database!!.query("SELECT COUNT(*) FROM tasks").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Task cũ phải còn sau migration", 1, cursor.getInt(0))
        }
    }

    /** 2. createdByUserId của task cũ = "local-user". */
    @Test
    fun migration2To3_createdByUserIdValid() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            insertVersionTwoTask(id = 1L, title = "Old Task")
            close()
        }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true,
            TaskDatabase.MIGRATION_2_3
        )

        database!!.query("SELECT createdByUserId FROM tasks WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("local-user", cursor.getString(0))
        }
    }

    /** 3. assigneeUserId của task cũ = "local-user". */
    @Test
    fun migration2To3_assigneeUserIdValid() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            insertVersionTwoTask(id = 1L, title = "Old Task")
            close()
        }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true,
            TaskDatabase.MIGRATION_2_3
        )

        database!!.query("SELECT assigneeUserId FROM tasks WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("local-user", cursor.getString(0))
        }
    }

    /** 4. Bảng users có row local-user sau migration. */
    @Test
    fun migration2To3_defaultUserExists() {
        helper.createDatabase(TEST_DATABASE, 2).apply { close() }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true,
            TaskDatabase.MIGRATION_2_3
        )

        database!!.query(
            "SELECT id, name FROM users WHERE id = 'local-user'"
        ).use { cursor ->
            assertTrue("Phải có row local-user trong bảng users", cursor.moveToFirst())
            assertEquals("local-user", cursor.getString(0))
            assertEquals("Me", cursor.getString(1))
        }
    }

    /** 5. Bảng task_attachments được tạo đúng cấu trúc. */
    @Test
    fun migration2To3_attachmentTableExists() {
        helper.createDatabase(TEST_DATABASE, 2).apply { close() }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true,
            TaskDatabase.MIGRATION_2_3
        )

        // INSERT không lỗi → bảng tồn tại với đúng schema
        database!!.execSQL(
            """
            INSERT INTO task_attachments (taskId, fileName, uri, mimeType, sizeBytes, createdAt)
            VALUES (-1, 'test.pdf', 'content://test', 'application/pdf', 1024, 1000)
            """.trimIndent()
        )

        database!!.query("SELECT COUNT(*) FROM task_attachments").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    /** 6. Foreign Key CASCADE: xóa task → attachment tự xóa theo. */
    @Test
    fun migration2To3_foreignKeyAndIndexWork() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            insertVersionTwoTask(id = 10L, title = "Task with attachment")
            close()
        }

        database = helper.runMigrationsAndValidate(
            TEST_DATABASE, 3, true,
            TaskDatabase.MIGRATION_2_3
        )

        // Bật foreign key enforcement (SQLite tắt mặc định)
        database!!.execSQL("PRAGMA foreign_keys = ON")

        // Insert attachment liên kết với task id=10
        database!!.execSQL(
            """
            INSERT INTO task_attachments (taskId, fileName, uri, mimeType, sizeBytes, createdAt)
            VALUES (10, 'file.png', 'content://img', 'image/png', 2048, 1000)
            """.trimIndent()
        )

        // Xóa task → CASCADE phải xóa attachment
        database!!.execSQL("DELETE FROM tasks WHERE id = 10")

        database!!.query("SELECT COUNT(*) FROM task_attachments WHERE taskId = 10").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Attachment phải bị xóa CASCADE khi Task bị xóa", 0, cursor.getInt(0))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun SupportSQLiteDatabase.insertVersionOneTask(
        id: Long,
        title: String,
        createdAt: Long,
        dueDateTime: Long
    ) {
        execSQL(
            """
            INSERT INTO tasks (
                id, title, description, dueDateTime, priority, status,
                isCompleted, reminderTime, recurrenceType, createdAt, updatedAt
            ) VALUES (?, ?, '', ?, 'MEDIUM', 'PENDING', 0, NULL, 'NONE', ?, ?)
            """.trimIndent(),
            arrayOf<Any>(id, title, dueDateTime, createdAt, createdAt)
        )
    }

    /** Helper cho DB v2: có cột startDateTime nhưng chưa có user fields. */
    private fun SupportSQLiteDatabase.insertVersionTwoTask(
        id: Long,
        title: String,
        createdAt: Long = 1000L,
        dueDateTime: Long = 9999999999L
    ) {
        execSQL(
            """
            INSERT INTO tasks (
                id, title, description, startDateTime, dueDateTime,
                priority, status, isCompleted, reminderTime,
                recurrenceType, createdAt, updatedAt
            ) VALUES (?, ?, '', ?, ?, 'MEDIUM', 'PENDING', 0, NULL, 'NONE', ?, ?)
            """.trimIndent(),
            arrayOf<Any>(id, title, createdAt, dueDateTime, createdAt, createdAt)
        )
    }

    private companion object {
        const val TEST_DATABASE = "task-migration-test"
    }
}

