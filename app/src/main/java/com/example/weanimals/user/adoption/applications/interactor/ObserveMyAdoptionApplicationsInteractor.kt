package com.example.weanimals.user.adoption.applications.interactor

import com.example.weanimals.user.adoption.applications.repository.AdoptionApplicationsRepository

class ObserveMyAdoptionApplicationsInteractor(
    private val repository: AdoptionApplicationsRepository,
) {
    operator fun invoke() = repository.observe()
}
