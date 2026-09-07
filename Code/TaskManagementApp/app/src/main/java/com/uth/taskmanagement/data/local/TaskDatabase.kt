package com.uth.taskmanagement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.UserEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, UserEntity::class, TaskAttachmentEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun userDao(): UserDao

    abstract fun attachmentDao(): AttachmentDao

    suspend fun replaceBackupData(
        tasks: List<TaskEntity>,
        attachments: List<TaskAttachmentEntity>
    ) {
        withTransaction {
            taskDao().deleteAllTasks()
            taskDao().insertTasks(tasks)
            if (attachments.isNotEmpty()) {
                attachmentDao().insertAll(attachments)
            }
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                        context.applicationContext,
                        TaskDatabase::class.java,
                        "task_management.db"
                    )
                    .addCallback(DEFAULT_USER_CALLBACK)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }

        /**
         * Migration 2 → 3 (gộp toàn bộ thay đổi User + Attachment):
         *
         * 1. Tạo bảng users
         * 2. Insert user mặc định local-user
         * 3. Thêm createdByUserId, assigneeUserId vào tasks
         * 4. Gán task cũ cho local-user
         * 5. Tạo bảng task_attachments với Foreign Key CASCADE
         * 6. Tạo index cho taskId
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. Tạo bảng users
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id    TEXT NOT NULL PRIMARY KEY,
                        name  TEXT NOT NULL,
                        email TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )

                // 2. Insert user mặc định (IGNORE nếu đã tồn tại)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO users (id, name, email)
                    VALUES ('local-user', 'Me', '')
                    """.trimIndent()
                )

                // 3. Thêm cột user vào tasks
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN createdByUserId TEXT NOT NULL DEFAULT 'local-user'"
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN assigneeUserId TEXT NOT NULL DEFAULT 'local-user'"
                )

                // 4. Gán task cũ cho local-user (đảm bảo không có giá trị rỗng)
                db.execSQL(
                    "UPDATE tasks SET createdByUserId = 'local-user' WHERE createdByUserId = ''"
                )
                db.execSQL(
                    "UPDATE tasks SET assigneeUserId = 'local-user' WHERE assigneeUserId = ''"
                )

                // 5. Tạo bảng task_attachments với Foreign Key CASCADE
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_attachments (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId    INTEGER NOT NULL,
                        fileName  TEXT    NOT NULL,
                        uri       TEXT    NOT NULL,
                        mimeType  TEXT    NOT NULL DEFAULT '',
                        sizeBytes INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // 6. Index cho taskId
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_task_attachments_taskId ON task_attachments(taskId)"
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    ALTER TABLE tasks
                    ADD COLUMN startDateTime INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                
                db.execSQL(
                    """
                    UPDATE tasks
                    SET startDateTime = createdAt
                    WHERE startDateTime = 0
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE tasks
                    SET startDateTime = dueDateTime
                    WHERE startDateTime > dueDateTime
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE task_attachments " +
                        "ADD COLUMN isAppOwned INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE task_attachments " +
                        "ADD COLUMN localRelativePath TEXT DEFAULT NULL"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                insertDefaultUser(db)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        startDateTime INTEGER NOT NULL,
                        dueDateTime INTEGER NOT NULL,
                        priority TEXT NOT NULL,
                        status TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        reminderTime INTEGER,
                        recurrenceType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        createdByUserId TEXT NOT NULL DEFAULT 'local-user',
                        assigneeUserId TEXT NOT NULL DEFAULT 'local-user',
                        FOREIGN KEY(assigneeUserId) REFERENCES users(id)
                            ON UPDATE CASCADE
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO tasks_new (
                        id, title, description, startDateTime, dueDateTime,
                        priority, status, isCompleted, reminderTime,
                        recurrenceType, createdAt, updatedAt,
                        createdByUserId, assigneeUserId
                    )
                    SELECT
                        id, title, description, startDateTime, dueDateTime,
                        priority, status, isCompleted, reminderTime,
                        recurrenceType, createdAt, updatedAt,
                        COALESCE(NULLIF(createdByUserId, ''), 'local-user'),
                        CASE
                            WHEN assigneeUserId IN (SELECT id FROM users)
                                THEN assigneeUserId
                            ELSE 'local-user'
                        END
                    FROM tasks
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS task_attachments_backup")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_attachments_backup AS
                    SELECT * FROM task_attachments
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_tasks_assigneeUserId ON tasks(assigneeUserId)"
                )
                db.execSQL("DELETE FROM task_attachments")
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO task_attachments (
                        id, taskId, fileName, uri, mimeType, sizeBytes,
                        createdAt, isAppOwned, localRelativePath
                    )
                    SELECT
                        id, taskId, fileName, uri, mimeType, sizeBytes,
                        createdAt, isAppOwned, localRelativePath
                    FROM task_attachments_backup
                    WHERE taskId IN (SELECT id FROM tasks)
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE task_attachments_backup")
            }
        }

        private val DEFAULT_USER_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                insertDefaultUser(db)
            }
        }

        private fun insertDefaultUser(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO users (id, name, email)
                VALUES ('local-user', 'Me', '')
                """.trimIndent()
            )
        }
    }
}

