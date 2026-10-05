package com.example.weanimals.entry.auth.repository

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

    suspend fun signInOrganization(
        identifier: String,
        password: String,
        pendingEmail: String? = null
    ) {
        val email = resolveOrganizationEmail(identifier, pendingEmail)
        val user = auth.signInWithEmailAndPassword(email, password)
            .await()
            .user ?: error("Firebase organization user was not found.")
        user.reload().await()
        if (!user.isEmailVerified) {
            auth.signOut()
            throw OrganizationEmailVerificationRequiredException()
        }
        val hasPendingAccreditation = firestore
            .collection(ORGANIZATION_VERIFICATION_REQUESTS_COLLECTION)
            .whereEqualTo("requesterId", user.uid)
            .whereEqualTo("status", STATUS_PENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()
        if (hasPendingAccreditation) {
            auth.signOut()
            throw OrganizationAccreditationPendingException()
        }
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

    private suspend fun resolveOrganizationEmail(
        identifier: String,
        pendingEmail: String?
    ): String {
        val trimmedIdentifier = identifier.trim()
        if (trimmedIdentifier.contains('@')) {
            return trimmedIdentifier.lowercase()
        }

        val normalizedCnpj = normalizeCnpj(trimmedIdentifier)
        if (normalizedCnpj.length != CNPJ_LENGTH) {
            throw InvalidOrganizationIdentifierException()
        }

        pendingEmail?.trim()?.lowercase()?.takeIf { it.contains('@') }?.let { return it }

        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }

        val document = firestore.collection(ORGANIZATION_IDENTIFIERS_COLLECTION)
            .document(hash(normalizedCnpj))
            .get()
            .await()
        return document.getString("email") ?: throw OrganizationAccountNotFoundException()
    }

    private fun normalizeCpf(cpf: String): String = cpf.filter(Char::isDigit)

    private fun normalizeCnpj(cnpj: String): String = cnpj.filter(Char::isDigit)

    private fun hash(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    class AccountNotFoundException : Exception()

    class OrganizationAccountNotFoundException : Exception()

    class OrganizationEmailVerificationRequiredException : Exception()

    class OrganizationAccreditationPendingException : Exception()

    class InvalidCpfException : Exception()

    class InvalidOrganizationIdentifierException : Exception()

    private companion object {
        const val PROFILES_COLLECTION = "user_profiles"
        const val AUTH_IDENTIFIERS_COLLECTION = "auth_identifiers"
        const val ORGANIZATION_IDENTIFIERS_COLLECTION = "organization_identifiers"
        const val ORGANIZATION_VERIFICATION_REQUESTS_COLLECTION = "organization_verification_requests"
        const val STATUS_PENDING = "pending"
        const val LEGAL_VERSION = "2026-10-04"
        const val CPF_LENGTH = 11
        const val CNPJ_LENGTH = 14
    }
}
