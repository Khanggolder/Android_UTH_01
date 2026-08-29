package com.uth.taskmanagement.ui.timeline

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.R
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.databinding.FragmentTimelineBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var viewModel: TimelineViewModel

    private var taskNameAdapter: TimelineTaskNameAdapter? = null
    private var timelineAdapter: TimelineAdapter? = null

    private var isSyncingScroll = false
    private var isSyncingHorizontalScroll = false

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
            requireActivity().application as TaskManagementApp

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
            LinearLayoutManager(
                requireContext()
            )

        binding.rvTimeline.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        taskNameAdapter =
            TimelineTaskNameAdapter()

        binding.rvTaskNames.adapter =
            taskNameAdapter
    }

    // ─────────────────────────────────────────────
    // Observe tasks
    // ─────────────────────────────────────────────

    private fun observeTasks() {

        viewModel.tasks.observe(
            viewLifecycleOwner
        ) { tasks ->

            val isEmpty =
                tasks.isNullOrEmpty()

            binding.timelineContent.visibility =
                if (isEmpty) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            binding.emptyState.visibility =
                if (isEmpty) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            if (isEmpty) {

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
    // Setup timeline
    // ─────────────────────────────────────────────

    private fun setupTimeline(
        tasks: List<TaskEntity>
    ) {

        val layout =
            TimelineLayoutCalculator.calculate(
                tasks
            ) ?: return

        val today =
            TimelineLayoutCalculator.startOfDay(
                System.currentTimeMillis()
            )

        createDateHeader(
            layout.timelineStart,
            layout.totalDays
        )

        timelineAdapter =
            TimelineAdapter(
                layout.timelineStart,
                layout.totalDays,
                today
            )

        binding.rvTimeline.adapter =
            timelineAdapter

        taskNameAdapter?.submitList(
            layout.tasks
        )

        timelineAdapter?.submitList(
            layout.tasks
        )
    }

    // ─────────────────────────────────────────────
    // Date header
    // ─────────────────────────────────────────────

    private fun createDateHeader(
        timelineStart: Long,
        totalDays: Int
    ) {

        binding.dateHeaderContainer
            .removeAllViews()

        val today =
            TimelineLayoutCalculator.startOfDay(
                System.currentTimeMillis()
            )

        for (dayIndex in 0 until totalDays) {

            val currentDate =
                TimelineLayoutCalculator.addDays(
                    timelineStart,
                    dayIndex.toLong()
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
                        Gravity.CENTER

                    textSize =
                        12f

                    setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_secondary
                        )
                    )

                    setBackgroundResource(
                        R.drawable.bg_timeline_header_cell
                    )

                    layoutParams =
                        ViewGroup.LayoutParams(
                            dpToPx(
                                TimelineLayoutCalculator.DAY_WIDTH_DP
                            ),
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                }

            if (currentDate == today) {

                dateText.setBackgroundResource(
                    R.drawable.bg_timeline_today_cell
                )

                dateText.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.accent
                    )
                )

                dateText.setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            binding.dateHeaderContainer
                .addView(
                    dateText
                )
        }
    }

    // ─────────────────────────────────────────────
    // DP helper
    // ─────────────────────────────────────────────

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
            dp *
                resources.displayMetrics.density
            ).toInt()
    }

    // ─────────────────────────────────────────────
    // Vertical scroll sync
    // ─────────────────────────────────────────────

    private fun syncVerticalScroll() {

        binding.rvTaskNames.addOnScrollListener(
            object :
                RecyclerView.OnScrollListener() {

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
            object :
                RecyclerView.OnScrollListener() {

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

    // ─────────────────────────────────────────────
    // Horizontal scroll sync
    // ─────────────────────────────────────────────

    private fun syncHorizontalScroll() {

        binding.timelineScroll
            .setOnScrollChangeListener {
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

        binding.headerScroll
            .setOnScrollChangeListener {
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

    // ─────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────

    override fun onDestroyView() {

        binding.timelineScroll
            .setOnScrollChangeListener(
                null as View.OnScrollChangeListener?
            )

        binding.headerScroll
            .setOnScrollChangeListener(
                null as View.OnScrollChangeListener?
            )

        binding.rvTaskNames
            .clearOnScrollListeners()

        binding.rvTimeline
            .clearOnScrollListeners()

        binding.rvTaskNames.adapter =
            null

        binding.rvTimeline.adapter =
            null

        taskNameAdapter =
            null

        timelineAdapter =
            null

        _binding =
            null

        super.onDestroyView()
    }
}