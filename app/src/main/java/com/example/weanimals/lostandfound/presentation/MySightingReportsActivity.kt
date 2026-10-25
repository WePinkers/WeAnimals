package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.graphics.Color
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
    private var activeExpanded = true
    private var endedExpanded = false

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
        loadReports()
    }

    private fun loadReports() {
        val container = binding.containerMySightingReports
        container.removeAllViews()

        val allAds = LostAndFoundRepository.getAllAds(this)
        val allRequests = mutableListOf<TutorVerificationRequest>()
        for (ad in allAds) {
            allRequests.addAll(LostAndFoundRepository.getVerificationRequestsForAd(this, ad.id))
        }

        val myReports = allRequests
            .filter { it.requesterUserId == currentUserId }
            .sortedByDescending { it.timestamp }

        if (myReports.isEmpty()) {
            val emptyLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(32, 64, 32, 64)
            }

            val tv1 = TextView(this).apply {
                text = "Você ainda não enviou relatos de encontro."
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
                gravity = Gravity.CENTER
            }

            val tv2 = TextView(this).apply {
                text = "Viu um animal perdido? Relate o encontro para ajudar o tutor a encontrá-lo."
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

        val activeReports = myReports.filter { req ->
            val ad = LostAndFoundRepository.getAdById(this, req.adId)
            ad != null && !ad.isEnded && !ad.isReunited && ad.getRemainingDays() > 0
        }

        val endedReports = myReports.filter { req ->
            val ad = LostAndFoundRepository.getAdById(this, req.adId)
            ad == null || ad.isEnded || ad.isReunited || ad.getRemainingDays() <= 0
        }

        // Section 1: Active
        val activeHeader = createAccordionHeader("Relatos ativos (${activeReports.size})", activeExpanded) { expanded ->
            activeExpanded = expanded
            containerActiveList.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        container.addView(activeHeader)

        containerActiveList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (activeExpanded) View.VISIBLE else View.GONE
            setPadding(0, 8, 0, 12)
        }

        if (activeReports.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Você não possui relatos ativos."
                textSize = 13f
                setTextColor(getColor(R.color.muted))
                setPadding(16, 8, 16, 12)
            }
            containerActiveList.addView(emptyTv)
        } else {
            for (req in activeReports) {
                containerActiveList.addView(createReportCard(req, isEndedSection = false))
            }
        }
        container.addView(containerActiveList)

        // Section 2: Ended
        val endedHeader = createAccordionHeader("Relatos encerrados (${endedReports.size})", endedExpanded) { expanded ->
            endedExpanded = expanded
            containerEndedList.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        container.addView(endedHeader)

        containerEndedList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (endedExpanded) View.VISIBLE else View.GONE
            setPadding(0, 8, 0, 12)
        }

        if (endedReports.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Você ainda não possui relatos encerrados."
                textSize = 13f
                setTextColor(getColor(R.color.muted))
                setPadding(16, 8, 16, 12)
            }
            containerEndedList.addView(emptyTv)
        } else {
            for (req in endedReports) {
                containerEndedList.addView(createReportCard(req, isEndedSection = true))
            }
        }
        container.addView(containerEndedList)
    }

    private lateinit var containerActiveList: LinearLayout
    private lateinit var containerEndedList: LinearLayout

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

    private fun createReportCard(req: TutorVerificationRequest, isEndedSection: Boolean): View {
        val ad = LostAndFoundRepository.getAdById(this, req.adId)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
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
            params.setMargins(0, 12, 0, 12)
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
            text = ad?.getDisplayTitle() ?: "Animal perdido"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
        }

        val locTv = TextView(this).apply {
            text = ad?.getDisplayLocation() ?: "Localização registrada"
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 2, 0, 0)
        }

        val publisherName = ad?.publisherName?.takeIf { it.isNotBlank() } ?: "Usuário"
        val pubTv = TextView(this).apply {
            text = "Tutor responsável: $publisherName"
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 2, 0, 0)
        }

        val dateStr = dateFormat.format(Date(req.timestamp))
        val dateTv = TextView(this).apply {
            text = "Relato enviado em $dateStr"
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 2, 0, 0)
        }

        infoLayout.addView(titleTv)
        infoLayout.addView(locTv)
        infoLayout.addView(pubTv)
        infoLayout.addView(dateTv)

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

        val statusUpper = req.status.uppercase()
        val statusBadge = TextView(this).apply {
            text = if (isEndedSection && ad != null) {
                if (ad.isReunited) "Reencontrado" else if (ad.isEnded) "Anúncio encerrado" else "Anúncio expirado"
            } else {
                when (statusUpper) {
                    "APROVADO" -> "Aprovada"
                    "RECUSADO", "RECUSADA" -> "Recusado"
                    else -> "Em análise"
                }
            }
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            val badgeColor = when {
                isEndedSection -> R.color.muted
                statusUpper == "APROVADO" -> R.color.pine800
                statusUpper == "RECUSADO" || statusUpper == "RECUSADA" -> R.color.alert600
                else -> R.color.terracotta
            }
            setTextColor(getColor(badgeColor))
        }
        card.addView(statusBadge)

        val msgTv = TextView(this).apply {
            text = if (isEndedSection && ad != null && ad.isReunited) {
                "Reencontro confirmado! Este caso foi encerrado com sucesso."
            } else {
                when (statusUpper) {
                    "APROVADO" -> "Seu relato foi aceito! Você já pode conversar com o tutor do animal."
                    "RECUSADO", "RECUSADA" -> "Seu relato não foi confirmado pelo tutor."
                    else -> "Seu relato foi enviado. Aguarde a análise do tutor."
                }
            }
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 4, 0, 12)
        }
        card.addView(msgTv)

        val actionButton = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            ).apply {
                setMargins(0, margin12Px, 0, 0)
            }
            isAllCaps = false
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER

            when {
                statusUpper == "APROVADO" -> {
                    text = "Abrir chat"
                    setTextColor(getColor(R.color.white))
                    setBackgroundResource(R.drawable.bg_btn_dark_green)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        val intent = Intent(this@MySightingReportsActivity, LostAndFoundChatActivity::class.java).apply {
                            putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, req.id)
                            putExtra(LostAndFoundChatActivity.EXTRA_VIEWER_ROLE, LostAndFoundChatActivity.ROLE_TUTOR)
                        }
                        startActivity(intent)
                    }
                }
                statusUpper == "RECUSADO" || statusUpper == "RECUSADA" -> {
                    text = "Recusado"
                    setTextColor(Color.parseColor("#A83232"))
                    setBackgroundResource(R.drawable.bg_bar_red)
                    isClickable = false
                    isFocusable = false
                    setOnClickListener(null)
                }
                else -> {
                    text = "Em análise"
                    setTextColor(Color.parseColor("#7A5710"))
                    setBackgroundResource(R.drawable.bg_bar_yellow)
                    isClickable = false
                    isFocusable = false
                    setOnClickListener(null)
                }
            }
        }
        card.addView(actionButton)

        return card
    }
}
