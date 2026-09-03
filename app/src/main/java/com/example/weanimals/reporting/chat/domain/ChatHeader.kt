package com.example.weanimals.reporting.chat.domain

sealed class ChatHeader {
    data object Unassigned : ChatHeader()

    data class Assigned(
        val teamMemberName: String?
    ) : ChatHeader()
}
