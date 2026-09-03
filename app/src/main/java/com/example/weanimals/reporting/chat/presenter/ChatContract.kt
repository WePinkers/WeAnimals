package com.example.weanimals.reporting.chat.presenter

import com.example.weanimals.reporting.chat.domain.ChatHeader
import com.example.weanimals.reporting.chat.domain.ChatMessage
import com.example.weanimals.reporting.chat.domain.ChatSendFailure

interface ChatContract {
    interface View {
        fun showLoading()
        fun showHeader(header: ChatHeader)
        fun showMessages(messages: List<ChatMessage>)
        fun showMessagesError(error: Throwable)
        fun showSending(isSending: Boolean)
        fun clearMessageInput()
        fun showSendError(error: Throwable, failure: ChatSendFailure)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun loadMessages()
        fun sendMessage(text: String)
    }
}
