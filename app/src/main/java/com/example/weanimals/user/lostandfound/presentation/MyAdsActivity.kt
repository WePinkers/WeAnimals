package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivityMyAdsBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAdsBinding
    private var activeExpanded = true
    private var endedExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadAds()
    }

    private fun loadAds() {
        val ads = LostAndFoundRepository.getAllAds(this)
        val container = binding.containerAdsList
        container.removeAllViews()

        if (ads.isEmpty()) {
            binding.tvAdsSummary.text = "Você ainda não possui anúncios publicados."
            return
        }

        binding.tvAdsSummary.text = "Exibindo ${ads.size} ${if (ads.size == 1) "anúncio publicado" else "anúncios publicados"}:"

        val activeAds = ads.filter { !it.isEnded && !it.isReunited && it.getRemainingDays() > 0 }
            .sortedByDescending { it.publicationTimestamp }

        val endedAds = ads.filter { it.isEnded || it.isReunited || it.getRemainingDays() <= 0 }
            .sortedByDescending { it.publicationTimestamp }

        // --- Section 1: Active Ads ---
        val activeSectionHeader = createAccordionHeader("Anúncios ativos (${activeAds.size})", activeExpanded) { expanded ->
            activeExpanded = expanded
            containerActiveList.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        container.addView(activeSectionHeader)

        containerActiveList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (activeExpanded) View.VISIBLE else View.GONE
            setPadding(0, 8, 0, 12)
        }

        if (activeAds.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Você não possui anúncios ativos."
                textSize = 13f
                setTextColor(getColor(R.color.muted))
                setPadding(16, 8, 16, 12)
            }
            containerActiveList.addView(emptyTv)
        } else {
            for (ad in activeAds) {
                containerActiveList.addView(createAdCard(ad))
            }
        }
        container.addView(containerActiveList)

        // --- Section 2: Ended Ads ---
        val endedSectionHeader = createAccordionHeader("Anúncios encerrados (${endedAds.size})", endedExpanded) { expanded ->
            endedExpanded = expanded
            containerEndedList.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        container.addView(endedSectionHeader)

        containerEndedList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (endedExpanded) View.VISIBLE else View.GONE
            setPadding(0, 8, 0, 12)
        }

        if (endedAds.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Você ainda não possui anúncios encerrados."
                textSize = 13f
                setTextColor(getColor(R.color.muted))
                setPadding(16, 8, 16, 12)
            }
            containerEndedList.addView(emptyTv)
        } else {
            for (ad in endedAds) {
                containerEndedList.addView(createAdCard(ad))
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

    private fun createAdCard(ad: FoundPetAd): View {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(28, 28, 28, 28)
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_card_white_border)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 12, 0, 12)
            layoutParams = params

            setOnClickListener {
                val intent = Intent(this@MyAdsActivity, MyAdDetailActivity::class.java).apply {
                    putExtra(MyAdDetailActivity.EXTRA_AD_ID, ad.id)
                }
                startActivity(intent)
            }
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setImageResource(R.drawable.ic_paw_print)
            setColorFilter(getColor(R.color.pine800))
        }

        if (ad.photoUris.isNotEmpty()) {
            val photoUri = ad.photoUris.first()
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(this@MyAdsActivity, photoUri, 120, 120)
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
                marginStart = 20
            }
        }

        val isLost = ad.category.equals("PERDIDO", ignoreCase = true)
        val padHorizontalPx = (10 * resources.displayMetrics.density).toInt()
        val padVerticalPx = (4 * resources.displayMetrics.density).toInt()
        val marginBottomPx = (6 * resources.displayMetrics.density).toInt()

        val categoryBadgeTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, marginBottomPx)
            }
            text = if (isLost) "ANIMAL PERDIDO" else "ANIMAL ENCONTRADO"
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.08f
            setTextColor(getColor(R.color.white))
            setBackgroundResource(if (isLost) R.drawable.bg_badge_lost else R.drawable.bg_badge_found)
            setPadding(padHorizontalPx, padVerticalPx, padHorizontalPx, padVerticalPx)
        }

        val titleTv = TextView(this).apply {
            text = ad.getDisplayTitle()
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.pine800))
        }

        val pubDateStr = dateFormat.format(Date(ad.publicationTimestamp))
        val subTitleTv = TextView(this).apply {
            text = "${ad.getDisplayLocation()} · $pubDateStr"
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(0, 2, 0, 2)
        }

        val statusText = if (ad.isReunited) {
            "Reencontrado"
        } else if (ad.isEnded) {
            "Encerrado"
        } else if (ad.getRemainingDays() <= 0) {
            "Expirado"
        } else {
            "${ad.currentSituation} · ${ad.getRemainingDaysText()}"
        }

        val statusTv = TextView(this).apply {
            text = statusText
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(if (ad.isEnded || ad.isReunited || ad.getRemainingDays() <= 0) R.color.muted else R.color.terracotta))
        }

        infoLayout.addView(categoryBadgeTv)
        infoLayout.addView(titleTv)
        infoLayout.addView(subTitleTv)
        infoLayout.addView(statusTv)

        val chevron = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(36, 36)
            setImageResource(R.drawable.ic_chevron_right)
            setColorFilter(getColor(R.color.muted))
        }

        card.addView(icon)
        card.addView(infoLayout)
        card.addView(chevron)

        return card
    }
}
