package com.example.weanimals.user.adoption.questionnaire.repository

import com.example.weanimals.user.adoption.questionnaire.domain.AdoptionProfile

interface AdoptionProfileRepository {
    suspend fun getProfile(): Result<AdoptionProfile?>
    suspend fun saveProfile(profile: AdoptionProfile): Result<Unit>
}
