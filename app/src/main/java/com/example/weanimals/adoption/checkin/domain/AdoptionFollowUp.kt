package com.example.weanimals.adoption.checkin.domain

data class AdoptionFollowUp(
    val animalId: String,
    val animalName: String,
    val adoptedAtMillis: Long,
    val adopterName: String?,
    val photoUrl: String?,
    val checkIns: Map<Int, AdoptionCheckIn>
)

data class AdoptionCheckIn(
    val milestoneDays: Int,
    val rating: AdaptationRating,
    val note: String,
    val submittedAtMillis: Long,
    val answers: Map<FollowUpQuestionId, FollowUpAnswer> = emptyMap()
)

enum class AdaptationRating(val storageValue: String) {
    DIFFICULT("difficult"),
    SOME_CHALLENGES("some_challenges"),
    GOOD("good"),
    EXCELLENT("excellent")
}

enum class FollowUpAnswer(val storageValue: String) {
    YES("yes"),
    NO("no"),
    PARTLY("partly"),
    YES_CERTAINLY("yes_certainly"),
    MAYBE("maybe")
}

enum class FollowUpQuestionId(val storageValue: String) {
    ROUTINE_ADJUSTED("routine_adjusted"),
    HEALTH_OR_BEHAVIOR_ISSUE("health_or_behavior_issue"),
    WALKS_AND_FEEDING_ESTABLISHED("walks_and_feeding_established"),
    BEHAVIOR_CHANGED("behavior_changed"),
    REGULAR_VET_VISITS("regular_vet_visits"),
    STRONG_BOND("strong_bond"),
    SHELTER_SUPPORT_NEEDED("shelter_support_needed"),
    RECOMMEND_ADOPTION("recommend_adoption")
}

data class FollowUpQuestion(
    val id: FollowUpQuestionId,
    val options: List<FollowUpAnswer>
)

enum class MilestoneState { COMPLETED, AVAILABLE, SCHEDULED }

data class FollowUpMilestone(
    val days: Int,
    val dueAtMillis: Long,
    val state: MilestoneState,
    val checkIn: AdoptionCheckIn? = null
)

class FollowUpNotReadyException : IllegalStateException("Adoption follow-up is not available yet.")

class AdoptionDateMissingException : IllegalStateException("The actual adoption date has not been recorded.")
