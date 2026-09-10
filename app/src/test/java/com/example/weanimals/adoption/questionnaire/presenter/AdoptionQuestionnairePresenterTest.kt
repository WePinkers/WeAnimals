package com.example.weanimals.adoption.questionnaire.presenter

import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.interactor.BuildAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.interactor.SaveAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.repository.AdoptionProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdoptionQuestionnairePresenterTest {
    private val repository = FakeProfileRepository()
    private val view = RecordingView()
    private lateinit var presenter: AdoptionQuestionnairePresenter

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        presenter = AdoptionQuestionnairePresenter(
            animalId = "animal-1",
            buildProfile = BuildAdoptionProfileInteractor(),
            saveProfile = SaveAdoptionProfileInteractor(repository)
        )
        presenter.attachView(view)
    }

    @After
    fun tearDown() {
        presenter.destroy()
        Dispatchers.resetMain()
    }

    @Test
    fun incompleteQuestionnaireDoesNotSave() {
        presenter.onSubmitClicked()

        assertTrue(view.incomplete)
        assertEquals(0, repository.savedProfiles.size)
    }

    @Test
    fun completeQuestionnaireIsSavedBeforeOpeningProfile() = runTest {
        presenter.onRoutineSelected(RoutineOption.HOME_OFFICE)
        presenter.onAvailableSpaceSelected(AvailableSpaceOption.APARTMENT_WITH_BALCONY)
        presenter.onPetExperienceSelected(PetExperienceOption.HAD_PETS_BEFORE)
        presenter.onDailyTimeSelected(DailyTimeOption.ONE_TO_THREE_HOURS)
        presenter.onHouseholdSelected(HouseholdOption.LIVES_ALONE)
        presenter.onSubmitClicked()
        advanceUntilIdle()

        assertNotNull(repository.savedProfiles.single())
        assertEquals("animal-1", view.openedAnimalId)
        assertTrue(!view.saving)
    }

    private class FakeProfileRepository : AdoptionProfileRepository {
        val savedProfiles = mutableListOf<AdoptionProfile>()
        override suspend fun getProfile(): Result<AdoptionProfile?> = Result.success(null)
        override suspend fun saveProfile(profile: AdoptionProfile): Result<Unit> {
            savedProfiles += profile
            return Result.success(Unit)
        }
    }

    private class RecordingView : AdoptionQuestionnaireContract.View {
        var answers = AdoptionQuestionnaireAnswers()
        var incomplete = false
        var saving = false
        var error: Throwable? = null
        var openedAnimalId: String? = null
        override fun showAnswers(answers: AdoptionQuestionnaireAnswers) { this.answers = answers }
        override fun showIncompleteQuestionnaire() { incomplete = true }
        override fun showSaving() { saving = true }
        override fun hideSaving() { saving = false }
        override fun showSaveError(error: Throwable) { this.error = error }
        override fun openCompatibleProfile(animalId: String) { openedAnimalId = animalId }
        override fun closeScreen() = Unit
    }
}
