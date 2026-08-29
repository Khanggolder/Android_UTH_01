package com.uth.taskmanagement

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.uth.taskmanagement.databinding.ActivityMainBinding
import com.uth.taskmanagement.notification.NotificationHelper
import com.uth.taskmanagement.security.PinLoginFragment
import com.uth.taskmanagement.ui.calendar.CalendarFragment
import com.uth.taskmanagement.ui.settings.SettingsFragment
import com.uth.taskmanagement.ui.tasklist.TaskListFragment
import com.uth.taskmanagement.ui.timeline.TimelineFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            val message =
                if (isGranted) {
                    "Notification permission granted"
                } else {
                    "Notifications are disabled. Reminders can be enabled later."
                }

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding =
            ActivityMainBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.main
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )

            binding.bottomNavigation.setPadding(
                0,
                0,
                0,
                systemBars.bottom
            )

            insets
        }

        NotificationHelper
            .createNotificationChannel(this)

        setupNavigation()

        if (savedInstanceState == null) {
            checkPinAndStart()
        }

        requestNotificationPermission()
    }

    // ─────────────────────────────────────────────
    // Bottom Navigation
    // ─────────────────────────────────────────────

    private fun setupNavigation() {

        binding.bottomNavigation
            .setOnItemSelectedListener { item ->

                val fragment =
                    when (item.itemId) {

                        R.id.nav_tasks ->
                            TaskListFragment()

                        R.id.nav_calendar ->
                            CalendarFragment()

                        R.id.nav_timeline ->
                            TimelineFragment()

                        R.id.nav_settings ->
                            SettingsFragment()

                        else ->
                            null
                    }

                fragment?.let {
                    showFragment(it)
                }

                fragment != null
            }
    }

    // ─────────────────────────────────────────────
    // Fragment Navigation
    // ─────────────────────────────────────────────

    private fun showFragment(
        fragment: Fragment
    ) {

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()
    }

    // ─────────────────────────────────────────────
    // Notification Permission
    // ─────────────────────────────────────────────

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permissionStatus =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permissionStatus !=
                PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // PIN
    // ─────────────────────────────────────────────

    private fun checkPinAndStart() {

        val app =
            application as TaskManagementApp

        lifecycleScope.launch {

            val isPinEnabled =
                app.pinPreferences
                    .isPinEnabled
                    .first()

            if (isPinEnabled) {

                binding.bottomNavigation.visibility =
                    View.GONE

                showFragment(
                    PinLoginFragment()
                )

            } else {

                binding.bottomNavigation.selectedItemId =
                    R.id.nav_tasks

                showFragment(
                    TaskListFragment()
                )
            }
        }
    }

    fun onPinLoginSuccess() {

        binding.bottomNavigation.visibility =
            View.VISIBLE

        binding.bottomNavigation.selectedItemId =
            R.id.nav_tasks

        showFragment(
            TaskListFragment()
        )
    }
}