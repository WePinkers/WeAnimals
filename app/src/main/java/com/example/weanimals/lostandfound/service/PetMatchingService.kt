package com.example.weanimals.lostandfound.service

import com.example.weanimals.lostandfound.model.FoundPetAd
import java.text.Normalizer
import java.util.Locale

data class MatchCriteriaDetail(
    val criteriaName: String,
    val lostValue: String,
    val foundValue: String,
    val pointsAwarded: Int,
    val maxPoints: Int,
    val isMatch: Boolean
)

data class PetMatchResult(
    val foundAd: FoundPetAd,
    val score: Int,
    val hasEnoughData: Boolean,
    val matchingReasons: List<String>,
    val divergingReasons: List<String>,
    val detailsList: List<MatchCriteriaDetail>
)

object PetMatchingService {

    private val STOP_WORDS = setOf(
        "de", "da", "do", "das", "dos", "em", "na", "no", "nas", "nos",
        "um", "uma", "uns", "umas", "com", "sem", "por", "para", "e", "ou",
        "que", "esta", "este", "estava", "meio", "muito", "pouco", "tem", "possui"
    )

    private val LABEL_WORDS = setOf(
        "raca", "raca:", "marca", "marcas", "detalhe", "detalhes", "caracteristica", "caracteristicas"
    )

    fun calculateMatch(lostAd: FoundPetAd, foundAd: FoundPetAd): PetMatchResult {
        val details = mutableListOf<MatchCriteriaDetail>()
        val matchingReasons = mutableListOf<String>()
        val divergingReasons = mutableListOf<String>()

        var totalScore = 0
        var evaluatedCriteriaCount = 0

        // 1. ESPÉCIE (Peso 20)
        val lostSpeciesNorm = normalizeText(lostAd.species)
        val foundSpeciesNorm = normalizeText(foundAd.species)
        val speciesMatch = lostSpeciesNorm.isNotBlank() && foundSpeciesNorm.isNotBlank() && lostSpeciesNorm == foundSpeciesNorm

        if (speciesMatch) {
            totalScore += 20
            matchingReasons.add("Espécie compatível (${lostAd.species})")
            details.add(MatchCriteriaDetail("Espécie", lostAd.species, foundAd.species, 20, 20, true))
        } else {
            // Strong mismatch
            divergingReasons.add("Espécie diferente (${lostAd.species} vs ${foundAd.species})")
            details.add(MatchCriteriaDetail("Espécie", lostAd.species, foundAd.species, 0, 20, false))
            return PetMatchResult(
                foundAd = foundAd,
                score = 0,
                hasEnoughData = true,
                matchingReasons = emptyList(),
                divergingReasons = listOf("Espécie incompatível"),
                detailsList = details
            )
        }
        evaluatedCriteriaCount++

        // 2. MARCAS FÍSICAS E RAÇA (Peso 25) - Filtered to physical traits and breed
        val rawLostMarks = lostAd.marks.orEmpty()
        val rawFoundMarks = foundAd.marks.orEmpty()

        val lostMarks = extractPhysicalMarksOnly(rawLostMarks)
        val foundMarks = extractPhysicalMarksOnly(rawFoundMarks)

        val textSimilarity = calculateTextSimilarity(lostMarks, foundMarks)

        if (lostMarks.isNotBlank() && foundMarks.isNotBlank()) {
            evaluatedCriteriaCount++
            val marksPoints = (textSimilarity * 25).toInt()
            totalScore += marksPoints
            if (marksPoints >= 12) {
                matchingReasons.add("Marcas físicas e raça semelhantes ($lostMarks)")
                details.add(MatchCriteriaDetail("Marcas físicas e raça", lostMarks, foundMarks, marksPoints, 25, true))
            } else {
                divergingReasons.add("Marcas físicas e raça com pouca semelhança")
                details.add(MatchCriteriaDetail("Marcas físicas e raça", lostMarks, foundMarks, marksPoints, 25, false))
            }
        } else {
            details.add(MatchCriteriaDetail("Marcas físicas e raça", lostMarks.ifBlank { "Não informado" }, foundMarks.ifBlank { "Não informado" }, 0, 25, false))
        }

        // 3. REAÇÃO AO NOME (Peso 15)
        val petName = lostAd.calledNames.firstOrNull()?.split(":")?.firstOrNull()?.trim() ?: ""
        if (petName.isNotBlank() && foundAd.calledNames.isNotEmpty()) {
            evaluatedCriteriaCount++
            val normalizedPetName = normalizeText(petName)
            val reactedCall = foundAd.calledNames.any { call ->
                val parts = call.split(":")
                val nameCalled = normalizeText(parts.firstOrNull().orEmpty())
                val reacted = call.contains("reagiu", ignoreCase = true) && !call.contains("não reagiu", ignoreCase = true)
                nameCalled.contains(normalizedPetName) || normalizedPetName.contains(nameCalled) && reacted
            }

            if (reactedCall) {
                totalScore += 15
                matchingReasons.add("Reagiu ao nome '$petName'")
                details.add(MatchCriteriaDetail("Reação ao nome", petName, "Reagiu", 15, 15, true))
            } else {
                divergingReasons.add("Não houve reação confirmada ao nome")
                details.add(MatchCriteriaDetail("Reação ao nome", petName, "Sem reação", 0, 15, false))
            }
        }

        // 4. LOCALIZAÇÃO (Peso 15)
        val lostCity = normalizeText(lostAd.city.orEmpty())
        val foundCity = normalizeText(foundAd.city.orEmpty())
        val lostNeigh = normalizeText(lostAd.neighborhood.orEmpty())
        val foundNeigh = normalizeText(foundAd.neighborhood.orEmpty())

        if (lostCity.isNotBlank() && foundCity.isNotBlank()) {
            evaluatedCriteriaCount++
            if (lostCity == foundCity) {
                if (lostNeigh.isNotBlank() && foundNeigh.isNotBlank() && lostNeigh == foundNeigh) {
                    totalScore += 15
                    matchingReasons.add("Mesmo bairro (${lostAd.neighborhood})")
                    details.add(MatchCriteriaDetail("Localização", "${lostAd.neighborhood}, ${lostAd.city}", "${foundAd.neighborhood}, ${foundAd.city}", 15, 15, true))
                } else {
                    totalScore += 10
                    matchingReasons.add("Mesma cidade (${lostAd.city})")
                    details.add(MatchCriteriaDetail("Localização", lostAd.city.orEmpty(), foundAd.city.orEmpty(), 10, 15, true))
                }
            } else {
                divergingReasons.add("Cidades diferentes (${lostAd.city} vs ${foundAd.city})")
                details.add(MatchCriteriaDetail("Localização", lostAd.city.orEmpty(), foundAd.city.orEmpty(), 0, 15, false))
            }
        }

        // 5. DATA (Peso 10)
        if (foundAd.publicationTimestamp >= lostAd.publicationTimestamp - 86_400_000L) {
            totalScore += 10
            matchingReasons.add("Sequência de datas compatível")
            details.add(MatchCriteriaDetail("Data", "Perdido em data anterior", "Encontrado em data coerente", 10, 10, true))
        } else {
            divergingReasons.add("Data de encontro anterior ao desaparecimento")
            details.add(MatchCriteriaDetail("Data", "Data posterior", "Data anterior", 0, 10, false))
        }

        // 6. PORTE (Peso 5)
        val lostSize = normalizeText(lostAd.size.orEmpty())
        val foundSize = normalizeText(foundAd.size.orEmpty())
        if (lostSize.isNotBlank() && foundSize.isNotBlank()) {
            evaluatedCriteriaCount++
            if (lostSize == foundSize) {
                totalScore += 5
                matchingReasons.add("Porte compatível (${lostAd.size})")
                details.add(MatchCriteriaDetail("Porte", lostAd.size.orEmpty(), foundAd.size.orEmpty(), 5, 5, true))
            } else {
                divergingReasons.add("Porte diferente (${lostAd.size} vs ${foundAd.size})")
                details.add(MatchCriteriaDetail("Porte", lostAd.size.orEmpty(), foundAd.size.orEmpty(), 0, 5, false))
            }
        }

        // 7. IDADE (Peso 5)
        val lostAge = normalizeText(lostAd.ageApprox.orEmpty())
        val foundAge = normalizeText(foundAd.ageApprox.orEmpty())
        if (lostAge.isNotBlank() && foundAge.isNotBlank()) {
            evaluatedCriteriaCount++
            if (lostAge == foundAge) {
                totalScore += 5
                matchingReasons.add("Idade aproximada compatível (${lostAd.ageApprox})")
                details.add(MatchCriteriaDetail("Idade", lostAd.ageApprox.orEmpty(), foundAd.ageApprox.orEmpty(), 5, 5, true))
            } else {
                divergingReasons.add("Idade aproximada diferente (${lostAd.ageApprox} vs ${foundAd.ageApprox})")
                details.add(MatchCriteriaDetail("Idade", lostAd.ageApprox.orEmpty(), foundAd.ageApprox.orEmpty(), 0, 5, false))
            }
        }

        val hasEnoughData = evaluatedCriteriaCount >= 3
        val finalScore = totalScore.coerceIn(0, 98)

        return PetMatchResult(
            foundAd = foundAd,
            score = finalScore,
            hasEnoughData = hasEnoughData,
            matchingReasons = matchingReasons,
            divergingReasons = divergingReasons,
            detailsList = details
        )
    }

    fun extractPhysicalMarksOnly(rawMarks: String): String {
        if (rawMarks.isBlank()) return ""

        val parts = rawMarks.split(" · ", "\n", ";")

        val filteredParts = parts.mapNotNull { part ->
            var trimmed = part.trim()
            if (trimmed.isBlank()) return@mapNotNull null

            // Clean leading label prefix for display
            if (trimmed.startsWith("Raça:", ignoreCase = true) || trimmed.startsWith("Raca:", ignoreCase = true)) {
                trimmed = trimmed.substringAfter(":").trim()
            }

            val lower = normalizeText(trimmed)

            // Ignore non-physical sections
            if (lower.startsWith("identificacao") ||
                lower.startsWith("comportamento") ||
                lower.startsWith("passeia por") ||
                lower.startsWith("situacao") ||
                lower.startsWith("contato") ||
                lower.startsWith("telefone") ||
                lower.startsWith("whatsapp") ||
                lower.startsWith("detalhe secreto") ||
                lower.startsWith("observacoes")
            ) {
                return@mapNotNull null
            }

            // Filter out non-physical clauses from free-text segments
            val textSegments = trimmed.split(",", " e ").map { it.trim() }
            val physicalSegments = textSegments.filter { segment ->
                val normSegment = normalizeText(segment)
                normSegment.isNotBlank() &&
                !normSegment.contains("coleira") &&
                !normSegment.contains("plaquinha") &&
                !normSegment.contains("microchip") &&
                !normSegment.contains("docil") &&
                !normSegment.contains("arisco") &&
                !normSegment.contains("medroso") &&
                !normSegment.contains("agressivo") &&
                !normSegment.contains("reagir") &&
                !normSegment.contains("passeia") &&
                !normSegment.contains("parque") &&
                !normSegment.contains("praca") &&
                !normSegment.contains("ruas proximas") &&
                !normSegment.contains("rotina") &&
                !normSegment.contains("tutor")
            }

            if (physicalSegments.isEmpty()) null
            else physicalSegments.joinToString(", ")
        }.filter { it.isNotBlank() }

        return filteredParts.joinToString(", ")
    }

    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val unaccented = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return unaccented.lowercase(Locale.getDefault())
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .trim()
    }

    private fun calculateTextSimilarity(text1: String, text2: String): Float {
        val norm1 = normalizeText(text1)
        val norm2 = normalizeText(text2)

        if (norm1.isBlank() || norm2.isBlank()) return 0f

        val tokens1 = norm1.split("\\s+".toRegex())
            .filter { it.length > 2 && !STOP_WORDS.contains(it) && !LABEL_WORDS.contains(it) }
            .toSet()
        val tokens2 = norm2.split("\\s+".toRegex())
            .filter { it.length > 2 && !STOP_WORDS.contains(it) && !LABEL_WORDS.contains(it) }
            .toSet()

        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0f

        val intersection = tokens1.intersect(tokens2).size
        val minSize = minOf(tokens1.size, tokens2.size)
        val maxSize = maxOf(tokens1.size, tokens2.size)

        val overlapRatio = intersection.toFloat() / minSize.toFloat()
        val jaccardRatio = intersection.toFloat() / maxSize.toFloat()

        return if (overlapRatio == 1.0f) {
            1.0f
        } else {
            (overlapRatio * 0.6f + jaccardRatio * 0.4f).coerceIn(0f, 1f)
        }
    }
}
