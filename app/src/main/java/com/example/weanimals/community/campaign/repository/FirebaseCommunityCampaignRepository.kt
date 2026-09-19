package com.example.weanimals.community.campaign.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.weanimals.R
import com.example.weanimals.community.campaign.domain.CommunityCampaignAnimal
import com.example.weanimals.community.campaign.domain.CommunityCampaignAnimalDraft
import com.example.weanimals.community.campaign.domain.CommunityCampaignDonationResult
import com.example.weanimals.community.campaign.domain.CommunityCampaignDraft
import com.example.weanimals.community.campaign.domain.CommunityCampaignParticipationResult
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

class FirebaseCommunityCampaignRepository(
    context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityCampaignRepository {
    private val applicationContext = context.applicationContext

    override suspend fun publish(draft: CommunityCampaignDraft): Result<String> = runCatching {
        require(draft.category in CAMPAIGN_CATEGORIES) { "Invalid campaign category." }
        val title = draft.title.trim()
        require(title.isNotEmpty() && title.length <= 120) { "Invalid campaign title." }
        val dateText = draft.dateText.trim()
        require(dateText.isNotEmpty() && dateText.length <= 120) { "Invalid campaign date." }
        val locationText = listOf(draft.location.address, draft.location.secondaryAddress)
            .filter(String::isNotBlank)
            .joinToString(" — ")
        require(locationText.isNotBlank() && locationText.length <= 160) {
            "Invalid campaign location."
        }
        val availability = draft.availabilityText.trim()
        require(availability.length <= 120) { "Invalid campaign availability." }
        val audience = draft.audienceText.trim()
        require(audience.length <= 160) { "Invalid campaign audience." }
        val description = draft.description.trim()
        require(description.isNotEmpty() && description.length <= 1200) {
            "Invalid campaign description."
        }
        val highlight = draft.highlightText.trim()
        require(highlight.length <= 240) { "Invalid campaign highlight." }
        val totalSlots = draft.totalSlots
        require(totalSlots == null || totalSlots in 1..100000) { "Invalid campaign capacity." }
        val donationGoalCents = draft.donationGoalCents
        require(donationGoalCents == null || donationGoalCents in 1..100_000_000_00L) {
            "Invalid campaign donation goal."
        }
        val pixKey = draft.pixKey.trim()
        require(pixKey.length <= 200) { "Invalid campaign Pix key." }
        val animals = draft.confirmedAnimals.map(::validateAnimal)
        require(draft.category == CommunityCategory.ADOPTION || animals.isEmpty()) {
            "Confirmed animals are only available for adoption campaigns."
        }

        val user = auth.currentUser ?: error("Could not find the organization account.")
        val claims = user.getIdToken(false).await().claims
        val organizationId = claims["ngoId"] as? String
            ?: error("The account is not linked to an organization.")
        val organizationName = sequenceOf(
            claims["ngoName"] as? String,
            claims["organizationName"] as? String,
            user.displayName
        ).mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
            ?: applicationContext.getString(R.string.community_organization_default)
        val locationLabel = draft.location.publicLocationLabel()
            ?: applicationContext.getString(R.string.community_location_region_unknown)
        val document = firestore.collection(COLLECTION).document()
        val campaignData = mutableMapOf<String, Any>(
            "organizationId" to organizationId,
            "organizationName" to organizationName,
            "createdBy" to user.uid,
            "category" to draft.category.name,
            "title" to title,
            "dateText" to dateText,
            "locationText" to locationText,
            "locationLabel" to locationLabel,
            "availabilityText" to availability,
            "audienceText" to audience,
            "description" to description,
            "highlightText" to highlight,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (totalSlots != null) {
            campaignData["totalSlots"] = totalSlots
            campaignData["filledSlots"] = 0L
        }
        if (donationGoalCents != null) {
            campaignData["donationGoalCents"] = donationGoalCents
            campaignData["donationRaisedCents"] = 0L
        }
        if (draft.category == CommunityCategory.DONATION) {
            campaignData["donationNeeds"] = draft.donationNeeds
                .map(String::trim)
                .filter(String::isNotBlank)
                .take(20)
            campaignData["pixKey"] = pixKey
        }
        campaignData["locationLatitude"] = draft.location.latitude
        campaignData["locationLongitude"] = draft.location.longitude
        val batch = firestore.batch()
        batch.set(document, campaignData)
        animals.forEach { animal ->
            val animalDocument = document.collection(ANIMALS_COLLECTION).document()
            val animalData = mutableMapOf<String, Any>(
                "name" to animal.name,
                "breed" to animal.breed,
                "ageText" to animal.ageText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            animal.photoUri?.let { uri ->
                val photoBytes = withContext(Dispatchers.IO) { compressPhoto(Uri.parse(uri)) }
                animalData["photoData"] = Blob.fromBytes(photoBytes)
            }
            batch.set(animalDocument, animalData)
        }
        batch.commit().await()
        document.id
    }

    override suspend fun getConfirmedAnimals(
        campaignId: String
    ): Result<List<CommunityCampaignAnimal>> = runCatching {
        require(campaignId.isNotBlank()) { "Invalid campaign id." }
        firestore.collection(COLLECTION)
            .document(campaignId)
            .collection(ANIMALS_COLLECTION)
            .orderBy("createdAt")
            .get()
            .await()
            .documents
            .map { document ->
                CommunityCampaignAnimal(
                    id = document.id,
                    name = document.getString("name").orEmpty(),
                    breed = document.getString("breed").orEmpty(),
                    ageText = document.getString("ageText").orEmpty(),
                    photoData = (document.get("photoData") as? Blob)?.toBytes()
                )
            }
    }

    override suspend fun participate(
        campaignId: String
    ): Result<CommunityCampaignParticipationResult> = runCatching {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Could not sign in to participate.")
        val campaignReference = firestore.collection(COLLECTION).document(campaignId)
        val participantReference = campaignReference
            .collection(PARTICIPANTS_COLLECTION)
            .document(user.uid)
        firestore.runTransaction { transaction ->
            val campaign = transaction.get(campaignReference)
            val participant = transaction.get(participantReference)
            val totalSlots = campaign.getLong("totalSlots")?.toInt()
            val filledSlots = campaign.getLong("filledSlots")?.toInt() ?: 0
            val category = campaign.getString("category")
            require(category in PARTICIPATION_CATEGORIES.map(CommunityCategory::name)) {
                "This campaign does not accept participation this way."
            }
            if (participant.exists()) {
                CommunityCampaignParticipationResult(filledSlots, totalSlots, true)
            } else {
                transaction.set(
                    participantReference,
                    mapOf(
                        "userId" to user.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (category == CommunityCategory.NEUTERING.name) {
                    require(totalSlots == null || filledSlots < totalSlots) { "Campaign is full." }
                    transaction.update(campaignReference, "filledSlots", filledSlots + 1)
                    CommunityCampaignParticipationResult(filledSlots + 1, totalSlots, false)
                } else {
                    CommunityCampaignParticipationResult(filledSlots, totalSlots, false)
                }
            }
        }.await()
    }

    override suspend fun isParticipating(campaignId: String): Result<Boolean> = runCatching {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Could not sign in to check participation.")
        firestore.collection(COLLECTION)
            .document(campaignId)
            .collection(PARTICIPANTS_COLLECTION)
            .document(user.uid)
            .get()
            .await()
            .exists()
    }

    override suspend fun donate(
        campaignId: String,
        amountCents: Long
    ): Result<CommunityCampaignDonationResult> = runCatching {
        require(amountCents in 100..100_000_000_00L) { "Invalid donation amount." }
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Could not sign in to donate.")
        val campaignReference = firestore.collection(COLLECTION).document(campaignId)
        val donationReference = campaignReference.collection(DONATIONS_COLLECTION).document()
        firestore.runTransaction { transaction ->
            val campaign = transaction.get(campaignReference)
            require(campaign.getString("category") == CommunityCategory.DONATION.name) {
                "This campaign does not accept donations this way."
            }
            val goalCents = campaign.getLong("donationGoalCents")
            val raisedCents = campaign.getLong("donationRaisedCents") ?: 0L
            require(goalCents == null || raisedCents + amountCents <= goalCents) {
                "Donation exceeds campaign goal."
            }
            transaction.set(
                donationReference,
                mapOf(
                    "userId" to user.uid,
                    "amountCents" to amountCents,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.update(
                campaignReference,
                "donationRaisedCents",
                raisedCents + amountCents
            )
            CommunityCampaignDonationResult(raisedCents + amountCents, goalCents)
        }.await()
    }

    override suspend fun notifyItemDonation(
        campaignId: String,
        selectedItems: List<String>,
        otherItem: String
    ): Result<Unit> = runCatching {
        val cleanItems = selectedItems.map(String::trim).filter(String::isNotBlank).distinct()
        val cleanOtherItem = otherItem.trim()
        require(cleanItems.isNotEmpty() || cleanOtherItem.isNotBlank()) {
            "Select at least one item."
        }

        val user = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Could not sign in to notify the organization.")
        val campaignReference = firestore.collection(COLLECTION).document(campaignId)
        val notificationReference = campaignReference
            .collection(ITEM_DONATIONS_COLLECTION)
            .document()
        notificationReference.set(
            mapOf(
                "userId" to user.uid,
                "items" to cleanItems,
                "otherItem" to cleanOtherItem,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun validateAnimal(animal: CommunityCampaignAnimalDraft): CommunityCampaignAnimalDraft {
        val name = animal.name.trim()
        val breed = animal.breed.trim()
        val ageText = animal.ageText.trim()
        require(name.isNotEmpty() && name.length <= 80) { "Invalid confirmed animal name." }
        require(breed.length <= 80) { "Invalid confirmed animal breed." }
        require(ageText.length <= 80) { "Invalid confirmed animal age." }
        return animal.copy(name = name, breed = breed, ageText = ageText)
    }

    private fun compressPhoto(uri: Uri): ByteArray {
        val resolver = applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid animal image." }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_DIMENSION
            || bounds.outHeight / sampleSize > MAX_DIMENSION
        ) sampleSize *= 2

        while (sampleSize <= MAX_SAMPLE_SIZE) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = resolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Could not read animal image.")
            val bytes = try {
                ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, output))
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
            if (bytes.size <= MAX_PHOTO_BYTES) return bytes
            sampleSize *= 2
        }
        error("Animal image is too large.")
    }

    private companion object {
        const val COLLECTION = "community_campaigns"
        const val ANIMALS_COLLECTION = "animals"
        const val PARTICIPANTS_COLLECTION = "participants"
        const val DONATIONS_COLLECTION = "donations"
        const val ITEM_DONATIONS_COLLECTION = "item_donations"
        const val MAX_DIMENSION = 1200
        const val MAX_SAMPLE_SIZE = 64
        const val MAX_PHOTO_BYTES = 200_000
        const val PHOTO_QUALITY = 72
        val CAMPAIGN_CATEGORIES = setOf(
            CommunityCategory.NEUTERING,
            CommunityCategory.VACCINATION,
            CommunityCategory.ADOPTION,
            CommunityCategory.DONATION
        )
        val PARTICIPATION_CATEGORIES = setOf(
            CommunityCategory.NEUTERING,
            CommunityCategory.VACCINATION,
            CommunityCategory.ADOPTION
        )
    }
}
