package com.example.weanimals.user.community.feed.repository

import com.example.weanimals.user.community.feed.domain.CommunityFeedItem

interface CommunityRepository {
    suspend fun getFeed(): Result<List<CommunityFeedItem>>
}
