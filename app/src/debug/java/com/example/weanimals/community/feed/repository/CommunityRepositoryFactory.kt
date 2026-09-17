package com.example.weanimals.community.feed.repository

import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Visual fixtures for debug builds only. Never uploaded or persisted. */
object CommunityRepositoryFactory {
    fun create(): CommunityRepository = object : CommunityRepository {
        private val realFeed = FirebaseCommunityRepository(
            FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()
        )

        override suspend fun getFeed(): Result<List<CommunityFeedItem>> {
            val examples = listOf(
                CommunityFeedItem.Campaign(
                    id = "debug-neutering",
                    category = CommunityCategory.NEUTERING,
                    neighborhood = "Vila Marlene",
                    organization = "Abrigo Esperança",
                    title = "Castração gratuita — mutirão de sábado",
                    dateText = "Sáb, 6 set · 8h às 13h",
                    locationText = "Vila Marlene · 2,3 km",
                    availabilityText = "32 vagas restantes"
                ),
                CommunityFeedItem.Post(
                    id = "debug-marta",
                    category = CommunityCategory.FOUND,
                    neighborhood = "Vila Marlene",
                    author = "Marta Nunes",
                    timeText = "há 2h",
                    body = "Achei essa gatinha perto da praça, parece bem cuidada mas está sem coleira. Alguém reconhece?",
                    likes = 18,
                    comments = 7
                ),
                CommunityFeedItem.Campaign(
                    id = "debug-vaccination",
                    category = CommunityCategory.VACCINATION,
                    neighborhood = "Praça Central",
                    organization = "ONG Patas Unidas",
                    title = "Vacinação antirrábica gratuita",
                    dateText = "Dom, 14 set · 9h às 16h",
                    locationText = "Praça Central · 3,8 km",
                    availabilityText = "Sem limite de vagas"
                ),
                CommunityFeedItem.Post(
                    id = "debug-joao",
                    category = CommunityCategory.OTHER,
                    neighborhood = "Zona Norte",
                    author = "João Klein",
                    timeText = "há 5h",
                    body = "Alguém sabe indicar uma ONG que ajude com ração pra quem cuida de gatos de rua? Já são 6 aqui em casa.",
                    likes = 9,
                    comments = 12
                )
            )
            // Keep the visual examples usable before Firestore rules are deployed.
            return Result.success(realFeed.getFeed().getOrDefault(emptyList()) + examples)
        }
    }
}
