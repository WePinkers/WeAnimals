package com.example.weanimals.adoption.questionnaire.domain

data class AdoptionQuestionnaireAnswers(
    val routine: RoutineOption? = null,
    val availableSpace: AvailableSpaceOption? = null,
    val petExperience: PetExperienceOption? = null,
    val dailyTime: DailyTimeOption? = null,
    val household: HouseholdOption? = null
) {
    val isComplete: Boolean
        get() = routine != null && availableSpace != null && petExperience != null &&
            dailyTime != null && household != null
}

enum class RoutineOption(val storageValue: String) {
    AWAY_ALL_DAY("away_all_day"),
    HOME_OFFICE("home_office"),
    OFTEN_HOME("often_home")
}

enum class AvailableSpaceOption(val storageValue: String) {
    SMALL_APARTMENT("small_apartment"),
    APARTMENT_WITH_BALCONY("apartment_with_balcony"),
    HOUSE_WITH_YARD("house_with_yard")
}

enum class PetExperienceOption(val storageValue: String) {
    FIRST_TIME("first_time"),
    HAD_PETS_BEFORE("had_pets_before"),
    HAS_OTHER_PETS("has_other_pets")
}

enum class DailyTimeOption(val storageValue: String) {
    LESS_THAN_ONE_HOUR("less_than_one_hour"),
    ONE_TO_THREE_HOURS("one_to_three_hours"),
    MORE_THAN_THREE_HOURS("more_than_three_hours")
}

enum class HouseholdOption(val storageValue: String) {
    LIVES_ALONE("lives_alone"),
    YOUNG_CHILDREN("young_children"),
    OTHER_PETS("other_pets")
}

data class AdoptionProfile(
    val answers: AdoptionQuestionnaireAnswers,
    val sizeProfile: SizeProfile,
    val temperamentProfile: TemperamentProfile,
    val energyProfile: EnergyProfile,
    val ageProfile: AgeProfile,
    val requiresGoodWithChildren: Boolean,
    val requiresGoodWithOtherAnimals: Boolean,
    val updatedAtMillis: Long
)

enum class SizeProfile(val storageValue: String) {
    SMALL("small"),
    SMALL_TO_MEDIUM("small_to_medium"),
    ANY("any")
}

enum class TemperamentProfile(val storageValue: String) {
    CALM_AND_INDEPENDENT("calm_and_independent"),
    BALANCED("balanced"),
    SOCIAL_AND_ACTIVE("social_and_active")
}

enum class EnergyProfile(val storageValue: String) {
    LOW("low"),
    MODERATE("moderate"),
    HIGH("high")
}

enum class AgeProfile(val storageValue: String) {
    ADULT("adult"),
    YOUNG_TO_ADULT("young_to_adult"),
    ANY("any")
}
