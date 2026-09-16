package com.example.weanimals.profile.overview.interactor

import com.example.weanimals.profile.overview.repository.ProfileRepository

class GetProfileIdentityInteractor(private val repository: ProfileRepository) {
    suspend operator fun invoke() = repository.getIdentity()
}
