package com.example.weanimals.user.home.interactor

import com.example.weanimals.user.home.repository.HomeRepository

class ObserveUserReportsInteractor(
    private val repository: HomeRepository
) {
    operator fun invoke() = repository.observeUserReports()
}
