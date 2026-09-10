package com.example.weanimals.adoption.compatibility.interactor

import com.example.weanimals.adoption.compatibility.domain.AnimalRecommendation
import com.example.weanimals.adoption.compatibility.repository.AnimalRecommendationRepository
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalEnergyLevel
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.domain.AgeProfile
import com.example.weanimals.adoption.questionnaire.domain.EnergyProfile
import com.example.weanimals.adoption.questionnaire.domain.SizeProfile
import com.example.weanimals.adoption.questionnaire.domain.TemperamentProfile
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import kotlinx.coroutines.CancellationException
import java.text.Normalizer
import kotlin.math.roundToInt

class GetAnimalRecommendationsInteractor(
    private val repository: AnimalRecommendationRepository,
    private val getUserLocation: GetUserLocationInteractor,
    private val calculateDistance: CalculateDistanceInteractor
) {
    suspend operator fun invoke(profile: AdoptionProfile): Result<List<AnimalRecommendation>> = try {
        val animals = repository.getAvailableAnimals().getOrThrow()
        val userCoordinates = getUserLocation()
        val recommendations = animals.map { animal ->
            AnimalRecommendation(
                animal = animal,
                matchPercentage = score(profile, animal),
                distanceKm = distance(userCoordinates, animal)
            )
        }.sortedWith(
            compareByDescending<AnimalRecommendation> { it.matchPercentage }
                .thenBy { it.distanceKm ?: Double.POSITIVE_INFINITY }
                .thenByDescending { it.animal.createdAtMillis }
        )
        Result.success(recommendations)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun score(profile: AdoptionProfile, animal: Animal): Int {
        val scores = listOf(
            sizeScore(profile, animal),
            energyScore(profile, animal),
            ageScore(profile, animal),
            temperamentScore(profile, animal),
            householdScore(profile, animal)
        )
        return scores.average().roundToInt().coerceIn(0, 100)
    }

    private fun sizeScore(profile: AdoptionProfile, animal: Animal): Int {
        val size = normalize(animal.size)
        val isSmall = "pequeno" in size || "small" in size
        val isMedium = "medio" in size || "medium" in size
        return when (profile.sizeProfile) {
            SizeProfile.SMALL -> when {
                isSmall -> 100
                isMedium -> 55
                else -> 20
            }
            SizeProfile.SMALL_TO_MEDIUM -> if (isSmall || isMedium) 100 else 45
            SizeProfile.ANY -> 100
        }
    }

    private fun energyScore(profile: AdoptionProfile, animal: Animal): Int {
        val desired = when (profile.energyProfile) {
            EnergyProfile.LOW -> AnimalEnergyLevel.LOW
            EnergyProfile.MODERATE -> AnimalEnergyLevel.MODERATE
            EnergyProfile.HIGH -> AnimalEnergyLevel.HIGH
        }
        val actual = animal.energyLevel ?: return 60
        if (actual == desired) return 100
        return if (kotlin.math.abs(actual.ordinal - desired.ordinal) == 1) 65 else 25
    }

    private fun ageScore(profile: AdoptionProfile, animal: Animal): Int {
        val age = animal.ageYears ?: NUMBER.find(animal.ageText)?.value?.toIntOrNull() ?: return 60
        return when (profile.ageProfile) {
            AgeProfile.ADULT -> if (age in 1..5) 100 else 50
            AgeProfile.YOUNG_TO_ADULT -> if (age in 0..5) 100 else 55
            AgeProfile.ANY -> 100
        }
    }

    private fun temperamentScore(profile: AdoptionProfile, animal: Animal): Int {
        val traits = animal.characteristics.joinToString(" ").let(::normalize)
        return when (profile.temperamentProfile) {
            TemperamentProfile.CALM_AND_INDEPENDENT -> when {
                animal.independent == true -> 100
                "independente" in traits || "calmo" in traits || "docil" in traits -> 90
                animal.independent == false -> 35
                else -> 60
            }
            TemperamentProfile.BALANCED -> if (animal.energyLevel == AnimalEnergyLevel.MODERATE) 100 else 70
            TemperamentProfile.SOCIAL_AND_ACTIVE -> when {
                "sociavel" in traits || "brincalhao" in traits ||
                    animal.energyLevel == AnimalEnergyLevel.HIGH -> 100
                else -> 60
            }
        }
    }

    private fun householdScore(profile: AdoptionProfile, animal: Animal): Int {
        val requiredAnswers = buildList {
            if (profile.requiresGoodWithChildren) add(animal.goodWithChildren)
            if (profile.requiresGoodWithOtherAnimals) add(animal.goodWithOtherAnimals)
        }
        if (requiredAnswers.isEmpty()) return 100
        return requiredAnswers.map { answer ->
            when (answer) {
                true -> 100
                false -> 0
                null -> 45
            }
        }.average().roundToInt()
    }

    private fun distance(user: Coordinates?, animal: Animal): Double? {
        val shelter = Coordinates.fromOrNull(animal.latitude, animal.longitude)
        return if (user != null && shelter != null) calculateDistance(user, shelter) else null
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS, "")
        .lowercase()

    private companion object {
        val NUMBER = "\\d+".toRegex()
        val NON_SPACING_MARKS = "\\p{Mn}+".toRegex()
    }
}
