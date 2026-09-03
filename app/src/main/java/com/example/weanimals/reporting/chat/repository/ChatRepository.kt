package com.example.weanimals.reporting.chat.repository

import com.example.weanimals.reporting.chat.domain.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(reportId: String): Flow<Result<List<ChatMessage>>>

    suspend fun sendMessage(reportId: String, text: String): Result<ChatMessage>
}
