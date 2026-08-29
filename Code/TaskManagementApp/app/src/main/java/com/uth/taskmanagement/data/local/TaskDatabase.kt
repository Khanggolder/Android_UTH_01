package com.uth.taskmanagement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uth.taskmanagement.data.model.TaskEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
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
