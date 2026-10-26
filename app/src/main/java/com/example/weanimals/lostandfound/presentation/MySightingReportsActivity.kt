package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityMySightingReportsBinding
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MySightingReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMySightingReportsBinding
    private var tutorExpanded = true
    private var finderExpanded = false

    private val currentUserId by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMySightingReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.headerTitleTv.text = "Meus relatos de encontro"
        binding.includeHeader.headerBackBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadChatsHub()
    }

    private fun loadChatsHub() {
        val container = binding.containerMySightingReports
        container.removeAllViews()

        val allAds = LostAndFoundRepository.getAllAds(this)
        val allRequests = mutableListOf<TutorVerificationRequest>()
        for (ad in allAds) {
            allRequests.addAll(LostAndFoundRepository.getVerificationRequestsForAd(this, ad.id))
        }

        val approvedRequests = allRequests
            .filter { it.status.equals("APROVADO", ignoreCase = true) }
            .filter { it.ownerUserId == currentUserId || it.requesterUserId == currentUserId }
            .sortedByDescending { it.timestamp }

        val tutorChats = mutableListOf<TutorVerificationRequest>()
        val finderChats = mutableListOf<TutorVerificationRequest>()

        for (req in approvedRequests) {
            val ad = LostAndFoundRepository.getAdById(this, req.adId) ?: continue
            val isUserOwner = (ad.ownerUserId == currentUserId)

            val isTutor = if (ad.category.equals("PERDIDO", ignoreCase = true)) {
                isUserOwner
            } else {
                !isUserOwner
            }

            if (isTutor) {
                tutorChats.add(req)
            } else {
                finderChats.add(req)
            }
        }

        if (tutorChats.isEmpty() && finderChats.isEmpty()) {
            val emptyLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(32, 64, 32, 64)
            }

            val tv1 = TextView(this).apply {
                text = "Você ainda não possui chats liberados."
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
                gravity = Gravity.CENTER
            }

            val tv2 = TextView(this).apply {
                text = "Assim que um relato ou verificação for aprovada, a conversa aparecerá aqui."
                textSize = 13f
                setTextColor(getColor(R.color.muted))
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 20)
            }

            val btnExplore = androidx.appcompat.widget.AppCompatButton(this).apply {
                text = "Explorar relatos"
                isAllCaps = false
                setTextColor(getColor(R.color.white))
                setBackgroundResource(R.drawable.bg_btn_terracotta)
                setOnClickListener { finish() }
            }

            emptyLayout.addView(tv1)
            emptyLayout.addView(tv2)
            emptyLayout.addView(btnExplore)
            container.addView(emptyLayout)
            return
        }

        // --- Section 1: Chats liberados como tutor ---
        val tutorActive = tutorChats.filter { req ->
            val ad = LostAndFoundRepository.getAdById(this, req.adId)
            ad != null && !ad.isEnded && !ad.isReunited && ad.getRemainingDays() > 0
        }
        val tutorEnded = tutorChats.filter { req ->
            val ad = LostAndFoundRepository.getAdById(this, req.adId)
            ad == null || ad.isEnded || ad.isReunited || ad.getRemainingDays() <= 0
        }

        val tutorHeader = createAccordionHeader("Chats liberados como tutor (${tutorChats.size})", tutorExpanded) { expanded ->
            tutorExpanded = expanded
            containerTutorList.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        container.addView(tutorHeader)

        containerTutorList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (tutorExpanded) View.VISIBLE else View.GONE
            setPadding(0, 8, 0, 12)
        }

        renderSubGroup(containerTutorList, "Em andamento", tutorActive, isTutorRole = true)
        renderSubGroup(containerTutorList, "Histórico de encerrados", tutorEnded, isTutorRole = true)
        container.addView(containerTutorList)

        // --- Section 2: Chats liberados como quem encontrou ---
        val finderActive = finderChats.filter { req ->
            val ad = LostAndFoundRepository.getAdById(this, req.adId)
            ad != null && !ad.isEnded && !ad.isReunited && ad.getRemainingDays() > 0
        }
        val finderEnded = finderChats.filter { req ->
            val ad = LostAndFoundRepository.getAdById(this, req.adId)
            ad == null || ad.isEnded || ad.isReunited || ad.getRemainingDays() <= 0
        }

        val finderHeader = createAccordionHeader("Chats liberados como quem encontrou (${finderChats.size})", finderExpanded) { expanded ->
            finderExpanded = expanded
            containerFinderList.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        container.addView(finderHeader)

        containerFinderList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (finderExpanded) View.VISIBLE else View.GONE
            setPadding(0, 8, 0, 12)
        }

        renderSubGroup(containerFinderList, "Em andamento", finderActive, isTutorRole = false)
        renderSubGroup(containerFinderList, "Histórico de encerrados", finderEnded, isTutorRole = false)
        container.addView(containerFinderList)
    }

    private lateinit var containerTutorList: LinearLayout
    private lateinit var containerFinderList: LinearLayout

    private fun renderSubGroup(
        parentContainer: LinearLayout,
        subTitle: String,
        requests: List<TutorVerificationRequest>,
        isTutorRole: Boolean
    ) {
        if (requests.isEmpty()) return

        val subHeaderTv = TextView(this).apply {
            text = "$subTitle (${requests.size})"
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
            letterSpacing = 0.08f
            setPadding(8, 12, 8, 6)
        }
        parentContainer.addView(subHeaderTv)

        for (req in requests) {
            val isEnded = subTitle.contains("encerrados", ignoreCase = true)
            parentContainer.addView(createChatCard(req, isTutorRole = isTutorRole, isEnded = isEnded))
        }
    }

    private fun createAccordionHeader(title: String, isExpanded: Boolean, onToggle: (Boolean) -> Unit): View {
        var expanded = isExpanded
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(28, 24, 28, 24)
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_card_white_border)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 12, 0, 0)
            layoutParams = params
        }

        val titleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
        }

        val chevron = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(36, 36)
            setImageResource(R.drawable.ic_chevron_right)
            rotation = if (expanded) 270f else 90f
            setColorFilter(getColor(R.color.pine800))
        }

        layout.addView(titleTv)
        layout.addView(chevron)

        layout.setOnClickListener {
            expanded = !expanded
            onToggle(expanded)
            chevron.animate().rotation(if (expanded) 270f else 90f).setDuration(150).start()
        }

        return layout
    }

    private fun createChatCard(req: TutorVerificationRequest, isTutorRole: Boolean, isEnded: Boolean = false): View {
        val ad = LostAndFoundRepository.getAdById(this, req.adId)
        val buttonHeightPx = (48 * resources.displayMetrics.density).toInt()
        val margin12Px = (12 * resources.displayMetrics.density).toInt()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_white_border)
            setPadding(28, 28, 28, 28)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            if (ad != null && ad.photoUris.isNotEmpty()) {
                try {
                    setImageURI(Uri.parse(ad.photoUris.first()))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (_: Exception) {
                    setImageResource(R.drawable.ic_paw_print)
                    setColorFilter(getColor(R.color.pine800))
                }
            } else {
                setImageResource(R.drawable.ic_paw_print)
                setColorFilter(getColor(R.color.pine800))
            }
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 16
            }
        }

        val titleTv = TextView(this).apply {
            text = ad?.getDisplayTitle() ?: "Animal"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
        }

        val otherPersonName = if (isTutorRole) {
            if (currentUserId == ad?.ownerUserId) req.requesterName else ad?.publisherName
        } else {
            if (currentUserId == ad?.ownerUserId) req.requesterName else ad?.publisherName
        } ?: "Participante"

        val convTv = TextView(this).apply {
            text = "Conversando com: $otherPersonName"
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 2, 0, 0)
        }

        val roleTv = TextView(this).apply {
            text = "Meu papel: ${if (isTutorRole) "Tutor" else "Quem encontrou"}"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.terracotta))
            setPadding(0, 2, 0, 0)
        }

        infoLayout.addView(titleTv)
        infoLayout.addView(convTv)
        infoLayout.addView(roleTv)

        topRow.addView(icon)
        topRow.addView(infoLayout)
        card.addView(topRow)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 16, 0, 16)
            }
            setBackgroundColor(getColor(R.color.border))
        }
        card.addView(divider)

        // Status Badge for Ended Cards
        if (isEnded && ad != null) {
            val statusBadge = TextView(this).apply {
                text = if (ad.isReunited) "Reencontrado" else if (ad.isEnded) "Anúncio encerrado" else "Anúncio expirado"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(if (ad.isReunited) R.color.pine800 else R.color.muted))
                setPadding(0, 0, 0, 8)
            }
            card.addView(statusBadge)
        }

        val buttonFrameLayout = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, margin12Px, 0, 0)
            }
            clipChildren = false
            clipToPadding = false
        }

        val actionButton = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            )
            isAllCaps = false
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            text = "Abrir chat"
            setTextColor(getColor(R.color.white))
            setBackgroundResource(R.drawable.bg_btn_dark_green)

            setOnClickListener {
                val intent = Intent(this@MySightingReportsActivity, LostAndFoundChatActivity::class.java).apply {
                    putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, req.id)
                    putExtra(
                        LostAndFoundChatActivity.EXTRA_VIEWER_ROLE,
                        if (isTutorRole) LostAndFoundChatActivity.ROLE_TUTOR else LostAndFoundChatActivity.ROLE_FOUNDER
                    )
                }
                startActivity(intent)
            }
        }

        buttonFrameLayout.addView(actionButton)

        // Query unread messages
        val unreadCount = LostAndFoundRepository.getUnreadCountForRequest(
            this,
            req.id,
            currentUserId,
            isTutorRole
        )

        if (unreadCount > 0) {
            val badgeTv = TextView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = (-6 * resources.displayMetrics.density).toInt()
                    marginEnd = (-6 * resources.displayMetrics.density).toInt()
                }
                text = if (unreadCount > 99) "99+" else unreadCount.toString()
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.white))
                setBackgroundResource(R.drawable.bg_badge_unread)
                gravity = Gravity.CENTER
                minWidth = (22 * resources.displayMetrics.density).toInt()
                minHeight = (22 * resources.displayMetrics.density).toInt()
                setPadding(
                    (6 * resources.displayMetrics.density).toInt(),
                    (2 * resources.displayMetrics.density).toInt(),
                    (6 * resources.displayMetrics.density).toInt(),
                    (2 * resources.displayMetrics.density).toInt()
                )
                contentDescription = "Abrir chat, $unreadCount mensagens não lidas"
            }
            buttonFrameLayout.addView(badgeTv)
        }

        card.addView(buttonFrameLayout)

        return card
    }
}
