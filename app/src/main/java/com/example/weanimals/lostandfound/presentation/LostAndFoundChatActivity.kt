package com.example.weanimals.lostandfound.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
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
            roleExtra == ROLE_TUTOR -> true
            roleExtra == ROLE_FOUNDER -> false
            currentUserId == req.requesterUserId && currentUserId != req.ownerUserId -> true
            currentUserId == req.ownerUserId && currentUserId != req.requesterUserId -> false
            else -> false
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

        binding.btnChatOptions.setOnClickListener {
            val req = verificationRequest ?: return@setOnClickListener
            val intent = Intent(this, ReportChatActivity::class.java).apply {
                putExtra(ReportChatActivity.EXTRA_REQUEST_ID, req.id)
            }
            startActivity(intent)
        }

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
            val req = verificationRequest ?: return@setOnClickListener
            val reunion = LostAndFoundRepository.getReunionForRequest(this, req.id)
            if (reunion != null && reunion.status.equals("CONFIRMED", ignoreCase = true)) {
                Toast.makeText(this, "Este reencontro já foi confirmado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (reunion != null && reunion.status.equals("PENDING_CONFIRMATION", ignoreCase = true)) {
                val isInitiator = if (req.ownerUserId == req.requesterUserId || currentUserId == "local_user") {
                    !isTutorView
                } else {
                    reunion.initiatedByUserId == currentUserId
                }

                if (isInitiator) {
                    val intent = Intent(this, ReunionSubmittedActivity::class.java).apply {
                        putExtra(ReunionSubmittedActivity.EXTRA_REQUEST_ID, req.id)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ConfirmReunionActivity::class.java).apply {
                        putExtra(ConfirmReunionActivity.EXTRA_REQUEST_ID, req.id)
                    }
                    startActivity(intent)
                }
                return@setOnClickListener
            }

            val intent = Intent(this, RegisterReunionActivity::class.java).apply {
                putExtra(RegisterReunionActivity.EXTRA_REQUEST_ID, req.id)
            }
            startActivity(intent)
        }
    }

    private fun sendCurrentMessage() {
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return

        val req = verificationRequest ?: return
        val isSimulationMode = (req.ownerUserId == req.requesterUserId) || (currentUserId == "local_user")

        val senderId = if (isSimulationMode) {
            if (isTutorView) "SIM_TUTOR" else "SIM_FOUNDER"
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

        LostAndFoundRepository.markMessagesAsRead(this, req.id, currentUserId, isTutorView)

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

        val reunion = LostAndFoundRepository.getReunionForRequest(this, req.id)
        if (reunion != null) {
            if (reunion.status.equals("CONFIRMED", ignoreCase = true)) {
                binding.btnRegisterReunion.visibility = View.GONE
                val confMsgBinding = ItemChatMessageBinding.inflate(layoutInflater, container, false)
                confMsgBinding.incomingMessageGroup.visibility = View.VISIBLE
                confMsgBinding.outgoingMessageGroup.visibility = View.GONE
                confMsgBinding.incomingMessageText.text = "Reencontro confirmado! O animal voltou ao tutor e o anúncio foi encerrado com sucesso."
                confMsgBinding.incomingMessageTime.text = formatTime(reunion.confirmedAt ?: reunion.createdAt)
                container.addView(confMsgBinding.root)
            } else if (reunion.status.equals("PENDING_CONFIRMATION", ignoreCase = true)) {
                val isInitiator = if (req.ownerUserId == req.requesterUserId || currentUserId == "local_user") {
                    !isTutorView
                } else {
                    reunion.initiatedByUserId == currentUserId
                }

                if (!isInitiator) {
                    val cardLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.bg_card_white_border)
                        setPadding(28, 24, 28, 24)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 16, 0, 16)
                        layoutParams = params
                    }

                    val titleTv = TextView(this).apply {
                        text = "Confirmação de reencontro"
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(getColor(R.color.pine800))
                    }

                    val descTv = TextView(this).apply {
                        text = "A outra pessoa informou que o animal foi entregue. Você confirma que o reencontro realmente aconteceu?"
                        textSize = 13f
                        setTextColor(getColor(R.color.muted))
                        setPadding(0, 4, 0, 14)
                    }

                    val btnsRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    val btnConfirm = androidx.appcompat.widget.AppCompatButton(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (46 * resources.displayMetrics.density).toInt(), 1f).apply {
                            marginEnd = 6
                        }
                        text = "Confirmar reencontro"
                        isAllCaps = false
                        textSize = 13f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(getColor(R.color.white))
                        setBackgroundResource(R.drawable.bg_btn_dark_green)
                        setOnClickListener {
                            val intent = Intent(this@LostAndFoundChatActivity, ConfirmReunionActivity::class.java).apply {
                                putExtra(ConfirmReunionActivity.EXTRA_REQUEST_ID, req.id)
                            }
                            startActivity(intent)
                        }
                    }

                    val btnNotYet = androidx.appcompat.widget.AppCompatButton(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (46 * resources.displayMetrics.density).toInt(), 1f).apply {
                            marginStart = 6
                        }
                        text = "Ainda não aconteceu"
                        isAllCaps = false
                        textSize = 13f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(getColor(R.color.pine800))
                        setBackgroundResource(R.drawable.bg_btn_outline)
                        setOnClickListener {
                            LostAndFoundRepository.updateReunionStatus(this@LostAndFoundChatActivity, reunion.reunionId, "NOT_CONFIRMED")
                            Toast.makeText(this@LostAndFoundChatActivity, "Resposta registrada: o reencontro ainda não foi confirmado.", Toast.LENGTH_LONG).show()
                            loadMessages()
                        }
                    }

                    btnsRow.addView(btnConfirm)
                    btnsRow.addView(btnNotYet)

                    cardLayout.addView(titleTv)
                    cardLayout.addView(descTv)
                    cardLayout.addView(btnsRow)
                    container.addView(cardLayout)
                } else {
                    val promptBinding = ItemChatMessageBinding.inflate(layoutInflater, container, false)
                    promptBinding.incomingMessageGroup.visibility = View.VISIBLE
                    promptBinding.outgoingMessageGroup.visibility = View.GONE
                    promptBinding.incomingMessageText.text = "Solicitação enviada. Aguardando a outra pessoa confirmar o reencontro."
                    promptBinding.incomingMessageTime.text = formatTime(reunion.createdAt)
                    container.addView(promptBinding.root)
                }
            } else if (reunion.status.equals("NOT_CONFIRMED", ignoreCase = true)) {
                val notConfMsgBinding = ItemChatMessageBinding.inflate(layoutInflater, container, false)
                notConfMsgBinding.incomingMessageGroup.visibility = View.VISIBLE
                notConfMsgBinding.outgoingMessageGroup.visibility = View.GONE
                notConfMsgBinding.incomingMessageText.text = "O reencontro ainda não foi confirmado. O anúncio permanecerá aberto para acompanhamento."
                notConfMsgBinding.incomingMessageTime.text = formatTime(reunion.respondedAt ?: reunion.createdAt)
                container.addView(notConfMsgBinding.root)
            }
        }

        val isSimulationMode = (req.ownerUserId == req.requesterUserId) || (currentUserId == "local_user")

        for (msg in messages) {
            val msgBinding = ItemChatMessageBinding.inflate(layoutInflater, container, false)

            val isMine = if (isSimulationMode) {
                if (isTutorView) {
                    msg.senderUserId == "SIM_TUTOR" || msg.senderUserId == "local_tutor"
                } else {
                    msg.senderUserId == "SIM_FOUNDER" || msg.senderUserId == "local_founder" || msg.senderUserId == "local_user"
                }
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
