package com.uth.taskmanagement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uth.taskmanagement.data.model.TaskAttachmentEntity
import com.uth.taskmanagement.data.model.TaskEntity
import com.uth.taskmanagement.data.model.UserEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, UserEntity::class, TaskAttachmentEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun userDao(): UserDao

    abstract fun attachmentDao(): AttachmentDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    }
}

