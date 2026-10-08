package com.example.weanimals.user.reporting.chat.interactor

import com.example.weanimals.user.reporting.chat.repository.ChatRepository

class SendCaseMessageInteractor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(reportId: String, text: String) =
        repository.sendMessage(reportId, text)
}
