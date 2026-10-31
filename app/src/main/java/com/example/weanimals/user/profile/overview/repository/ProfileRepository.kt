package com.example.weanimals.user.profile.overview.repository

import com.example.weanimals.user.profile.overview.domain.ProfileIdentity

interface ProfileRepository {
    suspend fun getIdentity(): Result<ProfileIdentity>
}
