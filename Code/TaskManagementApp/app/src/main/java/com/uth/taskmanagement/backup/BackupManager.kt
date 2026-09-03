package com.uth.taskmanagement.backup

import android.content.Context
import android.net.Uri
import com.uth.taskmanagement.attachment.AttachmentStorage
import com.uth.taskmanagement.data.local.TaskDatabase
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.data.model.UserEntity
import com.uth.taskmanagement.data.repository.AttachmentRepository
import com.uth.taskmanagement.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

interface ReminderScheduler {
    suspend fun cancelAll(taskIds: List<Long>)
    suspend fun scheduleAll(tasks: List<TaskEntity>)
}

class BackupManager(
    private val taskRepository: TaskRepository,
    private val attachmentRepository: AttachmentRepository,
    private val reminderScheduler: ReminderScheduler,
    private val database: TaskDatabase,
    private val attachmentStorage: AttachmentStorage,
    private val context: Context
) {
    suspend fun exportTasks(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { exportZip(uri) }
    }

    suspend fun exportTaskData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { exportJson(uri) }
    }

    suspend fun restoreTasks(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { restoreFromDocument(uri) }
    }

    private suspend fun exportJson(uri: Uri) {
        val exportTasks = loadExportTasks(includeArchivePaths = false)
        val outputStream = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("Could not open the selected file for writing")

        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(buildBackupJson(exportTasks, JSON_BACKUP_VERSION).toString())
        }
    }

    private suspend fun exportZip(uri: Uri) {
        val exportTasks = loadExportTasks(includeArchivePaths = true)
        val manifestBytes = buildBackupJson(exportTasks, BACKUP_VERSION)
            .toString()
            .toByteArray(Charsets.UTF_8)
        validateExportSizes(exportTasks, manifestBytes.size.toLong())

        val outputStream = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("Could not open the selected file for writing")

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
            zip.write(manifestBytes)
            zip.closeEntry()

            var totalBytes = manifestBytes.size.toLong()
            exportTasks.asSequence()
                .flatMap { it.attachments.asSequence() }
                .forEach { exportAttachment ->
                    zip.putNextEntry(ZipEntry(requireNotNull(exportAttachment.archivePath)))
                    try {
                        attachmentStorage.openInputStream(exportAttachment.entity).use { input ->
                            val buffer = ByteArray(COPY_BUFFER_SIZE)
                            var entryBytes = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (entryBytes > BackupArchiveSafety.MAX_SINGLE_ENTRY_BYTES - count) {
                                    throw BackupException(
                                        "Attachment '${exportAttachment.entity.fileName}' is too large for backup"
                                    )
                                }
                                if (totalBytes > BackupArchiveSafety.MAX_TOTAL_UNCOMPRESSED_BYTES - count) {
                                    throw BackupException("Total attachment size exceeds backup limit")
                                }
                                zip.write(buffer, 0, count)
                                entryBytes += count
                                totalBytes += count
                            }
                        }
                    } catch (error: BackupException) {
                        throw error
                    } catch (error: Exception) {
                        throw BackupException(
                            "Cannot export attachment '${exportAttachment.entity.fileName}' because the file is no longer available.",
                            error
                        )
                    } finally {
                        zip.closeEntry()
                    }
                }
        }
    }

    private suspend fun loadExportTasks(includeArchivePaths: Boolean): List<ExportTask> {
        return taskRepository.getAllTasks().map { task ->
            val attachments = attachmentRepository.getAttachments(task.id).map { attachment ->
                if (includeArchivePaths && !attachmentStorage.isReadable(attachment)) {
                    throw BackupException(
                        "Cannot export attachment '${attachment.fileName}' because the file is no longer available."
                    )
                }
                ExportAttachment(
                    entity = attachment,
                    archivePath = if (includeArchivePaths) {
                        createUniqueArchivePath(attachment.fileName)
                    } else {
                        null
                    }
                )
            }
            ExportTask(task, attachments)
        }
    }

    private fun validateExportSizes(tasks: List<ExportTask>, manifestSize: Long) {
        if (manifestSize > BackupArchiveSafety.MAX_SINGLE_ENTRY_BYTES) {
            throw BackupException("Backup manifest is too large")
        }

        var totalBytes = manifestSize
        tasks.asSequence()
            .flatMap { it.attachments.asSequence() }
            .forEach { exportAttachment ->
                val attachment = exportAttachment.entity
                val size = attachment.sizeBytes.coerceAtLeast(0L)
                if (size > BackupArchiveSafety.MAX_SINGLE_ENTRY_BYTES) {
                    throw BackupException(
                        "Attachment '${attachment.fileName}' is too large for backup"
                    )
                }
                if (totalBytes > BackupArchiveSafety.MAX_TOTAL_UNCOMPRESSED_BYTES - size) {
                    throw BackupException("Total attachment size exceeds backup limit")
                }
                totalBytes += size
            }
    }

    private suspend fun restoreFromDocument(uri: Uri) {
        val stagingDirectory = File(
            context.cacheDir,
            "$RESTORE_STAGING_DIRECTORY/${UUID.randomUUID()}"
        )
        if (!stagingDirectory.mkdirs()) {
            throw IOException("Could not prepare temporary restore storage")
        }

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not read the selected backup file")
            val bufferedInput = BufferedInputStream(input)
            val payload = bufferedInput.use { stream ->
                if (isZipStream(stream)) {
                    extractZip(stream, stagingDirectory)
                    val manifestFile = File(stagingDirectory, BackupArchiveSafety.MANIFEST_ENTRY)
                    if (!manifestFile.isFile) {
                        throw BackupException("The ZIP backup does not contain backup.json")
                    }
                    parseManifest(manifestFile.readText(Charsets.UTF_8), isZip = true)
                } else {
                    parseManifest(stream.bufferedReader(Charsets.UTF_8).readText(), isZip = false)
                }
            }

            val restoredAttachments = materializeAttachments(payload, stagingDirectory)
            replaceCurrentData(payload.tasks, restoredAttachments)
        } finally {
            stagingDirectory.deleteRecursively()
        }
    }

    private suspend fun replaceCurrentData(
        tasks: List<TaskEntity>,
        restoredAttachments: RestoredAttachments
    ) {
        val oldTasks = taskRepository.getAllTasks()
        val oldAttachments = attachmentRepository.getAllAttachments()
        val oldFileDeletion = try {
            attachmentRepository.stageOwnedFiles(oldAttachments)
        } catch (error: Exception) {
            deleteNewOwnedFiles(restoredAttachments.entities)
            throw error
        }

        try {
            database.replaceBackupData(tasks, restoredAttachments.entities)
        } catch (error: Exception) {
            attachmentRepository.rollbackFileDeletion(oldFileDeletion)
            deleteNewOwnedFiles(restoredAttachments.entities)
            throw error
        }

        attachmentRepository.commitFileDeletion(oldFileDeletion)
        reminderScheduler.cancelAll(oldTasks.map { it.id })
        reminderScheduler.scheduleAll(tasks)
    }

    private fun materializeAttachments(
        payload: RestorePayload,
        stagingDirectory: File
    ): RestoredAttachments {
        val entities = mutableListOf<TaskAttachmentEntity>()

        try {
            payload.attachments.forEach { attachment ->
                if (attachment.archivePath == null) {
                    entities += attachment.toLegacyEntity()
                    return@forEach
                }

                val source = BackupArchiveSafety.resolveBelow(stagingDirectory, attachment.archivePath)
                if (!source.isFile) {
                    throw BackupException(
                        "Attachment '${attachment.fileName}' is missing from the ZIP backup"
                    )
                }
                val stored = source.inputStream().use { input ->
                    attachmentStorage.copyIntoPrivateStorage(attachment.fileName, input)
                }
                entities += TaskAttachmentEntity(
                    taskId = attachment.taskId,
                    fileName = attachment.fileName,
                    uri = "",
                    mimeType = attachment.mimeType,
                    sizeBytes = stored.sizeBytes,
                    createdAt = attachment.createdAt,
                    isAppOwned = true,
                    localRelativePath = stored.relativePath
                )
            }
        } catch (error: Exception) {
            deleteNewOwnedFiles(entities)
            throw error
        }

        return RestoredAttachments(entities)
    }

    private fun deleteNewOwnedFiles(attachments: Collection<TaskAttachmentEntity>) {
        runCatching {
            val deletion = attachmentRepository.stageOwnedFiles(attachments)
            attachmentRepository.commitFileDeletion(deletion)
        }
    }

    private fun extractZip(input: InputStream, stagingDirectory: File) {
        var entryCount = 0
        var totalBytes = 0L
        val seenEntries = mutableSetOf<String>()

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw BackupException("The ZIP backup contains too many entries")
                }

                val normalizedName = BackupArchiveSafety.validateEntryPath(
                    entry.name,
                    allowDirectory = entry.isDirectory
                )
                if (!seenEntries.add(normalizedName)) {
                    throw BackupException("The ZIP backup contains duplicate entry '$normalizedName'")
                }
                val destination = BackupArchiveSafety.resolveBelow(stagingDirectory, normalizedName)

                if (entry.isDirectory) {
                    if (!destination.exists() && !destination.mkdirs()) {
                        throw IOException("Could not create restore directory")
                    }
                } else {
                    destination.parentFile?.let { parent ->
                        if (!parent.exists() && !parent.mkdirs()) {
                            throw IOException("Could not create restore directory")
                        }
                    }
                    BufferedOutputStream(destination.outputStream()).use { output ->
                        val buffer = ByteArray(COPY_BUFFER_SIZE)
                        var entryBytes = 0L
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            entryBytes += count
                            totalBytes += count
                            if (
                                entryBytes > BackupArchiveSafety.MAX_SINGLE_ENTRY_BYTES ||
                                totalBytes > BackupArchiveSafety.MAX_TOTAL_UNCOMPRESSED_BYTES
                            ) {
                                throw BackupException("The ZIP backup is too large")
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun parseManifest(jsonText: String, isZip: Boolean): RestorePayload {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) throw BackupException("The backup manifest is empty")

        try {
            val rootObject = if (trimmed.startsWith("{")) JSONObject(trimmed) else null
            val version = rootObject?.optInt("version", 1) ?: 1
            val tasksArray = rootObject?.optJSONArray("tasks") ?: JSONArray(trimmed)

            if (isZip && version != BACKUP_VERSION) {
                throw BackupException("Unsupported ZIP backup version: $version")
            }
            if (!isZip && version > LEGACY_JSON_MAX_VERSION) {
                throw BackupException("Backup version $version must be restored from a ZIP file")
            }

            val tasks = mutableListOf<TaskEntity>()
            val attachments = mutableListOf<RestoreAttachment>()
            val taskIds = mutableSetOf<Long>()
            val archivePaths = mutableSetOf<String>()

            for (index in 0 until tasksArray.length()) {
                val objectValue = tasksArray.getJSONObject(index)
                val task = parseTask(objectValue)
                if (!taskIds.add(task.id)) {
                    throw BackupException("Duplicate task ID ${task.id} in backup")
                }
                tasks += task

                val attachmentArray = objectValue.optJSONArray("attachments") ?: JSONArray()
                for (attachmentIndex in 0 until attachmentArray.length()) {
                    val parsed = parseAttachment(
                        objectValue = attachmentArray.getJSONObject(attachmentIndex),
                        taskId = task.id,
                        defaultCreatedAt = task.createdAt,
                        isZip = isZip
                    )
                    parsed.archivePath?.let { path ->
                        if (!archivePaths.add(path)) {
                            throw BackupException("Duplicate attachment path '$path' in backup")
                        }
                    }
                    attachments += parsed
                }
            }

            return RestorePayload(tasks, attachments)
        } catch (error: BackupException) {
            throw error
        } catch (error: JSONException) {
            throw BackupException("Invalid backup JSON: ${error.message}", error)
        } catch (error: IllegalArgumentException) {
            throw BackupException("Invalid value in backup: ${error.message}", error)
        }
    }

    private fun parseTask(objectValue: JSONObject): TaskEntity {
        val id = objectValue.getLong("id")
        if (id <= 0L) throw BackupException("Task ID must be greater than zero")

        val title = objectValue.getString("title").trim()
        if (title.isEmpty()) throw BackupException("Task title cannot be empty")

        val createdAt = objectValue.optLong("createdAt", System.currentTimeMillis())
        val dueDateTime = objectValue.getLong("dueDateTime")
        val startDateTime = objectValue.optLong("startDateTime", createdAt)
        if (startDateTime > dueDateTime) {
            throw BackupException(
                "Task '$title' has startDateTime after dueDateTime"
            )
        }

        return TaskEntity(
            id = id,
            title = title,
            description = objectValue.optString("description", ""),
            startDateTime = startDateTime,
            dueDateTime = dueDateTime,
            priority = enumValue(objectValue, "priority", TaskPriority.MEDIUM),
            status = enumValue(objectValue, "status", TaskStatus.PENDING),
            isCompleted = objectValue.optBoolean("isCompleted", false),
            reminderTime = if (!objectValue.has("reminderTime") || objectValue.isNull("reminderTime")) {
                null
            } else {
                objectValue.getLong("reminderTime")
            },
            recurrenceType = enumValue(objectValue, "recurrenceType", RecurrenceType.NONE),
            createdAt = createdAt,
            updatedAt = objectValue.optLong("updatedAt", createdAt),
            createdByUserId = objectValue.optString(
                "createdByUserId",
                UserEntity.DEFAULT_USER_ID
            ).ifBlank { UserEntity.DEFAULT_USER_ID },
            assigneeUserId = objectValue.optString(
                "assigneeUserId",
                UserEntity.DEFAULT_USER_ID
            ).ifBlank { UserEntity.DEFAULT_USER_ID }
        )
    }

    private fun parseAttachment(
        objectValue: JSONObject,
        taskId: Long,
        defaultCreatedAt: Long,
        isZip: Boolean
    ): RestoreAttachment {
        val fileName = objectValue.getString("fileName").trim()
        if (fileName.isEmpty()) throw BackupException("Attachment file name cannot be empty")

        val archivePath = if (isZip) {
            BackupArchiveSafety.validateEntryPath(
                objectValue.getString("archivePath"),
                allowDirectory = false
            ).also { path ->
                if (!path.startsWith(BackupArchiveSafety.ATTACHMENT_PREFIX)) {
                    throw BackupException("Invalid attachment path '$path'")
                }
            }
        } else {
            null
        }
        val legacyUri = if (isZip) null else objectValue.optString("uri", "")

        return RestoreAttachment(
            taskId = taskId,
            fileName = fileName,
            mimeType = objectValue.optString("mimeType", ""),
            sizeBytes = objectValue.optLong("sizeBytes", 0L).coerceAtLeast(0L),
            createdAt = objectValue.optLong("createdAt", defaultCreatedAt),
            archivePath = archivePath,
            legacyUri = legacyUri
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(
        objectValue: JSONObject,
        key: String,
        defaultValue: T
    ): T {
        val raw = objectValue.optString(key, defaultValue.name)
        return enumValues<T>().firstOrNull { it.name == raw }
            ?: throw BackupException("Unknown $key value '$raw'")
    }

    private fun buildBackupJson(tasks: List<ExportTask>, version: Int): JSONObject {
        val taskArray = JSONArray()
        tasks.forEach { exportTask ->
            val task = exportTask.entity
            val taskObject = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("startDateTime", task.startDateTime)
                put("dueDateTime", task.dueDateTime)
                put("priority", task.priority.name)
                put("status", task.status.name)
                put("isCompleted", task.isCompleted)
                put("reminderTime", task.reminderTime ?: JSONObject.NULL)
                put("recurrenceType", task.recurrenceType.name)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
                put("createdByUserId", task.createdByUserId)
                put("assigneeUserId", task.assigneeUserId)
            }
            val attachmentArray = JSONArray()
            exportTask.attachments.forEach { exportAttachment ->
                val attachment = exportAttachment.entity
                attachmentArray.put(JSONObject().apply {
                    put("fileName", attachment.fileName)
                    put("mimeType", attachment.mimeType)
                    put("sizeBytes", attachment.sizeBytes)
                    put("createdAt", attachment.createdAt)
                    if (version == BACKUP_VERSION) {
                        put("archivePath", requireNotNull(exportAttachment.archivePath))
                    } else {
                        put("uri", attachment.uri)
                    }
                })
            }
            taskObject.put("attachments", attachmentArray)
            taskArray.put(taskObject)
        }

        return JSONObject().apply {
            put("version", version)
            put("tasks", taskArray)
        }
    }

    private fun createUniqueArchivePath(fileName: String): String {
        return "${BackupArchiveSafety.ATTACHMENT_PREFIX}${UUID.randomUUID()}_" +
            AttachmentStorage.sanitizeFileName(fileName)
    }

    private fun isZipStream(input: BufferedInputStream): Boolean {
        input.mark(4)
        val first = input.read()
        val second = input.read()
        val third = input.read()
        val fourth = input.read()
        input.reset()
        return first == 0x50 && second == 0x4B &&
            ((third == 0x03 && fourth == 0x04) ||
                (third == 0x05 && fourth == 0x06) ||
                (third == 0x07 && fourth == 0x08))
    }

    private data class ExportTask(
        val entity: TaskEntity,
        val attachments: List<ExportAttachment>
    )

    private data class ExportAttachment(
        val entity: TaskAttachmentEntity,
        val archivePath: String?
    )

    private data class RestorePayload(
        val tasks: List<TaskEntity>,
        val attachments: List<RestoreAttachment>
    )

    private data class RestoreAttachment(
        val taskId: Long,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long,
        val createdAt: Long,
        val archivePath: String?,
        val legacyUri: String?
    ) {
        fun toLegacyEntity() = TaskAttachmentEntity(
            taskId = taskId,
            fileName = fileName,
            uri = legacyUri.orEmpty(),
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            createdAt = createdAt,
            isAppOwned = false,
            localRelativePath = null
        )
    }

    private data class RestoredAttachments(
        val entities: List<TaskAttachmentEntity>
    )

    private class BackupException(
        message: String,
        cause: Throwable? = null
    ) : IOException(message, cause)

    companion object {
        const val BACKUP_VERSION = 3
        internal const val BACKUP_JSON_ENTRY = BackupArchiveSafety.MANIFEST_ENTRY
        internal const val JSON_BACKUP_VERSION = 2
        private const val LEGACY_JSON_MAX_VERSION = JSON_BACKUP_VERSION
        private const val RESTORE_STAGING_DIRECTORY = "backup-restore"
        private const val COPY_BUFFER_SIZE = 16 * 1024
        private const val MAX_ZIP_ENTRIES = 2_000
    }
}
