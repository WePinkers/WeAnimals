package com.example.weanimals.home.interactor

import com.example.weanimals.home.repository.HomeRepository

class ObserveUserReportsInteractor(
    private val repository: HomeRepository
) {
    operator fun invoke() = repository.observeUserReports()
}
