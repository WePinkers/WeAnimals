package com.example.weanimals.user.reporting.chat.domain

sealed class ChatHeader {
    data object Unassigned : ChatHeader()

    data class Assigned(
        val teamMemberName: String?
    ) : ChatHeader()
}
