package com.example.weanimals.reporting.chat.presenter

import com.example.weanimals.reporting.chat.domain.ChatHeader
import com.example.weanimals.reporting.chat.domain.ChatMessage
import com.example.weanimals.reporting.chat.domain.ChatSendFailure
import com.example.weanimals.reporting.chat.interactor.ObserveCaseMessagesInteractor
import com.example.weanimals.reporting.chat.interactor.SendCaseMessageInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatPresenter(
    private val reportId: String,
    private val observeCaseMessagesInteractor: ObserveCaseMessagesInteractor,
    private val sendCaseMessageInteractor: SendCaseMessageInteractor
) : BasePresenter<ChatContract.View>(), ChatContract.Presenter {

    private var messagesJob: Job? = null
    private var sendJob: Job? = null
    private var lastMessages: List<ChatMessage>? = null
    private var lastError: Throwable? = null
    private var isSending = false

    override fun attachView(view: ChatContract.View) {
        super.attachView(view)
        view.showHeader(lastMessages?.let(::headerFor) ?: ChatHeader.Unassigned)
        lastMessages?.let {
            view.showMessages(it)
        }
        lastError?.let { view.showMessagesError(it) }
        view.showSending(isSending)
    }

    override fun loadMessages() {
        if (messagesJob?.isActive == true) return
        withView { it.showLoading() }
        messagesJob = presenterScope.launch {
            observeCaseMessagesInteractor(reportId).collect { result ->
                result.fold(
                    onSuccess = { messages ->
                        lastMessages = messages
                        lastError = null
                        withView {
                            it.showHeader(headerFor(messages))
                            it.showMessages(messages)
                        }
                    },
                    onFailure = { error ->
                        lastError = error
                        withView { it.showMessagesError(error) }
                    }
                )
            }
        }
    }

    private fun headerFor(messages: List<ChatMessage>): ChatHeader {
        val teamMessage = messages.lastOrNull { message ->
            message.senderRole == ChatMessage.SENDER_TEAM
        }
        return teamMessage?.let {
            ChatHeader.Assigned(it.senderName?.trim()?.takeIf(String::isNotBlank))
        } ?: ChatHeader.Unassigned
    }

    override fun sendMessage(text: String) {
        if (isSending) return
        if (text.trim().isEmpty()) {
            withView {
                it.showSendError(
                    IllegalArgumentException("Message cannot be empty."),
                    ChatSendFailure.EmptyMessage
                )
            }
            return
        }

        isSending = true
        withView { it.showSending(true) }
        sendJob?.cancel()
        sendJob = presenterScope.launch {
            sendCaseMessageInteractor(reportId, text).fold(
                onSuccess = {
                    isSending = false
                    withView {
                        it.showSending(false)
                        it.clearMessageInput()
                    }
                },
                onFailure = { error ->
                    isSending = false
                    withView {
                        it.showSending(false)
                        it.showSendError(error, sendFailure())
                    }
                }
            )
        }
    }

    private fun sendFailure(): ChatSendFailure = when {
        lastError != null -> ChatSendFailure.Generic
        lastMessages.orEmpty().any { it.senderRole == ChatMessage.SENDER_TEAM } ->
            ChatSendFailure.Generic
        else -> ChatSendFailure.UnassignedCase
    }
}
