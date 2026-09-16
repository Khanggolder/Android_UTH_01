package com.uth.taskmanagement.ui.taskform

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uth.taskmanagement.attachment.AttachmentStorage
import com.uth.taskmanagement.attachment.AttachmentFileHelper
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.UserEntity
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
        taskRepository = TaskRepository(database.taskDao(), attachmentRepository, database)
        runBlocking {
            database.userDao().insert(UserEntity.createDefault())
        }
        testDirectory = File(
            context.filesDir,
            "attachments/task-form-test-${UUID.randomUUID()}"
        ).apply {
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
        files.forEach { viewModel.addAttachment(uriFor(it)) }

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
        viewModel.addAttachment(uriFor(source))
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

    @Test
    fun editAddAttachmentWithoutSave_doesNotChangeDatabase() = runBlocking {
        val taskId = insertTask("Cancel add")
        val viewModel = loadTask(taskId)
        val source = File(testDirectory, "pending-add.txt").apply { writeText("pending") }

        viewModel.addAttachment(uriFor(source))
        withTimeout(5_000) {
            viewModel.formState.first { it.pendingAttachmentUris.size == 1 || it.errorMessage != null }
        }

        assertTrue(attachmentRepository.getAttachments(taskId).isEmpty())
    }

    @Test
    fun editRemoveAttachmentWithoutSave_doesNotChangeDatabase() = runBlocking {
        val taskId = insertTask("Cancel remove")
        val attachmentId = insertAttachment(taskId, "existing.txt", "existing")
        val viewModel = loadTask(taskId, expectedAttachmentCount = 1)
        val attachment = viewModel.formState.value.attachments.single()

        viewModel.removeAttachment(attachment)

        assertEquals(attachmentId, attachmentRepository.getAttachments(taskId).single().id)
        assertEquals(setOf(attachmentId), viewModel.formState.value.pendingDeleteAttachmentIds)
    }

    @Test
    fun editAddAndRemoveThenSave_commitsBothChanges() = runBlocking {
        val taskId = insertTask("Commit changes")
        insertAttachment(taskId, "old.txt", "old")
        val viewModel = loadTask(taskId, expectedAttachmentCount = 1)
        viewModel.removeAttachment(viewModel.formState.value.attachments.single())
        val newFile = File(testDirectory, "new.txt").apply { writeText("new") }
        viewModel.addAttachment(uriFor(newFile))
        withTimeout(5_000) {
            viewModel.formState.first { it.pendingAttachmentUris.size == 1 || it.errorMessage != null }
        }

        viewModel.saveTask()
        val savedState = withTimeout(5_000) {
            viewModel.formState.first { it.isSaved || it.errorMessage != null }
        }

        assertTrue(savedState.errorMessage, savedState.isSaved)
        assertEquals(listOf("new.txt"), attachmentRepository.getAttachments(taskId).map { it.fileName })
    }

    @Test
    fun editSaveFailure_keepsOriginalTaskAndAttachment() = runBlocking {
        val taskId = insertTask("Original title")
        val originalAttachmentId = insertAttachment(taskId, "keep.txt", "keep")
        val viewModel = loadTask(taskId, expectedAttachmentCount = 1)
        viewModel.setTitle("Changed title")
        viewModel.removeAttachment(viewModel.formState.value.attachments.single())
        val missingFile = File(testDirectory, "missing.txt").apply { writeText("temporary") }
        viewModel.addAttachment(uriFor(missingFile))
        withTimeout(5_000) {
            viewModel.formState.first { it.pendingAttachmentUris.size == 1 || it.errorMessage != null }
        }
        assertTrue(missingFile.delete())

        viewModel.saveTask()
        val failedState = withTimeout(5_000) {
            viewModel.formState.first { !it.isLoading && it.errorMessage != null }
        }

        assertTrue(failedState.errorMessage.orEmpty().contains("Failed to save task"))
        assertEquals("Original title", taskRepository.getTaskById(taskId)?.title)
        assertEquals(
            originalAttachmentId,
            attachmentRepository.getAttachments(taskId).single().id
        )
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

    private suspend fun insertTask(title: String): Long {
        val now = System.currentTimeMillis()
        return taskRepository.insertTask(
            TaskEntity(
                title = title,
                description = "Description",
                startDateTime = now + 60_000,
                dueDateTime = now + 120_000
            )
        )
    }

    private suspend fun insertAttachment(taskId: Long, name: String, contents: String): Long {
        val file = File(testDirectory, name).apply { writeText(contents) }
        return attachmentRepository.addAttachment(
            AttachmentFileHelper.buildAttachmentEntity(context, uriFor(file), taskId)
        )
    }

    private suspend fun loadTask(
        taskId: Long,
        expectedAttachmentCount: Int = 0
    ): TaskFormViewModel {
        val viewModel = createViewModel()
        viewModel.loadTask(taskId)
        withTimeout(5_000) {
            viewModel.formState.first {
                it.taskId == taskId && it.attachments.size == expectedAttachmentCount
            }
        }
        return viewModel
    }

    private fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
