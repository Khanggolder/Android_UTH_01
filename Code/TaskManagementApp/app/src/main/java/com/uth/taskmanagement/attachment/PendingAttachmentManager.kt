package com.uth.taskmanagement.attachment

import android.content.Context
import android.net.Uri
import com.uth.taskmanagement.data.repository.AttachmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class PendingAttachmentManager(
    private val attachmentRepository: AttachmentRepository
) {

    suspend fun commitPendingAttachments(
        context: Context,
        pendingUris: List<Uri>,
        taskId: Long
    ): List<Long> = withContext(Dispatchers.IO) {
        val entities = preparePendingAttachments(context, pendingUris, taskId)
        attachmentRepository.addAttachments(entities)
    }

    suspend fun preparePendingAttachments(
        context: Context,
        pendingUris: List<Uri>,
        taskId: Long
    ): List<com.uth.taskmanagement.data.model.TaskAttachmentEntity> =
        withContext(Dispatchers.IO) {
            require(taskId > 0L) { "Attachment task ID must be greater than 0" }
            pendingUris.map { uri ->
                context.contentResolver.openInputStream(uri)?.use { it.read() }
                    ?: throw FileNotFoundException("Pending attachment is no longer available")
                AttachmentFileHelper.buildAttachmentEntity(context, uri, taskId)
            }
        }
}
