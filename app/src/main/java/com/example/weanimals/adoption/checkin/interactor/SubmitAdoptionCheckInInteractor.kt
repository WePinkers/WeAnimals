package com.example.weanimals.adoption.checkin.interactor

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId
import com.example.weanimals.adoption.checkin.repository.AdoptionFollowUpRepository

class SubmitAdoptionCheckInInteractor(private val repository: AdoptionFollowUpRepository) {
    suspend operator fun invoke(
        animalId: String,
        milestoneDays: Int,
        rating: AdaptationRating,
        answers: Map<FollowUpQuestionId, FollowUpAnswer>,
        note: String
    ) = repository.submitCheckIn(animalId, milestoneDays, rating, answers, note)
}
