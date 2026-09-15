package com.example.weanimals.adoption.detail.presenter

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.domain.AnimalShareDocument
import com.example.weanimals.adoption.detail.domain.EnergyLevel
import com.example.weanimals.adoption.detail.domain.Shelter
import com.example.weanimals.adoption.detail.interactor.GetAnimalDetailsInteractor
import com.example.weanimals.adoption.detail.interactor.GetShelterDistanceInteractor
import com.example.weanimals.adoption.detail.interactor.CreateAnimalSharePdfInteractor
import com.example.weanimals.adoption.detail.repository.AnimalDetailsRepository
import com.example.weanimals.adoption.detail.repository.AnimalShareRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailsPresenterTest {

    private val repository = FakeRepository()
    private val locationRepository = FakeLocationRepository(
        Coordinates(-23.5505, -46.6333)
    )
    private val profileRepository = FakeProfileRepository()
    private val shareRepository = FakeShareRepository()
    private val view = RecordingView()
    private lateinit var presenter: AnimalDetailsPresenter

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = AnimalDetailsPresenter(
            animalId = "animal-1",
            getAnimalDetails = GetAnimalDetailsInteractor(repository),
            getShelterDistance = GetShelterDistanceInteractor(
                GetUserLocationInteractor(locationRepository),
                CalculateDistanceInteractor()
            ),
            getAdoptionProfile = GetAdoptionProfileInteractor(profileRepository),
            createAnimalSharePdf = CreateAnimalSharePdfInteractor(shareRepository)
        )
        presenter.attachView(view)
    }

    @After
    fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test
    fun loadsAnimalAndCalculatesDistanceFromUserToShelter() = runTest {
        val details = animalDetails()
        repository.result = Result.success(details)

        presenter.start()
        advanceUntilIdle()

        assertSame(details, view.details)
        assertEquals(360.75, view.distanceKm!!, 1.0)
        assertEquals("Endereço", view.shelterAddress)
        assertFalse(view.loading)
        assertNull(view.error)
        assertEquals(1, repository.requests)
    }

    @Test
    fun requestsLocationAndCalculatesAfterAccessBecomesAvailable() = runTest {
        locationRepository.coordinates = null
        repository.result = Result.success(animalDetails())

        presenter.start()
        advanceUntilIdle()

        assertTrue(view.locationRequested)
        assertNull(view.distanceKm)

        locationRepository.coordinates = Coordinates(-23.5505, -46.6333)
        presenter.onLocationAccessResult(true)
        advanceUntilIdle()

        assertEquals(360.75, view.distanceKm!!, 1.0)
    }

    @Test
    fun retryLoadsAgainAfterFailure() = runTest {
        repository.result = Result.failure(IllegalStateException("offline"))
        presenter.start()
        advanceUntilIdle()
        assertTrue(view.error is IllegalStateException)

        repository.result = Result.success(animalDetails())
        presenter.onRetryClicked()
        advanceUntilIdle()

        assertEquals(2, repository.requests)
        assertEquals("Canela", view.details?.name)
    }

    @Test
    fun favoriteAndShareRequireLoadedAnimal() = runTest {
        presenter.onFavoriteClicked()
        presenter.onShareClicked()
        assertEquals(0, shareRepository.requests)
        assertFalse(view.favorite)

        repository.result = Result.success(animalDetails())
        presenter.start()
        advanceUntilIdle()
        presenter.onFavoriteClicked()
        presenter.onShareClicked()
        advanceUntilIdle()

        assertTrue(view.favorite)
        assertEquals("Canela", shareRepository.details?.name)
        assertEquals(360.75, shareRepository.distanceKm!!, 1.0)
        assertEquals("animal.pdf", view.sharedDocument?.displayName)
        assertFalse(view.shareLoading)
    }

    @Test
    fun firstAdoptionInterestOpensQuestionnaire() = runTest {
        repository.result = Result.success(animalDetails())
        profileRepository.result = Result.success(null)
        presenter.start()
        advanceUntilIdle()

        presenter.onAdoptClicked()
        advanceUntilIdle()

        assertEquals("animal-1", view.questionnaireAnimalId)
        assertNull(view.confirmationAnimalId)
        assertFalse(view.adoptionProfileLoading)
    }

    @Test
    fun savedAdoptionProfileSkipsQuestionnaire() = runTest {
        repository.result = Result.success(animalDetails())
        profileRepository.result = Result.success(adoptionProfile())
        presenter.start()
        advanceUntilIdle()

        presenter.onAdoptClicked()
        advanceUntilIdle()

        assertEquals("animal-1", view.confirmationAnimalId)
        assertNull(view.questionnaireAnimalId)
        assertFalse(view.adoptionProfileLoading)
    }

    private class FakeRepository : AnimalDetailsRepository {
        var result: Result<AnimalDetails> = Result.failure(IllegalStateException())
        var requests = 0

        override suspend fun getAnimalDetails(animalId: String): Result<AnimalDetails> {
            requests++
            return result
        }
    }

    private class FakeLocationRepository(
        var coordinates: Coordinates?
    ) : UserLocationRepository {
        override suspend fun getUserLocation() = coordinates
    }

    private class FakeProfileRepository : AdoptionProfileRepository {
        var result: Result<AdoptionProfile?> = Result.success(null)
        override suspend fun getProfile() = result
        override suspend fun saveProfile(profile: AdoptionProfile) = Result.success(Unit)
    }

    private class FakeShareRepository : AnimalShareRepository {
        var requests = 0
        var details: AnimalDetails? = null
        var distanceKm: Double? = null
        var result = Result.success(
            AnimalShareDocument("C:/cache/animal.pdf", "animal.pdf", "application/pdf")
        )

        override suspend fun createPdf(
            details: AnimalDetails,
            distanceKm: Double?
        ): Result<AnimalShareDocument> {
            requests++
            this.details = details
            this.distanceKm = distanceKm
            return result
        }
    }

    private class RecordingView : AnimalDetailsContract.View {
        var loading = false
        var details: AnimalDetails? = null
        var distanceKm: Double? = null
        var error: Throwable? = null
        var favorite = false
        var shareLoading = false
        var sharedDocument: AnimalShareDocument? = null
        var shareError: Throwable? = null
        var shelterAddress = ""
        var locationRequested = false
        var adoptionProfileLoading = false
        var adoptionProfileError: Throwable? = null
        var questionnaireAnimalId: String? = null
        var confirmationAnimalId: String? = null

        override fun showLoading() { loading = true }
        override fun showAnimal(details: AnimalDetails) {
            loading = false
            this.details = details
        }
        override fun showDistance(distanceKm: Double?, shelterAddress: String) {
            this.distanceKm = distanceKm
            this.shelterAddress = shelterAddress
        }
        override fun showError(error: Throwable) {
            loading = false
            this.error = error
        }
        override fun showFavorite(favorite: Boolean) { this.favorite = favorite }
        override fun showShareLoading(loading: Boolean) { shareLoading = loading }
        override fun shareAnimalDocument(document: AnimalShareDocument) {
            sharedDocument = document
        }
        override fun showShareError(error: Throwable) { shareError = error }
        override fun requestUserLocation() { locationRequested = true }
        override fun showAdoptionProfileLoading(loading: Boolean) {
            adoptionProfileLoading = loading
        }
        override fun showAdoptionProfileError(error: Throwable) { adoptionProfileError = error }
        override fun openAdoptionQuestionnaire(animalId: String) {
            questionnaireAnimalId = animalId
        }
        override fun openAdoptionConfirmation(animalId: String) {
            confirmationAnimalId = animalId
        }
        override fun closeScreen() = Unit
        override fun showActionUnavailable() = Unit
    }

    private fun animalDetails() = AnimalDetails(
        id = "animal-1",
        name = "Canela",
        photoUrls = emptyList(),
        breed = "SRD",
        ageText = "2 anos",
        size = "Porte médio",
        vaccinated = true,
        neutered = true,
        goodWithChildren = true,
        goodWithOtherAnimals = true,
        energyLevel = EnergyLevel.MODERATE,
        description = "Descrição",
        adoptionRequirements = listOf("Requisito"),
        createdAtMillis = 1L,
        shelter = Shelter(
            id = "shelter",
            name = "Abrigo",
            address = "Endereço",
            coordinates = Coordinates(-22.9068, -43.1729)
        )
    )

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
