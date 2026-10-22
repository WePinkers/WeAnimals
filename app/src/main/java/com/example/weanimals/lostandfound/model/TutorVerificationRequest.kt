package com.example.weanimals.lostandfound.model

import com.google.firebase.auth.FirebaseAuth

data class TutorVerificationRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val adId: String,
    val requesterUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user",
    val ownerUserId: String,
    val petName: String,
    val specialMark: String,
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDENTE"
)
