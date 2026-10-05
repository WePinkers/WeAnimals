package com.example.weanimals.lostandfound.domain

enum class AnimalSpecies {
    DOG,
    CAT
}

enum class AnimalSize {
    SMALL,
    MEDIUM,
    LARGE
}

data class MatchTag(
    val label: String,
    val matched: Boolean = true
)

data class PetMatch(
    val id: String,
    val matchPercentage: Int,
    val title: String,
    val subtitle: String,
    val tags: List<MatchTag>
)

fun mockMatchesFor(species: AnimalSpecies): List<PetMatch> = when (species) {
    AnimalSpecies.DOG -> listOf(
        PetMatch(
            id = "dog_1",
            matchPercentage = 91,
            title = "Encontrado — Praça Central",
            subtitle = "1,1 km · achado há 2 dias",
            tags = listOf(
                MatchTag("Cor: caramelo"),
                MatchTag("Porte: médio"),
                MatchTag("Coleira vermelha"),
                MatchTag("Raio compatível")
            )
        ),
        PetMatch(
            id = "dog_2",
            matchPercentage = 68,
            title = "Encontrado — Zona Norte",
            subtitle = "4,6 km · achado há 5 dias",
            tags = listOf(
                MatchTag("Cor: caramelo", false),
                MatchTag("Porte: médio", false),
                MatchTag("Sem coleira", false)
            )
        )
    )

    AnimalSpecies.CAT -> listOf(
        PetMatch(
            id = "cat_1",
            matchPercentage = 88,
            title = "Encontrado — Vila Marlene",
            subtitle = "0,8 km · achado há 1 dia",
            tags = listOf(
                MatchTag("Espécie: gato"),
                MatchTag("Cor: siamês / marrom"),
                MatchTag("Coleira azul"),
                MatchTag("Raio compatível")
            )
        ),
        PetMatch(
            id = "cat_2",
            matchPercentage = 72,
            title = "Encontrado — Centro Histórico",
            subtitle = "3,2 km · achado há 3 dias",
            tags = listOf(
                MatchTag("Espécie: gato", false),
                MatchTag("Cor: branco e cinza", false),
                MatchTag("Sem coleira", false)
            )
        )
    )
}
