package com.uth.taskmanagement.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.uth.taskmanagement.databinding.FragmentCalendarBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCalendarView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = TaskCalendarAdapter { _, _ ->
            // TODO: Connect completion update after calendar edit flow is finalized.
        }
        binding.rvTasksOfDay.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CalendarFragment.adapter
        }
    }

    private fun setupCalendarView() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            Toast.makeText(requireContext(), "Task form will be connected by the task-form module.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                        binding.tvTaskCount.text = "${occurrences.size} tasks"
                    }
                }
            }
        }
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