package com.uth.taskmanagement.data.repository

import com.uth.taskmanagement.data.local.UserDao
import com.uth.taskmanagement.data.model.UserEntity

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

    suspend fun getDefaultUser(): UserEntity? =
        userDao.getDefaultUser()

    suspend fun getUserById(id: String): UserEntity? =
        userDao.getUserById(id)
}
