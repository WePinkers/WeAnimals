package com.example.weanimals.user.map.overview.interactor

import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.user.lostandfound.presentation.AdActionNavigator
import com.example.weanimals.user.lostandfound.presentation.AdActionNavigator.Action
import com.example.weanimals.user.map.overview.presentation.PublicAnimalDetails
import org.junit.Assert.*
import org.junit.Test

class AnimalMapDetailsTest {
    private fun ad() = FoundPetAd(id = "animal", ownerUserId = "creator", species = "Gato",
        category = "PERDIDO", calledNames = listOf("Pipoca: reagiu"), neighborhood = "Vila Maria",
        city = "São Paulo", street = "Rua residencial", number = "123", complement = "Casa privada",
        marks = "Raça: SRD · Mancha branca · SEGREDO: chave · Identificação: chip · Telefone: privado",
        whenDate = "10/10/2026")

    private fun request(user: String = "visitor", status: String = "PENDENTE") =
        TutorVerificationRequest(id = "existing-request", adId = "animal", requesterUserId = user,
            ownerUserId = "creator", petName = "resposta privada", specialMark = "segredo", status = status)

    @Test fun keepsLostAndFoundWorkflowsSeparateAndRequiresSignIn() {
        assertEquals(Action.LOGIN, AdActionNavigator.decide(ad(), null, null))
        assertEquals(Action.OWN_AD, AdActionNavigator.decide(ad(), "creator", null))
        assertEquals(Action.SIGHTING, AdActionNavigator.decide(ad(), "visitor", null))
        assertEquals(Action.VERIFICATION, AdActionNavigator.decide(ad().copy(category = "ENCONTRADO"), "visitor", null))
        assertEquals(Action.INACTIVE, AdActionNavigator.decide(ad().copy(isEnded = true), "visitor", null))
    }

    @Test fun reusesOnlyTheCurrentUsersRequestAndApprovedConversation() {
        assertEquals(Action.STATUS, AdActionNavigator.decide(ad(), "visitor", request()))
        assertEquals(Action.STATUS, AdActionNavigator.decide(ad(), "visitor", request(status = "RECUSADO")))
        assertEquals(Action.CHAT, AdActionNavigator.decide(ad(), "visitor", request(status = "APROVADO")))
        assertEquals(Action.CHAT, AdActionNavigator.decide(ad().copy(isEnded = true), "visitor", request(status = "APROVADO")))
        assertEquals(Action.SIGHTING, AdActionNavigator.decide(ad(), "another-user", request()))
        assertEquals(Action.SIGHTING, AdActionNavigator.decide(ad(), "visitor", request().copy(adId = "other-ad")))
    }

    @Test fun ownAdSimulationUsesExistingFormsRequestsAndChatWithoutChangingReleasePolicy() {
        assertEquals(Action.OWN_AD, AdActionNavigator.decide(ad(), "creator", null))
        assertEquals(Action.SIGHTING, AdActionNavigator.decide(ad(), "creator", null, true))
        assertEquals(Action.VERIFICATION, AdActionNavigator.decide(ad().copy(category = "ENCONTRADO"), "creator", null, true))
        assertEquals(Action.STATUS, AdActionNavigator.decide(ad(), "creator", request("creator"), true))
        assertEquals(Action.CHAT, AdActionNavigator.decide(ad(), "creator", request("creator", "APROVADO"), true))
        assertEquals(Action.INACTIVE, AdActionNavigator.decide(ad().copy(isEnded = true), "creator", null, true))
        assertEquals(Action.INACTIVE, AdActionNavigator.decide(ad().copy(isReunited = true), "creator", null, true))
        assertEquals(Action.INACTIVE, AdActionNavigator.decide(ad().copy(publicationTimestamp = 0), "creator", null, true))
        assertEquals(Action.LOGIN, AdActionNavigator.decide(ad(), null, null, true))
    }

    @Test fun rendersOnlyPublicInformationAndUsesEventDateInsteadOfPublicationDate() {
        val ad = ad()
        assertEquals("Pipoca", PublicAnimalDetails.title(ad))
        assertEquals("Vila Maria — São Paulo", PublicAnimalDetails.location(ad))
        assertEquals("10/10/2026", PublicAnimalDetails.date(ad))
        assertEquals("SRD", PublicAnimalDetails.breed(ad))
        assertEquals("Mancha branca", PublicAnimalDetails.marks(ad))
        val publicText = PublicAnimalDetails.physicalDescription(ad) + PublicAnimalDetails.location(ad)
        listOf("123", "Rua residencial", "Casa privada", "chave", "chip", "Telefone").forEach {
            assertFalse(publicText.contains(it))
        }
        assertEquals("Gato encontrado", PublicAnimalDetails.title(ad.copy(category = "ENCONTRADO")))
        assertEquals("Data não informada", PublicAnimalDetails.date(ad.copy(whenDate = null)))
    }
}
