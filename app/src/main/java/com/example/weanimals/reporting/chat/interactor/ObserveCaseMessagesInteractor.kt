package com.example.weanimals.reporting.chat.interactor

import com.example.weanimals.reporting.chat.repository.ChatRepository

class ObserveCaseMessagesInteractor(
    private val repository: ChatRepository
) {
    operator fun invoke(reportId: String) = repository.observeMessages(reportId)
}
