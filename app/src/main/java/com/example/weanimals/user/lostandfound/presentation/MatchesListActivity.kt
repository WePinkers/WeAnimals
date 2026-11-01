package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.location.Location
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
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.example.weanimals.user.lostandfound.service.PetMatchResult
import com.example.weanimals.user.lostandfound.service.PetMatchingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchesListBinding
    private var sourceAd: FoundPetAd? = null

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

        sourceAd = LostAndFoundRepository.getAdById(this, adId)
        if (sourceAd == null) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.includeHeader.headerTitleTv.text = "Possíveis matches"
        binding.includeHeader.headerBackBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadMatches()
    }

    private fun loadMatches() {
        val currentSourceAd = sourceAd ?: return
        val isSourceLost = currentSourceAd.category.equals("PERDIDO", ignoreCase = true)
        val targetCategory = if (isSourceLost) "ENCONTRADO" else "PERDIDO"

        val container = binding.containerMatchesList
        container.removeAllViews()

        val discardedSet = getSharedPreferences("discarded_matches_prefs", MODE_PRIVATE)
            .getStringSet("discarded_${currentSourceAd.id}", emptySet()) ?: emptySet()

        val allAds = LostAndFoundRepository.getAllAds(this)
        val activeTargetAds = allAds
            .filter { it.category.equals(targetCategory, ignoreCase = true) && !it.isEnded && !it.isReunited }
            .filter { !discardedSet.contains(it.id) }

        val matchResults = activeTargetAds
            .map { targetAd -> PetMatchingService.calculateMatch(currentSourceAd, targetAd) }
            .filter { it.score >= 30 }
            .sortedByDescending { it.score }

        if (matchResults.isEmpty()) {
            binding.tvMatchesSummary.text = "Nenhum possível match encontrado por enquanto."
            binding.btnViewMatchesMap.visibility = View.GONE
            return
        }

        binding.btnViewMatchesMap.visibility = View.VISIBLE
        binding.btnViewMatchesMap.setOnClickListener {
            val best = matchResults.firstOrNull()
            val intent = Intent(this, LostAndFoundMapActivity::class.java).apply {
                if (best != null && best.foundAd.latitude != null && best.foundAd.longitude != null) {
                    putExtra(LostAndFoundMapActivity.EXTRA_FOCUS_AD_ID, best.foundAd.id)
                }
            }
            startActivity(intent)
        }

        val categoryLabel = if (isSourceLost) "encontrados" else "perdidos"
        binding.tvMatchesSummary.text = "Exibindo ${matchResults.size} ${if (matchResults.size == 1) "animal $categoryLabel compatível" else "animais $categoryLabel compatíveis"}:"

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
            container.addView(createMatchCard(currentSourceAd, bestMatch, isBest = true))
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
                container.addView(createMatchCard(currentSourceAd, match, isBest = false))
            }
        }
    }

    private fun calculateDistanceText(lat1: Double?, lon1: Double?, lat2: Double?, lon2: Double?): String? {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        val meters = results[0]
        return if (meters < 1000f) {
            "${meters.toInt()} m"
        } else {
            String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", meters / 1000f)
        }
    }

    private fun createMatchCard(sourceAd: FoundPetAd, match: PetMatchResult, isBest: Boolean): View {
        val targetAd = match.foundAd
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

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
            layoutParams = LinearLayout.LayoutParams(52, 52)
            setImageResource(R.drawable.ic_paw_print)
            setColorFilter(getColor(R.color.pine800))
        }

        if (targetAd.photoUris.isNotEmpty()) {
            val photoUri = targetAd.photoUris.first()
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
            text = targetAd.getDisplayTitle()
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
        }

        val pubDateStr = dateFormat.format(Date(targetAd.publicationTimestamp))
        val distanceText = calculateDistanceText(sourceAd.latitude, sourceAd.longitude, targetAd.latitude, targetAd.longitude)
        val locationDetails = listOfNotNull(
            targetAd.getDisplayLocation(),
            pubDateStr,
            distanceText
        ).joinToString(" · ")

        val locTv = TextView(this).apply {
            text = locationDetails
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

        // Action Buttons Row (Discard, Ver no mapa, Ver detalhes)
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12
            }
        }

        val btnDiscard = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, (44 * resources.displayMetrics.density).toInt(), 1f).apply {
                marginEnd = 4
            }
            text = "Descartar"
            isAllCaps = false
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
            setBackgroundResource(R.drawable.bg_btn_outline)

            setOnClickListener {
                val prefs = getSharedPreferences("discarded_matches_prefs", MODE_PRIVATE)
                val currentSet = prefs.getStringSet("discarded_${sourceAd.id}", emptySet())?.toMutableSet() ?: mutableSetOf()
                currentSet.add(targetAd.id)
                prefs.edit().putStringSet("discarded_${sourceAd.id}", currentSet).apply()

                Toast.makeText(this@MatchesListActivity, "Candidato descartado", Toast.LENGTH_SHORT).show()
                loadMatches()
            }
        }

        val btnMap = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, (44 * resources.displayMetrics.density).toInt(), 1f).apply {
                marginEnd = 4
                marginStart = 4
            }
            text = "Ver no mapa"
            isAllCaps = false
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.white))
            setBackgroundResource(R.drawable.bg_btn_terracotta)

            setOnClickListener {
                if (targetAd.latitude != null && targetAd.longitude != null) {
                    val intent = Intent(this@MatchesListActivity, LostAndFoundMapActivity::class.java).apply {
                        putExtra(LostAndFoundMapActivity.EXTRA_FOCUS_AD_ID, targetAd.id)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MatchesListActivity, "Este candidato não possui localização geográfica válida para exibição no mapa.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnDetails = androidx.appcompat.widget.AppCompatButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, (44 * resources.displayMetrics.density).toInt(), 1.1f).apply {
                marginStart = 4
            }
            text = "Detalhes"
            isAllCaps = false
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.white))
            setBackgroundResource(R.drawable.bg_btn_dark_green)

            setOnClickListener {
                val intent = Intent(this@MatchesListActivity, PublicAdDetailActivity::class.java).apply {
                    putExtra(PublicAdDetailActivity.EXTRA_AD_ID, targetAd.id)
                }
                startActivity(intent)
            }
        }

        actionsRow.addView(btnDiscard)
        actionsRow.addView(btnMap)
        actionsRow.addView(btnDetails)
        card.addView(actionsRow)

        return card
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
