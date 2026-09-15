package com.example.weanimals.adoption.confirmation.presenter

import com.example.weanimals.adoption.compatibility.interactor.GetAnimalRecommendationsInteractor
import com.example.weanimals.adoption.compatibility.repository.AnimalRecommendationRepository
import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.confirmation.domain.AlreadyAppliedException
import com.example.weanimals.adoption.confirmation.domain.DemoApplicationUnavailableException
import com.example.weanimals.adoption.confirmation.interactor.GetAdoptionCandidateInteractor
import com.example.weanimals.adoption.confirmation.interactor.SubmitAdoptionApplicationInteractor
import com.example.weanimals.adoption.confirmation.repository.AdoptionApplicationRepository
import com.example.weanimals.adoption.listing.FakeAnimalRepository
import com.example.weanimals.adoption.listing.FakeUserLocationRepository
import com.example.weanimals.adoption.listing.animal
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.interactor.BuildAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.interactor.GetAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.repository.AdoptionProfileRepository
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionConfirmationPresenterTest {
    private val animals = FakeAnimalRepository()
    private val profiles = FakeProfileRepository()
    private val applications = FakeApplicationRepository()
    private val view = RecordingView()
    private lateinit var presenter: AdoptionConfirmationPresenter

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        animals.byIdAnswer = { Result.success(animal(it)) }
        profiles.profile = completeProfile()
        val recommendations = GetAnimalRecommendationsInteractor(
            repository = object : AnimalRecommendationRepository {
                override suspend fun getAvailableAnimals(): Result<List<Animal>> =
                    Result.success(emptyList())
            },
            getUserLocation = GetUserLocationInteractor(FakeUserLocationRepository()),
            calculateDistance = CalculateDistanceInteractor()
        )
        presenter = AdoptionConfirmationPresenter(
            animalId = "canela",
            getCandidate = GetAdoptionCandidateInteractor(
                GetAdoptionProfileInteractor(profiles), animals, recommendations
            ),
            submitApplication = SubmitAdoptionApplicationInteractor(applications)
        )
        presenter.attachView(view)
    }

    @After fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test fun loadsAnimalAndCalculatedMatchThenSubmitsOnce() = runTest {
        presenter.start()
        advanceUntilIdle()
        assertEquals("canela", view.candidate?.animal?.id)
        assertTrue(view.candidate!!.matchPercentage in 0..100)
        assertFalse(view.loading)

        presenter.onSubmitClicked()
        presenter.onSubmitClicked()
        advanceUntilIdle()
        assertEquals(1, applications.submissions)
        assertTrue(view.submitted)
        presenter.onSubmitClicked()
        assertEquals(1, applications.submissions)
    }

    @Test fun alreadyAppliedIsShownAsSubmittedNotRetried() = runTest {
        applications.result = Result.failure(AlreadyAppliedException())
        presenter.start()
        advanceUntilIdle()
        presenter.onSubmitClicked()
        advanceUntilIdle()
        assertTrue(view.submitted)
        assertEquals(1, applications.submissions)
    }

    @Test fun demoFailureDoesNotClaimAnApplicationWasSent() = runTest {
        applications.result = Result.failure(DemoApplicationUnavailableException())
        presenter.start()
        advanceUntilIdle()
        presenter.onSubmitClicked()
        advanceUntilIdle()
        assertFalse(view.submitted)
        assertTrue(view.submitError is DemoApplicationUnavailableException)
        assertFalse(view.submitting)
    }

    @Test fun missingQuestionnaireProfileBlocksConfirmation() = runTest {
        profiles.profile = null
        presenter.start()
        advanceUntilIdle()
        assertNotNull(view.loadError)
        assertEquals(0, applications.submissions)
    }

    private class FakeProfileRepository : AdoptionProfileRepository {
        var profile: AdoptionProfile? = null
        override suspend fun getProfile(): Result<AdoptionProfile?> = Result.success(profile)
        override suspend fun saveProfile(profile: AdoptionProfile): Result<Unit> = Result.success(Unit)
    }

    private class FakeApplicationRepository : AdoptionApplicationRepository {
        var result: Result<Unit> = Result.success(Unit)
        var submissions = 0
        override suspend fun submit(candidate: AdoptionCandidate): Result<Unit> {
            submissions++
            return result
        }
    }

    private class RecordingView : AdoptionConfirmationContract.View {
        var loading = false
        var candidate: AdoptionCandidate? = null
        var loadError: Throwable? = null
        var submitting = false
        var submitted = false
        var submitError: Throwable? = null
        override fun showLoading() { loading = true }
        override fun showCandidate(candidate: AdoptionCandidate) {
            loading = false
            this.candidate = candidate
        }
        override fun showLoadError(error: Throwable) {
            loading = false
            loadError = error
        }
        override fun showSubmitting(submitting: Boolean) { this.submitting = submitting }
        override fun showSubmitted() { submitted = true }
        override fun showSubmitError(error: Throwable) { submitError = error }
        override fun closeScreen() = Unit
    }

    private fun completeProfile() = BuildAdoptionProfileInteractor()(
        AdoptionQuestionnaireAnswers(
            routine = RoutineOption.HOME_OFFICE,
            availableSpace = AvailableSpaceOption.APARTMENT_WITH_BALCONY,
            petExperience = PetExperienceOption.HAD_PETS_BEFORE,
            dailyTime = DailyTimeOption.ONE_TO_THREE_HOURS,
            household = HouseholdOption.LIVES_ALONE
        )
    )
}
