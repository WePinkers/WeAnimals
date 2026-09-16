package com.example.weanimals.profile.overview.repository

import com.example.weanimals.profile.overview.domain.ProfileIdentity

interface ProfileRepository {
    suspend fun getIdentity(): Result<ProfileIdentity>
}
