package com.example.weanimals.community.feed.interactor

import com.example.weanimals.community.feed.repository.CommunityRepository

class GetCommunityFeedInteractor(private val repository: CommunityRepository) {
    suspend operator fun invoke() = repository.getFeed()
}
