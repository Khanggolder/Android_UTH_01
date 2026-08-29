package com.uth.taskmanagement.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
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

    private companion object {
        const val TEST_DATABASE = "task-migration-test"
    }
}
