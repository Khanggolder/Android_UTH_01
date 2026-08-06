package com.uth.taskmanagement.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.R
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.databinding.ItemTaskCalendarBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskCalendarAdapter(
    private val onCheckedChange: (TaskOccurrence, Boolean) -> Unit
) : ListAdapter<TaskOccurrence, TaskCalendarAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskCalendarBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTaskCalendarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(occurrence: TaskOccurrence) {
            val task = occurrence.task
            binding.tvTaskTitle.text = task.title
            binding.tvTaskTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(occurrence.occurrenceDateTime))

            binding.priorityBar.setBackgroundResource(
                when (task.priority) {
                    TaskPriority.HIGH -> R.drawable.bg_priority_high
                    TaskPriority.MEDIUM -> R.drawable.bg_priority_medium
                    TaskPriority.LOW -> R.drawable.bg_priority_low
                }
            )

            val isRecurring = task.recurrenceType != RecurrenceType.NONE
            binding.tvRecurringBadge.visibility = if (isRecurring) View.VISIBLE else View.GONE
            binding.tvRecurringBadge.text = when (task.recurrenceType) {
                RecurrenceType.DAILY -> "Daily"
                RecurrenceType.WEEKLY -> "Weekly"
                RecurrenceType.MONTHLY -> "Monthly"
                RecurrenceType.NONE -> ""
            }

            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = task.isCompleted
            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(occurrence, isChecked)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TaskOccurrence>() {
        override fun areItemsTheSame(old: TaskOccurrence, new: TaskOccurrence) =
            old.task.id == new.task.id && old.occurrenceDateTime == new.occurrenceDateTime

        override fun areContentsTheSame(old: TaskOccurrence, new: TaskOccurrence) = old == new
    }
}