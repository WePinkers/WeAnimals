package com.example.weanimals.user.community.feed.interactor

import com.example.weanimals.user.community.feed.repository.CommunityRepository

class GetCommunityFeedInteractor(private val repository: CommunityRepository) {
    suspend operator fun invoke() = repository.getFeed()
}
