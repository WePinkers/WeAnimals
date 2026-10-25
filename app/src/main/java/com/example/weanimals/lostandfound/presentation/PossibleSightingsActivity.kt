package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityPossibleSightingsBinding
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PossibleSightingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPossibleSightingsBinding
    private var adId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPossibleSightingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adId = intent.getStringExtra(EXTRA_AD_ID)
        if (adId.isNullOrBlank()) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.includeHeader.headerTitleTv.text = "Possíveis encontros"
        binding.includeHeader.headerBackBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadSightings()
    }

    private fun loadSightings() {
        val currentAdId = adId ?: return
        val requests = LostAndFoundRepository.getVerificationRequestsForAd(this, currentAdId)
        val container = binding.containerSightingsList
        container.removeAllViews()

        if (requests.isEmpty()) {
            binding.tvSightingsSummary.text = "Ninguém relatou ter encontrado este animal ainda."
            return
        }

        binding.tvSightingsSummary.text = "Exibindo ${requests.size} ${if (requests.size == 1) "relato recebido" else "relatos recebidos"}:"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

        for (req in requests) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 28, 32, 28)
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setBackgroundResource(R.drawable.bg_card_white_border)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 20)
                layoutParams = params

                setOnClickListener {
                    if (req.status.equals("APROVADO", ignoreCase = true)) {
                        val intent = Intent(this@PossibleSightingsActivity, LostAndFoundChatActivity::class.java).apply {
                            putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, req.id)
                            putExtra(LostAndFoundChatActivity.EXTRA_VIEWER_ROLE, LostAndFoundChatActivity.ROLE_FOUNDER)
                        }
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@PossibleSightingsActivity, SightingReportDetailActivity::class.java).apply {
                            putExtra(SightingReportDetailActivity.EXTRA_REQUEST_ID, req.id)
                        }
                        startActivity(intent)
                    }
                }
            }

            // Circular Avatar
            val requesterName = req.requesterName?.takeIf { it.isNotBlank() } ?: "Usuário"
            val avatarInitial = requesterName.firstOrNull()?.uppercase() ?: "U"

            val avatarContainer = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(96, 96)
                setBackgroundResource(R.drawable.bg_capsule_selected)
            }

            val tvInitial = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                text = avatarInitial
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.white))
            }
            avatarContainer.addView(tvInitial)

            // Info Column
            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 24
                }
            }

            val nameTv = TextView(this).apply {
                text = requesterName
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
            }

            val dateStr = dateFormat.format(Date(req.timestamp))
            val subTv = TextView(this).apply {
                text = "diz ter visto este animal · $dateStr"
                textSize = 12f
                setTextColor(getColor(R.color.muted))
                setPadding(0, 2, 0, 0)
            }

            val statusBadge = TextView(this).apply {
                text = when (req.status.uppercase()) {
                    "APROVADO" -> "Aprovado"
                    "RECUSADO", "RECUSADA" -> "Recusado"
                    else -> "Em análise"
                }
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                val badgeColor = when (req.status.uppercase()) {
                    "APROVADO" -> R.color.pine800
                    "RECUSADO", "RECUSADA" -> R.color.alert600
                    else -> R.color.terracotta
                }
                setTextColor(getColor(badgeColor))
                setPadding(0, 4, 0, 0)
            }

            infoLayout.addView(nameTv)
            infoLayout.addView(subTv)
            infoLayout.addView(statusBadge)

            val chevron = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40)
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(getColor(R.color.muted))
            }

            card.addView(avatarContainer)
            card.addView(infoLayout)
            card.addView(chevron)

            container.addView(card)
        }
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
