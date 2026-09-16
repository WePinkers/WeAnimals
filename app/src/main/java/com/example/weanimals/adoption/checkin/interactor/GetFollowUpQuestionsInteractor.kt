package com.example.weanimals.adoption.checkin.interactor

import com.example.weanimals.adoption.checkin.domain.FollowUpQuestion
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionCatalog

class GetFollowUpQuestionsInteractor {
    operator fun invoke(milestoneDays: Int): List<FollowUpQuestion> =
        FollowUpQuestionCatalog.questionsFor(milestoneDays)
}
