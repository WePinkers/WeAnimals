package com.example.weanimals.reporting.chat.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.databinding.ActivityCaseChatBinding
import com.example.weanimals.databinding.ItemChatMsgIncomingBinding
import com.example.weanimals.databinding.ItemChatMsgOutgoingBinding
import com.example.weanimals.databinding.ItemChatMsgSystemBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaseChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaseChatBinding
    private val messages = mutableListOf<LocalChatMessage>()
    private lateinit var chatAdapter: LocalChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCaseChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChatList()
        setupInteractions()
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
                    text = "Olá! Vi que você registrou que encontrou o Pipoca. É ele mesmo?",
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
            Toast.makeText(this, "Registrando reencontro...", Toast.LENGTH_LONG).show()
            finish()
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

        // Simulated Auto-Reply (2s delay)
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

    companion object {
        fun newIntent(context: Context, reportId: String, protocolNumber: Int) =
            Intent(context, CaseChatActivity::class.java).apply {
                putExtra("extra_report_id", reportId)
                putExtra("extra_protocol_number", protocolNumber)
            }

        fun newAdoptionIntent(
            context: Context,
            animalId: String,
            animalName: String,
            organizationName: String
        ) = Intent(context, CaseChatActivity::class.java).apply {
            putExtra("extra_adoption_chat", true)
            putExtra("extra_animal_id", animalId)
            putExtra("extra_animal_name", animalName)
            putExtra("extra_organization_name", organizationName)
        }
    }
}

enum class LocalMessageType { SYSTEM, INCOMING, OUTGOING }

data class LocalChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val time: String,
    val type: LocalMessageType
)

class LocalChatAdapter(
    private val items: MutableList<LocalChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SYSTEM = 0
        private const val TYPE_INCOMING = 1
        private const val TYPE_OUTGOING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            LocalMessageType.SYSTEM -> TYPE_SYSTEM
            LocalMessageType.INCOMING -> TYPE_INCOMING
            LocalMessageType.OUTGOING -> TYPE_OUTGOING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SYSTEM -> {
                val binding = ItemChatMsgSystemBinding.inflate(inflater, parent, false)
                SystemViewHolder(binding)
            }
            TYPE_INCOMING -> {
                val binding = ItemChatMsgIncomingBinding.inflate(inflater, parent, false)
                IncomingViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMsgOutgoingBinding.inflate(inflater, parent, false)
                OutgoingViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is SystemViewHolder -> holder.binding.msgSystemText.text = item.text
            is IncomingViewHolder -> {
                holder.binding.msgIncomingText.text = item.text
                holder.binding.msgIncomingTime.text = item.time
            }
            is OutgoingViewHolder -> {
                holder.binding.msgOutgoingText.text = item.text
                holder.binding.msgOutgoingTime.text = item.time
            }
        }
    }

    override fun getItemCount() = items.size

    fun addMessage(msg: LocalChatMessage) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    class SystemViewHolder(val binding: ItemChatMsgSystemBinding) : RecyclerView.ViewHolder(binding.root)
    class IncomingViewHolder(val binding: ItemChatMsgIncomingBinding) : RecyclerView.ViewHolder(binding.root)
    class OutgoingViewHolder(val binding: ItemChatMsgOutgoingBinding) : RecyclerView.ViewHolder(binding.root)
}
