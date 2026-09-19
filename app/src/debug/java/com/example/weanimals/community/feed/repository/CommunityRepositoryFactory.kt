package com.example.weanimals.community.feed.repository

import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.campaign.domain.DebugCampaignState
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
                    availabilityText = "32 vagas restantes",
                    totalSlots = 60,
                    filledSlots = DebugCampaignState.snapshot("debug-neutering", 28).filledSlots,
                    participatingByCurrentUser = DebugCampaignState
                        .snapshot("debug-neutering", 28)
                        .participated
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
                    availabilityText = "Sem limite de vagas",
                    audienceText = "Cães e gatos de qualquer idade",
                    description = "Vacinação antirrábica gratuita para cães e gatos, sem necessidade de castração prévia ou jejum. Leve a carteirinha de vacinação, se tiver.",
                    highlightText = "Vagas ilimitadas — confirme sua presença para ajudar a ONG a calcular quantas doses levar.",
                    participatingByCurrentUser = DebugCampaignState
                        .snapshot("debug-vaccination")
                        .participated
                ),
                CommunityFeedItem.Campaign(
                    id = "debug-adoption-campaign",
                    category = CommunityCategory.ADOPTION,
                    neighborhood = "Praça Central",
                    organization = "Abrigo Esperança",
                    title = "Feira de adoção — encontre seu novo melhor amigo",
                    dateText = "Sáb, 20 set · 10h às 17h",
                    locationText = "Praça Central · 3,8 km",
                    availabilityText = "14 animais confirmados para o evento",
                    audienceText = "Animais de 3 abrigos parceiros",
                    description = "Feira de adoção reunindo animais de abrigos parceiros. Não precisa agendar — leve um documento e, se possível, comprovante de endereço para agilizar a adoção no local.",
                    highlightText = "Veja quais animais estarão no evento antes de sair de casa.",
                    participatingByCurrentUser = DebugCampaignState
                        .snapshot("debug-adoption-campaign")
                        .participated,
                    confirmedAnimalsCount = 6
                ),
                CommunityFeedItem.Campaign(
                    id = "debug-donation-campaign",
                    category = CommunityCategory.DONATION,
                    neighborhood = "Vila Marlene",
                    organization = "ONG Patas Unidas",
                    title = "Arrecadação de ração e cobertores para o inverno",
                    dateText = "Até 30 de setembro",
                    locationText = "Entrega na sede · Vila Marlene",
                    availabilityText = "Meta: R$ 5.000 · R$ 2.340 arrecadados",
                    audienceText = "Ração, cobertores e itens para os animais",
                    description = "Ajude os animais acolhidos pela ONG durante o inverno. Você pode entregar doações na sede ou contribuir via Pix.",
                    highlightText = "Também aceitamos doação via Pix.",
                    donationGoalCents = 500_000L,
                    donationRaisedCents = DebugCampaignState.snapshot(
                        "debug-donation-campaign",
                        defaultRaisedCents = 234_000L
                    ).raisedCents,
                    donationNeeds = listOf(
                        "Ração para cães adultos (20kg) — mais urgente",
                        "Cobertores e mantas",
                        "Coleiras e guias"
                    ),
                    pixKey = "doacoes@patasunidas.org.br",
                    distanceKm = 2.3
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
