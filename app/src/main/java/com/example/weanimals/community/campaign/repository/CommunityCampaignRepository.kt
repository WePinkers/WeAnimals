package com.example.weanimals.community.campaign.repository

import com.example.weanimals.community.campaign.domain.CommunityCampaignAnimal
import com.example.weanimals.community.campaign.domain.CommunityCampaignDonationResult
import com.example.weanimals.community.campaign.domain.CommunityCampaignDraft
import com.example.weanimals.community.campaign.domain.CommunityCampaignParticipationResult

interface CommunityCampaignRepository {
    suspend fun publish(draft: CommunityCampaignDraft): Result<String>

    suspend fun getConfirmedAnimals(campaignId: String): Result<List<CommunityCampaignAnimal>>

    suspend fun participate(campaignId: String): Result<CommunityCampaignParticipationResult>

    suspend fun isParticipating(campaignId: String): Result<Boolean>

    suspend fun donate(
        campaignId: String,
        amountCents: Long
    ): Result<CommunityCampaignDonationResult>

    suspend fun notifyItemDonation(
        campaignId: String,
        selectedItems: List<String>,
        otherItem: String
    ): Result<Unit>
}
