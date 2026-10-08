package com.example.weanimals.user.community.create.repository

import com.example.weanimals.user.community.create.domain.CommunityPostDraft

interface CommunityPostRepository {
    suspend fun publish(draft: CommunityPostDraft): Result<String>
}
