package com.uth.taskmanagement.attachment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import java.io.FileNotFoundException

/** Xử lý sau khi có Uri: xin quyền, đọc metadata, mở file, bắt lỗi file mất. */
object AttachmentFileHelper {

    fun buildAttachmentEntity(
        context: Context,
        uri: Uri,
        taskId: Long
    ): TaskAttachmentEntity {
        persistUriPermission(context, uri)

        val (fileName, sizeBytes) = readFileMetadata(context, uri)
        val mimeType = context.contentResolver.getType(uri) ?: ""

        return TaskAttachmentEntity(
            taskId = taskId,
            fileName = fileName,
            uri = uri.toString(),
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }

    private fun persistUriPermission(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
        }
    }

    private fun readFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var fileName = "Unknown file"
        var sizeBytes = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
            }
        }

        return fileName to sizeBytes
    }

    fun openAttachment(context: Context, attachment: TaskAttachmentEntity) {
        try {
            val uri = if (attachment.isAppOwned) {
                val file = AttachmentStorage(context).resolveOwnedFile(attachment)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                val externalUri = Uri.parse(attachment.uri)
                if (externalUri.scheme != "content") {
                    throw FileNotFoundException("Unsupported attachment URI")
                }
                context.contentResolver.openInputStream(externalUri)?.use { }
                    ?: throw FileNotFoundException("File is no longer available")
                externalUri
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "File is no longer available", Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            Toast.makeText(context, "File is no longer available", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "File is no longer available", Toast.LENGTH_SHORT).show()
        }
    }
}
