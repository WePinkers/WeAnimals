package com.example.weanimals.adoption.questionnaire.interactor

import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.adoption.questionnaire.domain.AgeProfile
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.adoption.questionnaire.domain.EnergyProfile
import com.example.weanimals.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.domain.SizeProfile
import com.example.weanimals.adoption.questionnaire.domain.TemperamentProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BuildAdoptionProfileInteractorTest {
    private val buildProfile = BuildAdoptionProfileInteractor()

    @Test
    fun buildsProfileFromQuestionnaireAnswers() {
        val profile = buildProfile(
            AdoptionQuestionnaireAnswers(
                routine = RoutineOption.HOME_OFFICE,
                availableSpace = AvailableSpaceOption.APARTMENT_WITH_BALCONY,
                petExperience = PetExperienceOption.HAD_PETS_BEFORE,
                dailyTime = DailyTimeOption.ONE_TO_THREE_HOURS,
                household = HouseholdOption.LIVES_ALONE
            )
        )

        assertEquals(SizeProfile.SMALL_TO_MEDIUM, profile.sizeProfile)
        assertEquals(TemperamentProfile.CALM_AND_INDEPENDENT, profile.temperamentProfile)
        assertEquals(EnergyProfile.MODERATE, profile.energyProfile)
        assertEquals(AgeProfile.ADULT, profile.ageProfile)
        assertFalse(profile.requiresGoodWithChildren)
        assertFalse(profile.requiresGoodWithOtherAnimals)
    }

    @Test
    fun rejectsIncompleteQuestionnaire() {
        assertThrows(IllegalArgumentException::class.java) {
            buildProfile(AdoptionQuestionnaireAnswers())
        }
    }
}
