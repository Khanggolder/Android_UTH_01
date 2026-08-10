package com.uth.taskmanagement.ui.taskform

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.databinding.FragmentTaskFormBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskFormFragment : Fragment() {

    private var _binding: FragmentTaskFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TaskFormViewModel

    private var taskId: Long = 0L
    private var selectedDueDateTime: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupSpinners()
        setupListeners()

        taskId = arguments?.getLong(ARG_TASK_ID, 0L) ?: 0L

        if (taskId != 0L) {
            loadTask()
        } else {
            binding.btnDelete.visibility = View.GONE
            binding.btnComplete.visibility = View.GONE
        }
    }

    private fun setupViewModel() {
        val app = requireActivity().application as TaskManagementApp

        val factory = TaskFormViewModelFactory(
            app.taskRepository
        )

        viewModel = ViewModelProvider(
            this,
            factory
        )[TaskFormViewModel::class.java]
    }

    private fun setupSpinners() {
        val priorities = TaskPriority.entries.map { it.name }

        binding.spPriority.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            priorities
        )

        val statuses = TaskStatus.entries.map { it.name }

        binding.spStatus.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
    }

    private fun setupListeners() {

        binding.btnDueDate.setOnClickListener {
            selectDueDateTime()
        }

        binding.btnSave.setOnClickListener {
            saveTask()
        }

        binding.btnDelete.setOnClickListener {
            if (taskId != 0L) {
                viewModel.deleteTask(taskId) {
                    Toast.makeText(
                        requireContext(),
                        "Task deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                }
            }
        }

        binding.btnComplete.setOnClickListener {
            if (taskId != 0L) {
                viewModel.setCompleted(
                    taskId = taskId,
                    completed = true
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Task completed",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun saveTask() {

        val title = binding.etTitle.text.toString()
        val description = binding.etDescription.text.toString()

        val priority = TaskPriority.entries[
            binding.spPriority.selectedItemPosition
        ]

        val status = TaskStatus.entries[
            binding.spStatus.selectedItemPosition
        ]

        val error = viewModel.validate(
            title = title,
            dueDateTime = selectedDueDateTime,
            priority = priority,
            status = status
        )

        if (error != null) {
            Toast.makeText(
                requireContext(),
                error,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewModel.saveTask(
            taskId = taskId,
            title = title,
            description = description,
            dueDateTime = selectedDueDateTime!!,
            priority = priority,
            status = status
        ) {
            Toast.makeText(
                requireContext(),
                "Task saved",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
        }
    }

    private fun loadTask() {
        viewLifecycleOwner.lifecycleScope.launch {

            val task = viewModel.getTask(taskId) ?: return@launch

            binding.etTitle.setText(task.title)
            binding.etDescription.setText(task.description)

            selectedDueDateTime = task.dueDateTime
            updateDueDateText(task.dueDateTime)

            binding.spPriority.setSelection(
                TaskPriority.entries.indexOf(task.priority)
            )

            binding.spStatus.setSelection(
                TaskStatus.entries.indexOf(task.status)
            )
        }
    }

    private fun selectDueDateTime() {

        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->

                        calendar.set(
                            year,
                            month,
                            dayOfMonth,
                            hour,
                            minute,
                            0
                        )

                        selectedDueDateTime = calendar.timeInMillis

                        updateDueDateText(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDueDateText(timeMillis: Long) {
        val formatter = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        )

        binding.btnDueDate.text =
            formatter.format(timeMillis)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_TASK_ID = "task_id"

        fun newInstance(taskId: Long = 0L): TaskFormFragment {

            val fragment = TaskFormFragment()

            fragment.arguments = Bundle().apply {
                putLong(ARG_TASK_ID, taskId)
            }

            return fragment
        }
    }
}