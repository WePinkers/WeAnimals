package com.example.weanimals.lostandfound.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weanimals.databinding.ActivityCaseChatBinding
import com.example.weanimals.reporting.chat.presentation.LocalChatAdapter
import com.example.weanimals.reporting.chat.presentation.LocalChatMessage
import com.example.weanimals.reporting.chat.presentation.LocalMessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LostFoundChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaseChatBinding
    private val messages = mutableListOf<LocalChatMessage>()
    private lateinit var chatAdapter: LocalChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCaseChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChatHeader()
        setupChatList()
        setupInteractions()
    }

    private fun setupChatHeader() {
        binding.chatAvatar.text = "R"
        binding.chatAgentName.text = "Renata F."
        binding.chatProtocol.text = "tutora de Pipoca"
    }

    private fun setupChatList() {
        val initialTime = getCurrentTime()
        messages.addAll(
            listOf(
                LocalChatMessage(
                    text = "Chat liberado. Combinem a entrega em um lugar público.",
                    time = initialTime,
                    type = LocalMessageType.SYSTEM
                ),
                LocalChatMessage(
                    text = "Olá! Vi que você encontrou o Pipoca. É ele mesmo?",
                    time = initialTime,
                    type = LocalMessageType.INCOMING
                )
            )
        )

        chatAdapter = LocalChatAdapter(messages)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChat.adapter = chatAdapter
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener { sendMessage() }

        binding.inputMessage.setOnEditorActionListener { _, actionId, event ->
            val sendByAction = actionId == EditorInfo.IME_ACTION_SEND
            val sendByEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (sendByAction || sendByEnter) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.btnRegisterReunion.setOnClickListener {
            startActivity(Intent(this, CloseLostAdActivity::class.java))
        }
    }

    private fun sendMessage() {
        val text = binding.inputMessage.text.toString().trim()
        if (text.isBlank()) return

        val time = getCurrentTime()
        val userMsg = LocalChatMessage(
            text = text,
            time = time,
            type = LocalMessageType.OUTGOING
        )
        chatAdapter.addMessage(userMsg)
        binding.inputMessage.setText("")
        binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

        lifecycleScope.launch {
            delay(2000)
            val replyMsg = LocalChatMessage(
                text = "Perfeito! Te encontro lá com a coleira dele.",
                time = getCurrentTime(),
                type = LocalMessageType.INCOMING
            )
            chatAdapter.addMessage(replyMsg)
            binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
