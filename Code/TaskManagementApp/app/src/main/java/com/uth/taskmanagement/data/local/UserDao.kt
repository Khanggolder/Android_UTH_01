package com.uth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uth.taskmanagement.data.model.UserEntity

@Dao
interface UserDao {

    /**
     * Insert user. OnConflictStrategy.IGNORE đảm bảo không tạo trùng:
     * nếu user với cùng id đã tồn tại, lệnh insert sẽ bị bỏ qua.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = '${UserEntity.DEFAULT_USER_ID}' LIMIT 1")
    suspend fun getDefaultUser(): UserEntity?
}
