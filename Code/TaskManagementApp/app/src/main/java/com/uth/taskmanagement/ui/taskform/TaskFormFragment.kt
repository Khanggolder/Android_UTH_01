package com.uth.taskmanagement.ui.taskform

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uth.taskmanagement.R
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.databinding.FragmentTaskFormBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskFormFragment : Fragment() {

    companion object {

        private const val ARG_TASK_ID = "arg_task_id"

        fun newInstance(taskId: Long = -1L): TaskFormFragment {

            return TaskFormFragment().apply {

                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId)
                }
            }
        }
    }

    private var _binding: FragmentTaskFormBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var viewModel: TaskFormViewModel

    private val dateTimeFormat =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        )

    // Permission launcher cho Android 13+
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // Người dùng đã trả lời permission.
        }

    // ─────────────────────────────────────────────────────────────
    // Fragment lifecycle
    // ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentTaskFormBinding.inflate(
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

        setupTaskMode()

        setupClickListeners()

        observeState()
    }

    // ─────────────────────────────────────────────────────────────
    // ViewModel
    // ─────────────────────────────────────────────────────────────

    private fun setupViewModel() {

        val app =
            requireActivity().application
                    as TaskManagementApp

        viewModel =
            ViewModelProvider(
                this,
                TaskFormViewModelFactory(
                    app,
                    app.taskRepository
                )
            )[TaskFormViewModel::class.java]
    }

    // ─────────────────────────────────────────────────────────────
    // Create / Edit mode
    // ─────────────────────────────────────────────────────────────

    private fun setupTaskMode() {

        val taskId =
            arguments?.getLong(
                ARG_TASK_ID,
                -1L
            ) ?: -1L

        if (taskId > 0L) {

            binding.tvFormTitle.text =
                "Edit Task"

            viewModel.loadTask(taskId)

        } else {

            binding.tvFormTitle.text =
                "Create Task"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Click listeners
    // ─────────────────────────────────────────────────────────────

    private fun setupClickListeners() {

        // Start Date
        binding.btnStartDateTime.setOnClickListener {

            showStartDateTimePicker()
        }

        // Due Date
        binding.btnDueDateTime.setOnClickListener {

            showDueDateTimePicker()
        }

        // Priority
        binding.btnPriorityLow.setOnClickListener {

            viewModel.setPriority(
                TaskPriority.LOW
            )
        }

        binding.btnPriorityMedium.setOnClickListener {

            viewModel.setPriority(
                TaskPriority.MEDIUM
            )
        }

        binding.btnPriorityHigh.setOnClickListener {

            viewModel.setPriority(
                TaskPriority.HIGH
            )
        }

        // Status
        binding.btnStatusPending.setOnClickListener {

            viewModel.setStatus(
                TaskStatus.PENDING
            )
        }

        binding.btnStatusInProgress.setOnClickListener {

            viewModel.setStatus(
                TaskStatus.IN_PROGRESS
            )
        }

        binding.btnStatusCompleted.setOnClickListener {

            viewModel.setStatus(
                TaskStatus.COMPLETED
            )
        }

        // Reminder
        binding.btnReminderTime.setOnClickListener {

            requestNotificationPermissionIfNeeded()

            showReminderTimePicker()
        }

        // Clear reminder
        binding.tvClearReminder.setOnClickListener {

            viewModel.clearReminder()
        }

        // Recurrence
        binding.btnRecurrenceNone.setOnClickListener {

            viewModel.setRecurrenceType(
                RecurrenceType.NONE
            )
        }

        binding.btnRecurrenceDaily.setOnClickListener {

            viewModel.setRecurrenceType(
                RecurrenceType.DAILY
            )
        }

        binding.btnRecurrenceWeekly.setOnClickListener {

            viewModel.setRecurrenceType(
                RecurrenceType.WEEKLY
            )
        }

        binding.btnRecurrenceMonthly.setOnClickListener {

            viewModel.setRecurrenceType(
                RecurrenceType.MONTHLY
            )
        }

        // Title
        binding.etTitle.addTextChangedListener(

            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    viewModel.setTitle(
                        s?.toString() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )

        // Description
        binding.etDescription.addTextChangedListener(

            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    viewModel.setDescription(
                        s?.toString() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )

        // Save
        binding.btnSave.setOnClickListener {

            viewModel.saveTask()
        }

        // Complete
        binding.btnComplete.setOnClickListener {

            viewModel.markCompleted()
        }

        // Delete
        binding.btnDelete.setOnClickListener {

            showDeleteConfirmationDialog()
        }

        // Cancel
        binding.btnCancel.setOnClickListener {

            navigateBack()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Start Date / Time
    // ─────────────────────────────────────────────────────────────

    private fun showStartDateTimePicker() {

        val cal =
            Calendar.getInstance().apply {

                timeInMillis =
                    viewModel.formState
                        .value
                        .startDateTime
            }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                cal.set(
                    Calendar.YEAR,
                    year
                )

                cal.set(
                    Calendar.MONTH,
                    month
                )

                cal.set(
                    Calendar.DAY_OF_MONTH,
                    dayOfMonth
                )

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->

                        cal.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                        )

                        cal.set(
                            Calendar.MINUTE,
                            minute
                        )

                        cal.set(
                            Calendar.SECOND,
                            0
                        )

                        cal.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        viewModel.setStartDateTime(
                            cal.timeInMillis
                        )

                    },
                    cal.get(
                        Calendar.HOUR_OF_DAY
                    ),
                    cal.get(
                        Calendar.MINUTE
                    ),
                    true
                ).show()

            },
            cal.get(
                Calendar.YEAR
            ),
            cal.get(
                Calendar.MONTH
            ),
            cal.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }

    // ─────────────────────────────────────────────────────────────
    // Due Date / Time
    // ─────────────────────────────────────────────────────────────

    private fun showDueDateTimePicker() {

        val cal =
            Calendar.getInstance().apply {

                timeInMillis =
                    viewModel.formState
                        .value
                        .dueDateTime
            }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                cal.set(
                    Calendar.YEAR,
                    year
                )

                cal.set(
                    Calendar.MONTH,
                    month
                )

                cal.set(
                    Calendar.DAY_OF_MONTH,
                    dayOfMonth
                )

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->

                        cal.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                        )

                        cal.set(
                            Calendar.MINUTE,
                            minute
                        )

                        cal.set(
                            Calendar.SECOND,
                            0
                        )

                        cal.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        viewModel.setDueDateTime(
                            cal.timeInMillis
                        )

                    },
                    cal.get(
                        Calendar.HOUR_OF_DAY
                    ),
                    cal.get(
                        Calendar.MINUTE
                    ),
                    true
                ).show()

            },
            cal.get(
                Calendar.YEAR
            ),
            cal.get(
                Calendar.MONTH
            ),
            cal.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }

    // ─────────────────────────────────────────────────────────────
    // Reminder Date / Time
    // ─────────────────────────────────────────────────────────────

    private fun showReminderTimePicker() {

        val currentReminderTime =
            viewModel.formState
                .value
                .reminderTime
                ?: System.currentTimeMillis()

        val cal =
            Calendar.getInstance().apply {

                timeInMillis =
                    currentReminderTime
            }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                cal.set(
                    Calendar.YEAR,
                    year
                )

                cal.set(
                    Calendar.MONTH,
                    month
                )

                cal.set(
                    Calendar.DAY_OF_MONTH,
                    dayOfMonth
                )

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->

                        cal.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                        )

                        cal.set(
                            Calendar.MINUTE,
                            minute
                        )

                        cal.set(
                            Calendar.SECOND,
                            0
                        )

                        cal.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        viewModel.setReminderTime(
                            cal.timeInMillis
                        )

                    },
                    cal.get(
                        Calendar.HOUR_OF_DAY
                    ),
                    cal.get(
                        Calendar.MINUTE
                    ),
                    true
                ).show()

            },
            cal.get(
                Calendar.YEAR
            ),
            cal.get(
                Calendar.MONTH
            ),
            cal.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }

    // ─────────────────────────────────────────────────────────────
    // Observe ViewModel state
    // ─────────────────────────────────────────────────────────────

    private fun observeState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.formState.collect { state ->

                    updateFormFields(state)

                    updatePriorityButtons(
                        state.priority
                    )

                    updateStatusButtons(
                        state.status
                    )

                    updateEditButtons(
                        state.taskId,
                        state.status
                    )

                    updateReminderUI(
                        state.reminderTime,
                        state.recurrenceType
                    )

                    updateErrorUI(
                        state.errorMessage
                    )

                    updateLoadingUI(
                        state.isLoading
                    )

                    if (state.isSaved) {

                        navigateBack()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Form UI
    // ─────────────────────────────────────────────────────────────

    private fun updateFormFields(
        state: TaskFormState
    ) {

        if (
            !binding.etTitle.isFocused &&
            binding.etTitle.text.toString() != state.title
        ) {

            binding.etTitle.setText(
                state.title
            )

            binding.etTitle.setSelection(
                state.title.length
            )
        }

        if (
            !binding.etDescription.isFocused &&
            binding.etDescription.text.toString() !=
            state.description
        ) {

            binding.etDescription.setText(
                state.description
            )
        }

        binding.tvStartDateTime.text =
            dateTimeFormat.format(
                Date(
                    state.startDateTime
                )
            )

        binding.tvDueDateTime.text =
            dateTimeFormat.format(
                Date(
                    state.dueDateTime
                )
            )
    }

    // ─────────────────────────────────────────────────────────────
    // Edit mode UI
    // ─────────────────────────────────────────────────────────────

    private fun updateEditButtons(
        taskId: Long,
        status: TaskStatus
    ) {

        val isEditMode =
            taskId > 0L

        binding.btnDelete.visibility =
            if (isEditMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnComplete.visibility =
            if (
                isEditMode &&
                status != TaskStatus.COMPLETED
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    // ─────────────────────────────────────────────────────────────
    // Reminder UI
    // ─────────────────────────────────────────────────────────────

    private fun updateReminderUI(
        reminderTime: Long?,
        recurrenceType: RecurrenceType
    ) {

        if (reminderTime != null) {

            binding.tvReminderTime.text =
                dateTimeFormat.format(
                    Date(reminderTime)
                )

            binding.tvReminderTime.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_primary
                )
            )

            binding.tvClearReminder.visibility =
                View.VISIBLE

            binding.labelRecurrence.visibility =
                View.VISIBLE

            binding.recurrenceGroup.visibility =
                View.VISIBLE

            updateRecurrenceButtons(
                recurrenceType
            )

        } else {

            binding.tvReminderTime.text =
                "No reminder set"

            binding.tvReminderTime.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_secondary
                )
            )

            binding.tvClearReminder.visibility =
                View.GONE

            binding.labelRecurrence.visibility =
                View.GONE

            binding.recurrenceGroup.visibility =
                View.GONE
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Error UI
    // ─────────────────────────────────────────────────────────────

    private fun updateErrorUI(
        errorMessage: String?
    ) {

        if (errorMessage != null) {

            binding.tvError.text =
                errorMessage

            binding.tvError.visibility =
                View.VISIBLE

        } else {

            binding.tvError.visibility =
                View.GONE
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Loading UI
    // ─────────────────────────────────────────────────────────────

    private fun updateLoadingUI(
        isLoading: Boolean
    ) {

        binding.btnSave.isEnabled =
            !isLoading

        binding.btnComplete.isEnabled =
            !isLoading

        binding.btnDelete.isEnabled =
            !isLoading

        binding.btnSave.text =
            if (isLoading) {
                "Saving…"
            } else {
                "Save Task"
            }
    }

    // ─────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────

    private fun showDeleteConfirmationDialog() {

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                "Delete Task"
            )
            .setMessage(
                "Are you sure you want to delete this task?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                viewModel.deleteTask()
            }
            .show()
    }

    // ─────────────────────────────────────────────────────────────
    // Priority UI
    // ─────────────────────────────────────────────────────────────

    private fun updatePriorityButtons(
        priority: TaskPriority
    ) {

        val accent =
            ContextCompat.getColor(
                requireContext(),
                R.color.accent
            )

        val secondary =
            ContextCompat.getColor(
                requireContext(),
                R.color.text_secondary
            )

        fun applyStyle(
            button: MaterialButton,
            active: Boolean
        ) {

            button.setStrokeColorResource(
                if (active) {
                    R.color.accent
                } else {
                    R.color.divider
                }
            )

            button.setTextColor(
                if (active) {
                    accent
                } else {
                    secondary
                }
            )
        }

        applyStyle(
            binding.btnPriorityLow,
            priority == TaskPriority.LOW
        )

        applyStyle(
            binding.btnPriorityMedium,
            priority == TaskPriority.MEDIUM
        )

        applyStyle(
            binding.btnPriorityHigh,
            priority == TaskPriority.HIGH
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Status UI
    // ─────────────────────────────────────────────────────────────

    private fun updateStatusButtons(
        status: TaskStatus
    ) {

        val accent =
            ContextCompat.getColor(
                requireContext(),
                R.color.accent
            )

        val secondary =
            ContextCompat.getColor(
                requireContext(),
                R.color.text_secondary
            )

        fun applyStyle(
            button: MaterialButton,
            active: Boolean
        ) {

            button.setStrokeColorResource(
                if (active) {
                    R.color.accent
                } else {
                    R.color.divider
                }
            )

            button.setTextColor(
                if (active) {
                    accent
                } else {
                    secondary
                }
            )
        }

        applyStyle(
            binding.btnStatusPending,
            status == TaskStatus.PENDING
        )

        applyStyle(
            binding.btnStatusInProgress,
            status == TaskStatus.IN_PROGRESS
        )

        applyStyle(
            binding.btnStatusCompleted,
            status == TaskStatus.COMPLETED
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Recurrence UI
    // ─────────────────────────────────────────────────────────────

    private fun updateRecurrenceButtons(
        type: RecurrenceType
    ) {

        val accent =
            ContextCompat.getColor(
                requireContext(),
                R.color.accent
            )

        val secondary =
            ContextCompat.getColor(
                requireContext(),
                R.color.text_secondary
            )

        fun applyStyle(
            button: MaterialButton,
            active: Boolean
        ) {

            button.setStrokeColorResource(
                if (active) {
                    R.color.accent
                } else {
                    R.color.divider
                }
            )

            button.setTextColor(
                if (active) {
                    accent
                } else {
                    secondary
                }
            )
        }

        applyStyle(
            binding.btnRecurrenceNone,
            type == RecurrenceType.NONE
        )

        applyStyle(
            binding.btnRecurrenceDaily,
            type == RecurrenceType.DAILY
        )

        applyStyle(
            binding.btnRecurrenceWeekly,
            type == RecurrenceType.WEEKLY
        )

        applyStyle(
            binding.btnRecurrenceMonthly,
            type == RecurrenceType.MONTHLY
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Notification permission
    // ─────────────────────────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val status =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                status !=
                PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────

    private fun navigateBack() {

        parentFragmentManager
            .popBackStack()
    }

    // ─────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}