package com.example.weanimals.community.create.repository

import com.example.weanimals.community.create.domain.CommunityPostDraft

interface CommunityPostRepository {
    suspend fun publish(draft: CommunityPostDraft): Result<String>
}
