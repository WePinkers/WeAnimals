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
import com.example.weanimals.databinding.ActivityMyRequestsBinding
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyRequestsBinding
    private var currentFilter = RequestFilter.ALL
    private val currentUserId by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
    }

    private enum class RequestFilter { ALL, PENDING, APPROVED, REJECTED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackMyRequests.setOnClickListener { finish() }

        setupFilters()
    }

    override fun onResume() {
        super.onResume()
        loadRequests(currentFilter)
    }

    private fun setupFilters() {
        binding.filterChipAll.setOnClickListener { selectFilter(RequestFilter.ALL) }
        binding.filterChipPending.setOnClickListener { selectFilter(RequestFilter.PENDING) }
        binding.filterChipApproved.setOnClickListener { selectFilter(RequestFilter.APPROVED) }
        binding.filterChipRejected.setOnClickListener { selectFilter(RequestFilter.REJECTED) }
    }

    private fun selectFilter(filter: RequestFilter) {
        currentFilter = filter
        val activeBg = R.drawable.bg_chip_selected
        val inactiveBg = R.drawable.bg_chip_unselected
        val activeColor = getColor(R.color.white)
        val inactiveColor = getColor(R.color.ink)

        binding.filterChipAll.setBackgroundResource(if (filter == RequestFilter.ALL) activeBg else inactiveBg)
        binding.filterChipAll.setTextColor(if (filter == RequestFilter.ALL) activeColor else inactiveColor)

        binding.filterChipPending.setBackgroundResource(if (filter == RequestFilter.PENDING) activeBg else inactiveBg)
        binding.filterChipPending.setTextColor(if (filter == RequestFilter.PENDING) activeColor else inactiveColor)

        binding.filterChipApproved.setBackgroundResource(if (filter == RequestFilter.APPROVED) activeBg else inactiveBg)
        binding.filterChipApproved.setTextColor(if (filter == RequestFilter.APPROVED) activeColor else inactiveColor)

        binding.filterChipRejected.setBackgroundResource(if (filter == RequestFilter.REJECTED) activeBg else inactiveBg)
        binding.filterChipRejected.setTextColor(if (filter == RequestFilter.REJECTED) activeColor else inactiveColor)

        loadRequests(filter)
    }

    private fun loadRequests(filter: RequestFilter) {
        val container = binding.containerMyRequests
        container.removeAllViews()

        val allAds = LostAndFoundRepository.getAllAds(this)
        val allRequests = mutableListOf<com.example.weanimals.lostandfound.model.TutorVerificationRequest>()
        for (ad in allAds) {
            allRequests.addAll(LostAndFoundRepository.getVerificationRequestsForAd(this, ad.id))
        }

        val myRequests = allRequests
            .filter { it.requesterUserId == currentUserId }
            .filter { req ->
                when (filter) {
                    RequestFilter.ALL -> true
                    RequestFilter.PENDING -> req.status.equals("PENDENTE", ignoreCase = true)
                    RequestFilter.APPROVED -> req.status.equals("APROVADO", ignoreCase = true)
                    RequestFilter.REJECTED -> req.status.equals("RECUSADO", ignoreCase = true) || req.status.equals("RECUSADA", ignoreCase = true)
                }
            }
            .sortedByDescending { it.timestamp }

        if (myRequests.isEmpty()) {
            val emptyLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(32, 64, 32, 64)
            }

            val tv1 = TextView(this).apply {
                text = "Você ainda não enviou solicitações."
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
                gravity = Gravity.CENTER
            }

            val tv2 = TextView(this).apply {
                text = "Encontrou um anúncio que pode ser do seu animal? Envie uma verificação para acompanhar o pedido por aqui."
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

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val buttonHeightPx = (48 * resources.displayMetrics.density).toInt()
        val margin12Px = (12 * resources.displayMetrics.density).toInt()

        for (req in myRequests) {
            val ad = LostAndFoundRepository.getAdById(this, req.adId)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_white_border)
                setPadding(28, 28, 28, 28)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 20)
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
                text = ad?.getDisplayTitle() ?: "Animal encontrado"
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
                text = "Publicado por: $publisherName"
                textSize = 12f
                setTextColor(getColor(R.color.muted))
                setPadding(0, 2, 0, 0)
            }

            val dateStr = dateFormat.format(Date(req.timestamp))
            val dateTv = TextView(this).apply {
                text = "Solicitação enviada em $dateStr"
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
                text = when (statusUpper) {
                    "APROVADO" -> "Aprovada"
                    "RECUSADO", "RECUSADA" -> "Recusada"
                    else -> "Em análise"
                }
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                val badgeColor = when (statusUpper) {
                    "APROVADO" -> R.color.pine800
                    "RECUSADO", "RECUSADA" -> R.color.alert600
                    else -> R.color.terracotta
                }
                setTextColor(getColor(badgeColor))
            }
            card.addView(statusBadge)

            val msgTv = TextView(this).apply {
                text = when (statusUpper) {
                    "APROVADO" -> "Suas respostas foram aceitas! Você já pode conversar com quem encontrou o animal."
                    "RECUSADO", "RECUSADA" -> "Suas respostas não foram confirmadas por quem encontrou o animal."
                    else -> "Suas respostas foram enviadas. Aguarde a análise de quem encontrou o animal."
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

                when (statusUpper) {
                    "APROVADO" -> {
                        text = "Abrir chat"
                        setTextColor(getColor(R.color.white))
                        setBackgroundResource(R.drawable.bg_btn_dark_green)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            val intent = Intent(this@MyRequestsActivity, LostAndFoundChatActivity::class.java).apply {
                                putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, req.id)
                                putExtra(LostAndFoundChatActivity.EXTRA_VIEWER_ROLE, LostAndFoundChatActivity.ROLE_TUTOR)
                            }
                            startActivity(intent)
                        }
                    }
                    "RECUSADO", "RECUSADA" -> {
                        text = "Recusada"
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

            container.addView(card)
        }
    }
}
