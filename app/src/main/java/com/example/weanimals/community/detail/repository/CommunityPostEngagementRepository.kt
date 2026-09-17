package com.example.weanimals.community.detail.repository

import com.example.weanimals.community.detail.domain.CommunityComment
import com.example.weanimals.community.detail.domain.CommunityPostEngagement

interface CommunityPostEngagementRepository {
    suspend fun getEngagement(postId: String): Result<CommunityPostEngagement>

    suspend fun getComments(postId: String): Result<List<CommunityComment>>

    suspend fun toggleLike(postId: String, liked: Boolean): Result<CommunityPostEngagement>

    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ): Result<CommunityComment>
}
