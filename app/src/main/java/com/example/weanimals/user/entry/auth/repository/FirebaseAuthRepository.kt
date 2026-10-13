package com.example.weanimals.user.entry.auth.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun signIn(identifier: String, password: String) {
        val email = resolveEmail(identifier)
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun register(
        fullName: String,
        cpf: String,
        email: String,
        password: String
    ) {
        val normalizedCpf = normalizeCpf(cpf)
        val initialUserId = auth.currentUser?.uid
        var createdUser: FirebaseUser? = null

        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = requireNotNull(authResult.user) { "Firebase user was not created." }
            createdUser = user

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()
            ).await()

            val profile = mapOf(
                "fullName" to fullName,
                "cpf" to normalizedCpf,
                "email" to email,
                "createdAt" to FieldValue.serverTimestamp(),
                "termsAcceptedAt" to FieldValue.serverTimestamp(),
                "privacyAcceptedAt" to FieldValue.serverTimestamp(),
                "legalVersion" to LEGAL_VERSION
            )
            val identifier = mapOf(
                "email" to email,
                "userId" to user.uid
            )

            firestore.runBatch { batch ->
                batch.set(firestore.collection(PROFILES_COLLECTION).document(user.uid), profile)
                batch.set(
                    firestore.collection(AUTH_IDENTIFIERS_COLLECTION).document(hash(normalizedCpf)),
                    identifier
                )
            }.await()
        } catch (error: Throwable) {
            val failedUser = createdUser
                ?: auth.currentUser?.takeIf { initialUserId == null }
            if (failedUser != null && auth.currentUser?.uid == failedUser.uid) {
                runCatching { withContext(NonCancellable) { failedUser.delete().await() } }
                auth.signOut()
            }
            throw error
        }
    }
    private suspend fun resolveEmail(identifier: String): String {
        val trimmedIdentifier = identifier.trim()
        if (trimmedIdentifier.contains('@')) {
            return trimmedIdentifier.lowercase()
        }

        val normalizedCpf = normalizeCpf(trimmedIdentifier)
        if (normalizedCpf.length != CPF_LENGTH) {
            throw InvalidCpfException()
        }

        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }

        val document = firestore.collection(AUTH_IDENTIFIERS_COLLECTION)
            .document(hash(normalizedCpf))
            .get()
            .await()
        return document.getString("email") ?: throw AccountNotFoundException()
    }

    private fun normalizeCpf(cpf: String): String = cpf.filter(Char::isDigit)

    private fun hash(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    class AccountNotFoundException : Exception()

    class InvalidCpfException : Exception()

    companion object {
        const val PROFILES_COLLECTION = "user_profiles"
        const val AUTH_IDENTIFIERS_COLLECTION = "auth_identifiers"
        const val LEGAL_VERSION = "2026-10-04"
        const val CPF_LENGTH = 11
    }
}
