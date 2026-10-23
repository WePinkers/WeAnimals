package com.example.weanimals.lostandfound.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityLostAndFoundChatBinding
import com.example.weanimals.databinding.ItemChatMessageBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.model.LostAndFoundChatMessage
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LostAndFoundChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostAndFoundChatBinding
    private var verificationRequest: TutorVerificationRequest? = null
    private var associatedAd: FoundPetAd? = null
    private val currentUserId by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostAndFoundChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Conversa não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        verificationRequest = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (verificationRequest == null) {
            Toast.makeText(this, "Conversa não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        associatedAd = LostAndFoundRepository.getAdById(this, verificationRequest!!.adId)

        val req = verificationRequest!!
        if (currentUserId != req.ownerUserId && currentUserId != req.requesterUserId) {
            Toast.makeText(this, "Acesso não autorizado a esta conversa.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadMessages()
    }

    private fun setupUI() {
        val req = verificationRequest ?: return
        val ad = associatedAd

        val isOwner = (currentUserId == req.ownerUserId)
        val otherName = if (isOwner) {
            req.requesterName?.takeIf { it.isNotBlank() } ?: "Solicitante"
        } else {
            ad?.publisherName?.takeIf { it.isNotBlank() } ?: "Responsável"
        }

        binding.chatAgentName.text = otherName
        binding.chatAvatar.text = otherName.firstOrNull()?.uppercase() ?: "U"

        val animalTitle = ad?.getDisplayTitle() ?: "Animal encontrado"
        binding.chatSubtitle.text = if (isOwner) {
            "Possível tutor de $animalTitle"
        } else {
            "Encontrou o $animalTitle"
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.sendButton.setOnClickListener { sendCurrentMessage() }

        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            val sendByAction = actionId == EditorInfo.IME_ACTION_SEND
            val sendByEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (sendByAction || sendByEnter) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }

        binding.btnRegisterReunion.setOnClickListener {
            showRegisterReunionDialog()
        }
    }

    private fun sendCurrentMessage() {
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return

        val req = verificationRequest ?: return
        val message = LostAndFoundChatMessage(
            requestId = req.id,
            senderUserId = currentUserId,
            text = text
        )

        LostAndFoundRepository.saveChatMessage(this, message)
        binding.messageInput.text?.clear()
        loadMessages()
    }

    private fun loadMessages() {
        val req = verificationRequest ?: return
        val messages = LostAndFoundRepository.getMessagesForRequest(this, req.id)
        val container = binding.messagesContainer
        container.removeAllViews()

        // System initial greeting message
        val systemMsgBinding = ItemChatMessageBinding.inflate(layoutInflater, container, false)
        systemMsgBinding.incomingMessageGroup.visibility = View.VISIBLE
        systemMsgBinding.outgoingMessageGroup.visibility = View.GONE
        systemMsgBinding.incomingMessageText.text = "Chat liberado. Combinem a entrega em um lugar público."
        systemMsgBinding.incomingMessageTime.text = formatTime(req.timestamp)
        container.addView(systemMsgBinding.root)

        for (msg in messages) {
            val msgBinding = ItemChatMessageBinding.inflate(layoutInflater, container, false)
            val isMine = (msg.senderUserId == currentUserId)
            val timeStr = formatTime(msg.timestamp)

            if (isMine) {
                msgBinding.incomingMessageGroup.visibility = View.GONE
                msgBinding.outgoingMessageGroup.visibility = View.VISIBLE
                msgBinding.outgoingMessageText.text = msg.text
                msgBinding.outgoingMessageTime.text = timeStr
            } else {
                msgBinding.outgoingMessageGroup.visibility = View.GONE
                msgBinding.incomingMessageGroup.visibility = View.VISIBLE
                msgBinding.incomingMessageText.text = msg.text
                msgBinding.incomingMessageTime.text = timeStr
            }
            container.addView(msgBinding.root)
        }

        binding.messagesScroll.post {
            binding.messagesScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showRegisterReunionDialog() {
        val ad = associatedAd ?: return
        AlertDialog.Builder(this)
            .setTitle("Registrar reencontro?")
            .setMessage("Deseja marcar este caso como animal reencontrado com o tutor? Esta ação encerrará o anúncio.")
            .setPositiveButton("Sim, reencontrado") { _, _ ->
                LostAndFoundRepository.updateAdSituation(this, ad.id, "Reencontrado")
                Toast.makeText(this, "Reencontro registrado com sucesso!", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun formatTime(timestampMillis: Long): String {
        return if (timestampMillis > 0L) {
            SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(timestampMillis))
        } else {
            "—"
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"

        fun newIntent(context: Context, requestId: String): Intent {
            return Intent(context, LostAndFoundChatActivity::class.java).apply {
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
        }
    }
}
