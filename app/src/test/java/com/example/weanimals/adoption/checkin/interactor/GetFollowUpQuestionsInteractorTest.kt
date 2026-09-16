package com.example.weanimals.adoption.checkin.interactor

import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFollowUpQuestionsInteractorTest {
    private val getQuestions = GetFollowUpQuestionsInteractor()

    @Test fun thirtyDaysHasRoutineAndHealthQuestions() {
        val questions = getQuestions(30)
        assertEquals(listOf(
            FollowUpQuestionId.ROUTINE_ADJUSTED,
            FollowUpQuestionId.HEALTH_OR_BEHAVIOR_ISSUE
        ), questions.map { it.id })
        assertEquals(listOf(FollowUpAnswer.YES, FollowUpAnswer.NO, FollowUpAnswer.PARTLY),
            questions.first().options)
    }

    @Test fun ninetyDaysHasWalksBehaviorAndVetQuestions() {
        assertEquals(listOf(
            FollowUpQuestionId.WALKS_AND_FEEDING_ESTABLISHED,
            FollowUpQuestionId.BEHAVIOR_CHANGED,
            FollowUpQuestionId.REGULAR_VET_VISITS
        ), getQuestions(90).map { it.id })
    }

    @Test fun oneHundredEightyDaysEndsWithRecommendation() {
        val questions = getQuestions(180)
        assertEquals(FollowUpQuestionId.RECOMMEND_ADOPTION, questions.last().id)
        assertEquals(listOf(FollowUpAnswer.YES_CERTAINLY, FollowUpAnswer.MAYBE),
            questions.last().options)
    }
}
