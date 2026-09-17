package com.example.weanimals.community.feed.repository

import android.text.format.DateUtils
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseCommunityRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityRepository {
    override suspend fun getFeed(): Result<List<CommunityFeedItem>> = runCatching {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        val posts = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        posts.documents.mapNotNull { document ->
            val category = runCatching {
                CommunityCategory.valueOf(document.getString("category").orEmpty())
            }.getOrNull() ?: return@mapNotNull null
            val body = document.getString("body")?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val createdAt = document.getTimestamp("createdAt")?.toDate()?.time
                ?: System.currentTimeMillis()
            CommunityFeedItem.Post(
                id = document.id,
                category = category,
                neighborhood = document.getString("locationLabel"),
                author = "",
                timeText = DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                body = body,
                likes = 0,
                comments = 0,
                photoData = (document.get("photoData") as? Blob)?.toBytes()
            )
        }
    }

    private companion object {
        const val COLLECTION = "community_posts"
    }
}
