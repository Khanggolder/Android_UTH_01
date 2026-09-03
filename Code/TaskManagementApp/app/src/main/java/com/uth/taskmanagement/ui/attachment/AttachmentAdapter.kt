package com.uth.taskmanagement.ui.attachment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.databinding.ItemAttachmentBinding
import java.util.Locale

class AttachmentAdapter(
    private val onAttachmentClick: (TaskAttachmentEntity) -> Unit,
    private val onRemoveClick: (TaskAttachmentEntity) -> Unit
) : ListAdapter<TaskAttachmentEntity, AttachmentAdapter.AttachmentViewHolder>(
    AttachmentDiffCallback()
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AttachmentViewHolder {
        val binding = ItemAttachmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return AttachmentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AttachmentViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class AttachmentViewHolder(
        private val binding: ItemAttachmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(attachment: TaskAttachmentEntity) {
            binding.tvFileName.text = attachment.fileName

            binding.tvFileInfo.text = buildFileInfo(
                attachment.mimeType,
                attachment.sizeBytes
            )

            binding.root.setOnClickListener {
                onAttachmentClick(attachment)
            }

            binding.btnRemoveAttachment.setOnClickListener {
                onRemoveClick(attachment)
            }
        }
    }

    private fun buildFileInfo(
        mimeType: String,
        sizeBytes: Long
    ): String {
        val type = getFileType(mimeType)
        val size = formatFileSize(sizeBytes)

        return "$type • $size"
    }

    private fun getFileType(mimeType: String): String {
        if (mimeType.isBlank()) {
            return "FILE"
        }

        return when {
            mimeType.equals("application/pdf", ignoreCase = true) -> "PDF"

            mimeType.contains("word", ignoreCase = true) ||
                mimeType.contains("document", ignoreCase = true) -> "WORD"

            mimeType.contains("excel", ignoreCase = true) ||
                mimeType.contains("spreadsheet", ignoreCase = true) -> "EXCEL"

            mimeType.startsWith("image/", ignoreCase = true) -> "IMAGE"

            else -> {
                mimeType.substringAfterLast("/")
                    .uppercase(Locale.getDefault())
                    .ifBlank { "FILE" }
            }
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) {
            return "0 B"
        }

        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$sizeBytes B"
        }
    }

    private class AttachmentDiffCallback :
        DiffUtil.ItemCallback<TaskAttachmentEntity>() {

        override fun areItemsTheSame(
            oldItem: TaskAttachmentEntity,
            newItem: TaskAttachmentEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TaskAttachmentEntity,
            newItem: TaskAttachmentEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}