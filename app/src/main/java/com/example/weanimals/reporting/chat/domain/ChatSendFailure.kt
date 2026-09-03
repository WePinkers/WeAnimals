package com.example.weanimals.reporting.chat.domain

sealed class ChatSendFailure {
    data object EmptyMessage : ChatSendFailure()
    data object UnassignedCase : ChatSendFailure()
    data object Generic : ChatSendFailure()
}
