package com.example.weanimals.community.feed.repository

import android.util.Log
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
        val posts = runCatching {
            firestore.collection(POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
        }.onFailure { Log.w(TAG, "Could not load community posts", it) }
            .getOrDefault(emptyList())
        val campaigns = runCatching {
            firestore.collection(CAMPAIGNS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
        }.onFailure { Log.w(TAG, "Could not load community campaigns", it) }
            .getOrDefault(emptyList())
        val feed = mutableListOf<CommunityFeedItem>()

        for (document in posts) {
            val category = runCatching {
                CommunityCategory.valueOf(document.getString("category").orEmpty())
            }.getOrNull() ?: continue
            val body = document.getString("body")?.takeIf(String::isNotBlank)
                ?: continue
            val createdAt = document.getTimestamp("createdAt")?.toDate()?.time
                ?: System.currentTimeMillis()
            val likedByCurrentUser = runCatching {
                document.reference
                    .collection(LIKES_COLLECTION)
                    .document(user.uid)
                    .get()
                    .await()
                    .exists()
            }.onFailure { Log.w(TAG, "Could not load post engagement", it) }
                .getOrDefault(false)
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
                likedByCurrentUser = likedByCurrentUser,
                createdAtMillis = createdAt
            )
        }

        for (document in campaigns) {
            val category = runCatching {
                CommunityCategory.valueOf(document.getString("category").orEmpty())
            }.getOrNull()
            if (category == null || category !in CAMPAIGN_CATEGORIES) continue
            val title = document.getString("title")?.trim()?.takeIf(String::isNotBlank)
                ?: continue
            val createdAt = document.getTimestamp("createdAt")?.toDate()?.time
                ?: System.currentTimeMillis()
            val participatingByCurrentUser = runCatching {
                document.reference
                    .collection(PARTICIPANTS_COLLECTION)
                    .document(user.uid)
                    .get()
                    .await()
                    .exists()
            }.onFailure { Log.w(TAG, "Could not load campaign participation", it) }
                .getOrDefault(false)
            val confirmedAnimalsCount = if (category == CommunityCategory.ADOPTION) {
                runCatching {
                    document.reference
                        .collection(ANIMALS_COLLECTION)
                        .get()
                        .await()
                        .size()
                }.onFailure { Log.w(TAG, "Could not load confirmed animals", it) }
                    .getOrDefault(0)
            } else {
                null
            }
            feed += CommunityFeedItem.Campaign(
                id = document.id,
                category = category,
                neighborhood = document.getString("locationLabel"),
                organization = document.getString("organizationName").orEmpty(),
                title = title,
                dateText = document.getString("dateText").orEmpty(),
                locationText = document.getString("locationText")
                    ?.takeIf(String::isNotBlank)
                    ?: document.getString("locationLabel").orEmpty(),
                availabilityText = document.getString("availabilityText")
                    ?.takeIf(String::isNotBlank)
                    ?: "Sem limite de vagas",
                audienceText = document.getString("audienceText").orEmpty(),
                description = document.getString("description").orEmpty(),
                highlightText = document.getString("highlightText").orEmpty(),
                totalSlots = document.getLong("totalSlots")?.toInt(),
                filledSlots = document.getLong("filledSlots")?.toInt(),
                donationGoalCents = document.getLong("donationGoalCents"),
                donationRaisedCents = document.getLong("donationRaisedCents"),
                donationNeeds = (document.get("donationNeeds") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.map(String::trim)
                    ?.filter(String::isNotBlank)
                    .orEmpty(),
                pixKey = document.getString("pixKey").orEmpty(),
                locationLatitude = document.getDouble("locationLatitude"),
                locationLongitude = document.getDouble("locationLongitude"),
                participatingByCurrentUser = participatingByCurrentUser,
                confirmedAnimalsCount = confirmedAnimalsCount,
                createdAtMillis = createdAt
            )
        }

        feed.sortedByDescending {
            when (it) {
                is CommunityFeedItem.Campaign -> it.createdAtMillis
                is CommunityFeedItem.Post -> it.createdAtMillis
            }
        }
    }

    private companion object {
        const val TAG = "FirebaseCommunityFeed"
        const val POSTS_COLLECTION = "community_posts"
        const val CAMPAIGNS_COLLECTION = "community_campaigns"
        const val ANIMALS_COLLECTION = "animals"
        const val PARTICIPANTS_COLLECTION = "participants"
        const val LIKES_COLLECTION = "likes"
        val CAMPAIGN_CATEGORIES = setOf(
            CommunityCategory.NEUTERING,
            CommunityCategory.VACCINATION,
            CommunityCategory.ADOPTION,
            CommunityCategory.DONATION
        )
    }
}
