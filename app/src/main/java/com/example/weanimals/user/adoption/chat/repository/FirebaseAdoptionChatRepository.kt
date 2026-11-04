package com.example.weanimals.user.adoption.chat.repository

import android.content.Context
import com.example.weanimals.user.reporting.chat.domain.ChatMessage
import com.example.weanimals.user.reporting.chat.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** One conversation per user and animal, shared by the animal detail and application tracking. */
class FirebaseAdoptionChatRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context,
) : ChatRepository {

    override fun observeMessages(reportId: String): Flow<Result<List<ChatMessage>>> = callbackFlow {
        val user = try {
            require(reportId.isNotBlank()) { "Animal id is required." }
            ensureSignedIn()
        } catch (error: Exception) {
            trySend(Result.failure(error))
            close()
            return@callbackFlow
        }
        val registration = conversation(user.uid, reportId).collection("messages")
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    snapshot != null -> trySend(Result.success(
                        snapshot.documents.mapNotNull { it.toChatMessageOrNull(user.uid) }
                            .sortedBy(ChatMessage::createdAtMillis)
                    ))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(reportId: String, text: String): Result<ChatMessage> = try {
        require(reportId.isNotBlank()) { "Animal id is required." }
        val normalized = text.trim()
        require(normalized.isNotEmpty()) { "Message cannot be empty." }
        require(normalized.length <= MAX_MESSAGE_LENGTH) { "Message is too long." }
        val user = ensureSignedIn()
        val senderName = profileName(user)
        val conversation = conversation(user.uid, reportId)
        val message = conversation.collection("messages").document()
        val application = firestore.collection("adoption_applications")
            .document(user.uid).collection("animals").document(reportId)
        val animal = firestore.collection("animais").document(reportId)

        firestore.runTransaction { transaction ->
            val conversationDocument = transaction.get(conversation)
            if (!conversationDocument.exists()) {
                val applicationDocument = transaction.get(application)
                val ngoId = if (applicationDocument.exists()) {
                    applicationDocument.getString("ngoId")
                } else {
                    val animalDocument = transaction.get(animal)
                    check(animalDocument.exists() && animalDocument.getString("status") == "disponivel") {
                        "Animal is not available for conversation."
                    }
                    animalDocument.getString("ong_id")
                }
                check(!ngoId.isNullOrBlank()) { "Animal has no responsible organization." }
                val conversationData = hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "animalId" to reportId,
                    "ngoId" to ngoId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                senderName?.let { conversationData["userName"] = it }
                transaction.set(conversation, conversationData)
            } else if (senderName != null && conversationDocument.getString("userName").isNullOrBlank()) {
                transaction.update(conversation, "userName", senderName)
            }
            val messageData = hashMapOf<String, Any>(
                "senderId" to user.uid,
                "senderRole" to ChatMessage.SENDER_USER,
                "text" to normalized,
                "createdAt" to FieldValue.serverTimestamp()
            )
            senderName?.let { messageData["senderName"] = it }
            transaction.set(message, messageData)
        }.await()
        Result.success(ChatMessage(
            id = message.id,
            senderId = user.uid,
            senderRole = ChatMessage.SENDER_USER,
            text = normalized,
            createdAtMillis = System.currentTimeMillis(),
            isMine = true
        ))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun conversation(userId: String, animalId: String): DocumentReference = firestore
        .collection("adoption_conversations")
        .document(userId).collection("animals").document(animalId)

    private suspend fun ensureSignedIn(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Firebase user was not created.")
    }

    private suspend fun profileName(user: FirebaseUser): String? {
        val firestoreName = firestore.collection("user_profiles")
            .document(user.uid)
            .get()
            .await()
            .getString("fullName")
            ?.trim()
            ?.takeIf(String::isNotBlank)
        val localName = context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
            .getString(USER_NAME_KEY, null)
            ?.trim()
            ?.takeIf(String::isNotBlank)
        return firestoreName ?: localName ?: user.displayName?.trim()?.takeIf(String::isNotBlank)
    }

    private fun DocumentSnapshot.toChatMessageOrNull(userId: String): ChatMessage? {
        val senderId = getString("senderId") ?: return null
        val text = getString("text")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderRole = getString("senderRole") ?: ChatMessage.SENDER_USER,
            senderName = getString("senderName"),
            text = text,
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            isMine = senderId == userId
                && getString("senderRole") == ChatMessage.SENDER_USER
        )
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 1000
        const val PROFILE_PREFS = "profile_prefs"
        const val USER_NAME_KEY = "user_name"
    }
}
