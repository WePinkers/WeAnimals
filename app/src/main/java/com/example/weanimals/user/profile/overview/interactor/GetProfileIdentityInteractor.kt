package com.example.weanimals.user.profile.overview.interactor

import com.example.weanimals.user.profile.overview.repository.ProfileRepository

class GetProfileIdentityInteractor(private val repository: ProfileRepository) {
    suspend operator fun invoke() = repository.getIdentity()
}
