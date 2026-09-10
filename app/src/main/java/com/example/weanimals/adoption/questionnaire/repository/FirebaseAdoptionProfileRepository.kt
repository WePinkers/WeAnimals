package com.example.weanimals.adoption.questionnaire.repository

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAdoptionProfileRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AdoptionProfileRepository {

    override suspend fun getProfile(): Result<AdoptionProfile?> = try {
        val userId = authenticatedUserId()
        val document = firestore.collection(COLLECTION).document(userId).get().await()
        Result.success(if (document.exists()) document.toProfile() else null)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun saveProfile(profile: AdoptionProfile): Result<Unit> = try {
        val userId = authenticatedUserId()
        firestore.collection(COLLECTION).document(userId).set(profile.toMap()).await()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun authenticatedUserId(): String {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        return requireNotNull(auth.currentUser?.uid) { "Authenticated user is unavailable." }
    }

    private fun AdoptionProfile.toMap(): Map<String, Any> = mapOf(
        "answers" to mapOf(
            "routine" to answers.routine!!.storageValue,
            "available_space" to answers.availableSpace!!.storageValue,
            "pet_experience" to answers.petExperience!!.storageValue,
            "daily_time" to answers.dailyTime!!.storageValue,
            "household" to answers.household!!.storageValue
        ),
        "size_profile" to sizeProfile.storageValue,
        "temperament_profile" to temperamentProfile.storageValue,
        "energy_profile" to energyProfile.storageValue,
        "age_profile" to ageProfile.storageValue,
        "requires_good_with_children" to requiresGoodWithChildren,
        "requires_good_with_other_animals" to requiresGoodWithOtherAnimals,
        "updated_at" to FieldValue.serverTimestamp()
    )

    private fun DocumentSnapshot.toProfile(): AdoptionProfile? {
        val data = data ?: return null
        val answersData = data["answers"] as? Map<*, *> ?: return null
        val answers = AdoptionQuestionnaireAnswers(
            routine = enumByValue<RoutineOption>(answersData["routine"]),
            availableSpace = enumByValue<AvailableSpaceOption>(answersData["available_space"]),
            petExperience = enumByValue<PetExperienceOption>(answersData["pet_experience"]),
            dailyTime = enumByValue<DailyTimeOption>(answersData["daily_time"]),
            household = enumByValue<HouseholdOption>(answersData["household"])
        )
        if (!answers.isComplete) return null
        return AdoptionProfile(
            answers = answers,
            sizeProfile = enumByValue<SizeProfile>(data["size_profile"]) ?: return null,
            temperamentProfile = enumByValue<TemperamentProfile>(data["temperament_profile"])
                ?: return null,
            energyProfile = enumByValue<EnergyProfile>(data["energy_profile"]) ?: return null,
            ageProfile = enumByValue<AgeProfile>(data["age_profile"]) ?: return null,
            requiresGoodWithChildren = data["requires_good_with_children"] as? Boolean ?: false,
            requiresGoodWithOtherAnimals = data["requires_good_with_other_animals"] as? Boolean
                ?: false,
            updatedAtMillis = getTimestamp("updated_at")?.toDate()?.time ?: 0L
        )
    }

    private inline fun <reified T> enumByValue(rawValue: Any?): T? where T : Enum<T> {
        val value = rawValue as? String ?: return null
        return enumValues<T>().firstOrNull { enumValue ->
            when (enumValue) {
                is RoutineOption -> enumValue.storageValue == value
                is AvailableSpaceOption -> enumValue.storageValue == value
                is PetExperienceOption -> enumValue.storageValue == value
                is DailyTimeOption -> enumValue.storageValue == value
                is HouseholdOption -> enumValue.storageValue == value
                is SizeProfile -> enumValue.storageValue == value
                is TemperamentProfile -> enumValue.storageValue == value
                is EnergyProfile -> enumValue.storageValue == value
                is AgeProfile -> enumValue.storageValue == value
                else -> false
            }
        }
    }

    private companion object {
        const val COLLECTION = "adoption_profiles"
    }
}
