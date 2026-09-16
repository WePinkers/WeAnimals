package com.example.weanimals.adoption.checkin.domain

object FollowUpQuestionCatalog {
    fun questionsFor(milestoneDays: Int): List<FollowUpQuestion> = when (milestoneDays) {
        30 -> listOf(
            FollowUpQuestion(FollowUpQuestionId.ROUTINE_ADJUSTED, YES_NO_PARTLY),
            FollowUpQuestion(FollowUpQuestionId.HEALTH_OR_BEHAVIOR_ISSUE, YES_NO)
        )
        90 -> listOf(
            FollowUpQuestion(FollowUpQuestionId.WALKS_AND_FEEDING_ESTABLISHED, YES_NO_PARTLY),
            FollowUpQuestion(FollowUpQuestionId.BEHAVIOR_CHANGED, YES_NO),
            FollowUpQuestion(FollowUpQuestionId.REGULAR_VET_VISITS, YES_NO)
        )
        180 -> listOf(
            FollowUpQuestion(FollowUpQuestionId.STRONG_BOND, YES_NO_PARTLY),
            FollowUpQuestion(FollowUpQuestionId.SHELTER_SUPPORT_NEEDED, YES_NO),
            FollowUpQuestion(
                FollowUpQuestionId.RECOMMEND_ADOPTION,
                listOf(FollowUpAnswer.YES_CERTAINLY, FollowUpAnswer.MAYBE)
            )
        )
        else -> emptyList()
    }

    private val YES_NO = listOf(FollowUpAnswer.YES, FollowUpAnswer.NO)
    private val YES_NO_PARTLY = YES_NO + FollowUpAnswer.PARTLY
}
