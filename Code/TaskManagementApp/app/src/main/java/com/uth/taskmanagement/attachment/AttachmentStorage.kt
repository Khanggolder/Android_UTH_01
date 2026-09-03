package com.uth.taskmanagement.attachment

import android.content.Context
import android.net.Uri
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID

class AttachmentStorage(
    private val context: Context
) {
    data class StoredFile(
        val relativePath: String,
        val sizeBytes: Long
    )

    class StagedDeletion internal constructor(
        internal val moves: List<Pair<File, File>>
    )

    private val attachmentDirectory: File
        get() = File(context.filesDir, ATTACHMENT_DIRECTORY)

    fun openInputStream(attachment: TaskAttachmentEntity): InputStream {
        return if (attachment.isAppOwned) {
            resolveOwnedFile(attachment).inputStream().buffered()
        } else {
            val uri = runCatching { Uri.parse(attachment.uri) }
                .getOrElse { throw FileNotFoundException("Invalid attachment URI") }
            context.contentResolver.openInputStream(uri)?.buffered()
                ?: throw FileNotFoundException("File is no longer available")
        }
    }

    fun isReadable(attachment: TaskAttachmentEntity): Boolean {
        return runCatching {
            openInputStream(attachment).use { it.read() }
            true
        }.getOrDefault(false)
    }

    fun copyIntoPrivateStorage(
        sourceName: String,
        inputStream: InputStream
    ): StoredFile {
        val directory = attachmentDirectory.apply {
            if (!exists() && !mkdirs()) {
                throw FileNotFoundException("Could not create attachment storage")
            }
        }
        val destination = File(
            directory,
            "${UUID.randomUUID()}_${sanitizeFileName(sourceName)}"
        )

        try {
            destination.outputStream().buffered().use { output ->
                inputStream.buffered().use { input -> input.copyTo(output) }
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        }

        return StoredFile(
            relativePath = "$ATTACHMENT_DIRECTORY/${destination.name}",
            sizeBytes = destination.length()
        )
    }

    fun resolveOwnedFile(attachment: TaskAttachmentEntity): File {
        if (!attachment.isAppOwned) {
            throw FileNotFoundException("Attachment is not app-owned")
        }
        val relativePath = attachment.localRelativePath
            ?: throw FileNotFoundException("Missing internal attachment path")
        return resolveOwnedFile(relativePath)
    }

    fun resolveOwnedFile(relativePath: String): File {
        val base = attachmentDirectory.canonicalFile
        val candidate = File(context.filesDir, relativePath).canonicalFile
        val prefix = base.path + File.separator
        if (candidate.path != base.path && !candidate.path.startsWith(prefix)) {
            throw SecurityException("Attachment path escapes app storage")
        }
        if (!candidate.isFile || !candidate.canRead()) {
            throw FileNotFoundException("File is no longer available")
        }
        return candidate
    }

    fun stageOwnedFilesForDeletion(
        attachments: Collection<TaskAttachmentEntity>
    ): StagedDeletion {
        val trashDirectory = File(attachmentDirectory, TRASH_DIRECTORY)
        val moves = mutableListOf<Pair<File, File>>()

        try {
            attachments.asSequence()
                .filter { it.isAppOwned }
                .mapNotNull { it.localRelativePath }
                .distinct()
                .forEach { relativePath ->
                    val original = runCatching { resolveOwnedFile(relativePath) }
                        .getOrElse { error ->
                            if (error is FileNotFoundException) return@forEach
                            throw error
                        }
                    if (!trashDirectory.exists() && !trashDirectory.mkdirs()) {
                        throw FileNotFoundException("Could not create attachment trash")
                    }
                    val staged = File(trashDirectory, UUID.randomUUID().toString())
                    if (!original.renameTo(staged)) {
                        throw FileNotFoundException("Could not stage attachment deletion")
                    }
                    moves += original to staged
                }
        } catch (error: Exception) {
            rollbackDeletion(StagedDeletion(moves))
            throw error
        }

        return StagedDeletion(moves)
    }

    fun commitDeletion(stagedDeletion: StagedDeletion) {
        stagedDeletion.moves.forEach { (_, staged) -> staged.delete() }
        File(attachmentDirectory, TRASH_DIRECTORY).delete()
    }

    fun rollbackDeletion(stagedDeletion: StagedDeletion) {
        stagedDeletion.moves.asReversed().forEach { (original, staged) ->
            if (staged.exists()) {
                original.parentFile?.mkdirs()
                staged.renameTo(original)
            }
        }
    }

    companion object {
        const val ATTACHMENT_DIRECTORY = "attachments"
        private const val TRASH_DIRECTORY = ".trash"

        fun sanitizeFileName(fileName: String): String {
            val sanitized = fileName
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .map { character ->
                    if (character.isLetterOrDigit() || character in "._- ") character else '_'
                }
                .joinToString("")
                .trim('.', ' ')
                .take(120)
            return sanitized.ifBlank { "attachment" }
        }
    }
}
