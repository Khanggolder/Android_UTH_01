package com.uth.taskmanagement.attachment

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/** Mở SAF (ACTION_OPEN_DOCUMENT) để chọn file đính kèm. */
class AttachmentPicker(
    fragment: Fragment,
    private val onFilePicked: (Uri) -> Unit
) {
    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onFilePicked(it) }
    }

    fun launch(mimeTypes: Array<String> = arrayOf("*/*")) {
        launcher.launch(mimeTypes)
    }
}