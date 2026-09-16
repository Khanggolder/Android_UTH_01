package com.uth.taskmanagement.security

import androidx.lifecycle.ViewModel

data class PinAttemptState(
    val remainingAttempts: Int = PinLoginViewModel.MAX_ATTEMPTS,
    val lockoutEndsAt: Long? = null
)

class PinLoginViewModel : ViewModel() {

    var state: PinAttemptState = PinAttemptState()
        private set

    fun registerFailedAttempt(currentTime: Long = System.currentTimeMillis()): PinAttemptState {
        clearExpiredLockout(currentTime)
        if (isLockedOut(currentTime)) return state

        val remaining = state.remainingAttempts - 1
        state = if (remaining <= 0) {
            PinAttemptState(
                remainingAttempts = 0,
                lockoutEndsAt = currentTime + LOCKOUT_DURATION_MS
            )
        } else {
            state.copy(remainingAttempts = remaining)
        }
        return state
    }

    fun isLockedOut(currentTime: Long = System.currentTimeMillis()): Boolean {
        val lockoutEndsAt = state.lockoutEndsAt ?: return false
        return lockoutEndsAt > currentTime
    }

    fun remainingLockoutMillis(currentTime: Long = System.currentTimeMillis()): Long =
        ((state.lockoutEndsAt ?: currentTime) - currentTime).coerceAtLeast(0L)

    fun clearExpiredLockout(currentTime: Long = System.currentTimeMillis()) {
        val lockoutEndsAt = state.lockoutEndsAt ?: return
        if (lockoutEndsAt <= currentTime) state = PinAttemptState()
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
