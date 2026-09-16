package com.example.weanimals.adoption.checkin.repository

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId

interface AdoptionFollowUpRepository {
    suspend fun getFollowUp(animalId: String): Result<AdoptionFollowUp>
    suspend fun submitCheckIn(
        animalId: String,
        milestoneDays: Int,
        rating: AdaptationRating,
        answers: Map<FollowUpQuestionId, FollowUpAnswer>,
        note: String
    ): Result<Unit>
}
