package com.example.weanimals.user.lostandfound.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.weanimals.user.lostandfound.model.AnnouncementReport
import com.example.weanimals.user.lostandfound.model.ChatReport
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.model.LostAndFoundChatMessage
import com.example.weanimals.user.lostandfound.model.ReunionRequest
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import org.json.JSONArray
import org.json.JSONObject

data class IndicatorsSummary(
    val lostCount: Int,
    val foundCount: Int,
    val reunitedCount: Int
)

object LostAndFoundRepository {

    private const val PREFS_NAME = "lost_and_found_prefs"
    private const val KEY_ADS = "saved_ads"
    private const val KEY_REQUESTS = "saved_verification_requests"
    private const val KEY_MESSAGES = "saved_lost_found_messages"
    private const val KEY_REUNIONS = "saved_reunion_requests"
    private const val KEY_REPORTS = "saved_chat_reports"
    private const val KEY_ANNOUNCEMENT_REPORTS = "saved_announcement_reports"

    private val adsList = mutableListOf<FoundPetAd>()
    private val requestsList = mutableListOf<TutorVerificationRequest>()
    private val messagesList = mutableListOf<LostAndFoundChatMessage>()
    private val reunionsList = mutableListOf<ReunionRequest>()
    private val reportsList = mutableListOf<ChatReport>()
    private val announcementReportsList = mutableListOf<AnnouncementReport>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadFromPrefs(context)
        loadRequestsFromPrefs(context)
        loadMessagesFromPrefs(context)
        loadReunionsFromPrefs(context)
        loadReportsFromPrefs(context)
        loadAnnouncementReportsFromPrefs(context)
    }

    fun saveAd(context: Context, ad: FoundPetAd) {
        init(context)
        adsList.add(0, ad)
        saveToPrefs(context)
    }

    fun getAllAds(context: Context): List<FoundPetAd> {
        init(context)
        return adsList.toList()
    }

    fun getAdById(context: Context, id: String): FoundPetAd? {
        init(context)
        return adsList.firstOrNull { it.id == id }
    }

    fun updateAdSituation(context: Context, id: String, newSituation: String) {
        init(context)
        val ad = adsList.firstOrNull { it.id == id } ?: return
        ad.currentSituation = newSituation

        if (newSituation.equals("Reencontrado", ignoreCase = true) ||
            newSituation.equals("Entregue ao tutor", ignoreCase = true)
        ) {
            ad.isReunited = true
            if (ad.reunitedTimestamp == null) {
                ad.reunitedTimestamp = System.currentTimeMillis()
            }
            ad.isEnded = true
        }

        saveToPrefs(context)
    }

    fun endAd(context: Context, id: String) {
        init(context)
        val ad = adsList.firstOrNull { it.id == id } ?: return
        ad.isEnded = true
        ad.currentSituation = "Encerrado"
        saveToPrefs(context)
    }

    fun saveVerificationRequest(context: Context, request: TutorVerificationRequest): Boolean {
        init(context)
        val alreadySubmitted = requestsList.any { it.adId == request.adId && it.requesterUserId == request.requesterUserId }
        if (alreadySubmitted) return false

        requestsList.add(0, request)
        saveRequestsToPrefs(context)

        val ad = adsList.firstOrNull { it.id == request.adId }
        if (ad != null) {
            val uniqueCount = requestsList.filter { it.adId == request.adId }.map { it.requesterUserId }.distinct().size
            val updatedAd = ad.copy(possibleTutorsCount = uniqueCount)
            val index = adsList.indexOfFirst { it.id == ad.id }
            if (index >= 0) {
                adsList[index] = updatedAd
                saveToPrefs(context)
            }
        }
        return true
    }

    fun getVerificationRequestsForAd(context: Context, adId: String): List<TutorVerificationRequest> {
        init(context)
        return requestsList.filter { it.adId == adId }
    }

    fun getVerificationRequestById(context: Context, id: String): TutorVerificationRequest? {
        init(context)
        return requestsList.firstOrNull { it.id == id }
    }

    fun updateRequestStatus(context: Context, requestId: String, status: String) {
        init(context)
        val req = requestsList.firstOrNull { it.id == requestId } ?: return
        req.status = status
        saveRequestsToPrefs(context)
    }

    fun saveChatMessage(context: Context, message: LostAndFoundChatMessage) {
        init(context)
        messagesList.add(message)
        saveMessagesToPrefs(context)
    }

    fun getMessagesForRequest(context: Context, requestId: String): List<LostAndFoundChatMessage> {
        init(context)
        return messagesList.filter { it.requestId == requestId }.sortedBy { it.timestamp }
    }

    fun getUnreadCountForRequest(
        context: Context,
        requestId: String,
        currentUserId: String,
        isTutorRole: Boolean
    ): Int {
        init(context)
        val req = requestsList.firstOrNull { it.id == requestId } ?: return 0
        val isSimulationMode = (req.ownerUserId == req.requesterUserId) || (currentUserId == "local_user")
        val activeUserId = if (isSimulationMode) {
            if (isTutorRole) "SIM_TUTOR" else "SIM_FOUNDER"
        } else {
            currentUserId
        }

        val messages = messagesList.filter { it.requestId == requestId }
        val readSet = context.getSharedPreferences("read_messages_prefs", Context.MODE_PRIVATE)
            .getStringSet("read_msgs_${requestId}_$activeUserId", emptySet()) ?: emptySet()

        return messages.count { msg ->
            val isOtherSender = if (isSimulationMode) {
                if (isTutorRole) {
                    msg.senderUserId == "SIM_FOUNDER" || msg.senderUserId == "local_founder" || msg.senderUserId == "local_user"
                } else {
                    msg.senderUserId == "SIM_TUTOR" || msg.senderUserId == "local_tutor"
                }
            } else {
                msg.senderUserId != currentUserId
            }
            isOtherSender && !readSet.contains(msg.id)
        }
    }

    fun markMessagesAsRead(
        context: Context,
        requestId: String,
        currentUserId: String,
        isTutorRole: Boolean
    ) {
        init(context)
        val req = requestsList.firstOrNull { it.id == requestId } ?: return
        val isSimulationMode = (req.ownerUserId == req.requesterUserId) || (currentUserId == "local_user")
        val activeUserId = if (isSimulationMode) {
            if (isTutorRole) "SIM_TUTOR" else "SIM_FOUNDER"
        } else {
            currentUserId
        }

        val messages = messagesList.filter { it.requestId == requestId }
        val prefs = context.getSharedPreferences("read_messages_prefs", Context.MODE_PRIVATE)
        val key = "read_msgs_${requestId}_$activeUserId"
        val currentReadSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

        for (msg in messages) {
            currentReadSet.add(msg.id)
        }

        prefs.edit().putStringSet(key, currentReadSet).apply()
    }

    fun saveReunionRequest(context: Context, reunion: ReunionRequest) {
        init(context)
        reunionsList.removeAll { it.requestId == reunion.requestId }
        reunionsList.add(0, reunion)
        saveReunionsToPrefs(context)
    }

    fun getReunionForRequest(context: Context, requestId: String): ReunionRequest? {
        init(context)
        return reunionsList.firstOrNull { it.requestId == requestId }
    }

    fun updateReunionStatus(context: Context, reunionId: String, status: String) {
        init(context)
        val reunion = reunionsList.firstOrNull { it.reunionId == reunionId } ?: return
        reunion.status = status
        val now = System.currentTimeMillis()
        reunion.respondedAt = now
        if (status.equals("CONFIRMED", ignoreCase = true)) {
            reunion.confirmedAt = now
            val ad = adsList.firstOrNull { it.id == reunion.announcementId }
            if (ad != null) {
                updateAdSituation(context, ad.id, "Reencontrado")
            }
        }
        saveReunionsToPrefs(context)
    }

    fun saveChatReport(context: Context, report: ChatReport) {
        init(context)
        reportsList.add(0, report)
        saveReportsToPrefs(context)
    }

    fun saveAnnouncementReport(context: Context, report: AnnouncementReport) {
        init(context)
        announcementReportsList.add(0, report)
        saveAnnouncementReportsToPrefs(context)
    }

    fun getIndicatorsSummary(context: Context): IndicatorsSummary {
        init(context)
        val now = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        val windowStart = now - thirtyDaysMillis

        var lostCount = 0
        var foundCount = 0
        var reunitedCount = 0

        for (ad in adsList) {
            if (ad.isReunited) {
                val reunitedTime = ad.reunitedTimestamp ?: ad.publicationTimestamp
                if (reunitedTime >= windowStart) {
                    reunitedCount++
                }
            } else if (!ad.isEnded && ad.getRemainingDays() > 0 && ad.publicationTimestamp >= windowStart) {
                if (ad.category.equals("PERDIDO", ignoreCase = true)) {
                    lostCount++
                } else {
                    foundCount++
                }
            }
        }

        return IndicatorsSummary(lostCount, foundCount, reunitedCount)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveToPrefs(context: Context) {
        val array = JSONArray()
        for (ad in adsList) {
            val obj = JSONObject().apply {
                put("id", ad.id)
                put("ownerUserId", ad.ownerUserId)
                put("publisherName", ad.publisherName ?: "")
                put("category", ad.category)
                put("species", ad.species)
                put("otherType", ad.otherType ?: "")
                put("size", ad.size ?: "")
                put("ageApprox", ad.ageApprox ?: "")
                put("condition", ad.condition ?: "")
                put("photoUris", JSONArray(ad.photoUris))
                put("street", ad.street ?: "")
                put("number", ad.number ?: "")
                put("neighborhood", ad.neighborhood ?: "")
                put("city", ad.city ?: "")
                put("state", ad.state ?: "")
                put("complement", ad.complement ?: "")
                put("whenFound", ad.whenFound ?: "")
                put("whenDate", ad.whenDate ?: "")
                put("marks", ad.marks ?: "")
                put("calledNames", JSONArray(ad.calledNames))
                put("currentSituation", ad.currentSituation)
                put("publicationTimestamp", ad.publicationTimestamp)
                put("possibleTutorsCount", ad.possibleTutorsCount)
                put("isEnded", ad.isEnded)
                put("isReunited", ad.isReunited)
                put("reunitedTimestamp", ad.reunitedTimestamp ?: 0L)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_ADS, array.toString()).apply()
    }

    private fun loadFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_ADS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            adsList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val photoUrisArray = obj.optJSONArray("photoUris")
                val photoUrisList = mutableListOf<String>()
                if (photoUrisArray != null) {
                    for (j in 0 until photoUrisArray.length()) {
                        photoUrisList.add(photoUrisArray.getString(j))
                    }
                }

                val calledNamesArray = obj.optJSONArray("calledNames")
                val calledNamesList = mutableListOf<String>()
                if (calledNamesArray != null) {
                    for (j in 0 until calledNamesArray.length()) {
                        calledNamesList.add(calledNamesArray.getString(j))
                    }
                }

                val reunitedTs = obj.optLong("reunitedTimestamp", 0L)

                val ad = FoundPetAd(
                    id = obj.optString("id"),
                    ownerUserId = obj.optString("ownerUserId", "local_user"),
                    publisherName = obj.optString("publisherName").ifBlank { null },
                    category = obj.optString("category", "ENCONTRADO"),
                    species = obj.optString("species"),
                    otherType = obj.optString("otherType").ifBlank { null },
                    size = obj.optString("size").ifBlank { null },
                    ageApprox = obj.optString("ageApprox").ifBlank { null },
                    condition = obj.optString("condition").ifBlank { null },
                    photoUris = photoUrisList,
                    street = obj.optString("street").ifBlank { null },
                    number = obj.optString("number").ifBlank { null },
                    neighborhood = obj.optString("neighborhood").ifBlank { null },
                    city = obj.optString("city").ifBlank { null },
                    state = obj.optString("state").ifBlank { null },
                    complement = obj.optString("complement").ifBlank { null },
                    whenFound = obj.optString("whenFound").ifBlank { null },
                    whenDate = obj.optString("whenDate").ifBlank { null },
                    marks = obj.optString("marks").ifBlank { null },
                    calledNames = calledNamesList,
                    currentSituation = obj.optString("currentSituation", "Comigo"),
                    publicationTimestamp = obj.optLong("publicationTimestamp", System.currentTimeMillis()),
                    possibleTutorsCount = obj.optInt("possibleTutorsCount", 0),
                    isEnded = obj.optBoolean("isEnded", false),
                    isReunited = obj.optBoolean("isReunited", false),
                    reunitedTimestamp = if (reunitedTs > 0L) reunitedTs else null
                )
                adsList.add(ad)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveRequestsToPrefs(context: Context) {
        val array = JSONArray()
        for (req in requestsList) {
            val obj = JSONObject().apply {
                put("id", req.id)
                put("adId", req.adId)
                put("requesterUserId", req.requesterUserId)
                put("requesterName", req.requesterName ?: "")
                put("ownerUserId", req.ownerUserId)
                put("petName", req.petName)
                put("specialMark", req.specialMark)
                put("photoUri", req.photoUri ?: "")
                put("timestamp", req.timestamp)
                put("status", req.status)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_REQUESTS, array.toString()).apply()
    }

    private fun loadRequestsFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_REQUESTS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            requestsList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val req = TutorVerificationRequest(
                    id = obj.optString("id"),
                    adId = obj.optString("adId"),
                    requesterUserId = obj.optString("requesterUserId", "local_user"),
                    requesterName = obj.optString("requesterName").ifBlank { null },
                    ownerUserId = obj.optString("ownerUserId"),
                    petName = obj.optString("petName"),
                    specialMark = obj.optString("specialMark"),
                    photoUri = obj.optString("photoUri").ifBlank { null },
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    status = obj.optString("status", "PENDENTE")
                )
                requestsList.add(req)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMessagesToPrefs(context: Context) {
        val array = JSONArray()
        for (msg in messagesList) {
            val obj = JSONObject().apply {
                put("id", msg.id)
                put("requestId", msg.requestId)
                put("senderUserId", msg.senderUserId)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    private fun loadMessagesFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_MESSAGES, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            messagesList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val msg = LostAndFoundChatMessage(
                    id = obj.optString("id"),
                    requestId = obj.optString("requestId"),
                    senderUserId = obj.optString("senderUserId"),
                    text = obj.optString("text"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                messagesList.add(msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveReunionsToPrefs(context: Context) {
        val array = JSONArray()
        for (r in reunionsList) {
            val obj = JSONObject().apply {
                put("reunionId", r.reunionId)
                put("announcementId", r.announcementId)
                put("requestId", r.requestId)
                put("initiatedByUserId", r.initiatedByUserId)
                put("confirmingUserId", r.confirmingUserId)
                put("status", r.status)
                put("createdAt", r.createdAt)
                put("respondedAt", r.respondedAt ?: 0L)
                put("confirmedAt", r.confirmedAt ?: 0L)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_REUNIONS, array.toString()).apply()
    }

    private fun loadReunionsFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_REUNIONS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            reunionsList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val respAt = obj.optLong("respondedAt", 0L)
                val confAt = obj.optLong("confirmedAt", 0L)
                val r = ReunionRequest(
                    reunionId = obj.optString("reunionId"),
                    announcementId = obj.optString("announcementId"),
                    requestId = obj.optString("requestId"),
                    initiatedByUserId = obj.optString("initiatedByUserId"),
                    confirmingUserId = obj.optString("confirmingUserId"),
                    status = obj.optString("status", "PENDING_CONFIRMATION"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    respondedAt = if (respAt > 0L) respAt else null,
                    confirmedAt = if (confAt > 0L) confAt else null
                )
                reunionsList.add(r)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveReportsToPrefs(context: Context) {
        val array = JSONArray()
        for (rep in reportsList) {
            val obj = JSONObject().apply {
                put("reportId", rep.reportId)
                put("conversationId", rep.conversationId)
                put("announcementId", rep.announcementId)
                put("reporterUserId", rep.reporterUserId)
                put("reportedUserId", rep.reportedUserId)
                put("reason", rep.reason)
                put("description", rep.description ?: "")
                put("createdAt", rep.createdAt)
                put("status", rep.status)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_REPORTS, array.toString()).apply()
    }

    private fun loadReportsFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_REPORTS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            reportsList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rep = ChatReport(
                    reportId = obj.optString("reportId"),
                    conversationId = obj.optString("conversationId"),
                    announcementId = obj.optString("announcementId"),
                    reporterUserId = obj.optString("reporterUserId"),
                    reportedUserId = obj.optString("reportedUserId"),
                    reason = obj.optString("reason"),
                    description = obj.optString("description").ifBlank { null },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    status = obj.optString("status", "PENDING_REVIEW")
                )
                reportsList.add(rep)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAnnouncementReportsToPrefs(context: Context) {
        val array = JSONArray()
        for (rep in announcementReportsList) {
            val obj = JSONObject().apply {
                put("reportId", rep.reportId)
                put("reportType", rep.reportType)
                put("announcementId", rep.announcementId)
                put("announcementCategory", rep.announcementCategory)
                put("reporterUserId", rep.reporterUserId)
                put("reportedUserId", rep.reportedUserId)
                put("reason", rep.reason)
                put("description", rep.description ?: "")
                put("evidenceUri", rep.evidenceUri ?: "")
                put("createdAt", rep.createdAt)
                put("status", rep.status)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_ANNOUNCEMENT_REPORTS, array.toString()).apply()
    }

    private fun loadAnnouncementReportsFromPrefs(context: Context) {
        val jsonStr = getPrefs(context).getString(KEY_ANNOUNCEMENT_REPORTS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            announcementReportsList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rep = AnnouncementReport(
                    reportId = obj.optString("reportId"),
                    reportType = obj.optString("reportType", "ANNOUNCEMENT"),
                    announcementId = obj.optString("announcementId"),
                    announcementCategory = obj.optString("announcementCategory"),
                    reporterUserId = obj.optString("reporterUserId"),
                    reportedUserId = obj.optString("reportedUserId"),
                    reason = obj.optString("reason"),
                    description = obj.optString("description").ifBlank { null },
                    evidenceUri = obj.optString("evidenceUri").ifBlank { null },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    status = obj.optString("status", "PENDING_REVIEW")
                )
                announcementReportsList.add(rep)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
