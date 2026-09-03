package com.example.weanimals.reporting.chat.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.reporting.chat.domain.ChatHeader
import com.example.weanimals.reporting.chat.domain.ChatMessage
import com.example.weanimals.reporting.chat.domain.ChatSendFailure
import com.example.weanimals.reporting.chat.presenter.ChatContract
import com.example.weanimals.reporting.chat.presenter.ChatPresenter
import com.example.weanimals.databinding.ActivityCaseChatBinding
import com.example.weanimals.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaseChatActivity : AppCompatActivity(), ChatContract.View {

    private lateinit var binding: ActivityCaseChatBinding
    private val reportId: String by lazy { intent.getStringExtra(EXTRA_REPORT_ID).orEmpty() }
    private val protocolNumber: Int by lazy { intent.getIntExtra(EXTRA_PROTOCOL_NUMBER, 0) }
    private val presenter: ChatPresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createChatPresenter(reportId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCaseChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.chatProtocol.text = getString(R.string.chat_protocol_format, protocolNumber)
        setupInteractions()
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.loadMessages()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showLoading() {
        binding.chatState.visibility = View.VISIBLE
        binding.chatState.setText(R.string.chat_loading)
        binding.messagesScroll.visibility = View.GONE
    }

    override fun showMessages(messages: List<ChatMessage>) {
        binding.messagesScroll.visibility = View.VISIBLE
        binding.chatState.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        if (messages.isEmpty()) {
            binding.chatState.setText(R.string.chat_unassigned_message)
        }

        binding.messagesContainer.removeAllViews()
        messages.forEach { message ->
            val messageBinding = ItemChatMessageBinding.inflate(
                layoutInflater,
                binding.messagesContainer,
                false
            )
            bindMessage(messageBinding, message)
            binding.messagesContainer.addView(messageBinding.root)
        }
        binding.messagesScroll.post { binding.messagesScroll.fullScroll(View.FOCUS_DOWN) }
    }

    override fun showMessagesError(error: Throwable) {
        Log.e(TAG, "Could not load case chat", error)
        binding.messagesScroll.visibility = View.GONE
        binding.chatState.visibility = View.VISIBLE
        binding.chatState.setText(R.string.chat_messages_error)
    }

    override fun showHeader(header: ChatHeader) {
        when (header) {
            ChatHeader.Unassigned -> {
                binding.chatAvatar.setText(R.string.chat_unassigned_avatar)
                binding.chatAgentName.setText(R.string.chat_unassigned_title)
            }

            is ChatHeader.Assigned -> {
                val name = header.teamMemberName
                binding.chatAgentName.text = name?.let {
                    getString(R.string.chat_agent_format, it)
                } ?: getString(R.string.chat_team_default_name)
                binding.chatAvatar.text = name?.firstOrNull()?.uppercaseChar()?.toString()
                    ?: getString(R.string.chat_team_avatar)
            }
        }
    }

    override fun showSending(isSending: Boolean) {
        binding.sendButton.isEnabled = !isSending
        binding.messageInput.isEnabled = !isSending
    }

    override fun clearMessageInput() {
        binding.messageInput.text?.clear()
        binding.chatError.visibility = View.GONE
    }

    override fun showSendError(error: Throwable, failure: ChatSendFailure) {
        Log.e(TAG, "Could not send case chat message", error)
        binding.chatError.setText(
            when (failure) {
                ChatSendFailure.EmptyMessage -> R.string.chat_empty_message_error
                ChatSendFailure.UnassignedCase -> R.string.chat_unassigned_send_error
                ChatSendFailure.Generic -> R.string.chat_send_error
            }
        )
        binding.chatError.visibility = View.VISIBLE
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener { finish() }
        binding.sendButton.setOnClickListener { sendCurrentMessage() }
        binding.messageInput.doAfterTextChanged {
            if (!it.isNullOrBlank()) binding.chatError.visibility = View.GONE
        }
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            val sendByAction = actionId == EditorInfo.IME_ACTION_SEND
            val sendByEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_UP
            if (sendByAction || sendByEnter) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendCurrentMessage() {
        presenter.sendMessage(binding.messageInput.text?.toString().orEmpty())
    }

    private fun bindMessage(binding: ItemChatMessageBinding, message: ChatMessage) {
        val time = formatTime(message.createdAtMillis)
        if (message.isMine) {
            binding.incomingMessageGroup.visibility = View.GONE
            binding.outgoingMessageGroup.visibility = View.VISIBLE
            binding.outgoingMessageText.text = message.text
            binding.outgoingMessageTime.text = time
        } else {
            binding.outgoingMessageGroup.visibility = View.GONE
            binding.incomingMessageGroup.visibility = View.VISIBLE
            binding.incomingMessageText.text = message.text
            binding.incomingMessageTime.text = time
        }
    }

    private fun formatTime(timestampMillis: Long): String =
        if (timestampMillis > 0L) {
            SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(Date(timestampMillis))
        } else {
            "—"
        }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val TAG = "CaseChatActivity"
        private const val EXTRA_REPORT_ID = "extra_report_id"
        private const val EXTRA_PROTOCOL_NUMBER = "extra_protocol_number"

        fun newIntent(context: Context, reportId: String, protocolNumber: Int) =
            Intent(context, CaseChatActivity::class.java).apply {
                putExtra(EXTRA_REPORT_ID, reportId)
                putExtra(EXTRA_PROTOCOL_NUMBER, protocolNumber)
            }
    }
}
