package com.uth.taskmanagement.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.uth.taskmanagement.R
import com.uth.taskmanagement.databinding.FragmentCalendarBinding
import com.uth.taskmanagement.ui.taskform.TaskFormFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(
            repo = (requireActivity().application as com.uth.taskmanagement.TaskManagementApp).taskRepository
        )
    }

    private lateinit var adapter: TaskCalendarAdapter
    private lateinit var gridAdapter: CalendarGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCalendarGrid()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = TaskCalendarAdapter { occurrence, isChecked ->
            viewModel.setTaskCompleted(
                context = requireContext(),
                taskId = occurrence.task.id,
                completed = isChecked
            )
        }
        binding.rvTasksOfDay.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CalendarFragment.adapter
        }
    }

    private fun setupCalendarGrid() {
        gridAdapter = CalendarGridAdapter { date ->
            viewModel.onDateSelected(date)
        }
        binding.rvCalendarGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = gridAdapter
        }

        binding.btnPrevMonth.setOnClickListener { viewModel.goToPreviousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.goToNextMonth() }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TaskFormFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.displayedMonth.collect { month ->
                        binding.tvMonthYear.text = formatMonthYear(month)
                    }
                }
                launch {
                    viewModel.monthDays.collect { days ->
                        gridAdapter.submitDays(days, viewModel.displayedMonth.value)
                    }
                }
                launch {
                    viewModel.selectedDate.collect { date ->
                        binding.tvSelectedDate.text = formatDateLabel(date)
                    }
                }
                launch {
                    viewModel.tasksForSelectedDay.collect { occurrences ->
                        adapter.submitList(occurrences)
                        val isEmpty = occurrences.isEmpty()
                        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.rvTasksOfDay.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.tvTaskCount.text = if (occurrences.size == 1) {
                            "1 event"
                        } else {
                            "${occurrences.size} events"
                        }
                    }
                }
            }
        }
    }

    private fun formatMonthYear(month: YearMonth): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        return month.format(formatter).uppercase(Locale.ENGLISH)
    }
    private fun formatDateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Today, " + formatFull(date)
            today.plusDays(1) -> "Tomorrow, " + formatFull(date)
            else -> formatFull(date)
        }
    }

    private fun formatFull(date: LocalDate): String {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(millis))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
