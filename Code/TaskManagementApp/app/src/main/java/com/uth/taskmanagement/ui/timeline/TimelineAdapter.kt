package com.uth.taskmanagement.ui.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.databinding.ItemTimelineTaskBinding
import kotlin.math.max
import androidx.core.content.ContextCompat
import com.uth.taskmanagement.R
import com.uth.taskmanagement.data.model.TaskStatus
import android.content.res.ColorStateList

class TimelineAdapter(
    private val timelineStart: Long,
    private val totalDays: Int,
    private val today: Long
) : ListAdapter<TaskEntity, TimelineAdapter.TimelineViewHolder>(DiffCallback())
{

    companion object {
        private const val MIN_BAR_WIDTH_DP = 40
    }

    inner class TimelineViewHolder(
        private val binding: ItemTimelineTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {

            val context = binding.root.context

            val isOverdue =
                task.dueDateTime < System.currentTimeMillis() &&
                    task.status != TaskStatus.COMPLETED

            val barColorRes =
                when {

                    isOverdue ->
                        R.color.timeline_overdue

                    task.status == TaskStatus.PENDING ->
                        R.color.timeline_pending

                    task.status == TaskStatus.IN_PROGRESS ->
                        R.color.timeline_in_progress

                    else ->
                        R.color.timeline_completed
                }

            binding.timelineBar.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        barColorRes
                    )
                )

            val dayWidthPx =
                (TimelineLayoutCalculator.DAY_WIDTH_DP *
                    context.resources.displayMetrics.density)
                    .toInt()

            val minBarWidthPx =
                (MIN_BAR_WIDTH_DP * context.resources.displayMetrics.density)
                    .toInt()

            val timelineWidthPx = totalDays * dayWidthPx
            binding.timelineContainer.layoutParams =
                binding.timelineContainer.layoutParams.apply {
                    width = timelineWidthPx
                }

            val barPosition =
                TimelineLayoutCalculator.barPosition(
                    task = task,
                    timelineStart = timelineStart,
                    totalDays = totalDays
                ) ?: return

            val leftMargin =
                barPosition.startDayIndex * dayWidthPx

            val barWidth =
                barPosition.durationDays * dayWidthPx

            binding.gridContainer.removeAllViews()

            repeat(totalDays) { dayIndex ->

                val gridCell =
                    android.view.View(context)

                gridCell.layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        dayWidthPx,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )

                val cellDate =
                    TimelineLayoutCalculator.addDays(
                        timelineStart,
                        dayIndex.toLong()
                    )

                if (cellDate == today) {

                    gridCell.setBackgroundResource(
                        R.drawable.bg_timeline_today_cell
                    )

                } else {

                    gridCell.setBackgroundResource(
                        com.uth.taskmanagement.R.drawable.bg_timeline_grid_cell
                    )
                }

                binding.gridContainer.addView(
                    gridCell
                )
            }

            val params =
                binding.timelineBar.layoutParams
                        as FrameLayout.LayoutParams

            params.leftMargin =
                leftMargin

            params.width =
                max(
                    barWidth,
                    minBarWidthPx
                )

            binding.timelineBar.layoutParams =
                params
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TimelineViewHolder {

        val binding =
            ItemTimelineTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TimelineViewHolder,
        position: Int
    ) {
        holder.bind(
            getItem(position)
        )
    }

    class DiffCallback :
        DiffUtil.ItemCallback<TaskEntity>() {

        override fun areItemsTheSame(
            oldItem: TaskEntity,
            newItem: TaskEntity
        ): Boolean {

            return oldItem.id ==
                newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TaskEntity,
            newItem: TaskEntity
        ): Boolean {

            return oldItem ==
                newItem
        }
    }
}
