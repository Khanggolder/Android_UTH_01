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
    version = 5,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_task_attachments_taskId ON task_attachments(taskId)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN createdByUserId TEXT NOT NULL DEFAULT 'local-user'"
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN assigneeUserId TEXT NOT NULL DEFAULT 'local-user'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
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
