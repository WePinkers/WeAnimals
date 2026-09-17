package com.example.weanimals.community.detail.domain

data class CommunityComment(
    val id: String,
    val author: String,
    val timeText: String,
    val text: String,
    val parentCommentId: String? = null
)

data class CommunityPostEngagement(
    val likes: Int,
    val comments: Int,
    val likedByCurrentUser: Boolean
)
