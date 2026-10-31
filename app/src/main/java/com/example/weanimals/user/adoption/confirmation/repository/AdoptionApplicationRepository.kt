package com.example.weanimals.user.adoption.confirmation.repository

import com.example.weanimals.user.adoption.confirmation.domain.AdoptionCandidate

interface AdoptionApplicationRepository {
    suspend fun submit(candidate: AdoptionCandidate): Result<Unit>
}
