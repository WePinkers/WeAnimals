package com.example.weanimals.adoption.checkin.presenter

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.FollowUpMilestone
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestion
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId

interface AdoptionFollowUpContract {
    interface View {
        fun showLoading()
        fun showFollowUp(
            followUp: AdoptionFollowUp,
            milestones: List<FollowUpMilestone>,
            selectedRating: AdaptationRating?,
            questions: List<FollowUpQuestion>,
            selectedAnswers: Map<FollowUpQuestionId, FollowUpAnswer>
        )
        fun showUnavailable()
        fun showAdoptionDateMissing()
        fun showLoadError(error: Throwable)
        fun showRatingRequired()
        fun showQuestionRequired(questionId: FollowUpQuestionId)
        fun showSending(sending: Boolean)
        fun showSubmitError(error: Throwable)
        fun showSubmitSuccess()
        fun clearForm()
        fun closeScreen()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun start()
        fun onRetryClicked()
        fun onBackClicked()
        fun onRatingSelected(rating: AdaptationRating?)
        fun onQuestionAnswered(questionId: FollowUpQuestionId, answer: FollowUpAnswer?)
        fun onSubmitClicked(note: String)
    }
}
