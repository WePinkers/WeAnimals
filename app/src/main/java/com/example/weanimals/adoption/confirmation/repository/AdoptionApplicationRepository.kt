package com.example.weanimals.adoption.confirmation.repository

import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate

interface AdoptionApplicationRepository {
    suspend fun submit(candidate: AdoptionCandidate): Result<Unit>
}
