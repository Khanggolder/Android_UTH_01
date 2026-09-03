package com.uth.taskmanagement.ui.taskform

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uth.taskmanagement.attachment.AttachmentStorage
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.repository.AttachmentRepository
import com.uth.taskmanagement.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskFormAttachmentInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: TaskDatabase
    private lateinit var storage: AttachmentStorage
    private lateinit var attachmentRepository: AttachmentRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var testDirectory: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        storage = AttachmentStorage(context)
        attachmentRepository = AttachmentRepository(database.attachmentDao(), storage)
        taskRepository = TaskRepository(database.taskDao(), attachmentRepository)
        testDirectory = File(context.cacheDir, "task-form-test-${UUID.randomUUID()}").apply {
            assertTrue(mkdirs())
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            val deletion = attachmentRepository.stageOwnedFiles(
                attachmentRepository.getAllAttachments()
            )
            attachmentRepository.commitFileDeletion(deletion)
        }
        database.close()
        testDirectory.deleteRecursively()
    }

    @Test
    fun createThenEditTask_keepsAllThreeAttachments() = runBlocking {
        val viewModel = createViewModel()
        configureValidTask(viewModel, "Three attachments")
        val files = listOf("A", "B", "C").map { label ->
            File(testDirectory, "$label.txt").apply { writeText(label) }
        }
        files.forEach { viewModel.addAttachment(Uri.fromFile(it)) }

        val pendingState = withTimeout(5_000) {
            viewModel.formState.first { it.attachments.size == 3 || it.errorMessage != null }
        }
        assertEquals(null, pendingState.errorMessage)

        viewModel.saveTask()
        val savedState = withTimeout(5_000) {
            viewModel.formState.first { it.isSaved || it.errorMessage != null }
        }
        assertTrue(savedState.errorMessage, savedState.isSaved)

        val savedTask = taskRepository.getAllTasks().single()
        assertEquals(3, attachmentRepository.getAttachments(savedTask.id).size)

        val editViewModel = createViewModel()
        editViewModel.loadTask(savedTask.id)
        withTimeout(5_000) {
            editViewModel.formState.first { it.taskId == savedTask.id && it.attachments.size == 3 }
        }
        editViewModel.setTitle("Edited task")
        editViewModel.saveTask()
        val editedState = withTimeout(5_000) {
            editViewModel.formState.first { it.isSaved || it.errorMessage != null }
        }

        assertTrue(editedState.errorMessage, editedState.isSaved)
        assertEquals("Edited task", taskRepository.getTaskById(savedTask.id)?.title)
        assertEquals(3, attachmentRepository.getAttachments(savedTask.id).size)
    }

    @Test
    fun attachmentDisappearsDuringCreate_rollsBackInsertedTask() = runBlocking {
        val viewModel = createViewModel()
        configureValidTask(viewModel, "Rollback task")
        val source = File(testDirectory, "temporary.txt").apply { writeText("temporary") }
        viewModel.addAttachment(Uri.fromFile(source))
        withTimeout(5_000) {
            viewModel.formState.first { it.attachments.size == 1 || it.errorMessage != null }
        }
        assertTrue(source.delete())

        viewModel.saveTask()
        val failedState = withTimeout(5_000) {
            viewModel.formState.first { !it.isLoading && it.errorMessage != null }
        }

        assertTrue(failedState.errorMessage.orEmpty().contains("Failed to save task"))
        assertTrue(taskRepository.getAllTasks().isEmpty())
        assertTrue(attachmentRepository.getAllAttachments().isEmpty())
    }

    private fun createViewModel(): TaskFormViewModel {
        return TaskFormViewModel(
            context.applicationContext as Application,
            taskRepository,
            attachmentRepository
        )
    }

    private fun configureValidTask(viewModel: TaskFormViewModel, title: String) {
        val now = System.currentTimeMillis()
        viewModel.setTitle(title)
        viewModel.setDescription("Description")
        viewModel.setStartDateTime(now + 60_000)
        viewModel.setDueDateTime(now + 120_000)
    }
}
