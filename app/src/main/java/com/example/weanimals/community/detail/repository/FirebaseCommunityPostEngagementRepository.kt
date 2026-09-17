package com.example.weanimals.community.detail.repository

import android.text.format.DateUtils
import com.example.weanimals.community.detail.domain.CommunityComment
import com.example.weanimals.community.detail.domain.CommunityPostEngagement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseCommunityPostEngagementRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityPostEngagementRepository {

    override suspend fun getEngagement(postId: String): Result<CommunityPostEngagement> = runCatching {
        require(postId.isNotBlank()) { "Post id is required." }
        val user = ensureSignedIn()
        val post = postsReference().document(postId).get().await()
        val liked = likesReference(postId).document(user.uid).get().await().exists()
        CommunityPostEngagement(
            likes = post.getLong(FIELD_LIKES_COUNT)?.toInt() ?: 0,
            comments = post.getLong(FIELD_COMMENTS_COUNT)?.toInt() ?: 0,
            likedByCurrentUser = liked
        )
    }

    override suspend fun getComments(postId: String): Result<List<CommunityComment>> = runCatching {
        require(postId.isNotBlank()) { "Post id is required." }
        ensureSignedIn()
        commentsReference(postId)
            .orderBy(FIELD_CREATED_AT)
            .limit(MAX_COMMENTS)
            .get()
            .await()
            .documents
            .mapNotNull { it.toCommentOrNull() }
    }

    override suspend fun toggleLike(
        postId: String,
        liked: Boolean
    ): Result<CommunityPostEngagement> = runCatching {
        require(postId.isNotBlank()) { "Post id is required." }
        val user = ensureSignedIn()
        val postReference = postsReference().document(postId)
        val likeReference = likesReference(postId).document(user.uid)

        firestore.runTransaction { transaction ->
            val post = transaction.get(postReference)
            val likeExists = transaction.get(likeReference).exists()
            val currentLikes = post.getLong(FIELD_LIKES_COUNT)?.toInt() ?: 0
            val currentComments = post.getLong(FIELD_COMMENTS_COUNT)?.toInt() ?: 0
            val shouldLike = liked && !likeExists
            val shouldUnlike = !liked && likeExists
            val updatedLikes = when {
                shouldLike -> currentLikes + 1
                shouldUnlike -> (currentLikes - 1).coerceAtLeast(0)
                else -> currentLikes
            }

            if (shouldLike || shouldUnlike) {
                when {
                    shouldLike -> transaction.set(
                        likeReference,
                        mapOf(
                            FIELD_USER_ID to user.uid,
                            FIELD_CREATED_AT to FieldValue.serverTimestamp()
                        )
                    )
                    shouldUnlike -> transaction.delete(likeReference)
                }
                transaction.update(
                    postReference,
                    mapOf(
                        FIELD_LIKES_COUNT to updatedLikes,
                        FIELD_COMMENTS_COUNT to currentComments
                    )
                )
            }
            updatedLikes
        }.await()

        getEngagement(postId).getOrThrow()
    }

    override suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String?
    ): Result<CommunityComment> = runCatching {
        require(postId.isNotBlank()) { "Post id is required." }
        val normalizedText = text.trim()
        require(normalizedText.isNotEmpty()) { "Comment cannot be empty." }
        require(normalizedText.length <= MAX_COMMENT_LENGTH) { "Comment is too long." }

        val user = ensureSignedIn()
        val commentReference = commentsReference(postId).document()
        val author = user.displayName?.trim().takeIf { !it.isNullOrEmpty() }
            ?: "Você"
        val postReference = postsReference().document(postId)

        firestore.runTransaction { transaction ->
            val post = transaction.get(postReference)
            val comments = post.getLong(FIELD_COMMENTS_COUNT)?.toInt() ?: 0
            val likes = post.getLong(FIELD_LIKES_COUNT)?.toInt() ?: 0
            val commentData = mutableMapOf<String, Any>(
                FIELD_AUTHOR_ID to user.uid,
                FIELD_AUTHOR_NAME to author,
                FIELD_TEXT to normalizedText,
                FIELD_CREATED_AT to FieldValue.serverTimestamp()
            )
            parentCommentId?.takeIf(String::isNotBlank)?.let {
                commentData[FIELD_PARENT_COMMENT_ID] = it
            }
            transaction.set(commentReference, commentData)
            transaction.update(
                postReference,
                mapOf(
                    FIELD_LIKES_COUNT to likes,
                    FIELD_COMMENTS_COUNT to comments + 1
                )
            )
        }.await()

        CommunityComment(
            id = commentReference.id,
            author = author,
            timeText = "agora",
            text = normalizedText,
            parentCommentId = parentCommentId
        )
    }

    private fun postsReference() = firestore.collection(POSTS_COLLECTION)

    private fun commentsReference(postId: String) = postsReference()
        .document(postId)
        .collection(COMMENTS_COLLECTION)

    private fun likesReference(postId: String) = postsReference()
        .document(postId)
        .collection(LIKES_COLLECTION)

    private suspend fun ensureSignedIn(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Firebase user was not created.")
    }

    private fun DocumentSnapshot.toCommentOrNull(): CommunityComment? {
        val text = getString(FIELD_TEXT)?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val createdAt = getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L
        return CommunityComment(
            id = id,
            author = getString(FIELD_AUTHOR_NAME)?.takeIf(String::isNotBlank)
                ?: "Membro da comunidade",
            timeText = if (createdAt == 0L) "agora" else DateUtils.getRelativeTimeSpanString(
                createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString(),
            text = text,
            parentCommentId = getString(FIELD_PARENT_COMMENT_ID)
        )
    }

    private companion object {
        const val POSTS_COLLECTION = "community_posts"
        const val COMMENTS_COLLECTION = "comments"
        const val LIKES_COLLECTION = "likes"
        const val FIELD_USER_ID = "userId"
        const val FIELD_AUTHOR_ID = "authorId"
        const val FIELD_AUTHOR_NAME = "authorName"
        const val FIELD_PARENT_COMMENT_ID = "parentCommentId"
        const val FIELD_TEXT = "text"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_LIKES_COUNT = "likesCount"
        const val FIELD_COMMENTS_COUNT = "commentsCount"
        const val MAX_COMMENTS = 100L
        const val MAX_COMMENT_LENGTH = 500
    }
}
