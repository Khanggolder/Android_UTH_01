package com.uth.taskmanagement.attachment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import java.io.FileNotFoundException
import java.util.Locale

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
        } catch (_: SecurityException) {
            // Một số provider không cho persist quyền. Quyền tạm thời vẫn có thể dùng.
        }
    }

    private fun readFileMetadata(
        context: Context,
        uri: Uri
    ): Pair<String, Long> {
        var fileName = "Unknown file"
        var sizeBytes = 0L

        context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                val sizeIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.SIZE
                    )

                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        fileName =
                            cursor.getString(nameIndex)
                                ?: fileName
                    }

                    if (
                        sizeIndex >= 0 &&
                        !cursor.isNull(sizeIndex)
                    ) {
                        sizeBytes =
                            cursor.getLong(sizeIndex)
                    }
                }
            }

        return fileName to sizeBytes
    }

    fun openAttachment(
        context: Context,
        attachment: TaskAttachmentEntity
    ) {
        try {
            val uri = resolveOpenUri(
                context,
                attachment
            )

            /*
             * Không dùng resolveActivity() trước khi startActivity().
             *
             * Từ Android 11, package visibility có thể khiến
             * resolveActivity() trả null dù thiết bị thật sự có app
             * xử lý Intent. Khi đó code cũ báo sai:
             * "No app found to open this file type".
             *
             * Thay vào đó thử mở trực tiếp và chỉ coi là không có app
             * khi startActivity() thực sự ném ActivityNotFoundException.
             */
            val mimeCandidates =
                buildMimeCandidates(attachment)

            for (mimeType in mimeCandidates) {
                if (
                    tryOpenWithMimeType(
                        context = context,
                        uri = uri,
                        mimeType = mimeType
                    )
                ) {
                    return
                }
            }

            /*
             * Fallback cuối: để Android tự xử lý Uri mà không ép MIME.
             * Hữu ích với một số document provider/file viewer.
             */
            if (tryOpenWithoutMimeType(context, uri)) {
                return
            }

            Toast.makeText(
                context,
                "No app found to open this file type",
                Toast.LENGTH_SHORT
            ).show()

        } catch (_: SecurityException) {
            showFileUnavailable(context)
        } catch (_: FileNotFoundException) {
            showFileUnavailable(context)
        } catch (_: IllegalArgumentException) {
            showFileUnavailable(context)
        }
    }

    private fun resolveOpenUri(
        context: Context,
        attachment: TaskAttachmentEntity
    ): Uri {
        return if (attachment.isAppOwned) {

            val file =
                AttachmentStorage(context)
                    .resolveOwnedFile(attachment)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        } else {

            val externalUri =
                Uri.parse(attachment.uri)

            if (externalUri.scheme != "content") {
                throw FileNotFoundException(
                    "Unsupported attachment URI"
                )
            }

            context.contentResolver
                .openInputStream(externalUri)
                ?.use { }
                ?: throw FileNotFoundException(
                    "File is no longer available"
                )

            externalUri
        }
    }

    private fun buildMimeCandidates(
        attachment: TaskAttachmentEntity
    ): List<String> {
        val result = linkedSetOf<String>()

        val storedMime =
            attachment.mimeType.trim()

        if (storedMime.isNotBlank()) {
            result += storedMime
        }

        val extension =
            attachment.fileName
                .substringAfterLast('.', "")
                .lowercase(Locale.ROOT)

        if (extension.isNotBlank()) {
            MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension)
                ?.takeIf { it.isNotBlank() }
                ?.let { result += it }
        }

        /*
         * JSON/XML/CSV/log/markdown thường có thể được mở
         * bằng text viewer dù máy không đăng ký application/json.
         */
        val textLikeExtensions =
            setOf(
                "txt",
                "json",
                "xml",
                "csv",
                "log",
                "md"
            )

        val isTextLikeMime =
            storedMime.startsWith(
                "text/",
                ignoreCase = true
            ) ||
                storedMime.equals(
                    "application/json",
                    ignoreCase = true
                ) ||
                storedMime.equals(
                    "application/xml",
                    ignoreCase = true
                )

        if (
            extension in textLikeExtensions ||
            isTextLikeMime
        ) {
            result += "text/plain"
        }

        /* Generic fallback cho các app viewer đăng ký wildcard MIME. */
        result += "*/*"

        return result.toList()
    }

    private fun tryOpenWithMimeType(
        context: Context,
        uri: Uri,
        mimeType: String
    ): Boolean {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {

                setDataAndType(
                    uri,
                    mimeType
                )

                clipData =
                    ClipData.newRawUri(
                        "attachment",
                        uri
                    )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                if (context !is Activity) {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
            }

        return tryStartActivity(
            context,
            intent
        )
    }

    private fun tryOpenWithoutMimeType(
        context: Context,
        uri: Uri
    ): Boolean {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {

                data = uri

                clipData =
                    ClipData.newRawUri(
                        "attachment",
                        uri
                    )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                if (context !is Activity) {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
            }

        return tryStartActivity(
            context,
            intent
        )
    }

    private fun tryStartActivity(
        context: Context,
        intent: Intent
    ): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun showFileUnavailable(
        context: Context
    ) {
        Toast.makeText(
            context,
            "File is no longer available",
            Toast.LENGTH_SHORT
        ).show()
    }
}
