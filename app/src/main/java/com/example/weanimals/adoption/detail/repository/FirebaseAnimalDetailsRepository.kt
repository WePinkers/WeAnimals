package com.example.weanimals.adoption.detail.repository

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.domain.EnergyLevel
import com.example.weanimals.adoption.detail.domain.Shelter
import com.example.weanimals.core.location.domain.Coordinates
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

class FirebaseAnimalDetailsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AnimalDetailsRepository {

    override suspend fun getAnimalDetails(animalId: String): Result<AnimalDetails> = try {
        require(animalId.isNotBlank())
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val document = firestore.collection(ANIMALS_COLLECTION)
            .document(animalId)
            .get(Source.SERVER)
            .await()
        Result.success(toAnimalDetails(document))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun toAnimalDetails(document: DocumentSnapshot): AnimalDetails {
        check(document.exists()) { "Animal not found." }
        val data = requireNotNull(document.data)
        check(data["status"] == AVAILABLE_STATUS) { "Animal is not available." }
        val characteristics = (data["caracteristicas"] as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty()
        val normalizedCharacteristics = characteristics.map(::normalize)

        return AnimalDetails(
            id = document.id,
            name = requiredString(data, "nome"),
            photoUrls = photoUrls(data),
            breed = string(data, "raca").orEmpty(),
            ageText = string(data, "idade_texto").orEmpty(),
            size = string(data, "porte").orEmpty(),
            vaccinated = data["vacinado"] as? Boolean
                ?: normalizedCharacteristics.any { "vacinad" in it },
            neutered = data["castrado"] as? Boolean
                ?: normalizedCharacteristics.any { "castrad" in it },
            goodWithChildren = data["bom_com_criancas"] as? Boolean
                ?: normalizedCharacteristics.takeIf { it.any { value -> "crianca" in value } }?.let { true },
            goodWithOtherAnimals = data["bom_com_outros_animais"] as? Boolean
                ?: normalizedCharacteristics.takeIf {
                    it.any { value -> "outros animais" in value || "outros caes" in value }
                }?.let { true },
            energyLevel = energyLevel(string(data, "nivel_energia")),
            description = string(data, "descricao").orEmpty(),
            adoptionRequirements = (data["requisitos_adocao"] as? List<*>)
                ?.filterIsInstance<String>()
                ?.filter(String::isNotBlank)
                .orEmpty(),
            createdAtMillis = document.getTimestamp("criado_em")?.toDate()?.time ?: 0L,
            shelter = shelter(data)
        )
    }

    private fun shelter(data: Map<String, Any>): Shelter? {
        val id = string(data, "ong_id").orEmpty()
        val name = string(data, "abrigo_nome").orEmpty()
        val address = string(data, "abrigo_endereco").orEmpty()
        val coordinates = Coordinates.fromOrNull(
            latitude = (data["latitude"] as? Number)?.toDouble(),
            longitude = (data["longitude"] as? Number)?.toDouble()
        )
        return Shelter(id, name, address, coordinates).takeIf {
            it.id.isNotBlank() || it.name.isNotBlank() ||
                it.address.isNotBlank() || it.coordinates != null
        }
    }

    private fun energyLevel(value: String?) = when (value?.let(::normalize)) {
        "baixo", "baixa", "low" -> EnergyLevel.LOW
        "moderado", "moderada", "medium" -> EnergyLevel.MODERATE
        "alto", "alta", "high" -> EnergyLevel.HIGH
        else -> null
    }

    private fun photoUrls(data: Map<String, Any>): List<String> {
        val gallery = (data["foto_urls"] as? List<*>)
            ?.filterIsInstance<String>()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        return (listOfNotNull(string(data, "foto_url")) + gallery).distinct()
    }

    private fun requiredString(data: Map<String, Any>, field: String): String =
        requireNotNull(string(data, field)) { "Missing field: $field" }

    private fun string(data: Map<String, Any>, field: String): String? =
        (data[field] as? String)?.trim()?.takeIf(String::isNotEmpty)

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS, "")
        .lowercase()

    private companion object {
        const val ANIMALS_COLLECTION = "animais"
        const val AVAILABLE_STATUS = "disponivel"
        val NON_SPACING_MARKS = "\\p{Mn}+".toRegex()
    }
}
