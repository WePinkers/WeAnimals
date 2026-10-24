package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchesListBinding
    private var lostAd: FoundPetAd? = null

    data class MatchResult(val foundAd: FoundPetAd, val score: Int)

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
        val allAds = LostAndFoundRepository.getAllAds(this)
        val activeFoundAds = allAds.filter { it.category == "ENCONTRADO" && !it.isEnded && !it.isReunited }

        val matchResults = activeFoundAds.map { foundAd ->
            MatchResult(foundAd, calculateScore(currentLostAd, foundAd))
        }.sortedByDescending { it.score }

        val container = binding.containerMatchesList
        container.removeAllViews()

        if (matchResults.isEmpty()) {
            binding.tvMatchesSummary.text = "Nenhum possível match encontrado por enquanto."
            return
        }

        binding.tvMatchesSummary.text = "Exibindo ${matchResults.size} ${if (matchResults.size == 1) "animal encontrado compatível" else "animais encontrados compatíveis"}:"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

        for (match in matchResults) {
            val foundAd = match.foundAd
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
                textSize = 15f
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
                text = "${match.score}% compatível"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.white))
                setBackgroundResource(R.drawable.bg_capsule_selected)
                setPadding(20, 8, 20, 8)
            }

            topRow.addView(icon)
            topRow.addView(infoLayout)
            topRow.addView(badgeTv)

            card.addView(topRow)

            val actionButton = androidx.appcompat.widget.AppCompatButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (46 * resources.displayMetrics.density).toInt()
                ).apply {
                    setMargins(0, 16, 0, 0)
                }
                text = "Ver detalhes"
                isAllCaps = false
                textSize = 14f
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

            card.addView(actionButton)
            container.addView(card)
        }
    }

    private fun calculateScore(lostAd: FoundPetAd, foundAd: FoundPetAd): Int {
        var score = 0
        if (lostAd.species.equals(foundAd.species, ignoreCase = true)) score += 40
        if (!lostAd.size.isNullOrBlank() && lostAd.size.equals(foundAd.size, ignoreCase = true)) score += 20 else score += 10
        if (!lostAd.ageApprox.isNullOrBlank() && lostAd.ageApprox.equals(foundAd.ageApprox, ignoreCase = true)) score += 20 else score += 10
        if (!lostAd.city.isNullOrBlank() && lostAd.city.equals(foundAd.city, ignoreCase = true)) score += 20 else score += 10
        return score.coerceIn(25, 96)
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
