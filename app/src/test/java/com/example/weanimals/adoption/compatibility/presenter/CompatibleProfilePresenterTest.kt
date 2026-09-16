package com.example.weanimals.adoption.compatibility.presenter

import com.example.weanimals.adoption.compatibility.domain.AnimalRecommendation
import com.example.weanimals.adoption.compatibility.interactor.GetAnimalRecommendationsInteractor
import com.example.weanimals.adoption.compatibility.repository.AnimalRecommendationRepository
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
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.core.location.repository.UserLocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompatibleProfilePresenterTest {
    private val profileRepository = FakeProfileRepository()
    private val recommendationRepository = FakeRecommendationRepository()
    private val view = RecordingView()
    private lateinit var presenter: CompatibleProfilePresenter

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = CompatibleProfilePresenter(
            originAnimalId = "animal-origin",
            getProfile = GetAdoptionProfileInteractor(profileRepository),
            getRecommendations = GetAnimalRecommendationsInteractor(
                repository = recommendationRepository,
                getUserLocation = GetUserLocationInteractor(
                    FakeUserLocationRepository(Coordinates(0.0, 0.0))
                ),
                calculateDistance = CalculateDistanceInteractor()
            )
        )
        presenter.attachView(view)
    }

    @After
    fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test
    fun missingProfileReturnsToQuestionnaire() = runTest {
        profileRepository.result = Result.success(null)

        presenter.start()
        advanceUntilIdle()

        assertEquals("animal-origin", view.questionnaireAnimalId)
        assertNull(view.profile)
    }

    @Test
    fun savedProfileShowsAvailableRecommendations() = runTest {
        val profile = adoptionProfile()
        profileRepository.result = Result.success(profile)
        recommendationRepository.animals = listOf(animal("animal-1"))

        presenter.start()
        advanceUntilIdle()

        assertSame(profile, view.profile)
        assertEquals("animal-1", view.recommendations.single().animal.id)
        assertTrue(!view.loading)

        presenter.onAnimalClicked("animal-1")
        assertEquals("animal-1", view.openedAnimalId)
    }

    @Test
    fun editOpensQuestionnaireWithSavedAnswersAndReloadsAfterSaving() = runTest {
        val original = adoptionProfile()
        profileRepository.result = Result.success(original)
        recommendationRepository.animals = listOf(animal("animal-1"))
        presenter.start()
        advanceUntilIdle()

        presenter.onEditAnswersClicked()
        assertEquals("animal-origin", view.editAnimalId)
        assertEquals(original.answers, view.editAnswers)

        val updated = BuildAdoptionProfileInteractor()(
            original.answers.copy(routine = RoutineOption.OFTEN_HOME)
        )
        profileRepository.result = Result.success(updated)
        recommendationRepository.animals = listOf(animal("animal-2"))
        presenter.onAnswersUpdated()
        advanceUntilIdle()
        assertSame(updated, view.profile)
        assertEquals("animal-2", view.recommendations.single().animal.id)
    }

    private class FakeProfileRepository : AdoptionProfileRepository {
        var result: Result<AdoptionProfile?> = Result.success(null)
        override suspend fun getProfile() = result
        override suspend fun saveProfile(profile: AdoptionProfile) = Result.success(Unit)
    }

    private class FakeRecommendationRepository : AnimalRecommendationRepository {
        var animals: List<Animal> = emptyList()
        override suspend fun getAvailableAnimals() = Result.success(animals)
    }

    private class FakeUserLocationRepository(
        private val coordinates: Coordinates?
    ) : UserLocationRepository {
        override suspend fun getUserLocation() = coordinates
    }

    private class RecordingView : CompatibleProfileContract.View {
        var loading = false
        var profile: AdoptionProfile? = null
        var recommendations = emptyList<AnimalRecommendation>()
        var error: Throwable? = null
        var questionnaireAnimalId: String? = null
        var editAnimalId: String? = null
        var editAnswers: AdoptionQuestionnaireAnswers? = null
        var openedAnimalId: String? = null

        override fun showLoading() { loading = true }
        override fun showProfile(
            profile: AdoptionProfile,
            recommendations: List<AnimalRecommendation>
        ) {
            loading = false
            this.profile = profile
            this.recommendations = recommendations
        }
        override fun showEmpty(profile: AdoptionProfile) {
            loading = false
            this.profile = profile
            recommendations = emptyList()
        }
        override fun showError(error: Throwable) {
            loading = false
            this.error = error
        }
        override fun openQuestionnaire(animalId: String) {
            questionnaireAnimalId = animalId
        }
        override fun openQuestionnaireForEditing(
            animalId: String,
            answers: AdoptionQuestionnaireAnswers
        ) {
            editAnimalId = animalId
            editAnswers = answers
        }
        override fun openAnimalDetails(animalId: String) { openedAnimalId = animalId }
        override fun closeScreen() = Unit
    }

    private fun adoptionProfile() = BuildAdoptionProfileInteractor()(
        AdoptionQuestionnaireAnswers(
            routine = RoutineOption.HOME_OFFICE,
            availableSpace = AvailableSpaceOption.APARTMENT_WITH_BALCONY,
            petExperience = PetExperienceOption.HAD_PETS_BEFORE,
            dailyTime = DailyTimeOption.ONE_TO_THREE_HOURS,
            household = HouseholdOption.LIVES_ALONE
        )
    )
}
