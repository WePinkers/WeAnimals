package com.example.weanimals.user.community.detail.repository

import com.example.weanimals.user.community.detail.domain.CommunityComment
import com.example.weanimals.user.community.detail.domain.CommunityPostEngagement
import com.google.firebase.firestore.ListenerRegistration

interface CommunityPostEngagementRepository {
    suspend fun getEngagement(postId: String): Result<CommunityPostEngagement>

    suspend fun getComments(postId: String): Result<List<CommunityComment>>

    fun observeComments(
        postId: String,
        onResult: (Result<List<CommunityComment>>) -> Unit,
    ): ListenerRegistration

    suspend fun toggleLike(postId: String, liked: Boolean): Result<CommunityPostEngagement>

    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ): Result<CommunityComment>
}
