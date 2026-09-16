package com.example.weanimals.adoption.checkin.presenter

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.FollowUpMilestone
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.AdoptionDateMissingException
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId
import com.example.weanimals.adoption.checkin.domain.FollowUpNotReadyException
import com.example.weanimals.adoption.checkin.domain.MilestoneState
import com.example.weanimals.adoption.checkin.interactor.CalculateFollowUpMilestonesInteractor
import com.example.weanimals.adoption.checkin.interactor.GetAdoptionFollowUpInteractor
import com.example.weanimals.adoption.checkin.interactor.GetFollowUpQuestionsInteractor
import com.example.weanimals.adoption.checkin.interactor.SubmitAdoptionCheckInInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AdoptionFollowUpPresenter(
    private val animalId: String,
    private val getFollowUp: GetAdoptionFollowUpInteractor,
    private val calculateMilestones: CalculateFollowUpMilestonesInteractor,
    private val getQuestions: GetFollowUpQuestionsInteractor,
    private val submitCheckIn: SubmitAdoptionCheckInInteractor
) : BasePresenter<AdoptionFollowUpContract.View>(), AdoptionFollowUpContract.Presenter {
    private var loadJob: Job? = null
    private var milestones = emptyList<FollowUpMilestone>()
    private var selectedRating: AdaptationRating? = null
    private val selectedAnswers = mutableMapOf<FollowUpQuestionId, FollowUpAnswer>()
    private var activeMilestoneDays: Int? = null
    private var sending = false

    override fun start() = load()
    override fun onRetryClicked() = load()
    override fun onBackClicked() = withView(AdoptionFollowUpContract.View::closeScreen)

    override fun onRatingSelected(rating: AdaptationRating?) {
        selectedRating = rating
    }

    override fun onQuestionAnswered(questionId: FollowUpQuestionId, answer: FollowUpAnswer?) {
        val question = getQuestions(activeMilestoneDays ?: return)
            .firstOrNull { it.id == questionId } ?: return
        if (answer == null) selectedAnswers.remove(questionId)
        else if (answer in question.options) selectedAnswers[questionId] = answer
    }

    override fun onSubmitClicked(note: String) {
        if (sending) return
        val current = milestones.firstOrNull { it.state == MilestoneState.AVAILABLE } ?: return
        val rating = selectedRating ?: run {
            withView { it.showRatingRequired() }
            return
        }
        val missing = getQuestions(current.days).firstOrNull { it.id !in selectedAnswers }
        if (missing != null) {
            withView { it.showQuestionRequired(missing.id) }
            return
        }
        sending = true
        withView { it.showSending(true) }
        presenterScope.launch {
            try {
                submitCheckIn(animalId, current.days, rating, selectedAnswers.toMap(), note).fold(
                    onSuccess = {
                        sending = false
                        selectedRating = null
                        selectedAnswers.clear()
                        withView {
                            it.showSending(false)
                            it.clearForm()
                            it.showSubmitSuccess()
                        }
                        load()
                    },
                    onFailure = { error ->
                        sending = false
                        withView {
                            it.showSending(false)
                            it.showSubmitError(error)
                        }
                    }
                )
            } catch (cancelled: CancellationException) {
                sending = false
                throw cancelled
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        milestones = emptyList()
        withView { it.showLoading() }
        loadJob = presenterScope.launch {
            try {
                getFollowUp(animalId).fold(
                    onSuccess = { loaded ->
                        milestones = calculateMilestones(
                            loaded, System.currentTimeMillis()
                        )
                        val active = milestones.firstOrNull { it.state == MilestoneState.AVAILABLE }
                            ?.days
                        if (active != activeMilestoneDays) {
                            activeMilestoneDays = active
                            selectedRating = null
                            selectedAnswers.clear()
                        }
                        withView {
                            it.showFollowUp(
                                loaded, milestones, selectedRating,
                                active?.let(getQuestions::invoke).orEmpty(),
                                selectedAnswers.toMap()
                            )
                        }
                    },
                    onFailure = { error ->
                        if (error is FollowUpNotReadyException)
                            withView { it.showUnavailable() }
                        else if (error is AdoptionDateMissingException)
                            withView { it.showAdoptionDateMissing() }
                        else withView { it.showLoadError(error) }
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }
}
