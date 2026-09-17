package com.example.weanimals.community.feed.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Production shows only posts actually saved by community members. */
object CommunityRepositoryFactory {
    fun create(): CommunityRepository = FirebaseCommunityRepository(
        FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()
    )
}
