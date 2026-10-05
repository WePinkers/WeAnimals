package com.example.weanimals.community.feed.repository

import com.example.weanimals.community.feed.domain.CommunityFeedItem

interface CommunityRepository {
    suspend fun getFeed(): Result<List<CommunityFeedItem>>
}
