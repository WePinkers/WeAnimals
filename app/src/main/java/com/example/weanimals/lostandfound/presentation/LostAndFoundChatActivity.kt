package com.example.weanimals.lostandfound.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private var isTutorView: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostAndFoundChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        val roleExtra = intent.getStringExtra(EXTRA_VIEWER_ROLE)

        Log.d(TAG, "Opening chat for requestId: $requestId, roleExtra: $roleExtra, currentUserId: $currentUserId")

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

        // Determine role robustly
        isTutorView = when {
            currentUserId == req.requesterUserId && currentUserId != req.ownerUserId -> true
            currentUserId == req.ownerUserId && currentUserId != req.requesterUserId -> false
            else -> roleExtra == ROLE_TUTOR
        }

        Log.d(TAG, "Resolved isTutorView: $isTutorView (ownerUserId: ${req.ownerUserId}, requesterUserId: ${req.requesterUserId})")

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

        val otherName = if (isTutorView) {
            ad?.publisherName?.takeIf { it.isNotBlank() } ?: "Responsável"
        } else {
            req.requesterName?.takeIf { it.isNotBlank() } ?: "Solicitante"
        }

        binding.chatAgentName.text = otherName
        binding.chatAvatar.text = otherName.firstOrNull()?.uppercase() ?: "U"

        val animalTitle = ad?.getDisplayTitle() ?: "Animal encontrado"
        binding.chatSubtitle.text = if (isTutorView) {
            "Responsável pelo anúncio do $animalTitle"
        } else {
            "Possível tutor de $animalTitle"
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
        val senderId = if (currentUserId == req.ownerUserId && currentUserId == req.requesterUserId) {
            if (isTutorView) req.requesterUserId else req.ownerUserId
        } else {
            currentUserId
        }

        val message = LostAndFoundChatMessage(
            requestId = req.id,
            senderUserId = senderId,
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
            val isMine = if (currentUserId == req.ownerUserId && currentUserId == req.requesterUserId) {
                if (isTutorView) msg.senderUserId == req.requesterUserId else msg.senderUserId == req.ownerUserId
            } else {
                msg.senderUserId == currentUserId
            }
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
        private const val TAG = "LostAndFoundChatActivity"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_VIEWER_ROLE = "extra_viewer_role"
        const val ROLE_TUTOR = "ROLE_TUTOR"
        const val ROLE_FOUNDER = "ROLE_FOUNDER"

        fun newIntent(context: Context, requestId: String, isTutor: Boolean): Intent {
            return Intent(context, LostAndFoundChatActivity::class.java).apply {
                putExtra(EXTRA_REQUEST_ID, requestId)
                putExtra(EXTRA_VIEWER_ROLE, if (isTutor) ROLE_TUTOR else ROLE_FOUNDER)
            }
        }
    }
}
