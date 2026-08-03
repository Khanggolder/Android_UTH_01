package com.uth.taskmanagement.ui.tasklist

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.uth.taskmanagement.R
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.data.model.TaskEntity

/**
 * Adapter hiển thị danh sách Task.
 */
class TaskAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var tasks = emptyList<TaskEntity>()

    fun submitList(newList: List<TaskEntity>) {
        tasks = newList
        notifyDataSetChanged()
    }
    class TaskViewHolder(itemView: View)
    : RecyclerView.ViewHolder(itemView) {

    val tvTitle: TextView =
        itemView.findViewById(R.id.tvTitle)

    val tvDescription: TextView =
        itemView.findViewById(R.id.tvDescription)

    val tvPriority: TextView =
        itemView.findViewById(R.id.tvPriority)

    val tvStatus: TextView =
        itemView.findViewById(R.id.tvStatus)

    val tvDueDate: TextView =
        itemView.findViewById(R.id.tvDueDate)

    val cbCompleted: CheckBox =
        itemView.findViewById(R.id.cbCompleted)
}
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
    .inflate(R.layout.item_task, parent, false)

return TaskViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val task = tasks[position]

        val taskHolder = holder as TaskViewHolder

        taskHolder.tvTitle.text = task.title
        taskHolder.tvDescription.text = task.description
        taskHolder.tvPriority.text = "Priority: ${task.priority}"
        taskHolder.tvStatus.text = "Status: ${task.status}"
        taskHolder.tvDueDate.text = "Due: ${task.dueDateTime}"
        taskHolder.cbCompleted.isChecked = task.isCompleted
    }
    override fun getItemCount(): Int = tasks.size
}