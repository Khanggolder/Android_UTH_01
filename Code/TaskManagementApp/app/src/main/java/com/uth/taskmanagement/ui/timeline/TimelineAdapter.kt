package com.uth.taskmanagement.ui.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.databinding.ItemTimelineTaskBinding
import java.util.concurrent.TimeUnit
import kotlin.math.max

class TimelineAdapter(
    private val timelineStart: Long,
    private val totalDays: Int
) : ListAdapter<TaskEntity, TimelineAdapter.TimelineViewHolder>(DiffCallback())
{

    companion object {
        private const val DAY_WIDTH_DP = 80
        private const val MIN_BAR_WIDTH_DP = 40
    }

    inner class TimelineViewHolder(
        private val binding: ItemTimelineTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {

            val context = binding.root.context

            val dayWidthPx =
                (DAY_WIDTH_DP * context.resources.displayMetrics.density)
                    .toInt()

            val minBarWidthPx =
                (MIN_BAR_WIDTH_DP * context.resources.displayMetrics.density)
                    .toInt()

            val startOffsetDays =
                daysBetween(
                    timelineStart,
                    task.startDateTime
                )

            val durationDays =
                daysBetween(
                    task.startDateTime,
                    task.dueDateTime
                ) + 1

            val leftMargin =
                max(
                    0,
                    startOffsetDays.toInt()
                ) * dayWidthPx

            val barWidth =
                max(
                    1,
                    durationDays.toInt()
                ) * dayWidthPx

            binding.gridContainer.removeAllViews()

            repeat(totalDays) {

                val gridCell =
                    android.view.View(context)

                gridCell.layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                        dayWidthPx,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )

                gridCell.setBackgroundResource(
                    com.uth.taskmanagement.R.drawable.bg_timeline_grid_cell
                )

                binding.gridContainer.addView(gridCell)
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

    private fun daysBetween(
        startMillis: Long,
        endMillis: Long
    ): Long {

        val diff =
            endMillis - startMillis

        return TimeUnit.MILLISECONDS
            .toDays(diff)
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