package com.uth.taskmanagement.backup

import android.content.Context
import android.net.Uri
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class BackupManager(
    private val repo: TaskRepository,
    private val context: Context
) {
    suspend fun exportTasks(uri: Uri): Result<Unit> = runCatching {
        val tasks = repo.observeAllTasks().first()
        val jsonArray = JSONArray()

        tasks.forEach { task ->
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("description", task.description)
            obj.put("dueDateTime", task.dueDateTime)
            obj.put("priority", task.priority.name)
            obj.put("status", task.status.name)
            obj.put("isCompleted", task.isCompleted)
            obj.put("reminderTime", task.reminderTime ?: JSONObject.NULL)
            obj.put("recurrenceType", task.recurrenceType.name)
            obj.put("createdAt", task.createdAt)
            obj.put("updatedAt", task.updatedAt)
            jsonArray.put(obj)
        }

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonArray.toString().toByteArray())
        } ?: throw IOException("Không mở được file để ghi")
    }
    suspend fun restoreTasks(uri: Uri): Result<Unit> = runCatching {
        val jsonText = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: throw IOException("Không đọc được file")

        val jsonArray = JSONArray(jsonText)
        val tasks = mutableListOf<TaskEntity>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            tasks.add(
                TaskEntity(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    dueDateTime = obj.getLong("dueDateTime"),
                    priority = TaskPriority.valueOf(obj.getString("priority")),
                    status = TaskStatus.valueOf(obj.getString("status")),
                    isCompleted = obj.getBoolean("isCompleted"),
                    reminderTime = if (obj.isNull("reminderTime")) null else obj.getLong("reminderTime"),
                    recurrenceType = RecurrenceType.valueOf(obj.getString("recurrenceType")),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt")
                )
            )
        }

        repo.replaceAllTasks(tasks)
    }
}