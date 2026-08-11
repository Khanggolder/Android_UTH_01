package com.uth.taskmanagement.ui.tasklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.R
import com.uth.taskmanagement.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks = emptyList<TaskEntity>()
    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    fun submitList(newList: List<TaskEntity>) {
        tasks = newList
        notifyDataSetChanged()
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
        val task = tasks[position]
        holder.tvTitle.text = task.title
        holder.tvDescription.text = task.description.ifBlank { "No description" }
        holder.tvPriority.text = task.priority.name
        holder.tvStatus.text = task.status.name
        holder.tvDueDate.text = dateFormat.format(Date(task.dueDateTime))
        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = task.isCompleted
        holder.cbCompleted.setOnCheckedChangeListener { _, _ ->
            // TODO: Wire completion updates through TaskListViewModel when edit flow is finalized.
        }
    }

    override fun getItemCount(): Int = tasks.size
}