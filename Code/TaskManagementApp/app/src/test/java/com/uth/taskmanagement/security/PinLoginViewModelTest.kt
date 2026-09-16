package com.uth.taskmanagement.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinLoginViewModelTest {

    @Test
    fun failedAttemptsAndLockoutSurviveViewRecreation() {
        val viewModel = PinLoginViewModel()
        repeat(PinLoginViewModel.MAX_ATTEMPTS) {
            viewModel.registerFailedAttempt(currentTime = 1_000L)
        }

        assertTrue(viewModel.isLockedOut(currentTime = 2_000L))
        assertEquals(29_000L, viewModel.remainingLockoutMillis(currentTime = 2_000L))

        viewModel.clearExpiredLockout(currentTime = 31_001L)
        assertFalse(viewModel.isLockedOut(currentTime = 31_001L))
        assertEquals(PinLoginViewModel.MAX_ATTEMPTS, viewModel.state.remainingAttempts)
    }
}
