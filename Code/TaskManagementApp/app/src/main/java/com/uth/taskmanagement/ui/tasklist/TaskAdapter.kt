package com.uth.taskmanagement.ui.tasklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.R
import com.uth.taskmanagement.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onItemClick: (TaskEntity) -> Unit = {},
    private val onCompletedChanged: (TaskEntity, Boolean) -> Unit = { _, _ -> }
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TaskEntity>() {
            override fun areItemsTheSame(old: TaskEntity, new: TaskEntity) = old.id == new.id
            override fun areContentsTheSame(old: TaskEntity, new: TaskEntity) = old == new
        }
    }

    private var overdueTaskIds: Set<Long> = emptySet()
    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    fun submitList(newList: List<TaskEntity>?, overdueIds: Set<Long> = emptySet()) {
        overdueTaskIds = overdueIds
        super.submitList(newList)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)

        holder.tvTitle.text = task.title
        holder.tvDescription.text = task.description.ifBlank { "No description" }
        holder.tvPriority.text = task.priority.name
        holder.tvStatus.text = task.status.name

        holder.tvDueDate.text = if (overdueTaskIds.contains(task.id)) {
            "Overdue " + dateFormat.format(Date(task.dueDateTime))
        } else {
            dateFormat.format(Date(task.dueDateTime))
        }
        holder.tvDueDate.setTextColor(
            holder.itemView.context.getColor(
                if (overdueTaskIds.contains(task.id))
                    R.color.priority_high else R.color.text_secondary
            )
        )

        // Bind checkbox without triggering the listener
        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = task.isCompleted
        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            onCompletedChanged(task, isChecked)
        }

        // Item click → open edit form
        holder.itemView.setOnClickListener { onItemClick(task) }
    }
}