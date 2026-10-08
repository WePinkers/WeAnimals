package com.example.weanimals.user.reporting.chat.interactor

import com.example.weanimals.user.reporting.chat.repository.ChatRepository

class ObserveCaseMessagesInteractor(
    private val repository: ChatRepository
) {
    operator fun invoke(reportId: String) = repository.observeMessages(reportId)
}
