package com.example.weanimals.community.create.interactor

import com.example.weanimals.community.create.domain.CommunityPostDraft
import com.example.weanimals.community.create.repository.CommunityPostRepository

class PublishCommunityPostInteractor(private val repository: CommunityPostRepository) {
    suspend operator fun invoke(draft: CommunityPostDraft): Result<String> = repository.publish(draft)
}
