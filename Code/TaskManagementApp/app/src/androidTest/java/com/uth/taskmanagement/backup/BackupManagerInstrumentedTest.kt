package com.uth.taskmanagement.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uth.taskmanagement.attachment.AttachmentStorage
import com.uth.taskmanagement.attachment.PendingAttachmentManager
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.data.repository.AttachmentRepository
import com.uth.taskmanagement.data.repository.TaskRepository
import com.uth.taskmanagement.recurrence.RecurrenceScheduler
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BackupManagerInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: TaskDatabase
    private lateinit var storage: AttachmentStorage
    private lateinit var attachmentRepository: AttachmentRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var scheduler: RecordingReminderScheduler
    private lateinit var backupManager: BackupManager
    private lateinit var testDirectory: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        storage = AttachmentStorage(context)
        attachmentRepository = AttachmentRepository(database.attachmentDao(), storage)
        taskRepository = TaskRepository(database.taskDao(), attachmentRepository)
        scheduler = RecordingReminderScheduler()
        backupManager = BackupManager(
            taskRepository,
            attachmentRepository,
            scheduler,
            database,
            storage,
            context
        )
        testDirectory = File(context.cacheDir, "backup-test-${UUID.randomUUID()}").apply {
            assertTrue(mkdirs())
        }
    }

    @After
    fun tearDown() {
        runBlocking {
        runCatching {
            val deletion = attachmentRepository.stageOwnedFiles(
                attachmentRepository.getAllAttachments()
            )
            attachmentRepository.commitFileDeletion(deletion)
        }
        database.close()
        testDirectory.deleteRecursively()
        }
    }

    @Test
    fun exportAndRestoreZip_preservesTaskAndTwoAttachmentFiles() = runBlocking {
        val taskId = insertTask("Portable task")
        val pdfBytes = "pdf-content".toByteArray()
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        addExternalAttachment(taskId, createFile("report.pdf", pdfBytes), "application/pdf")
        addExternalAttachment(taskId, createFile("photo.jpg", imageBytes), "image/jpeg")

        val backupFile = File(testDirectory, "backup.zip")
        backupManager.exportTasks(Uri.fromFile(backupFile)).getOrThrow()

        ZipFile(backupFile).use { zip ->
            assertTrue(zip.getEntry("backup.json") != null)
            val attachmentEntries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("attachments/") }
                .toList()
            assertEquals(2, attachmentEntries.size)
        }

        database.replaceBackupData(emptyList(), emptyList())
        backupManager.restoreTasks(Uri.fromFile(backupFile)).getOrThrow()

        val restoredTask = taskRepository.getAllTasks().single()
        val restoredAttachments = attachmentRepository.getAttachments(restoredTask.id)
        assertEquals("Portable task", restoredTask.title)
        assertEquals(2, restoredAttachments.size)
        assertTrue(restoredAttachments.all { it.isAppOwned })
        val storageAfterRestart = AttachmentStorage(context)
        assertArrayEquals(
            pdfBytes,
            storageAfterRestart.openInputStream(restoredAttachments.first { it.fileName == "report.pdf" })
                .use { it.readBytes() }
        )
        assertArrayEquals(
            imageBytes,
            storage.openInputStream(restoredAttachments.first { it.fileName == "photo.jpg" })
                .use { it.readBytes() }
        )
        val restoredPdf = restoredAttachments.first { it.fileName == "report.pdf" }
        val providerUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            storageAfterRestart.resolveOwnedFile(restoredPdf)
        )
        assertEquals("content", providerUri.scheme)
    }

    @Test
    fun exportJsonVersionTwo_createsValidJsonAndRestoresTaskData() = runBlocking {
        val taskId = insertTask("JSON task")
        val source = createFile("json-metadata.txt", "metadata-source".toByteArray())
        addExternalAttachment(taskId, source, "text/plain")

        val backupFile = File(testDirectory, "task-data.json")
        backupManager.exportTaskData(Uri.fromFile(backupFile)).getOrThrow()

        val root = JSONObject(backupFile.readText())
        assertEquals(BackupManager.JSON_BACKUP_VERSION, root.getInt("version"))
        val exportedTask = root.getJSONArray("tasks").getJSONObject(0)
        val exportedAttachment = exportedTask.getJSONArray("attachments").getJSONObject(0)
        assertEquals("JSON task", exportedTask.getString("title"))
        assertEquals("json-metadata.txt", exportedAttachment.getString("fileName"))
        assertTrue(exportedAttachment.has("uri"))
        assertFalse(exportedAttachment.has("archivePath"))

        database.replaceBackupData(emptyList(), emptyList())
        backupManager.restoreTasks(Uri.fromFile(backupFile)).getOrThrow()

        assertEquals("JSON task", taskRepository.getAllTasks().single().title)
        assertEquals("json-metadata.txt", attachmentRepository.getAllAttachments().single().fileName)
    }

    @Test
    fun portableExport_rejectsAttachmentLargerThanSingleEntryLimit() = runBlocking {
        val taskId = insertTask("Oversized attachment")
        addExternalAttachment(
            taskId = taskId,
            file = createSparseFile(
                "oversized.bin",
                BackupArchiveSafety.MAX_SINGLE_ENTRY_BYTES + 1
            ),
            mimeType = "application/octet-stream"
        )

        val result = backupManager.exportTasks(
            Uri.fromFile(File(testDirectory, "oversized.zip"))
        )

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message.orEmpty()
                .contains("Attachment 'oversized.bin' is too large for backup")
        )
    }

    @Test
    fun portableExport_rejectsTotalAttachmentSizeAboveLimit() = runBlocking {
        val taskId = insertTask("Oversized total")
        repeat(3) { index ->
            addExternalAttachment(
                taskId = taskId,
                file = createSparseFile("part-$index.bin", 200L * 1024 * 1024),
                mimeType = "application/octet-stream"
            )
        }

        val result = backupManager.exportTasks(
            Uri.fromFile(File(testDirectory, "oversized-total.zip"))
        )

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message.orEmpty()
                .contains("Total attachment size exceeds backup limit")
        )
    }

    @Test
    fun invalidTaskDateRange_isRejectedWithoutReplacingCurrentDatabase() = runBlocking {
        insertTask("Keep existing task")
        val invalidTask = taskJson(70L, "Invalid dates").apply {
            put("startDateTime", 3000L)
            put("dueDateTime", 2000L)
        }
        val backupFile = File(testDirectory, "invalid-dates.json")
        backupFile.writeText(
            JSONObject()
                .put("version", BackupManager.JSON_BACKUP_VERSION)
                .put("tasks", JSONArray().put(invalidTask))
                .toString()
        )

        val result = backupManager.restoreTasks(Uri.fromFile(backupFile))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("startDateTime"))
        assertEquals("Keep existing task", taskRepository.getAllTasks().single().title)
    }

    @Test
    fun duplicateFileNames_exportAndRestoreWithoutOverwrite() = runBlocking {
        val firstTaskId = insertTask("First")
        val secondTaskId = insertTask("Second")
        addExternalAttachment(
            firstTaskId,
            createFile("first/report.pdf", "first".toByteArray()),
            "application/pdf"
        )
        addExternalAttachment(
            secondTaskId,
            createFile("second/report.pdf", "second".toByteArray()),
            "application/pdf"
        )

        val backupFile = File(testDirectory, "same-name.zip")
        backupManager.exportTasks(Uri.fromFile(backupFile)).getOrThrow()
        ZipFile(backupFile).use { zip ->
            val names = zip.entries().asSequence()
                .filter { it.name.startsWith("attachments/") && !it.isDirectory }
                .map { it.name }
                .toList()
            assertEquals(2, names.distinct().size)
        }

        database.replaceBackupData(emptyList(), emptyList())
        backupManager.restoreTasks(Uri.fromFile(backupFile)).getOrThrow()
        val restored = attachmentRepository.getAllAttachments()
        assertEquals(2, restored.size)
        assertEquals(
            setOf("first", "second"),
            restored.map { storage.openInputStream(it).use { stream -> stream.readBytes().decodeToString() } }
                .toSet()
        )
    }

    @Test
    fun missingExternalAttachment_failsExportWithClearMessage() = runBlocking {
        val taskId = insertTask("Missing file")
        val missing = createFile("missing.pdf", "temporary".toByteArray())
        addExternalAttachment(taskId, missing, "application/pdf")
        assertTrue(missing.delete())

        val result = backupManager.exportTasks(Uri.fromFile(File(testDirectory, "missing.zip")))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("no longer available"))
    }

    @Test
    fun malformedZip_doesNotReplaceCurrentDatabase() = runBlocking {
        insertTask("Keep me")
        val malformed = File(testDirectory, "malformed.zip")
        writeZip(malformed, mapOf("backup.json" to "{not-json".toByteArray()))

        val result = backupManager.restoreTasks(Uri.fromFile(malformed))

        assertTrue(result.isFailure)
        assertEquals("Keep me", taskRepository.getAllTasks().single().title)
    }

    @Test
    fun zipSlipEntry_isRejectedAndDoesNotReplaceCurrentDatabase() = runBlocking {
        insertTask("Keep me")
        val malicious = File(testDirectory, "zip-slip.zip")
        writeZip(
            malicious,
            linkedMapOf(
                "backup.json" to versionThreeManifest(JSONArray()).toByteArray(),
                "../../bad.txt" to "bad".toByteArray()
            )
        )

        val result = backupManager.restoreTasks(Uri.fromFile(malicious))

        assertTrue(result.isFailure)
        assertEquals("Keep me", taskRepository.getAllTasks().single().title)
        assertFalse(File(testDirectory.parentFile, "bad.txt").exists())
    }

    @Test
    fun legacyJsonArray_restoresTask() = runBlocking {
        val legacyFile = File(testDirectory, "legacy.json")
        legacyFile.writeText(JSONArray().put(taskJson(50L, "Legacy array")).toString())

        backupManager.restoreTasks(Uri.fromFile(legacyFile)).getOrThrow()

        assertEquals("Legacy array", taskRepository.getAllTasks().single().title)
    }

    @Test
    fun versionTwoJson_preservesUsableExternalUriSemantics() = runBlocking {
        val source = createFile("legacy.pdf", "legacy-file".toByteArray())
        val task = taskJson(60L, "Version two").apply {
            put(
                "attachments",
                JSONArray().put(
                    JSONObject().apply {
                        put("fileName", "legacy.pdf")
                        put("uri", Uri.fromFile(source).toString())
                        put("mimeType", "application/pdf")
                        put("sizeBytes", source.length())
                        put("createdAt", 1000L)
                    }
                )
            )
        }
        val backupFile = File(testDirectory, "version-two.json")
        backupFile.writeText(
            JSONObject().put("version", 2).put("tasks", JSONArray().put(task)).toString()
        )

        backupManager.restoreTasks(Uri.fromFile(backupFile)).getOrThrow()

        val attachment = attachmentRepository.getAllAttachments().single()
        assertFalse(attachment.isAppOwned)
        assertEquals(Uri.fromFile(source).toString(), attachment.uri)
        assertArrayEquals("legacy-file".toByteArray(), storage.openInputStream(attachment).use { it.readBytes() })
    }

    @Test
    fun deletingExternalAttachment_keepsOriginalFile() = runBlocking {
        val taskId = insertTask("External")
        val source = createFile("external.txt", "owned-by-user".toByteArray())
        val attachmentId = addExternalAttachment(taskId, source, "text/plain")

        attachmentRepository.deleteAttachment(attachmentId)

        assertTrue(source.exists())
        assertTrue(attachmentRepository.getAttachments(taskId).isEmpty())
    }

    @Test
    fun deletingRestoredAttachment_removesDatabaseRowAndPrivateFile() = runBlocking {
        val taskId = insertTask("Owned")
        val stored = storage.copyIntoPrivateStorage(
            "owned.txt",
            "app-owned".byteInputStream()
        )
        val attachmentId = attachmentRepository.addAttachment(
            TaskAttachmentEntity(
                taskId = taskId,
                fileName = "owned.txt",
                uri = "",
                mimeType = "text/plain",
                sizeBytes = stored.sizeBytes,
                isAppOwned = true,
                localRelativePath = stored.relativePath
            )
        )
        val attachment = attachmentRepository.getAttachmentById(attachmentId)!!
        val internalFile = storage.resolveOwnedFile(attachment)

        attachmentRepository.deleteAttachment(attachmentId)

        assertFalse(internalFile.exists())
        assertTrue(attachmentRepository.getAttachments(taskId).isEmpty())
    }

    @Test
    fun deletingTask_removesRestoredPhysicalFiles() = runBlocking {
        val taskId = insertTask("Owned task")
        val stored = storage.copyIntoPrivateStorage(
            "owned.txt",
            "app-owned".byteInputStream()
        )
        val attachmentId = attachmentRepository.addAttachment(
            TaskAttachmentEntity(
                taskId = taskId,
                fileName = "owned.txt",
                uri = "",
                isAppOwned = true,
                localRelativePath = stored.relativePath
            )
        )
        val internalFile = storage.resolveOwnedFile(
            attachmentRepository.getAttachmentById(attachmentId)!!
        )

        taskRepository.deleteTaskById(taskId)

        assertFalse(internalFile.exists())
        assertTrue(taskRepository.getAllTasks().isEmpty())
    }

    @Test
    fun pendingAttachmentManager_commitsThreeFilesAndHonorsRemovedMiddleFile() = runBlocking {
        val firstTaskId = insertTask("Three files")
        val files = listOf(
            createFile("pending-a.txt", "A".toByteArray()),
            createFile("pending-b.txt", "B".toByteArray()),
            createFile("pending-c.txt", "C".toByteArray())
        )
        val manager = PendingAttachmentManager(attachmentRepository)
        manager.commitPendingAttachments(
            context,
            files.map(Uri::fromFile),
            firstTaskId
        )
        assertEquals(3, attachmentRepository.getAttachments(firstTaskId).size)

        val secondTaskId = insertTask("Middle removed")
        manager.commitPendingAttachments(
            context,
            listOf(Uri.fromFile(files[0]), Uri.fromFile(files[2])),
            secondTaskId
        )
        val savedUris = attachmentRepository.getAttachments(secondTaskId).map { it.uri }.toSet()
        assertEquals(setOf(Uri.fromFile(files[0]).toString(), Uri.fromFile(files[2]).toString()), savedUris)
    }

    @Test
    fun recurringPastReminder_updatesDatabaseToScheduledFutureTime() = runBlocking {
        val now = System.currentTimeMillis()
        val taskId = taskRepository.insertTask(
            TaskEntity(
                title = "Recurring reminder",
                description = "Description",
                startDateTime = now - 3 * DAY_MILLIS,
                dueDateTime = now + DAY_MILLIS,
                reminderTime = now - 2 * DAY_MILLIS,
                recurrenceType = RecurrenceType.DAILY
            )
        )
        val task = taskRepository.getTaskById(taskId)!!

        try {
            ReminderSchedulerImpl(context, taskRepository).scheduleAll(listOf(task))
            val updated = taskRepository.getTaskById(taskId)!!
            assertTrue(updated.reminderTime!! > now)
            assertTrue(updated.reminderTime != task.reminderTime)
        } finally {
            RecurrenceScheduler.cancelAlarm(context, taskId)
        }
    }

    private suspend fun insertTask(title: String): Long {
        return taskRepository.insertTask(
            TaskEntity(
                title = title,
                description = "Description",
                startDateTime = 1000L,
                dueDateTime = 2000L,
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.PENDING,
                recurrenceType = RecurrenceType.NONE
            )
        )
    }

    private suspend fun addExternalAttachment(
        taskId: Long,
        file: File,
        mimeType: String
    ): Long {
        return attachmentRepository.addAttachment(
            TaskAttachmentEntity(
                taskId = taskId,
                fileName = file.name,
                uri = Uri.fromFile(file).toString(),
                mimeType = mimeType,
                sizeBytes = file.length()
            )
        )
    }

    private fun createFile(relativePath: String, bytes: ByteArray): File {
        return File(testDirectory, relativePath).apply {
            parentFile?.mkdirs()
            writeBytes(bytes)
        }
    }

    private fun createSparseFile(relativePath: String, sizeBytes: Long): File {
        return File(testDirectory, relativePath).apply {
            parentFile?.mkdirs()
            RandomAccessFile(this, "rw").use { file -> file.setLength(sizeBytes) }
        }
    }

    private fun taskJson(id: Long, title: String): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("description", "Description")
            put("startDateTime", 1000L)
            put("dueDateTime", 2000L)
            put("priority", "MEDIUM")
            put("status", "PENDING")
            put("isCompleted", false)
            put("reminderTime", JSONObject.NULL)
            put("recurrenceType", "NONE")
            put("createdAt", 1000L)
            put("updatedAt", 1000L)
        }
    }

    private fun versionThreeManifest(tasks: JSONArray): String {
        return JSONObject().put("version", 3).put("tasks", tasks).toString()
    }

    private fun writeZip(file: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private class RecordingReminderScheduler : ReminderScheduler {
        val cancelledTaskIds = mutableListOf<Long>()
        val scheduledTasks = mutableListOf<TaskEntity>()

        override suspend fun cancelAll(taskIds: List<Long>) {
            cancelledTaskIds += taskIds
        }

        override suspend fun scheduleAll(tasks: List<TaskEntity>) {
            scheduledTasks += tasks
        }
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
