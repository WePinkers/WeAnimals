package com.example.weanimals.community.campaign.domain

import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails

data class CommunityCampaignDraft(
    val category: CommunityCategory,
    val title: String,
    val dateText: String,
    val location: LocationDetails,
    val availabilityText: String,
    val audienceText: String,
    val description: String,
    val highlightText: String,
    val confirmedAnimals: List<CommunityCampaignAnimalDraft> = emptyList(),
    val totalSlots: Int? = null,
    val donationGoalCents: Long? = null,
    val donationNeeds: List<String> = emptyList(),
    val pixKey: String = ""
)

data class CommunityCampaignAnimalDraft(
    val name: String,
    val breed: String,
    val ageText: String,
    val photoUri: String? = null
)

data class CommunityCampaignAnimal(
    val id: String,
    val name: String,
    val breed: String,
    val ageText: String,
    val photoData: ByteArray?
)

data class CommunityCampaignParticipationResult(
    val filledSlots: Int,
    val totalSlots: Int?,
    val alreadyParticipating: Boolean
)

data class CommunityCampaignDonationResult(
    val raisedCents: Long,
    val goalCents: Long?
)
