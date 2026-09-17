package com.example.weanimals.community.feed.domain

enum class CommunityCategory { FOUND, QUESTION, NOTICE, NEUTERING, VACCINATION, OTHER }

enum class CommunityFilter { ALL, CAMPAIGNS, POSTS, FOUND, NEUTERING, VACCINATION, MY_NEIGHBORHOOD }

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
        val availabilityText: String
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
        val photoData: ByteArray? = null
    ) : CommunityFeedItem
}
