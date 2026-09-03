package com.example.weanimals.reporting.chat.interactor

import com.example.weanimals.reporting.chat.repository.ChatRepository

class SendCaseMessageInteractor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(reportId: String, text: String) =
        repository.sendMessage(reportId, text)
}
