package com.example.weanimals.lostandfound.model

import com.google.firebase.auth.FirebaseAuth

data class FoundPetAd(
    val id: String = java.util.UUID.randomUUID().toString(),
    val ownerUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user",
    val publisherName: String? = null,
    val category: String = "ENCONTRADO",
    val species: String,
    val otherType: String? = null,
    val size: String? = null,
    val ageApprox: String? = null,
    val condition: String? = null,
    val photoUris: List<String> = emptyList(),
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val complement: String? = null,
    val whenFound: String? = null,
    val whenDate: String? = null,
    val marks: String? = null,
    val calledNames: List<String> = emptyList(),
    var currentSituation: String = "Comigo",
    val publicationTimestamp: Long = System.currentTimeMillis(),
    val possibleTutorsCount: Int = 0,
    var isEnded: Boolean = false,
    var isReunited: Boolean = false,
    var reunitedTimestamp: Long? = null
) {
    fun isOwnedByCurrent(): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
        return ownerUserId == currentUid
    }

    fun getDisplayTitle(): String {
        val isLost = category.equals("PERDIDO", ignoreCase = true)
        val verbMasculine = if (isLost) "perdido" else "encontrado"
        val verbFeminine = if (isLost) "perdida" else "encontrada"

        if (isLost && calledNames.isNotEmpty()) {
            val petName = calledNames.first().split(":").firstOrNull()?.trim()
            if (!petName.isNullOrBlank()) {
                val lower = petName.lowercase()
                val isFeminine = lower.endsWith("a") || lower.endsWith("ã")
                val daysElapsed = ((System.currentTimeMillis() - publicationTimestamp) / 86_400_000L).toInt()
                val daysStr = if (daysElapsed <= 0) "hoje" else "há $daysElapsed ${if (daysElapsed == 1) "dia" else "dias"}"
                val verb = if (isFeminine) "desaparecida" else "desaparecido"
                return "$petName, $verb $daysStr"
            }
        }

        return when (species.trim().lowercase()) {
            "cão", "cao", "cachorro" -> "Cão $verbMasculine"
            "gato" -> "Gato $verbMasculine"
            else -> {
                val custom = (otherType ?: species).trim()
                if (custom.isNotBlank() && !custom.equals("Outro", ignoreCase = true)) {
                    val formatted = custom.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    val lower = custom.lowercase()
                    val isFeminine = lower.endsWith("a") || lower.endsWith("ã")
                    val verb = if (isFeminine) verbFeminine else verbMasculine
                    "$formatted $verb"
                } else {
                    "Animal $verbMasculine"
                }
            }
        }
    }

    fun getDisplayLocation(): String {
        val locList = listOfNotNull(
            neighborhood?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() }
        )
        return if (locList.isNotEmpty()) locList.joinToString(" · ") else "Localização registrada"
    }

    fun getRelativeTimeText(): String {
        val elapsed = System.currentTimeMillis() - publicationTimestamp
        val seconds = elapsed / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Publicado agora"
            minutes < 60 -> "Publicado há $minutes min"
            hours == 1L -> "Publicado há 1 hora"
            hours < 24 -> "Publicado há $hours horas"
            days == 1L -> "Publicado há 1 dia"
            else -> "Publicado há $days dias"
        }
    }

    fun getRemainingDays(): Int {
        if (isEnded || isReunited) return 0
        val millisPerDay = 86_400_000L
        val elapsed = System.currentTimeMillis() - publicationTimestamp
        val daysElapsed = (elapsed / millisPerDay).toInt()
        val remaining = 7 - daysElapsed
        return if (remaining < 0) 0 else remaining
    }

    fun getRemainingDaysText(): String {
        if (isReunited) return "Reencontrado"
        if (isEnded) return "Encerrado"
        val remaining = getRemainingDays()
        return when {
            remaining <= 0 -> "Encerrado"
            remaining == 1 -> "Resta 1 dia"
            else -> "Restam $remaining dias"
        }
    }
}
