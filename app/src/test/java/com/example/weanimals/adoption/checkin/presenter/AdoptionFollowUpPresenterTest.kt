package com.example.weanimals.adoption.checkin.presenter

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.AdoptionCheckIn
import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.AdoptionDateMissingException
import com.example.weanimals.adoption.checkin.domain.FollowUpMilestone
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestion
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId
import com.example.weanimals.adoption.checkin.domain.FollowUpNotReadyException
import com.example.weanimals.adoption.checkin.domain.MilestoneState
import com.example.weanimals.adoption.checkin.interactor.CalculateFollowUpMilestonesInteractor
import com.example.weanimals.adoption.checkin.interactor.GetAdoptionFollowUpInteractor
import com.example.weanimals.adoption.checkin.interactor.GetFollowUpQuestionsInteractor
import com.example.weanimals.adoption.checkin.interactor.SubmitAdoptionCheckInInteractor
import com.example.weanimals.adoption.checkin.repository.AdoptionFollowUpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionFollowUpPresenterTest {
    private val repository = FakeRepository()
    private val view = RecordingView()
    private lateinit var presenter: AdoptionFollowUpPresenter

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = AdoptionFollowUpPresenter(
            "animal-1",
            GetAdoptionFollowUpInteractor(repository),
            CalculateFollowUpMilestonesInteractor(),
            GetFollowUpQuestionsInteractor(),
            SubmitAdoptionCheckInInteractor(repository)
        )
        presenter.attachView(view)
    }

    @After fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test fun answeringTheCurrentMilestoneRefreshesTheTimeline() = runTest {
        presenter.start()
        advanceUntilIdle()
        assertEquals(MilestoneState.AVAILABLE, view.milestones?.first()?.state)
        assertEquals("Canela", view.followUp?.animalName)
        assertEquals(listOf(
            FollowUpQuestionId.ROUTINE_ADJUSTED,
            FollowUpQuestionId.HEALTH_OR_BEHAVIOR_ISSUE
        ), view.questions.map { it.id })

        presenter.onSubmitClicked("Tudo bem")
        assertTrue(view.ratingRequired)
        assertEquals(0, repository.submissions)

        presenter.onRatingSelected(AdaptationRating.EXCELLENT)
        presenter.onSubmitClicked("Tudo bem")
        assertEquals(FollowUpQuestionId.ROUTINE_ADJUSTED, view.requiredQuestion)
        assertEquals(0, repository.submissions)

        presenter.onQuestionAnswered(FollowUpQuestionId.ROUTINE_ADJUSTED, FollowUpAnswer.YES)
        presenter.onQuestionAnswered(FollowUpQuestionId.HEALTH_OR_BEHAVIOR_ISSUE, FollowUpAnswer.NO)
        presenter.onSubmitClicked("Tudo bem")
        advanceUntilIdle()

        assertEquals(1, repository.submissions)
        assertEquals("Tudo bem", repository.followUp.checkIns[30]?.note)
        assertEquals(mapOf(
            FollowUpQuestionId.ROUTINE_ADJUSTED to FollowUpAnswer.YES,
            FollowUpQuestionId.HEALTH_OR_BEHAVIOR_ISSUE to FollowUpAnswer.NO
        ), repository.followUp.checkIns[30]?.answers)
        assertEquals(MilestoneState.COMPLETED, view.milestones?.first()?.state)
        assertEquals(MilestoneState.AVAILABLE, view.milestones?.get(1)?.state)
        assertEquals(3, view.questions.size)
        assertTrue(view.submitSuccess)
        assertTrue(view.formCleared)
        assertFalse(view.sending)
    }

    @Test fun adoptionNotYetConfirmedHasItsOwnState() = runTest {
        repository.loadResult = Result.failure(FollowUpNotReadyException())
        presenter.start()
        advanceUntilIdle()
        assertTrue(view.unavailable)
        assertEquals(null, view.followUp)
    }

    @Test fun confirmedAdoptionWithoutRealDateHasItsOwnState() = runTest {
        repository.loadResult = Result.failure(AdoptionDateMissingException())
        presenter.start()
        advanceUntilIdle()
        assertTrue(view.dateMissing)
        assertEquals(null, view.followUp)
    }

    @Test fun ninetyAndOneHundredEightyDaysShowTheirOwnQuestions() = runTest {
        repository.followUp = repository.followUp.copy(checkIns = mapOf(
            30 to AdoptionCheckIn(30, AdaptationRating.GOOD, "", 0L)
        ))
        presenter.start()
        advanceUntilIdle()
        assertEquals(listOf(
            FollowUpQuestionId.WALKS_AND_FEEDING_ESTABLISHED,
            FollowUpQuestionId.BEHAVIOR_CHANGED,
            FollowUpQuestionId.REGULAR_VET_VISITS
        ), view.questions.map { it.id })

        presenter.onRatingSelected(AdaptationRating.GOOD)
        view.questions.forEach { presenter.onQuestionAnswered(it.id, it.options.first()) }
        presenter.onSubmitClicked("")
        advanceUntilIdle()
        assertEquals(3, repository.followUp.checkIns[90]?.answers?.size)
        assertEquals(listOf(
            FollowUpQuestionId.STRONG_BOND,
            FollowUpQuestionId.SHELTER_SUPPORT_NEEDED,
            FollowUpQuestionId.RECOMMEND_ADOPTION
        ), view.questions.map { it.id })
        assertEquals(listOf(FollowUpAnswer.YES_CERTAINLY, FollowUpAnswer.MAYBE),
            view.questions.last().options)
    }

    private class FakeRepository : AdoptionFollowUpRepository {
        var followUp = AdoptionFollowUp(
            "animal-1", "Canela",
            System.currentTimeMillis() - 400L * 24 * 60 * 60 * 1000,
            null, null, emptyMap()
        )
        var loadResult: Result<AdoptionFollowUp>? = null
        var submissions = 0
        override suspend fun getFollowUp(animalId: String) = loadResult ?: Result.success(followUp)
        override suspend fun submitCheckIn(
            animalId: String,
            milestoneDays: Int,
            rating: AdaptationRating,
            answers: Map<FollowUpQuestionId, FollowUpAnswer>,
            note: String
        ): Result<Unit> {
            submissions++
            followUp = followUp.copy(checkIns = followUp.checkIns + (
                milestoneDays to AdoptionCheckIn(milestoneDays, rating, note, 0L, answers)
            ))
            return Result.success(Unit)
        }
    }

    private class RecordingView : AdoptionFollowUpContract.View {
        var followUp: AdoptionFollowUp? = null
        var milestones: List<FollowUpMilestone>? = null
        var unavailable = false
        var dateMissing = false
        var ratingRequired = false
        var sending = false
        var submitSuccess = false
        var formCleared = false
        var questions = emptyList<FollowUpQuestion>()
        var requiredQuestion: FollowUpQuestionId? = null
        override fun showLoading() = Unit
        override fun showFollowUp(
            followUp: AdoptionFollowUp,
            milestones: List<FollowUpMilestone>,
            selectedRating: AdaptationRating?,
            questions: List<FollowUpQuestion>,
            selectedAnswers: Map<FollowUpQuestionId, FollowUpAnswer>
        ) { this.followUp = followUp; this.milestones = milestones; this.questions = questions }
        override fun showUnavailable() { unavailable = true }
        override fun showAdoptionDateMissing() { dateMissing = true }
        override fun showLoadError(error: Throwable) = Unit
        override fun showRatingRequired() { ratingRequired = true }
        override fun showQuestionRequired(questionId: FollowUpQuestionId) { requiredQuestion = questionId }
        override fun showSending(sending: Boolean) { this.sending = sending }
        override fun showSubmitError(error: Throwable) = Unit
        override fun showSubmitSuccess() { submitSuccess = true }
        override fun clearForm() { formCleared = true }
        override fun closeScreen() = Unit
    }
}
