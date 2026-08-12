package com.uth.taskmanagement.ui.tasklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.databinding.FragmentTaskListBinding
import com.uth.taskmanagement.ui.taskform.TaskFormFragment
import com.uth.taskmanagement.R
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.databinding.FragmentTaskListBinding
import com.uth.taskmanagement.utils.TaskDueDateFilter
import com.uth.taskmanagement.utils.TaskPriorityFilter
import com.uth.taskmanagement.utils.TaskSortOption
import com.uth.taskmanagement.utils.TaskStatusFilter
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TaskAdapter
    private lateinit var viewModel: TaskListViewModel

    private var currentStatusFilter = TaskStatusFilter.ALL

    private var currentPriorityFilter = TaskPriorityFilter.ALL

    private var currentDueDateFilter = TaskDueDateFilter.ALL

    private var currentSort = TaskSortOption.DUE_DATE_SOONEST_FIRST

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as TaskManagementApp
        viewModel = ViewModelProvider(
            this,
            TaskListViewModelFactory(app.taskRepository)
        )[TaskListViewModel::class.java]

        setupAdapter()
        setupFab()
        observeTasks()
    }

    private fun setupAdapter() {
        adapter = TaskAdapter(
            onItemClick = { task ->
                // Open edit form for the tapped task
                navigateToForm(task.id)
            },
            onCompletedChanged = { task, isChecked ->
                viewModel.setTaskCompleted(
                    context = requireContext(),
                    taskId = task.id,
                    completed = isChecked
                )
            }
        )
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            navigateToForm(taskId = -1L)
        }
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasks.collect { tasks ->
                    adapter.submitList(tasks)
                    binding.tvTotalTasks.text = tasks.size.toString()
                    binding.tvOpenTasks.text = tasks.count { !it.isCompleted }.toString()
                    binding.tvEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun navigateToForm(taskId: Long) {
        val fragment = TaskFormFragment.newInstance(taskId)
        parentFragmentManager.beginTransaction()
            .replace(com.uth.taskmanagement.R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        adapter = TaskAdapter()
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        binding.fabAddTask.setOnClickListener {
            Toast.makeText(requireContext(), "Task form will be connected by the task-form module.", Toast.LENGTH_SHORT).show()
        }

        setupFilerChips()
        observeTotals()
        observeUiState()
    }

    private fun setupFilerChips() {
        binding.btnFilterStatus.setOnClickListener { showStatusFilterMenu() }
        binding.btnFilterPriority.setOnClickListener {
            showPriorityFilterMenu() }

        binding.btnFilterDueDate.setOnClickListener { showDueDateFilterMenu() }
        binding.btnSort.setOnClickListener { showSortMenu() }
        binding.btnClearAll.setOnClickListener { clearAllFilters() }

        updateChipLabels()
    }

    private fun showStatusFilterMenu() {
        val popup = PopupMenu(requireContext(), binding.btnFilterStatus)

        popup.menuInflater.inflate(R.menu.menu_filter_status, popup.menu)
        checkCurrentItem(popup, currentStatusFilter.ordinal)
        popup.setOnMenuItemClickListener { item -> currentStatusFilter = when(item.itemId) {
            R.id.menuStatusPending -> TaskStatusFilter.PENDING
            R.id.menuStatusInProgress -> TaskStatusFilter.IN_PROGRESS
            R.id.menuStatusCompleted -> TaskStatusFilter.COMPLETED
            else -> TaskStatusFilter.ALL
        }
            viewModel.setStatusFilter(currentStatusFilter)
            updateChipLabels()
            true
        }
        popup.show()
    }

    private fun showPriorityFilterMenu() {
        val popup = PopupMenu(requireContext(), binding.btnFilterPriority)
        popup.menuInflater.inflate(R.menu.menu_filter_priority, popup.menu)
        checkCurrentItem(popup, currentPriorityFilter.ordinal)
        popup.setOnMenuItemClickListener { item ->
            currentPriorityFilter = when (item.itemId) {
                R.id.menuPriorityLow -> TaskPriorityFilter.LOW
                R.id.menuPriorityMedium -> TaskPriorityFilter.MEDIUM
                R.id.menuPriorityHigh -> TaskPriorityFilter.HIGH
                else -> TaskPriorityFilter.ALL
            }
            viewModel.setPriorityFilter(currentPriorityFilter)
            updateChipLabels()
            true
        }
        popup.show()
    }

    private fun showDueDateFilterMenu() {
        val popup = PopupMenu(requireContext(), binding.btnFilterDueDate)
        popup.menuInflater.inflate(R.menu.menu_filter_due_date, popup.menu)
        checkCurrentItem(popup, currentDueDateFilter.ordinal)
        popup.setOnMenuItemClickListener { item ->
            currentDueDateFilter = when (item.itemId) {
                R.id.menuDueDateOverdue -> TaskDueDateFilter.OVERDUE
                R.id.menuDueDateToday -> TaskDueDateFilter.DUE_TODAY
                R.id.menuDueDateUpcoming -> TaskDueDateFilter.UPCOMING
                else -> TaskDueDateFilter.ALL
            }
            viewModel.setDueDateFilter(currentDueDateFilter)
            updateChipLabels()
            true
        }
        popup.show()
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.btnSort)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
        checkCurrentItem(popup, currentSort.ordinal)
        popup.setOnMenuItemClickListener { item ->
            currentSort = when (item.itemId) {
                R.id.menuSortDueLatest -> TaskSortOption.DUE_DATE_LATEST_FIRST
                R.id.menuSortPriority -> TaskSortOption.PRIORITY_HIGH_TO_LOW
                else -> TaskSortOption.DUE_DATE_SOONEST_FIRST
            }
            viewModel.setSortOption(currentSort)
            updateChipLabels()
            true
        }
        popup.show()
    }

    private fun checkCurrentItem(popup: PopupMenu, checkedIndex: Int) {
        val menu = popup.menu
        if (checkedIndex in 0 until menu.size()) {
            menu.getItem(checkedIndex).isChecked = true
        }
    }

    private fun clearAllFilters() {
        currentStatusFilter = TaskStatusFilter.ALL
        currentPriorityFilter = TaskPriorityFilter.ALL
        currentDueDateFilter = TaskDueDateFilter.ALL
        viewModel.setStatusFilter(currentStatusFilter)
        viewModel.setPriorityFilter(currentPriorityFilter)
        viewModel.setDueDateFilter(currentDueDateFilter)
        updateChipLabels()
    }

    private fun updateChipLabels() {
        binding.btnFilterStatus.text = buildChipText("Trạng thái", statusLabel(currentStatusFilter))
        binding.btnFilterPriority.text = buildChipText("Ưu tiên", priorityLabel(currentPriorityFilter))
        binding.btnFilterDueDate.text = buildChipText("Hạn", dueDateLabel(currentDueDateFilter))
        binding.btnSort.text = buildChipText("Sắp xếp", sortLabel(currentSort))

        val hasAnyFilter = currentStatusFilter != TaskStatusFilter.ALL ||
                currentPriorityFilter != TaskPriorityFilter.ALL ||
                currentDueDateFilter != TaskDueDateFilter.ALL
        binding.btnClearAll.visibility = if (hasAnyFilter) View.VISIBLE else View.GONE
    }

    private fun buildChipText(label: String, value: String?): String {
        return if (value == null) "$label ▾" else "$label: $value ▾"
    }

    private fun statusLabel(filter: TaskStatusFilter): String? = when (filter) {
        TaskStatusFilter.ALL -> null
        TaskStatusFilter.PENDING -> "Chưa làm"
        TaskStatusFilter.IN_PROGRESS -> "Đang làm"
        TaskStatusFilter.COMPLETED -> "Đã xong"
    }

    private fun priorityLabel(filter: TaskPriorityFilter): String? = when (filter) {
        TaskPriorityFilter.ALL -> null
        TaskPriorityFilter.LOW -> "Thấp"
        TaskPriorityFilter.MEDIUM -> "Trung bình"
        TaskPriorityFilter.HIGH -> "Cao"
    }

    private fun dueDateLabel(filter: TaskDueDateFilter): String? = when (filter) {
        TaskDueDateFilter.ALL -> null
        TaskDueDateFilter.OVERDUE -> "Quá hạn"
        TaskDueDateFilter.DUE_TODAY -> "Hôm nay"
        TaskDueDateFilter.UPCOMING -> "Sắp tới"
    }

    private fun sortLabel(sort: TaskSortOption): String = when (sort) {
        TaskSortOption.DUE_DATE_SOONEST_FIRST -> "Hạn gần nhất"
        TaskSortOption.DUE_DATE_LATEST_FIRST -> "Hạn xa nhất"
        TaskSortOption.PRIORITY_HIGH_TO_LOW -> "Ưu tiên cao trước"
    }


    private fun observeTotals() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasks.collect { tasks ->
                    binding.tvTotalTasks.text = tasks.size.toString()
                    binding.tvOpenTasks.text = tasks.count { !it.isCompleted }.toString()
                }
            }
        }
    }


    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.recyclerViewTasks.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE

                    when (state) {
                        is TaskListUiState.Loading -> {
                            // TODO: hien progress bar rieng neu can
                        }

                        is TaskListUiState.Success -> {
                            binding.recyclerViewTasks.visibility = View.VISIBLE
                            adapter.submitList(state.tasks, state.overdueTaskIds)
                        }

                        is TaskListUiState.Empty -> {
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmptyTitle.text = if (state.isBecauseOfFilter) {
                                "Không có công việc khớp bộ lọc"
                            } else {
                                "No tasks yet"
                            }
                        }

                        is TaskListUiState.Error -> {
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmptyTitle.text = state.message
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}