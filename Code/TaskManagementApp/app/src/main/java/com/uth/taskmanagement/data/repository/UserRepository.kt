package com.uth.taskmanagement.data.repository

import com.uth.taskmanagement.data.local.UserDao
import com.uth.taskmanagement.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao
) {

    /**
     * Đảm bảo user mặc định tồn tại trong database.
     * Idempotent: gọi nhiều lần cũng chỉ có đúng 1 user mặc định
     * vì UserDao dùng OnConflictStrategy.IGNORE.
     */
    suspend fun ensureDefaultUserExists() {
        userDao.insert(UserEntity.createDefault())
    }

    /** One-shot — lấy user mặc định. */
    suspend fun getDefaultUser(): UserEntity? =
        userDao.getDefaultUser()

    /** One-shot — lấy user theo id. */
    suspend fun getUserById(id: String): UserEntity? =
        userDao.getUserById(id)

    /** Reactive — observe user theo id, tự cập nhật khi data thay đổi. */
    fun observeUserById(id: String): Flow<UserEntity?> =
        userDao.observeUserById(id)

    /** Toàn bộ danh sách user — dùng cho màn hình chọn assignee. */
    suspend fun getAllUsers(): List<UserEntity> =
        userDao.getAllUsers()
}

