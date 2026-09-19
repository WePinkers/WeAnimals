package com.example.weanimals.community.campaign.domain

import android.content.Context
import android.content.Intent
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem

data class CampaignDonationInfo(
    val campaignId: String,
    val organization: String,
    val campaignTitle: String,
    val donationNeeds: List<String>,
    val receivingLocation: String,
    val pixKey: String,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val fallbackDistanceKm: Double?,
    val dateText: String = "",
    val availabilityText: String = "",
    val audienceText: String = "",
    val description: String = "",
    val highlightText: String = "",
    val donationGoalCents: Long? = null,
    val donationRaisedCents: Long? = null
) {
    fun toIntent(context: Context, destination: Class<*>): Intent =
        Intent(context, destination).apply {
            putExtra(EXTRA_CAMPAIGN_ID, campaignId)
            putExtra(EXTRA_ORGANIZATION, organization)
            putExtra(EXTRA_CAMPAIGN_TITLE, campaignTitle)
            putStringArrayListExtra(EXTRA_DONATION_NEEDS, ArrayList(donationNeeds))
            putExtra(EXTRA_RECEIVING_LOCATION, receivingLocation)
            putExtra(EXTRA_PIX_KEY, pixKey)
            locationLatitude?.let { putExtra(EXTRA_LOCATION_LATITUDE, it) }
            locationLongitude?.let { putExtra(EXTRA_LOCATION_LONGITUDE, it) }
            fallbackDistanceKm?.let { putExtra(EXTRA_FALLBACK_DISTANCE_KM, it) }
            putExtra(EXTRA_DATE, dateText)
            putExtra(EXTRA_AVAILABILITY, availabilityText)
            putExtra(EXTRA_AUDIENCE, audienceText)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_HIGHLIGHT, highlightText)
            donationGoalCents?.let { putExtra(EXTRA_DONATION_GOAL_CENTS, it) }
            donationRaisedCents?.let { putExtra(EXTRA_DONATION_RAISED_CENTS, it) }
        }

    fun toCampaign(): CommunityFeedItem.Campaign = CommunityFeedItem.Campaign(
        id = campaignId,
        category = CommunityCategory.DONATION,
        neighborhood = receivingLocation.substringBefore(" · ").takeIf(String::isNotBlank),
        organization = organization,
        title = campaignTitle,
        dateText = dateText,
        locationText = receivingLocation,
        availabilityText = availabilityText,
        audienceText = audienceText,
        description = description,
        highlightText = highlightText,
        donationGoalCents = donationGoalCents,
        donationRaisedCents = donationRaisedCents,
        donationNeeds = donationNeeds,
        pixKey = pixKey,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        distanceKm = fallbackDistanceKm
    )

    companion object {
        private const val EXTRA_CAMPAIGN_ID = "donation_campaign_id"
        private const val EXTRA_ORGANIZATION = "donation_organization"
        private const val EXTRA_CAMPAIGN_TITLE = "donation_campaign_title"
        private const val EXTRA_DONATION_NEEDS = "donation_needs"
        private const val EXTRA_RECEIVING_LOCATION = "donation_receiving_location"
        private const val EXTRA_PIX_KEY = "donation_pix_key"
        private const val EXTRA_LOCATION_LATITUDE = "donation_location_latitude"
        private const val EXTRA_LOCATION_LONGITUDE = "donation_location_longitude"
        private const val EXTRA_FALLBACK_DISTANCE_KM = "donation_fallback_distance_km"
        private const val EXTRA_DATE = "donation_campaign_date"
        private const val EXTRA_AVAILABILITY = "donation_campaign_availability"
        private const val EXTRA_AUDIENCE = "donation_campaign_audience"
        private const val EXTRA_DESCRIPTION = "donation_campaign_description"
        private const val EXTRA_HIGHLIGHT = "donation_campaign_highlight"
        private const val EXTRA_DONATION_GOAL_CENTS = "donation_campaign_goal_cents"
        private const val EXTRA_DONATION_RAISED_CENTS = "donation_campaign_raised_cents"

        fun fromIntent(intent: Intent): CampaignDonationInfo = CampaignDonationInfo(
            campaignId = intent.getStringExtra(EXTRA_CAMPAIGN_ID).orEmpty(),
            organization = intent.getStringExtra(EXTRA_ORGANIZATION).orEmpty(),
            campaignTitle = intent.getStringExtra(EXTRA_CAMPAIGN_TITLE).orEmpty(),
            donationNeeds = intent.getStringArrayListExtra(EXTRA_DONATION_NEEDS).orEmpty(),
            receivingLocation = intent.getStringExtra(EXTRA_RECEIVING_LOCATION).orEmpty(),
            pixKey = intent.getStringExtra(EXTRA_PIX_KEY).orEmpty(),
            locationLatitude = intent.getDoubleExtra(EXTRA_LOCATION_LATITUDE, Double.NaN)
                .takeIf(Double::isFinite),
            locationLongitude = intent.getDoubleExtra(EXTRA_LOCATION_LONGITUDE, Double.NaN)
                .takeIf(Double::isFinite),
            fallbackDistanceKm = intent.getDoubleExtra(EXTRA_FALLBACK_DISTANCE_KM, Double.NaN)
                .takeIf(Double::isFinite),
            dateText = intent.getStringExtra(EXTRA_DATE).orEmpty(),
            availabilityText = intent.getStringExtra(EXTRA_AVAILABILITY).orEmpty(),
            audienceText = intent.getStringExtra(EXTRA_AUDIENCE).orEmpty(),
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            highlightText = intent.getStringExtra(EXTRA_HIGHLIGHT).orEmpty(),
            donationGoalCents = intent.getLongExtra(EXTRA_DONATION_GOAL_CENTS, -1L)
                .takeIf { it > 0L },
            donationRaisedCents = intent.getLongExtra(EXTRA_DONATION_RAISED_CENTS, -1L)
                .takeIf { it >= 0L }
        )
    }
}
