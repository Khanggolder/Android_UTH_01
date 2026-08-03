package com.uth.taskmanagement.ui.tasklist

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.data.model.TaskEntity

/**
 * Adapter hiển thị danh sách Task.
 * Chưa implement ViewHolder.
 */
class TaskAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var tasks = emptyList<TaskEntity>()

    fun submitList(newList: List<TaskEntity>) {
        tasks = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        TODO("Implement later")
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        // Implement later
    }

    override fun getItemCount(): Int = tasks.size
}