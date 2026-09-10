package com.example.weanimals.adoption.listing.repository

import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalEnergyLevel
import com.example.weanimals.adoption.listing.domain.AnimalPage
import com.example.weanimals.adoption.listing.domain.AnimalPageCursor
import com.example.weanimals.adoption.listing.domain.AnimalStatus
import com.example.weanimals.adoption.listing.domain.Species
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

class AnimalRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AnimalRepository {

    override suspend fun getAvailableAnimals(
        filter: SpeciesFilter?,
        lastDocument: AnimalPageCursor?,
        limit: Int
    ): Result<AnimalPage> = try {
        require(limit in 1..100)
        require(lastDocument == null || lastDocument is FirestoreCursor)
        if (auth.currentUser == null) auth.signInAnonymously().await()

        var query = firestore.collection(COLLECTION)
            .whereEqualTo("status", AnimalStatus.AVAILABLE.value)
        filter?.species?.let { query = query.whereEqualTo("especie", it.value) }
        query = query.orderBy("criado_em", Query.Direction.DESCENDING).limit(limit.toLong())
        (lastDocument as? FirestoreCursor)?.let { cursor ->
            require(cursor.species == filter?.species) { "Cursor belongs to a different filter." }
            query = query.startAfter(cursor.document)
        }

        // An offline cache miss must not be presented as an empty adoption catalog.
        val snapshot = query.get(Source.SERVER).await()
        val next = snapshot.documents.lastOrNull()
            ?.takeIf { snapshot.size() == limit }
            ?.let { FirestoreCursor(it, filter?.species) }
        Result.success(AnimalPage(snapshot.documents.mapNotNull(::toAnimal), next))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun toAnimal(document: DocumentSnapshot): Animal? {
        val data = document.data ?: return null
        val name = (data["nome"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val ngoId = (data["ong_id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val species = Species.entries.firstOrNull { it.value == data["especie"] } ?: return null
        val status = AnimalStatus.entries.firstOrNull { it.value == data["status"] } ?: return null
        val createdAt = document.getTimestamp("criado_em")?.toDate()?.time ?: return null
        val characteristics = (data["caracteristicas"] as? List<*>)
            ?.filterIsInstance<String>()
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val normalizedCharacteristics = characteristics.map(::normalize)
        return Animal(
            id = document.id,
            ngoId = ngoId,
            shelterName = data["abrigo_nome"] as? String ?: "",
            shelterAddress = data["abrigo_endereco"] as? String ?: "",
            name = name,
            species = species,
            breed = data["raca"] as? String ?: "",
            ageText = data["idade_texto"] as? String ?: "",
            ageYears = (data["idade_anos"] as? Number)?.toInt(),
            size = data["porte"] as? String ?: "",
            photoUrl = photoUrl(data),
            characteristics = characteristics,
            energyLevel = energyLevel(data["nivel_energia"] as? String),
            independent = data["independente"] as? Boolean
                ?: normalizedCharacteristics.takeIf {
                    it.any { value -> "independente" in value }
                }?.let { true },
            goodWithChildren = data["bom_com_criancas"] as? Boolean
                ?: normalizedCharacteristics.takeIf {
                    it.any { value -> "crianca" in value }
                }?.let { true },
            goodWithOtherAnimals = data["bom_com_outros_animais"] as? Boolean
                ?: normalizedCharacteristics.takeIf {
                    it.any { value -> "outros animais" in value || "outros caes" in value }
                }?.let { true },
            status = status,
            createdAtMillis = createdAt,
            latitude = (data["latitude"] as? Number)?.toDouble(),
            longitude = (data["longitude"] as? Number)?.toDouble()
        )
    }

    private fun energyLevel(value: String?): AnimalEnergyLevel? = when (value?.let(::normalize)) {
        "baixo", "baixa", "low" -> AnimalEnergyLevel.LOW
        "moderado", "moderada", "medium" -> AnimalEnergyLevel.MODERATE
        "alto", "alta", "high" -> AnimalEnergyLevel.HIGH
        else -> null
    }

    private fun photoUrl(data: Map<String, Any>): String? {
        val cover = (data["foto_url"] as? String)?.trim()?.takeIf(String::isNotEmpty)
        if (cover != null) return cover
        return (data["foto_urls"] as? List<*>)
            ?.filterIsInstance<String>()
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS, "")
        .lowercase()

    private data class FirestoreCursor(
        val document: DocumentSnapshot,
        val species: Species?
    ) : AnimalPageCursor

    private companion object {
        const val COLLECTION = "animais"
        val NON_SPACING_MARKS = "\\p{Mn}+".toRegex()
    }
}
