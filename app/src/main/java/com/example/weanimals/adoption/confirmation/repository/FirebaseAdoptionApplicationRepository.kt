package com.example.weanimals.adoption.confirmation.repository

import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.confirmation.domain.AlreadyAppliedException
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAdoptionApplicationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AdoptionApplicationRepository {

    override suspend fun submit(candidate: AdoptionCandidate): Result<Unit> = try {
        require(candidate.animal.id.isNotBlank() && candidate.animal.ngoId.isNotBlank())
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val userId = requireNotNull(auth.currentUser?.uid)
        val animalId = candidate.animal.id
        val animalRef = firestore.collection("animais").document(animalId)
        val applicationRef = firestore.collection("adoption_applications")
            .document(userId).collection("animals").document(animalId)

        val alreadyApplied = firestore.runTransaction { transaction ->
            if (transaction.get(applicationRef).exists()) {
                true
            } else {
                val animal = transaction.get(animalRef)
                check(animal.exists() && animal.getString("status") == "disponivel") {
                    "Animal is not available."
                }
                check(animal.getString("ong_id") == candidate.animal.ngoId) {
                    "Shelter has changed."
                }
                transaction.set(applicationRef, mapOf(
                    "userId" to userId,
                    "animalId" to animalId,
                    "ngoId" to candidate.animal.ngoId,
                    "status" to "pending",
                    "matchPercentage" to candidate.matchPercentage,
                    "animalName" to animal.getString("nome").orEmpty(),
                    "shelterName" to animal.getString("abrigo_nome").orEmpty(),
                    "breed" to animal.getString("raca").orEmpty(),
                    "ageText" to animal.getString("idade_texto").orEmpty(),
                    "photoUrl" to (animal.getString("foto_url")?.takeIf(String::isNotBlank)
                        ?: (animal.get("foto_urls") as? List<*>)
                            ?.filterIsInstance<String>()?.firstOrNull(String::isNotBlank)).orEmpty(),
                    "profile" to candidate.profile.toSnapshot(),
                    "submittedAt" to FieldValue.serverTimestamp()
                ))
                false
            }
        }.await()
        if (alreadyApplied) Result.failure(AlreadyAppliedException()) else Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun AdoptionProfile.toSnapshot(): Map<String, Any> = mapOf(
        "routine" to requireNotNull(answers.routine).storageValue,
        "availableSpace" to requireNotNull(answers.availableSpace).storageValue,
        "petExperience" to requireNotNull(answers.petExperience).storageValue,
        "dailyTime" to requireNotNull(answers.dailyTime).storageValue,
        "household" to requireNotNull(answers.household).storageValue,
        "size" to sizeProfile.storageValue,
        "temperament" to temperamentProfile.storageValue,
        "energy" to energyProfile.storageValue,
        "age" to ageProfile.storageValue,
        "requiresGoodWithChildren" to requiresGoodWithChildren,
        "requiresGoodWithOtherAnimals" to requiresGoodWithOtherAnimals
    )
}
