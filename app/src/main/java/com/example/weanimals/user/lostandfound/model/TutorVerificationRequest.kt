package com.example.weanimals.user.lostandfound.model

import com.google.firebase.auth.FirebaseAuth

data class TutorVerificationRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val adId: String,
    val requesterUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user",
    val requesterName: String? = null,
    val ownerUserId: String,
    val petName: String,
    val specialMark: String,
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var status: String = "PENDENTE"
)
