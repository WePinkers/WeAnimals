package com.example.weanimals.adoption.compatibility.interactor

import com.example.weanimals.adoption.compatibility.repository.AnimalRecommendationRepository
import com.example.weanimals.adoption.listing.animal
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalEnergyLevel
import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.interactor.BuildAdoptionProfileInteractor
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.core.location.repository.UserLocationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAnimalRecommendationsInteractorTest {

    @Test
    fun recommendsBestMatchingAvailableAnimalFirst() = runTest {
        val matching = animal("matching").copy(
            size = "Porte médio",
            ageYears = 2,
            energyLevel = AnimalEnergyLevel.MODERATE,
            independent = true
        )
        val incompatible = animal("incompatible").copy(
            size = "Porte grande",
            ageYears = 9,
            energyLevel = AnimalEnergyLevel.HIGH,
            independent = false
        )
        val repository = FakeRecommendationRepository(listOf(incompatible, matching))
        val interactor = GetAnimalRecommendationsInteractor(
            repository,
            GetUserLocationInteractor(FakeUserLocationRepository(Coordinates(0.0, 0.0))),
            CalculateDistanceInteractor()
        )

        val recommendations = interactor(profile()).getOrThrow()

        assertEquals("matching", recommendations.first().animal.id)
        assertTrue(recommendations.first().matchPercentage > recommendations.last().matchPercentage)
        assertNotNull(recommendations.first().distanceKm)
    }

    private fun profile() = BuildAdoptionProfileInteractor()(
        AdoptionQuestionnaireAnswers(
            routine = RoutineOption.HOME_OFFICE,
            availableSpace = AvailableSpaceOption.APARTMENT_WITH_BALCONY,
            petExperience = PetExperienceOption.HAD_PETS_BEFORE,
            dailyTime = DailyTimeOption.ONE_TO_THREE_HOURS,
            household = HouseholdOption.LIVES_ALONE
        )
    )

    private class FakeRecommendationRepository(
        private val animals: List<Animal>
    ) : AnimalRecommendationRepository {
        override suspend fun getAvailableAnimals() = Result.success(animals)
    }

    private class FakeUserLocationRepository(
        private val coordinates: Coordinates?
    ) : UserLocationRepository {
        override suspend fun getUserLocation(): Coordinates? = coordinates
    }
}
