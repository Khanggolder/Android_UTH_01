package com.uth.taskmanagement.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.IOException

class BackupArchiveSafetyTest {
    @Test
    fun acceptsManifestAndFlatAttachmentEntries() {
        assertEquals(
            "backup.json",
            BackupArchiveSafety.validateEntryPath("backup.json", allowDirectory = false)
        )
        assertEquals(
            "attachments/unique_report.pdf",
            BackupArchiveSafety.validateEntryPath(
                "attachments/unique_report.pdf",
                allowDirectory = false
            )
        )
        assertEquals(
            "attachments/",
            BackupArchiveSafety.validateEntryPath("attachments/", allowDirectory = true)
        )
    }

    @Test
    fun rejectsZipSlipAndUnexpectedEntries() {
        listOf(
            "../../bad.txt",
            "attachments/../bad.txt",
            "/absolute.txt",
            "attachments\\bad.txt",
            "other/file.txt",
            "attachments/nested/file.txt"
        ).forEach { path ->
            assertThrows(IOException::class.java) {
                BackupArchiveSafety.validateEntryPath(path, allowDirectory = false)
            }
        }
    }

    @Test
    fun canonicalResolutionCannotEscapeStagingDirectory() {
        val staging = File(System.getProperty("java.io.tmpdir"), "backup-safety-test")
        assertThrows(IOException::class.java) {
            BackupArchiveSafety.resolveBelow(staging, "../outside.txt")
        }
    }
}
