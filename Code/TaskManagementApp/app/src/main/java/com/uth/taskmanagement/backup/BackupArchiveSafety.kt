package com.uth.taskmanagement.backup

import java.io.File
import java.io.IOException

internal object BackupArchiveSafety {
    const val MANIFEST_ENTRY = "backup.json"
    const val ATTACHMENT_PREFIX = "attachments/"

    fun validateEntryPath(path: String, allowDirectory: Boolean): String {
        if (
            path.isBlank() ||
            path.contains('\\') ||
            path.startsWith('/') ||
            path.contains('\u0000') ||
            path.split('/').any { it == "." || it == ".." }
        ) {
            throw IOException("Unsafe ZIP entry path '$path'")
        }

        val valid = path == MANIFEST_ENTRY ||
            (allowDirectory && path == ATTACHMENT_PREFIX) ||
            (path.startsWith(ATTACHMENT_PREFIX) &&
                path.removePrefix(ATTACHMENT_PREFIX).isNotBlank() &&
                !path.removePrefix(ATTACHMENT_PREFIX).contains('/'))
        if (!valid) throw IOException("Unexpected ZIP entry '$path'")
        return path
    }

    fun resolveBelow(directory: File, relativePath: String): File {
        val base = directory.canonicalFile
        val destination = File(base, relativePath).canonicalFile
        val prefix = base.path + File.separator
        if (destination.path != base.path && !destination.path.startsWith(prefix)) {
            throw IOException("Unsafe ZIP entry path '$relativePath'")
        }
        return destination
    }
}
