package com.uth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uth.taskmanagement.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    /**
     * Insert user. OnConflictStrategy.IGNORE đảm bảo không tạo trùng:
     * nếu user với cùng id đã tồn tại, lệnh insert sẽ bị bỏ qua.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity)

    /** One-shot — lấy user theo id. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    /** Reactive — tự cập nhật khi user thay đổi. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: String): Flow<UserEntity?>

    /** Lấy user mặc định (one-shot). */
    @Query("SELECT * FROM users WHERE id = '${UserEntity.DEFAULT_USER_ID}' LIMIT 1")
    suspend fun getDefaultUser(): UserEntity?

    /** Toàn bộ danh sách user — dùng cho màn hình chọn assignee sau này. */
    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllUsers(): List<UserEntity>
}

