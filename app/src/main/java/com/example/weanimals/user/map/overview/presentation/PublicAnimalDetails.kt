package com.example.weanimals.user.map.overview.presentation

import com.example.weanimals.user.lostandfound.model.FoundPetAd
import java.text.Normalizer
import java.util.Locale

/** Public projection: no residential address, secret identifiers or verification answers. */
object PublicAnimalDetails {
    fun title(ad: FoundPetAd): String {
        val name = if (ad.category.equals("PERDIDO", true))
            ad.calledNames.firstOrNull()?.substringBefore(":")?.trim() else null
        return name?.takeIf { it.isNotBlank() } ?: ad.getDisplayTitle()
    }

    fun location(ad: FoundPetAd): String = listOfNotNull(
        ad.neighborhood?.takeIf { it.isNotBlank() }, ad.city?.takeIf { it.isNotBlank() }
    ).joinToString(" — ").ifBlank { "Localização aproximada não informada" }

    private fun parts(ad: FoundPetAd) = ad.marks.orEmpty().split('·', '\n', ';').map { it.trim() }
        .filter { part ->
            val normalized = Normalizer.normalize(part, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)
            part.isNotBlank() && !Regex(
                "segredo|secret|privad|identifica|microchip|verifica|resposta|contato|telefone|whatsapp|cpf|comportamento|passeia"
            ).containsMatchIn(normalized)
        }

    fun breed(ad: FoundPetAd): String? = parts(ad).firstOrNull {
        it.startsWith("Raça:", true) || it.startsWith("Raca:", true)
    }?.substringAfter(":")?.trim()?.takeIf { it.isNotBlank() }

    fun marks(ad: FoundPetAd): String = parts(ad).filterNot {
        it.startsWith("Raça:", true) || it.startsWith("Raca:", true)
    }.joinToString(" · ")

    fun date(ad: FoundPetAd): String = ad.whenDate?.takeIf { it.isNotBlank() }
        ?: ad.whenFound?.takeIf { it.isNotBlank() } ?: "Data não informada"

    fun physicalDescription(ad: FoundPetAd): String = listOfNotNull(
        ad.species.takeIf { it.isNotBlank() }?.let { "Espécie: $it" },
        ad.size?.takeIf { it.isNotBlank() }?.let { "Porte: $it" },
        ad.ageApprox?.takeIf { it.isNotBlank() }?.let { "Idade: $it" },
        breed(ad)?.let { "Raça: $it" },
        marks(ad).takeIf { it.isNotBlank() }?.let { "Marcas: $it" }
    ).joinToString("\n")
}
