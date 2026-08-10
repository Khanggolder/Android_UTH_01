package com.uth.taskmanagement.ui.tasklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.uth.taskmanagement.TaskManagementApp
import com.uth.taskmanagement.databinding.FragmentTaskListBinding
import com.uth.taskmanagement.ui.taskform.TaskFormFragment
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TaskAdapter
    private lateinit var viewModel: TaskListViewModel

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}