package com.uth.taskmanagement.ui.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.databinding.FragmentTimelineBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TimelineFragment : Fragment() {

    companion object {

        private const val DAY_WIDTH_DP = 80

        // Tạm thời hiển thị thêm một ít ngày
        // trước và sau các task.
        private const val EXTRA_DAYS_BEFORE = 2
        private const val EXTRA_DAYS_AFTER = 5
    }

    private var _binding: FragmentTimelineBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var viewModel: TimelineViewModel

    private var timelineAdapter: TimelineAdapter? = null

    private val dayFormat =
        SimpleDateFormat(
            "dd\nMMM",
            Locale.getDefault()
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentTimelineBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        setupViewModel()

        setupRecyclerView()

        observeTasks()
    }

    // ─────────────────────────────────────────────
    // ViewModel
    // ─────────────────────────────────────────────

    private fun setupViewModel() {

        val app =
            requireActivity().application
                    as TaskManagementApp

        viewModel =
            ViewModelProvider(
                this,
                TimelineViewModelFactory(
                    app.taskRepository
                )
            )[TimelineViewModel::class.java]
    }

    // ─────────────────────────────────────────────
    // RecyclerView
    // ─────────────────────────────────────────────

    private fun setupRecyclerView() {

        binding.rvTimeline.layoutManager =
            LinearLayoutManager(
                requireContext()
            )
    }

    // ─────────────────────────────────────────────
    // Observe Tasks
    // ─────────────────────────────────────────────

    private fun observeTasks() {

        viewModel.tasks.observe(
            viewLifecycleOwner
        ) { tasks ->

            if (tasks.isNullOrEmpty()) {

                binding.dateHeaderContainer
                    .removeAllViews()

                timelineAdapter = null

                binding.rvTimeline.adapter = null

                return@observe
            }

            setupTimeline(tasks)
        }
    }

    // ─────────────────────────────────────────────
    // Setup Timeline
    // ─────────────────────────────────────────────

    private fun setupTimeline(
        tasks: List<TaskEntity>
    ) {

        /*
         * Tìm Start Date sớm nhất.
         */
        val earliestTaskStart =
            tasks.minOf {
                it.startDateTime
            }

        /*
         * Tìm Due Date muộn nhất.
         */
        val latestTaskEnd =
            tasks.maxOf {
                it.dueDateTime
            }

        /*
         * Chuẩn hóa về đầu ngày.
         */
        var timelineStart =
            startOfDay(
                earliestTaskStart
            )

        var timelineEnd =
            startOfDay(
                latestTaskEnd
            )

        /*
         * Cho thêm vài ngày trước task đầu tiên
         * để giao diện dễ nhìn hơn.
         */
        timelineStart =
            addDays(
                timelineStart,
                -EXTRA_DAYS_BEFORE
            )

        /*
         * Cho thêm vài ngày sau deadline cuối.
         */
        timelineEnd =
            addDays(
                timelineEnd,
                EXTRA_DAYS_AFTER
            )

        createDateHeader(
            timelineStart,
            timelineEnd
        )

        /*
         * Adapter cần biết timeline bắt đầu
         * từ ngày nào để tính leftMargin.
         */
        timelineAdapter =
            TimelineAdapter(
                timelineStart
            )

        binding.rvTimeline.adapter =
            timelineAdapter

        /*
         * Sắp xếp task theo Start Date.
         */
        timelineAdapter?.submitList(
            tasks.sortedBy {
                it.startDateTime
            }
        )
    }

    // ─────────────────────────────────────────────
    // Date Header
    // ─────────────────────────────────────────────

    private fun createDateHeader(
        timelineStart: Long,
        timelineEnd: Long
    ) {

        binding.dateHeaderContainer
            .removeAllViews()

        /*
         * Vì item timeline có 140dp dành cho
         * tên Task nên Header cũng cần khoảng
         * trống 140dp ở bên trái.
         */
        val taskNameSpacer =
            View(
                requireContext()
            )

        taskNameSpacer.layoutParams =
            ViewGroup.LayoutParams(
                dpToPx(140),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        binding.dateHeaderContainer
            .addView(
                taskNameSpacer
            )

        val totalDays =
            daysBetween(
                timelineStart,
                timelineEnd
            )

        for (dayIndex in 0..totalDays) {

            val currentDate =
                addDays(
                    timelineStart,
                    dayIndex.toInt()
                )

            val dateText =
                TextView(
                    requireContext()
                ).apply {

                    text =
                        dayFormat.format(
                            Date(currentDate)
                        )

                    gravity =
                        android.view.Gravity.CENTER

                    textSize =
                        12f

                    layoutParams =
                        ViewGroup.LayoutParams(
                            dpToPx(
                                DAY_WIDTH_DP
                            ),
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                }

            binding.dateHeaderContainer
                .addView(
                    dateText
                )
        }
    }

    // ─────────────────────────────────────────────
    // Date Utilities
    // ─────────────────────────────────────────────

    private fun startOfDay(
        millis: Long
    ): Long {

        val calendar =
            Calendar.getInstance().apply {

                timeInMillis =
                    millis

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        return calendar.timeInMillis
    }

    private fun addDays(
        millis: Long,
        days: Int
    ): Long {

        val calendar =
            Calendar.getInstance().apply {

                timeInMillis =
                    millis

                add(
                    Calendar.DAY_OF_MONTH,
                    days
                )
            }

        return calendar.timeInMillis
    }

    private fun daysBetween(
        startMillis: Long,
        endMillis: Long
    ): Long {

        return TimeUnit.MILLISECONDS
            .toDays(
                endMillis -
                    startMillis
            )
    }

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
            dp *
                resources.displayMetrics.density
            ).toInt()
    }

    // ─────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────

    override fun onDestroyView() {

        super.onDestroyView()

        binding.rvTimeline.adapter =
            null

        timelineAdapter =
            null

        _binding =
            null
    }
}