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
import androidx.recyclerview.widget.RecyclerView

class TimelineFragment : Fragment() {

    companion object {

        private const val DAY_WIDTH_DP = 80

        // Tạm thời hiển thị thêm một ít ngày
        // trước và sau các task.
        private const val EXTRA_DAYS_BEFORE = 2
        private const val EXTRA_DAYS_AFTER = 5
        private const val MAX_TIMELINE_DAYS = 180
    }

    private var taskNameAdapter: TimelineTaskNameAdapter? = null

    private var _binding: FragmentTimelineBinding? = null

    private var isSyncingScroll = false
    private var isSyncingHorizontalScroll = false

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

        syncVerticalScroll()

        syncHorizontalScroll()

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

        binding.rvTaskNames.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvTimeline.layoutManager =
            LinearLayoutManager(requireContext())

        taskNameAdapter =
            TimelineTaskNameAdapter()

        binding.rvTaskNames.adapter =
            taskNameAdapter
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

                taskNameAdapter?.submitList(
                    emptyList()
                )

                timelineAdapter?.submitList(
                    emptyList()
                )

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

        val normalizedTasks =
            tasks.map { task ->

                val safeStart =
                    when {
                        task.startDateTime > 0L ->
                            task.startDateTime

                        task.createdAt > 0L ->
                            task.createdAt

                        else ->
                            task.dueDateTime
                    }

                val safeDue =
                    if (task.dueDateTime >= safeStart) {
                        task.dueDateTime
                    } else {
                        safeStart
                    }

                task.copy(
                    startDateTime = safeStart,
                    dueDateTime = safeDue
                )
            }

        val earliestTaskStart =
            normalizedTasks.minOf {
                it.startDateTime
            }

        val latestTaskEnd =
            normalizedTasks.maxOf {
                it.dueDateTime
            }

        var timelineStart =
            startOfDay(
                earliestTaskStart
            )

        var timelineEnd =
            startOfDay(
                latestTaskEnd
            )

        val timelineDays =
            daysBetween(
                timelineStart,
                timelineEnd
            ) + 1

        if (timelineDays > MAX_TIMELINE_DAYS) {

            timelineEnd =
                addDays(
                    timelineStart,
                    MAX_TIMELINE_DAYS - 1
                )
        }

        val today =
            startOfDay(
                System.currentTimeMillis()
            )

        timelineStart =
            addDays(
                timelineStart,
                -EXTRA_DAYS_BEFORE
            )

        timelineEnd =
            addDays(
                timelineEnd,
                EXTRA_DAYS_AFTER
            )

        val totalDays =
            daysBetween(
                timelineStart,
                timelineEnd
            ).toInt() + 1

        createDateHeader(
            timelineStart,
            timelineEnd
        )

        timelineAdapter =
            TimelineAdapter(
                timelineStart,
                totalDays,
                today
            )
        binding.rvTimeline.adapter =
            timelineAdapter

        val sortedTasks =
            normalizedTasks.sortedBy {
                it.startDateTime
            }

        taskNameAdapter?.submitList(
            sortedTasks
        )

        timelineAdapter?.submitList(
            sortedTasks
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

        val totalDays =
            daysBetween(
                timelineStart,
                timelineEnd
            )

        val today =
            startOfDay(
                System.currentTimeMillis()
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

            if (currentDate == today) {

                dateText.setBackgroundColor(
                    android.graphics.Color.parseColor(
                        "#E3F2FD"
                    )
                )

                dateText.setTextColor(
                    android.graphics.Color.parseColor(
                        "#1565C0"
                    )
                )

                dateText.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
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
    // Scrolls
    // ─────────────────────────────────────────────

    private fun syncVerticalScroll() {

    binding.rvTaskNames.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {

                if (
                    dy == 0 ||
                    isSyncingScroll ||
                    _binding == null
                ) {
                    return
                }

                isSyncingScroll = true

                binding.rvTimeline.scrollBy(
                    0,
                    dy
                )

                isSyncingScroll = false
            }
        }
    )

    binding.rvTimeline.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {

                if (
                    dy == 0 ||
                    isSyncingScroll ||
                    _binding == null
                ) {
                    return
                }

                isSyncingScroll = true

                binding.rvTaskNames.scrollBy(
                    0,
                    dy
                )

                isSyncingScroll = false
            }
        }
    )
}

    private fun syncHorizontalScroll() {

        binding.timelineScroll.setOnScrollChangeListener {
                _,
                scrollX,
                _,
                _,
                _ ->

            if (
                isSyncingHorizontalScroll ||
                _binding == null
            ) {
                return@setOnScrollChangeListener
            }

            isSyncingHorizontalScroll = true

            binding.headerScroll.scrollTo(
                scrollX,
                0
            )

            isSyncingHorizontalScroll = false
        }

        binding.headerScroll.setOnScrollChangeListener {
                _,
                scrollX,
                _,
                _,
                _ ->

            if (
                isSyncingHorizontalScroll ||
                _binding == null
            ) {
                return@setOnScrollChangeListener
            }

            isSyncingHorizontalScroll = true

            binding.timelineScroll.scrollTo(
                scrollX,
                0
            )

            isSyncingHorizontalScroll = false
        }
    }

    override fun onDestroyView() {

        binding.timelineScroll.setOnScrollChangeListener(
            null as View.OnScrollChangeListener?
        )

        binding.headerScroll.setOnScrollChangeListener(
            null as View.OnScrollChangeListener?
        )

        binding.rvTaskNames.clearOnScrollListeners()
        binding.rvTimeline.clearOnScrollListeners()

        binding.rvTaskNames.adapter = null
        binding.rvTimeline.adapter = null

        taskNameAdapter = null
        timelineAdapter = null

        _binding = null

        super.onDestroyView()
    }
    } 