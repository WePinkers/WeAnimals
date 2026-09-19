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

data class DebugCampaignSnapshot(
    val filledSlots: Int,
    val raisedCents: Long,
    val participated: Boolean
)

/** In-memory state used only by the visual campaign fixtures in debug builds. */
object DebugCampaignState {
    private val snapshots = mutableMapOf<String, DebugCampaignSnapshot>()

    @Synchronized
    fun snapshot(
        campaignId: String,
        defaultFilledSlots: Int = 0,
        defaultRaisedCents: Long = 0L
    ): DebugCampaignSnapshot = snapshots.getOrPut(campaignId) {
        DebugCampaignSnapshot(defaultFilledSlots, defaultRaisedCents, false)
    }

    @Synchronized
    fun participate(
        campaignId: String,
        defaultFilledSlots: Int,
        totalSlots: Int?,
        incrementSlots: Boolean = true
    ): CommunityCampaignParticipationResult {
        val current = snapshot(campaignId, defaultFilledSlots)
        if (current.participated) {
            return CommunityCampaignParticipationResult(
                current.filledSlots, totalSlots, alreadyParticipating = true
            )
        }
        if (incrementSlots) {
            require(totalSlots == null || current.filledSlots < totalSlots) { "Campaign is full." }
        }
        val updated = current.copy(
            filledSlots = if (incrementSlots) current.filledSlots + 1 else current.filledSlots,
            participated = true
        )
        snapshots[campaignId] = updated
        return CommunityCampaignParticipationResult(
            updated.filledSlots, totalSlots, alreadyParticipating = false
        )
    }

    @Synchronized
    fun donate(
        campaignId: String,
        defaultRaisedCents: Long,
        goalCents: Long?,
        amountCents: Long
    ): CommunityCampaignDonationResult {
        val current = snapshot(campaignId, defaultRaisedCents = defaultRaisedCents)
        require(goalCents == null || current.raisedCents + amountCents <= goalCents) {
            "Donation exceeds campaign goal."
        }
        val updated = current.copy(raisedCents = current.raisedCents + amountCents)
        snapshots[campaignId] = updated
        return CommunityCampaignDonationResult(updated.raisedCents, goalCents)
    }
}
