package com.example.weanimals.adoption.checkin.repository

import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.AdoptionCheckIn
import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.FollowUpNotReadyException
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionCatalog
import com.example.weanimals.adoption.checkin.domain.AdoptionDateMissingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAdoptionFollowUpRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AdoptionFollowUpRepository {

    override suspend fun getFollowUp(animalId: String): Result<AdoptionFollowUp> = try {
        val reference = applicationReference(animalId)
        val application = reference.get(Source.SERVER).await()
        if (!application.exists() || application.getString("status") != "adopted") {
            throw FollowUpNotReadyException()
        }
        val adoptedAt = application.getTimestamp("adoptedAt")?.toDate()?.time
            ?: throw AdoptionDateMissingException()
        val animalName = application.getString("animalName")
            ?.takeIf(String::isNotBlank) ?: throw FollowUpNotReadyException()
        val checkIns = reference.collection("checkins").get(Source.SERVER).await()
            .documents.mapNotNull { it.toCheckInOrNull() }
            .associateBy(AdoptionCheckIn::milestoneDays)
        Result.success(AdoptionFollowUp(
            animalId = animalId,
            animalName = animalName,
            adoptedAtMillis = adoptedAt,
            adopterName = auth.currentUser?.displayName?.takeIf(String::isNotBlank)
                ?: application.getString("adopterName")?.takeIf(String::isNotBlank),
            photoUrl = application.getString("photoUrl"),
            checkIns = checkIns
        ))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun submitCheckIn(
        animalId: String,
        milestoneDays: Int,
        rating: AdaptationRating,
        answers: Map<FollowUpQuestionId, FollowUpAnswer>,
        note: String
    ): Result<Unit> = try {
        require(milestoneDays in MILESTONES) { "Invalid follow-up milestone." }
        val questions = FollowUpQuestionCatalog.questionsFor(milestoneDays)
        require(answers.keys == questions.map { it.id }.toSet()) { "All milestone questions are required." }
        require(questions.all { answers[it.id] in it.options }) { "Invalid follow-up answer." }
        val normalizedNote = note.trim()
        require(normalizedNote.length <= MAX_NOTE_LENGTH) { "Note is too long." }
        applicationReference(animalId).collection("checkins")
            .document(milestoneDays.toString())
            .set(mapOf(
                "milestoneDays" to milestoneDays,
                "rating" to rating.storageValue,
                "answers" to answers.mapKeys { it.key.storageValue }
                    .mapValues { it.value.storageValue },
                "note" to normalizedNote,
                "submittedAt" to FieldValue.serverTimestamp()
            )).await()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun applicationReference(animalId: String): DocumentReference {
        require(animalId.isNotBlank()) { "Animal id is required." }
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val userId = requireNotNull(auth.currentUser?.uid)
        return firestore.collection("adoption_applications")
            .document(userId).collection("animals").document(animalId)
    }

    private fun DocumentSnapshot.toCheckInOrNull(): AdoptionCheckIn? {
        val days = getLong("milestoneDays")?.toInt() ?: return null
        if (days !in MILESTONES || id != days.toString()) return null
        val rating = AdaptationRating.entries.firstOrNull {
            it.storageValue == getString("rating")
        } ?: return null
        return AdoptionCheckIn(
            milestoneDays = days,
            rating = rating,
            note = getString("note").orEmpty(),
            submittedAtMillis = getTimestamp("submittedAt")?.toDate()?.time ?: 0L,
            answers = (get("answers") as? Map<*, *>)?.entries.orEmpty().mapNotNull { (key, value) ->
                val questionId = FollowUpQuestionId.entries.firstOrNull {
                    it.storageValue == key
                } ?: return@mapNotNull null
                val answer = FollowUpAnswer.entries.firstOrNull {
                    it.storageValue == value
                } ?: return@mapNotNull null
                questionId to answer
            }.toMap()
        )
    }

    private companion object {
        val MILESTONES = setOf(30, 90, 180)
        const val MAX_NOTE_LENGTH = 1000
    }
}
