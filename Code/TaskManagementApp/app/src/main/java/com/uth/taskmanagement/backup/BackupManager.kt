package com.uth.taskmanagement.backup

import android.content.Context
import android.net.Uri
import com.uth.taskmanagement.data.model.RecurrenceType
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.TaskPriority
import com.uth.taskmanagement.data.model.TaskStatus
import com.uth.taskmanagement.data.repository.AttachmentRepository
import com.uth.taskmanagement.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class BackupManager(
    private val taskRepository: TaskRepository,
    private val attachmentRepository: AttachmentRepository,
    private val context: Context
) {
    companion object {
        private const val BACKUP_VERSION = 2
    }

    suspend fun exportTasks(uri: Uri): Result<Unit> = runCatching {
        val tasks = taskRepository.observeAllTasks().first()

        val root = JSONObject()
        root.put("version", BACKUP_VERSION)

        val tasksArray = JSONArray()

        tasks.forEach { task ->
            val taskObj = JSONObject()
            taskObj.put("id", task.id)
            taskObj.put("title", task.title)
            taskObj.put("description", task.description)
            taskObj.put("startDateTime", task.startDateTime)
            taskObj.put("dueDateTime", task.dueDateTime)
            taskObj.put("priority", task.priority.name)
            taskObj.put("status", task.status.name)
            taskObj.put("isCompleted", task.isCompleted)
            taskObj.put("reminderTime", task.reminderTime ?: JSONObject.NULL)
            taskObj.put("recurrenceType", task.recurrenceType.name)
            taskObj.put("createdAt", task.createdAt)
            taskObj.put("updatedAt", task.updatedAt)
            taskObj.put("createdByUserId", task.createdByUserId)
            taskObj.put("assigneeUserId", task.assigneeUserId)

            val attachments = attachmentRepository.getAttachments(task.id)
            val attachmentsArray = JSONArray()
            attachments.forEach { att ->
                val attObj = JSONObject()
                attObj.put("fileName", att.fileName)
                attObj.put("uri", att.uri)
                attObj.put("mimeType", att.mimeType)
                attObj.put("sizeBytes", att.sizeBytes)
                attObj.put("createdAt", att.createdAt)
                attachmentsArray.put(attObj)
            }
            taskObj.put("attachments", attachmentsArray)

            tasksArray.put(taskObj)
        }

        root.put("tasks", tasksArray)

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString().toByteArray())
        } ?: throw IOException("Could not open file for writing")
    }

    suspend fun restoreTasks(uri: Uri): Result<Unit> = runCatching {
        val jsonText = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: throw IOException("Could not read file")

        val root = JSONObject(jsonText)
        val version = root.optInt("version", 1)

        val tasksArray = if (version >= 2) {
            root.getJSONArray("tasks")
        } else {
            JSONArray(jsonText)
        }

        val tasks = mutableListOf<TaskEntity>()
        val attachmentsByTaskId = mutableMapOf<Long, List<TaskAttachmentEntity>>()

        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            val taskId = obj.getLong("id")

            tasks.add(
                TaskEntity(
                    id = taskId,
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    startDateTime = obj.optLong("startDateTime", System.currentTimeMillis()),
                    dueDateTime = obj.getLong("dueDateTime"),
                    priority = TaskPriority.valueOf(obj.getString("priority")),
                    status = TaskStatus.valueOf(obj.getString("status")),
                    isCompleted = obj.getBoolean("isCompleted"),
                    reminderTime = if (obj.isNull("reminderTime")) null else obj.getLong("reminderTime"),
                    recurrenceType = RecurrenceType.valueOf(obj.getString("recurrenceType")),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt"),
                    createdByUserId = obj.optString("createdByUserId", "local-user"),
                    assigneeUserId = obj.optString("assigneeUserId", "local-user")
                )
            )

            if (obj.has("attachments")) {
                val attArray = obj.getJSONArray("attachments")
                val attList = mutableListOf<TaskAttachmentEntity>()
                for (j in 0 until attArray.length()) {
                    val attObj = attArray.getJSONObject(j)
                    attList.add(
                        TaskAttachmentEntity(
                            taskId = taskId,
                            fileName = attObj.getString("fileName"),
                            uri = attObj.getString("uri"),
                            mimeType = attObj.optString("mimeType", ""),
                            sizeBytes = attObj.optLong("sizeBytes", 0L),
                            createdAt = attObj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                attachmentsByTaskId[taskId] = attList
            }
        }

        taskRepository.replaceAllTasks(tasks)

        attachmentsByTaskId.forEach { (taskId, attachments) ->
            if (attachments.isNotEmpty()) {
                attachmentRepository.addAttachments(attachments)
            }
        }
    }
}