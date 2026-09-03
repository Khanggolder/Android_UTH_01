package com.uth.taskmanagement.attachment

import android.content.Context
import android.net.Uri
import com.uth.taskmanagement.data.repository.AttachmentRepository

class PendingAttachmentManager(
    private val attachmentRepository: AttachmentRepository
) {

    suspend fun commitPendingAttachments(
        context: Context,
        pendingUris: List<Uri>,
        taskId: Long
    ): List<Long> {
        if (pendingUris.isEmpty()) return emptyList()

        val entities = pendingUris.map { uri ->
            AttachmentFileHelper.buildAttachmentEntity(context, uri, taskId)
        }

        return attachmentRepository.addAttachments(entities)
    }
}