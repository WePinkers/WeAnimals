package com.example.weanimals.user.entry.auth.security

import android.content.Context
import java.security.MessageDigest
import kotlin.math.ceil

/**
 * Local-only lock used for the TCC demonstration.
 *
 * It is intentionally kept outside Firebase so the app can demonstrate the
 * five-attempt/30-minute behavior without a Cloud Function.
 */
class LocalLoginAttemptLock(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun check(identifier: String): LockState {
        val key = keyFor(identifier)
        val now = System.currentTimeMillis()
        val lockedUntil = preferences.getLong(lockUntilKey(key), 0L)

        if (lockedUntil > now) {
            return LockState(
                isLocked = true,
                remainingMinutes = remainingMinutes(lockedUntil - now)
            )
        }

        if (lockedUntil != 0L) {
            clear(identifier)
        }
        return LockState(isLocked = false)
    }

    fun registerFailure(identifier: String): LockState {
        val current = check(identifier)
        if (current.isLocked) return current

        val key = keyFor(identifier)
        val failures = preferences.getInt(failuresKey(key), 0) + 1
        if (failures >= MAX_FAILED_ATTEMPTS) {
            val lockedUntil = System.currentTimeMillis() + LOCK_DURATION_MS
            preferences.edit()
                .putInt(failuresKey(key), failures)
                .putLong(lockUntilKey(key), lockedUntil)
                .apply()
            return LockState(
                isLocked = true,
                remainingMinutes = remainingMinutes(LOCK_DURATION_MS)
            )
        }

        preferences.edit()
            .putInt(failuresKey(key), failures)
            .remove(lockUntilKey(key))
            .apply()
        return LockState(isLocked = false)
    }

    fun clear(identifier: String) {
        val key = keyFor(identifier)
        preferences.edit()
            .remove(failuresKey(key))
            .remove(lockUntilKey(key))
            .apply()
    }

    private fun keyFor(identifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identifier.trim().lowercase().toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun failuresKey(key: String): String = "failed_attempts_$key"

    private fun lockUntilKey(key: String): String = "locked_until_$key"

    private fun remainingMinutes(milliseconds: Long): Int =
        ceil(milliseconds / MILLIS_PER_MINUTE.toDouble()).toInt().coerceAtLeast(1)

    data class LockState(
        val isLocked: Boolean,
        val remainingMinutes: Int = 0
    )

    companion object {
        private const val PREFERENCES_NAME = "local_login_attempt_lock"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MS = 30 * 60 * 1000L
        private const val MILLIS_PER_MINUTE = 60 * 1000L
    }
}