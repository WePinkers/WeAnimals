package com.example.weanimals.user.profile.overview.repository

import com.example.weanimals.user.profile.overview.domain.ProfileIdentity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository(private val auth: FirebaseAuth) : ProfileRepository {
    override suspend fun getIdentity(): Result<ProfileIdentity> = runCatching {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        ProfileIdentity(user?.displayName?.trim()?.takeIf(String::isNotBlank))
    }
}
