package com.example.weanimals.reporting.chat.repository

import com.example.weanimals.reporting.chat.domain.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseChatRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun observeMessages(reportId: String): Flow<Result<List<ChatMessage>>> = callbackFlow {
        if (reportId.isBlank()) {
            trySend(Result.failure(IllegalArgumentException("Report id is required.")))
            close()
            return@callbackFlow
        }

        val user = try {
            ensureSignedIn()
        } catch (exception: Exception) {
            trySend(Result.failure(exception))
            close()
            return@callbackFlow
        }

        val registration = messagesReference(reportId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot != null -> {
                        val messages = snapshot.documents
                            .mapNotNull { it.toChatMessageOrNull(user.uid) }
                            .sortedBy(ChatMessage::createdAtMillis)
                        trySend(Result.success(messages))
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(reportId: String, text: String): Result<ChatMessage> = runCatching {
        require(reportId.isNotBlank()) { "Report id is required." }
        val normalizedText = text.trim()
        require(normalizedText.isNotEmpty()) { "Message cannot be empty." }
        require(normalizedText.length <= MAX_MESSAGE_LENGTH) { "Message is too long." }

        val user = ensureSignedIn()
        val createdAt = Timestamp.now()
        val reference = messagesReference(reportId).document()
        reference.set(
            mapOf(
                FIELD_REPORT_ID to reportId,
                FIELD_SENDER_ID to user.uid,
                FIELD_SENDER_ROLE to ChatMessage.SENDER_USER,
                FIELD_TEXT to normalizedText,
                FIELD_CREATED_AT to createdAt
            )
        ).await()

        ChatMessage(
            id = reference.id,
            senderId = user.uid,
            senderRole = ChatMessage.SENDER_USER,
            text = normalizedText,
            createdAtMillis = createdAt.toDate().time,
            isMine = true
        )
    }

    private fun messagesReference(reportId: String) = firestore
        .collection(REPORTS_COLLECTION)
        .document(reportId)
        .collection(MESSAGES_COLLECTION)

    private suspend fun ensureSignedIn(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Firebase user was not created.")
    }

    private fun DocumentSnapshot.toChatMessageOrNull(currentUserId: String): ChatMessage? {
        val senderId = getString(FIELD_SENDER_ID) ?: return null
        val text = getString(FIELD_TEXT)?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderRole = getString(FIELD_SENDER_ROLE) ?: ChatMessage.SENDER_USER,
            senderName = getString(FIELD_SENDER_NAME),
            text = text,
            createdAtMillis = getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
            isMine = senderId == currentUserId
                && (getString(FIELD_SENDER_ROLE) ?: ChatMessage.SENDER_USER) == ChatMessage.SENDER_USER
        )
    }

    private companion object {
        const val REPORTS_COLLECTION = "reports"
        const val MESSAGES_COLLECTION = "messages"
        const val FIELD_REPORT_ID = "reportId"
        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_SENDER_ROLE = "senderRole"
        const val FIELD_SENDER_NAME = "senderName"
        const val FIELD_TEXT = "text"
        const val FIELD_CREATED_AT = "createdAt"
        const val MAX_MESSAGE_LENGTH = 1000
    }
}
