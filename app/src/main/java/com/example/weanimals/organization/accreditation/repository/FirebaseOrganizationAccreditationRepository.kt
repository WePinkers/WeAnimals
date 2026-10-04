package com.example.weanimals.organization.accreditation.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseOrganizationAccreditationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun submit(
        cnpj: String,
        legalName: String,
        institutionalEmail: String,
        password: String,
        documentName: String,
        documentSizeBytes: Long,
        documentContentType: String,
        pixKey: String
    ) {
        val requester = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Could not create the accreditation requester.")
        var linkedAnonymousAccount = false
        val user = if (requester.isAnonymous) {
            val credential = EmailAuthProvider.getCredential(institutionalEmail, password)
            try {
                val linkedUser = requester.linkWithCredential(credential).await().user
                    ?: error("Could not create the organization account.")
                linkedAnonymousAccount = true
                linkedUser
            } catch (collision: FirebaseAuthUserCollisionException) {
                // Reinstalling the app does not remove the Firebase Auth account.
                // If this is the same unapproved institutional account, continue
                // the verification flow instead of showing a false duplicate error.
                val existingUser = try {
                    auth.signInWithEmailAndPassword(institutionalEmail, password)
                        .await()
                        .user
                } catch (_: Throwable) {
                    null
                } ?: throw collision

                val isCitizenAccount = firestore
                    .collection(CITIZEN_PROFILES_COLLECTION)
                    .document(existingUser.uid)
                    .get()
                    .await()
                    .exists()
                if (isCitizenAccount) {
                    auth.signOut()
                    throw collision
                }
                existingUser
            }
        } else if (requester.email.equals(institutionalEmail, ignoreCase = true)) {
            requester
        } else {
            error("An authenticated account is already active on this device.")
        }
        user.reload().await()
        user.getIdToken(true).await()
        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
            throw EmailVerificationRequiredException()
        }
        val request = firestore.collection(REQUESTS_COLLECTION).document()
        try {
            request.set(
                mapOf(
                    "requesterId" to user.uid,
                    "cnpj" to cnpj.filter(Char::isDigit),
                    "legalName" to legalName,
                    "institutionalEmail" to institutionalEmail,
                    "documentName" to documentName,
                    "documentSizeBytes" to documentSizeBytes,
                    "documentContentType" to documentContentType,
                    "pixKey" to pixKey,
                    "status" to STATUS_PENDING,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "termsAcceptedAt" to FieldValue.serverTimestamp(),
                    "privacyAcceptedAt" to FieldValue.serverTimestamp(),
                    "legalVersion" to LEGAL_VERSION
                )
            ).await()
        } catch (error: Throwable) {
            if (linkedAnonymousAccount) {
                runCatching {
                    auth.currentUser?.unlink(EmailAuthProvider.PROVIDER_ID)?.await()
                }
            }
            throw error
        }
    }

    private companion object {
        const val REQUESTS_COLLECTION = "organization_verification_requests"
        const val LEGAL_VERSION = "2026-10-04"
        const val CITIZEN_PROFILES_COLLECTION = "user_profiles"
        const val STATUS_PENDING = "pending"
    }

    class EmailVerificationRequiredException : Exception()
}
