package com.example.weanimals.user.lostandfound.presentation

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.example.weanimals.user.entry.presentation.LoginActivity
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth

/** Resolves existing workflows; never creates requests or conversations. */
object AdActionNavigator {
    enum class Action { LOGIN, OWN_AD, INACTIVE, SIGHTING, VERIFICATION, STATUS, CHAT }

    fun decide(ad: FoundPetAd, userId: String?, request: TutorVerificationRequest?,
        allowOwnAdForTesting: Boolean = false): Action {
        if (userId.isNullOrBlank()) return Action.LOGIN
        if (ad.ownerUserId == userId && !allowOwnAdForTesting) return Action.OWN_AD
        val ownRequest = request?.takeIf { it.adId == ad.id && it.requesterUserId == userId }
        if (ownRequest != null) return if (ownRequest.status.equals("APROVADO", true)) Action.CHAT else Action.STATUS
        if (ad.isEnded || ad.isReunited || ad.getRemainingDays() <= 0) return Action.INACTIVE
        return if (ad.category.equals("PERDIDO", true)) Action.SIGHTING else Action.VERIFICATION
    }

    private fun viewerId(activity: Activity): String? =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: if (LostAndFoundRepository.canTestOwnAnnouncement(activity)) "local_user" else null

    fun action(activity: Activity, ad: FoundPetAd): Action = decide(ad,
        viewerId(activity), request(activity, ad.id),
        allowOwnAdForTesting = LostAndFoundRepository.canTestOwnAnnouncement(activity))

    private fun request(activity: Activity, adId: String): TutorVerificationRequest? {
        val uid = viewerId(activity) ?: return null
        return LostAndFoundRepository.getVerificationRequestsForAd(activity, adId)
            .firstOrNull { it.requesterUserId == uid }
    }

    fun open(activity: Activity, adId: String) {
        val ad = LostAndFoundRepository.getAdById(activity, adId) ?: return
        val destination = when (action(activity, ad)) {
            Action.LOGIN -> Intent(activity, LoginActivity::class.java)
            Action.OWN_AD -> {
                Toast.makeText(activity, "Você não pode solicitar seu próprio anúncio.", Toast.LENGTH_SHORT).show()
                return
            }
            Action.INACTIVE -> {
                Toast.makeText(activity, "Este anúncio não está ativo.", Toast.LENGTH_SHORT).show()
                return
            }
            Action.SIGHTING -> Intent(activity, SightingFormActivity::class.java)
                .putExtra(SightingFormActivity.EXTRA_AD_ID, ad.id)
            Action.VERIFICATION -> Intent(activity, TutorVerificationActivity::class.java)
                .putExtra(TutorVerificationActivity.EXTRA_AD_ID, ad.id)
            Action.STATUS -> Intent(activity, SubmittedAnswersActivity::class.java)
                .putExtra(SubmittedAnswersActivity.EXTRA_REQUEST_ID, request(activity, ad.id)?.id)
            Action.CHAT -> Intent(activity, LostAndFoundChatActivity::class.java)
                .putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, request(activity, ad.id)?.id)
                .putExtra(LostAndFoundChatActivity.EXTRA_VIEWER_ROLE,
                    if (ad.category.equals("PERDIDO", true)) LostAndFoundChatActivity.ROLE_FOUNDER
                    else LostAndFoundChatActivity.ROLE_TUTOR)
        }
        activity.startActivity(destination)
    }

    /** Recheck on entry and submission, including a changed user or a concurrent request. */
    fun redirectIfNeeded(activity: Activity, adId: String, expected: Action): Boolean {
        val ad = LostAndFoundRepository.getAdById(activity, adId) ?: return true
        if (action(activity, ad) == expected) return false
        open(activity, adId)
        return true
    }
}
