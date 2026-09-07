package com.uth.taskmanagement.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserTaskRelationshipTest {

    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var userDao: UserDao

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
        userDao = database.userDao()
        database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")

        runBlocking {
            userDao.insert(UserEntity.createDefault())
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTask_withExistingOwner_keepsOwnerReference() = runBlocking {
        userDao.insert(UserEntity(id = "owner-a", name = "Owner A"))

        val taskId = taskDao.insertTask(
            taskWithOwner("owner-a")
        )

        val savedTask = taskDao.getTaskById(taskId)

        assertNotNull(savedTask)
        assertEquals("owner-a", savedTask?.assigneeUserId)
    }

    @Test
    fun updateUserId_cascadesToTaskOwner() = runBlocking {
        userDao.insert(UserEntity(id = "owner-a", name = "Owner A"))
        val taskId = taskDao.insertTask(
            taskWithOwner("owner-a")
        )

        database.openHelper.writableDatabase.execSQL(
            "UPDATE users SET id = 'owner-b' WHERE id = 'owner-a'"
        )

        val savedTask = taskDao.getTaskById(taskId)

        assertEquals("owner-b", savedTask?.assigneeUserId)
    }

    @Test
    fun insertTask_withMissingOwner_fails() = runBlocking {
        val error = try {
            taskDao.insertTask(taskWithOwner("missing-owner"))
            fail("Task insert should fail when owner does not exist.")
            null
        } catch (error: Exception) {
            error
        }

        assertNotNull(error)
        assertTrue(error.isForeignKeyFailure())
    }

    @Test
    fun deleteReferencedUser_isRestricted() = runBlocking {
        userDao.insert(UserEntity(id = "owner-a", name = "Owner A"))
        taskDao.insertTask(taskWithOwner("owner-a"))

        val error = try {
            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM users WHERE id = 'owner-a'"
            )
            fail("User delete should fail while tasks still reference that user.")
            null
        } catch (error: Exception) {
            error
        }

        assertNotNull(error)
        assertTrue(error.isForeignKeyFailure())
    }

    private fun taskWithOwner(ownerId: String): TaskEntity {
        return TaskEntity(
            title = "Owner task",
            description = "Description",
            startDateTime = 1000L,
            dueDateTime = 2000L,
            createdByUserId = UserEntity.DEFAULT_USER_ID,
            assigneeUserId = ownerId
        )
    }

    private fun Throwable?.isForeignKeyFailure(): Boolean {
        if (this == null) {
            return false
        }
        if (this is SQLiteConstraintException) {
            return true
        }
        return message.orEmpty().contains("FOREIGN KEY", ignoreCase = true) ||
            cause.isForeignKeyFailure()
    }
}
