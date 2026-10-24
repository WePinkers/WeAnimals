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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivityMatchesListBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.example.weanimals.lostandfound.service.PetMatchResult
import com.example.weanimals.lostandfound.service.PetMatchingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchesListBinding
    private var lostAd: FoundPetAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adId = intent.getStringExtra(EXTRA_AD_ID)
        if (adId.isNullOrBlank()) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lostAd = LostAndFoundRepository.getAdById(this, adId)
        if (lostAd == null) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBackMatches.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadMatches()
    }

    private fun loadMatches() {
        val currentLostAd = lostAd ?: return
        val container = binding.containerMatchesList
        container.removeAllViews()

        val discardedSet = getSharedPreferences("discarded_matches_prefs", MODE_PRIVATE)
            .getStringSet("discarded_${currentLostAd.id}", emptySet()) ?: emptySet()

        val allAds = LostAndFoundRepository.getAllAds(this)
        val activeFoundAds = allAds
            .filter { it.category == "ENCONTRADO" && !it.isEnded && !it.isReunited }
            .filter { !discardedSet.contains(it.id) }

        val matchResults = activeFoundAds
            .map { foundAd -> PetMatchingService.calculateMatch(currentLostAd, foundAd) }
            .filter { it.score >= 25 }
            .sortedByDescending { it.score }

        if (matchResults.isEmpty()) {
            binding.tvMatchesSummary.text = "Nenhum possível match encontrado por enquanto."
            return
        }

        binding.tvMatchesSummary.text = "Exibindo ${matchResults.size} ${if (matchResults.size == 1) "animal encontrado compatível" else "animais encontrados compatíveis"}:"

        val bestMatch = matchResults.firstOrNull { it.score >= 50 }
        val remainingMatches = if (bestMatch != null) matchResults.drop(1) else matchResults

        if (bestMatch != null) {
            val bestHeader = TextView(this).apply {
                text = "MELHOR COMPATIBILIDADE"
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
                letterSpacing = 0.1f
                setPadding(0, 8, 0, 8)
            }
            container.addView(bestHeader)
            container.addView(createMatchCard(currentLostAd, bestMatch, isBest = true))
        }

        if (remainingMatches.isNotEmpty()) {
            val partialHeader = TextView(this).apply {
                text = "COMPATIBILIDADE PARCIAL"
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.muted))
                letterSpacing = 0.1f
                setPadding(0, 16, 0, 8)
            }
            container.addView(partialHeader)

            for (match in remainingMatches) {
                container.addView(createMatchCard(currentLostAd, match, isBest = false))
            }
        }
    }

    private fun createMatchCard(lostAd: FoundPetAd, match: PetMatchResult, isBest: Boolean): View {
        val foundAd = match.foundAd
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(if (isBest) R.drawable.bg_card_white_border else R.drawable.bg_card_white_border)
            setPadding(28, 28, 28, 28)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 20)
            layoutParams = params
        }

        // Top Row: Icon + Info + Score Badge
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(52, 52)
            setImageResource(R.drawable.ic_paw_print)
            setColorFilter(getColor(R.color.pine800))
        }

        if (foundAd.photoUris.isNotEmpty()) {
            val photoUri = foundAd.photoUris.first()
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(this@MatchesListActivity, photoUri, 120, 120)
                if (bitmap != null) {
                    icon.clearColorFilter()
                    icon.setImageBitmap(bitmap)
                    icon.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 16
            }
        }

        val titleTv = TextView(this).apply {
            text = foundAd.getDisplayTitle()
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
        }

        val pubDateStr = dateFormat.format(Date(foundAd.publicationTimestamp))
        val locTv = TextView(this).apply {
            text = "${foundAd.getDisplayLocation()} · $pubDateStr"
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 2, 0, 0)
        }

        infoLayout.addView(titleTv)
        infoLayout.addView(locTv)

        val badgeTv = TextView(this).apply {
            text = "${match.score}% — MATCH"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.white))
            setBackgroundResource(if (isBest) R.drawable.bg_btn_dark_green else R.drawable.bg_capsule_selected)
            setPadding(20, 10, 20, 10)
        }

        topRow.addView(icon)
        topRow.addView(infoLayout)
        topRow.addView(badgeTv)
        card.addView(topRow)

        if (isBest) {
            val disclaimerTv = TextView(this).apply {
                text = "Índice de compatibilidade estimado, não confirmação de identidade."
                textSize = 11f
                setTextColor(getColor(R.color.terracotta))
                setPadding(0, 8, 0, 0)
            }
            card.addView(disclaimerTv)
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 16, 0, 16)
            }
            setBackgroundColor(getColor(R.color.border))
        }
        card.addView(divider)

        // Matching Reasons Bullet Points
        if (match.matchingReasons.isNotEmpty()) {
            for (reason in match.matchingReasons) {
                val reasonRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 4)
                }
                val check = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(18, 18)
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(getColor(R.color.pine800))
                }
                val reasonTv = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = 8
                    }
                    text = reason
                    textSize = 13f
                    setTextColor(getColor(R.color.ink))
                }
                reasonRow.addView(check)
                reasonRow.addView(reasonTv)
                card.addView(reasonRow)
            }
        }

        // Expandable "Ver todos os critérios"
        val detailsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 12, 0, 8)
        }

        for (detail in match.detailsList) {
            val detailTv = TextView(this).apply {
                text = "• ${detail.criteriaName}: ${detail.lostValue} vs ${detail.foundValue} (${detail.pointsAwarded}/${detail.maxPoints} pts)"
                textSize = 12f
                setTextColor(getColor(if (detail.isMatch) R.color.pine800 else R.color.muted))
                setPadding(0, 2, 0, 2)
            }
            detailsContainer.addView(detailTv)
        }
        card.addView(detailsContainer)

        var detailsExpanded = false
        val toggleDetailsBtn = TextView(this).apply {
            text = "Ver todos os critérios ▼"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.terracotta))
            setPadding(0, 10, 0, 10)
            setOnClickListener {
                detailsExpanded = !detailsExpanded
                detailsContainer.visibility = if (detailsExpanded) View.VISIBLE else View.GONE
                text = if (detailsExpanded) "Ocultar critérios ▲" else "Ver todos os critérios ▼"
            }
        }
        card.addView(toggleDetailsBtn)

        // Action Buttons Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12
            }
        }

        val btnDiscard = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, (46 * resources.displayMetrics.density).toInt(), 1f).apply {
                marginEnd = 8
            }
            text = "Não é o mesmo animal"
            isAllCaps = false
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
            setBackgroundResource(R.drawable.bg_btn_outline)

            setOnClickListener {
                val prefs = getSharedPreferences("discarded_matches_prefs", MODE_PRIVATE)
                val currentSet = prefs.getStringSet("discarded_${lostAd.id}", emptySet())?.toMutableSet() ?: mutableSetOf()
                currentSet.add(foundAd.id)
                prefs.edit().putStringSet("discarded_${lostAd.id}", currentSet).apply()

                Toast.makeText(this@MatchesListActivity, "Candidato descartado", Toast.LENGTH_SHORT).show()
                loadMatches()
            }
        }

        val btnDetails = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, (46 * resources.displayMetrics.density).toInt(), 1.2f).apply {
                marginStart = 8
            }
            text = "Ver detalhes"
            isAllCaps = false
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.white))
            setBackgroundResource(R.drawable.bg_btn_dark_green)

            setOnClickListener {
                val intent = Intent(this@MatchesListActivity, PublicAdDetailActivity::class.java).apply {
                    putExtra(PublicAdDetailActivity.EXTRA_AD_ID, foundAd.id)
                }
                startActivity(intent)
            }
        }

        actionsRow.addView(btnDiscard)
        actionsRow.addView(btnDetails)
        card.addView(actionsRow)

        return card
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
