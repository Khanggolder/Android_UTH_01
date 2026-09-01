package com.uth.taskmanagement.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    @ColumnInfo(defaultValue = "''")
    val email: String = ""
) {
    companion object {
        const val DEFAULT_USER_ID = "local-user"
        const val DEFAULT_USER_NAME = "Me"

        fun createDefault(): UserEntity = UserEntity(
            id = DEFAULT_USER_ID,
            name = DEFAULT_USER_NAME,
            email = ""
        )
    }
}
