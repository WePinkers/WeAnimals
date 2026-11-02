package com.example.weanimals.user.profile.overview.repository

import android.content.Context
import com.example.weanimals.user.profile.overview.domain.ProfileIdentity
import com.google.firebase.auth.FirebaseAuth

class FirebaseProfileRepository(
    private val auth: FirebaseAuth,
    private val context: Context,
) : ProfileRepository {
    override suspend fun getIdentity(): Result<ProfileIdentity> = runCatching {
        val savedName = context
            .getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, null)
            ?.trim()
            ?.takeIf(String::isNotBlank)
        val authName = auth.currentUser?.displayName?.trim()?.takeIf(String::isNotBlank)
        ProfileIdentity(savedName ?: authName)
    }

    private companion object {
        const val PROFILE_PREFS = "profile_prefs"
        const val KEY_USER_NAME = "user_name"
    }
}
