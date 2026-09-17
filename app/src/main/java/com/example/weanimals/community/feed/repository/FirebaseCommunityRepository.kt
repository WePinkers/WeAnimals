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
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Firebase user was not created.")
        val posts = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        val feed = mutableListOf<CommunityFeedItem>()
        posts.documents.forEach { document ->
            val category = runCatching {
                CommunityCategory.valueOf(document.getString("category").orEmpty())
            }.getOrNull() ?: return@forEach
            val body = document.getString("body")?.takeIf(String::isNotBlank)
                ?: return@forEach
            val createdAt = document.getTimestamp("createdAt")?.toDate()?.time
                ?: System.currentTimeMillis()
            val likedByCurrentUser = document.reference
                .collection(LIKES_COLLECTION)
                .document(user.uid)
                .get()
                .await()
                .exists()
            feed += CommunityFeedItem.Post(
                id = document.id,
                category = category,
                neighborhood = document.getString("locationLabel"),
                author = document.getString("authorName").orEmpty(),
                timeText = DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                body = body,
                likes = document.getLong("likesCount")?.toInt() ?: 0,
                comments = document.getLong("commentsCount")?.toInt() ?: 0,
                photoData = (document.get("photoData") as? Blob)?.toBytes(),
                likedByCurrentUser = likedByCurrentUser
            )
        }
        feed
    }

    private companion object {
        const val COLLECTION = "community_posts"
        const val LIKES_COLLECTION = "likes"
    }
}
