package com.uth.taskmanagement.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        hash(pin, salt) == expectedHash
}