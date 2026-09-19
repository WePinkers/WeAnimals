package com.example.weanimals.community.feed.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Debug builds use the same Firestore-backed feed as production. */
object CommunityRepositoryFactory {
    fun create(): CommunityRepository = FirebaseCommunityRepository(
        FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()
    )
}
