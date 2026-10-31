package com.example.weanimals.user.reporting.chat.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.databinding.ItemChatMsgIncomingBinding
import com.example.weanimals.databinding.ItemChatMsgOutgoingBinding
import com.example.weanimals.databinding.ItemChatMsgSystemBinding

class LocalChatAdapter(
    private val messages: MutableList<LocalChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = messages[position].type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (LocalMessageType.entries[viewType]) {
            LocalMessageType.INCOMING -> IncomingHolder(
                ItemChatMsgIncomingBinding.inflate(inflater, parent, false)
            )
            LocalMessageType.OUTGOING -> OutgoingHolder(
                ItemChatMsgOutgoingBinding.inflate(inflater, parent, false)
            )
            LocalMessageType.SYSTEM -> SystemHolder(
                ItemChatMsgSystemBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is IncomingHolder -> holder.binding.apply {
                msgIncomingText.text = message.text
                msgIncomingTime.text = message.time
            }
            is OutgoingHolder -> holder.binding.apply {
                msgOutgoingText.text = message.text
                msgOutgoingTime.text = message.time
            }
            is SystemHolder -> holder.binding.msgSystemText.text = message.text
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: LocalChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.lastIndex)
    }

    private class IncomingHolder(
        val binding: ItemChatMsgIncomingBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private class OutgoingHolder(
        val binding: ItemChatMsgOutgoingBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private class SystemHolder(
        val binding: ItemChatMsgSystemBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
