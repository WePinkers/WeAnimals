package com.example.weanimals.ombudsman.presentation

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weanimals.databinding.ActivitySupportChatBinding
import com.example.weanimals.reporting.chat.presentation.LocalChatAdapter
import com.example.weanimals.reporting.chat.presentation.LocalChatMessage
import com.example.weanimals.reporting.chat.presentation.LocalMessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportChatBinding
    private val messages = mutableListOf<LocalChatMessage>()
    private lateinit var chatAdapter: LocalChatAdapter
    private var isChatEnded = false

    private val attachFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            sendUserMessage("Anexo enviado: ${uri.lastPathSegment?.substringAfterLast('/') ?: "imagem.jpg"}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivitySupportChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChatList()
        setupInteractions()
    }

    private fun setupChatList() {
        val initialTime = getCurrentTime()
        messages.add(
            LocalChatMessage(
                text = "Olá! Você está falando com a equipe de atendimento. Sobre o que podemos ajudar?",
                time = initialTime,
                type = LocalMessageType.INCOMING
            )
        )

        chatAdapter = LocalChatAdapter(messages)
        binding.recyclerViewSupportChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSupportChat.adapter = chatAdapter
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSendSupport.setOnClickListener {
            val text = binding.inputSupportMessage.text.toString().trim()
            if (text.isNotBlank()) {
                sendUserMessage(text)
                binding.inputSupportMessage.setText("")
            }
        }

        binding.btnAttachFile.setOnClickListener {
            attachFileLauncher.launch("image/*")
        }

        binding.inputSupportMessage.setOnEditorActionListener { _, actionId, event ->
            val sendByAction = actionId == EditorInfo.IME_ACTION_SEND
            val sendByEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (sendByAction || sendByEnter) {
                val text = binding.inputSupportMessage.text.toString().trim()
                if (text.isNotBlank()) {
                    sendUserMessage(text)
                    binding.inputSupportMessage.setText("")
                }
                true
            } else {
                false
            }
        }

        // Quick Reply Chips
        binding.chipQrReport.setOnClickListener { sendUserMessage("Denúncia de animal") }
        binding.chipQrAdoption.setOnClickListener { sendUserMessage("Adoção") }
        binding.chipQrAccount.setOnClickListener { sendUserMessage("Minha conta") }
        binding.chipQrScams.setOnClickListener { sendUserMessage("Golpe ou cobrança") }
        binding.chipQrOther.setOnClickListener { sendUserMessage("Outro assunto") }

        // Rating Stars
        val stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        stars.forEachIndexed { index, starView ->
            starView.setOnClickListener {
                stars.forEachIndexed { i, s ->
                    s.setTextColor(if (i <= index) 0xFFD9A441.toInt() else 0xFFDDD6C4.toInt())
                }
            }
        }

        binding.btnSubmitRating.setOnClickListener {
            Toast.makeText(this, "Obrigado por avaliar nosso atendimento!", Toast.LENGTH_LONG).show()
            binding.cardRating.visibility = View.GONE
        }

        binding.btnNewChat.setOnClickListener {
            restartChat()
        }
    }

    private fun sendUserMessage(text: String) {
        if (isChatEnded) return

        val userMsg = LocalChatMessage(
            text = text,
            time = getCurrentTime(),
            type = LocalMessageType.OUTGOING
        )
        chatAdapter.addMessage(userMsg)
        binding.recyclerViewSupportChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

        // Simulated Agent Auto-Reply
        lifecycleScope.launch {
            delay(1500)
            val replyText = when {
                text.contains("Denúncia", true) -> "Entendido! Você pode acompanhar o status da sua denúncia direto em Perfil › Minhas denúncias."
                text.contains("Adoção", true) -> "Para adotar, navegue até a aba Adoção e escolha um pet para preencher o questionário."
                text.contains("Golpe", true) -> "Segurança em 1º lugar! Nunca envie dinheiro adiantado. Posso te ajudar a registrar uma ocorrência na Ouvidoria."
                else -> "Oi! Sou da equipe WeAnimals. Como posso te ajudar com essa solicitação?"
            }

            val replyMsg = LocalChatMessage(
                text = replyText,
                time = getCurrentTime(),
                type = LocalMessageType.INCOMING
            )
            chatAdapter.addMessage(replyMsg)
            binding.recyclerViewSupportChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

            // Show option to end chat if conversation has progressed
            if (messages.size >= 4 && !isChatEnded) {
                delay(1000)
                endChatSession()
            }
        }
    }

    private fun endChatSession() {
        isChatEnded = true
        binding.chatStatus.text = "Conversa encerrada"
        binding.chatStatus.setTextColor(0xFF5C5548.toInt())
        binding.onlineIndicator.visibility = View.GONE
        binding.quickRepliesScroll.visibility = View.GONE
        binding.inputRowContainer.visibility = View.GONE
        binding.cardRating.visibility = View.VISIBLE

        chatAdapter.addMessage(
            LocalChatMessage(
                text = "Atendimento encerrado às ${getCurrentTime()}",
                time = getCurrentTime(),
                type = LocalMessageType.SYSTEM
            )
        )
        binding.recyclerViewSupportChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun restartChat() {
        isChatEnded = false
        binding.chatStatus.text = "● Online"
        binding.chatStatus.setTextColor(0xFF2C5240.toInt())
        binding.onlineIndicator.visibility = View.VISIBLE
        binding.quickRepliesScroll.visibility = View.VISIBLE
        binding.inputRowContainer.visibility = View.VISIBLE
        binding.cardRating.visibility = View.GONE

        setupChatList()
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
