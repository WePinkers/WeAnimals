package com.example.weanimals.community.feed.domain

enum class CommunityCategory {
    FOUND,
    QUESTION,
    NOTICE,
    NEUTERING,
    VACCINATION,
    ADOPTION,
    DONATION,
    OTHER
}

enum class CommunityFilter {
    ALL,
    CAMPAIGNS,
    POSTS,
    FOUND,
    QUESTION,
    NOTICE,
    NEUTERING,
    VACCINATION,
    ADOPTION,
    DONATION,
    CAMPAIGN_NEIGHBORHOOD,
    POST_NEIGHBORHOOD,
    MY_NEIGHBORHOOD
}

sealed interface CommunityFeedItem {
    val id: String
    val category: CommunityCategory
    val neighborhood: String?

    data class Campaign(
        override val id: String,
        override val category: CommunityCategory,
        override val neighborhood: String?,
        val organization: String,
        val title: String,
        val dateText: String,
        val locationText: String,
        val availabilityText: String,
        val audienceText: String = "",
        val description: String = "",
        val highlightText: String = "",
        val totalSlots: Int? = null,
        val filledSlots: Int? = null,
        val donationGoalCents: Long? = null,
        val donationRaisedCents: Long? = null,
        val donationNeeds: List<String> = emptyList(),
        val pixKey: String = "",
        val locationLatitude: Double? = null,
        val locationLongitude: Double? = null,
        val distanceKm: Double? = null,
        val participatingByCurrentUser: Boolean = false,
        val confirmedAnimalsCount: Int? = null,
        val createdAtMillis: Long = 0L
    ) : CommunityFeedItem

    data class Post(
        override val id: String,
        override val category: CommunityCategory,
        override val neighborhood: String?,
        val author: String,
        val timeText: String,
        val body: String,
        val likes: Int,
        val comments: Int,
        val photoData: ByteArray? = null,
        val likedByCurrentUser: Boolean = false,
        val createdAtMillis: Long = 0L
    ) : CommunityFeedItem
}
