package com.example.weanimals.adoption.questionnaire.interactor

import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
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

class BuildAdoptionProfileInteractor {
    operator fun invoke(answers: AdoptionQuestionnaireAnswers): AdoptionProfile {
        require(answers.isComplete) { "All questionnaire answers are required." }
        return AdoptionProfile(
            answers = answers,
            sizeProfile = when (answers.availableSpace) {
                AvailableSpaceOption.SMALL_APARTMENT -> SizeProfile.SMALL
                AvailableSpaceOption.APARTMENT_WITH_BALCONY -> SizeProfile.SMALL_TO_MEDIUM
                AvailableSpaceOption.HOUSE_WITH_YARD -> SizeProfile.ANY
                null -> error("Available space is required.")
            },
            temperamentProfile = when (answers.routine) {
                RoutineOption.AWAY_ALL_DAY,
                RoutineOption.HOME_OFFICE -> TemperamentProfile.CALM_AND_INDEPENDENT
                RoutineOption.OFTEN_HOME -> TemperamentProfile.SOCIAL_AND_ACTIVE
                null -> error("Routine is required.")
            },
            energyProfile = when (answers.dailyTime) {
                DailyTimeOption.LESS_THAN_ONE_HOUR -> EnergyProfile.LOW
                DailyTimeOption.ONE_TO_THREE_HOURS -> EnergyProfile.MODERATE
                DailyTimeOption.MORE_THAN_THREE_HOURS -> EnergyProfile.HIGH
                null -> error("Daily time is required.")
            },
            ageProfile = when (answers.petExperience) {
                PetExperienceOption.FIRST_TIME,
                PetExperienceOption.HAD_PETS_BEFORE -> AgeProfile.ADULT
                PetExperienceOption.HAS_OTHER_PETS -> AgeProfile.YOUNG_TO_ADULT
                null -> error("Pet experience is required.")
            },
            requiresGoodWithChildren = answers.household == HouseholdOption.YOUNG_CHILDREN,
            requiresGoodWithOtherAnimals = answers.household == HouseholdOption.OTHER_PETS ||
                answers.petExperience == PetExperienceOption.HAS_OTHER_PETS,
            updatedAtMillis = System.currentTimeMillis()
        )
    }
}
