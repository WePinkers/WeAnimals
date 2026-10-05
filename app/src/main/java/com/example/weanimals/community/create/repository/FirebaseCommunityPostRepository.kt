package com.example.weanimals.community.create.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.weanimals.R
import com.example.weanimals.community.create.domain.CommunityPostDraft
import com.example.weanimals.community.create.domain.publicLocationLabel
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class FirebaseCommunityPostRepository(
    context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityPostRepository {
    private val applicationContext = context.applicationContext

    override suspend fun publish(draft: CommunityPostDraft): Result<String> = runCatching {
        require(draft.category in POST_CATEGORIES) { "Invalid post category." }
        val body = draft.body.trim()
        require(body.isNotEmpty() && body.length <= 1000) { "Invalid post body." }
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Could not sign in to publish.")
        val photoBytes = draft.photoUri?.let { uri ->
            withContext(Dispatchers.IO) { compressPhoto(Uri.parse(uri)) }
        }
        val document = firestore.collection(COLLECTION).document()
        val data = mutableMapOf<String, Any>(
            "authorId" to user.uid,
            "authorName" to (user.displayName?.trim().takeIf { !it.isNullOrEmpty() }
                ?: applicationContext.getString(R.string.community_post_author_default)),
            "category" to draft.category.name,
            "body" to body,
            "locationLabel" to (draft.location.publicLocationLabel()
                ?: applicationContext.getString(R.string.community_location_region_unknown)),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (photoBytes != null) data["photoData"] = Blob.fromBytes(photoBytes)
        document.set(data).await()
        document.id
    }

    private fun compressPhoto(uri: Uri): ByteArray {
        val resolver = applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image." }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_DIMENSION
            || bounds.outHeight / sampleSize > MAX_DIMENSION) sampleSize *= 2

        while (sampleSize <= MAX_SAMPLE_SIZE) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = resolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Could not read image.")
            val bytes = try {
                ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 72, output))
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
            if (bytes.size <= MAX_PHOTO_BYTES) return bytes
            sampleSize *= 2
        }
        error("Image is too large.")
    }

    private companion object {
        const val COLLECTION = "community_posts"
        const val MAX_DIMENSION = 1200
        const val MAX_SAMPLE_SIZE = 64
        const val MAX_PHOTO_BYTES = 200_000
        val POST_CATEGORIES = setOf(
            CommunityCategory.FOUND,
            CommunityCategory.QUESTION,
            CommunityCategory.NOTICE,
            CommunityCategory.OTHER
        )
    }
}
