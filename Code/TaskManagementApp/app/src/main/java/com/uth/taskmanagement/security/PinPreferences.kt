package com.uth.taskmanagement.security

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("pin_prefs")

class PinPreferences(private val context: Context) {
    private val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    private val PIN_HASH = stringPreferencesKey("pin_hash")
    private val PIN_SALT = stringPreferencesKey("pin_salt")

    val isPinEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PIN_ENABLED] ?: false }

    suspend fun setupPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        context.dataStore.edit {
            it[PIN_SALT] = salt
            it[PIN_HASH] = hash
            it[PIN_ENABLED] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[PIN_SALT] ?: return false
        val hash = prefs[PIN_HASH] ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    suspend fun disablePin() {
        context.dataStore.edit {
            it.remove(PIN_HASH)
            it.remove(PIN_SALT)
            it[PIN_ENABLED] = false
        }
    }
}