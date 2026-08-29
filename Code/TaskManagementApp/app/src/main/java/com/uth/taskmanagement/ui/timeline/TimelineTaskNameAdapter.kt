package com.uth.taskmanagement.ui.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.databinding.ItemTimelineTaskNameBinding

class TimelineTaskNameAdapter :
    ListAdapter<TaskEntity, TimelineTaskNameAdapter.TaskNameViewHolder>(
        DiffCallback()
    ) {

    inner class TaskNameViewHolder(
        private val binding: ItemTimelineTaskNameBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {
            binding.tvTaskTitle.text = task.title
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskNameViewHolder {

        val binding =
            ItemTimelineTaskNameBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return TaskNameViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TaskNameViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class DiffCallback :
        DiffUtil.ItemCallback<TaskEntity>() {

        override fun areItemsTheSame(
            oldItem: TaskEntity,
            newItem: TaskEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TaskEntity,
            newItem: TaskEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}