package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityLostAndFoundBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.example.weanimals.map.overview.presentation.MapActivity

class LostAndFoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostAndFoundBinding
    private var currentFilter = FilterType.ALL

    private enum class FilterType { ALL, LOST, FOUND }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostAndFoundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MainNavigation.bind(this, binding.mainNavigation, null)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateIndicators()
        loadRecentReports(currentFilter)
    }

    private fun updateIndicators() {
        val summary = LostAndFoundRepository.getIndicatorsSummary(this)
        binding.statLostCount.text = summary.lostCount.toString()
        binding.statFoundCount.text = summary.foundCount.toString()
        binding.statReunitedCount.text = summary.reunitedCount.toString()
    }

    private fun setupListeners() {
        binding.btnLostPet.setOnClickListener {
            Toast.makeText(this, "Funcionalidade em desenvolvimento", Toast.LENGTH_SHORT).show()
        }

        binding.btnFoundPet.setOnClickListener {
            startActivity(Intent(this, FoundPetFormActivity::class.java))
        }

        binding.btnMyAds.setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.filterAll.setOnClickListener {
            selectFilter(FilterType.ALL)
        }
        binding.filterLost.setOnClickListener {
            selectFilter(FilterType.LOST)
        }
        binding.filterFound.setOnClickListener {
            selectFilter(FilterType.FOUND)
        }
    }

    private fun selectFilter(filter: FilterType) {
        currentFilter = filter
        val activeBg = R.drawable.bg_chip_selected
        val inactiveBg = R.drawable.bg_chip_unselected
        val activeColor = getColor(R.color.white)
        val inactiveColor = getColor(R.color.ink)

        binding.filterAll.setBackgroundResource(if (filter == FilterType.ALL) activeBg else inactiveBg)
        binding.filterAll.setTextColor(if (filter == FilterType.ALL) activeColor else inactiveColor)

        binding.filterLost.setBackgroundResource(if (filter == FilterType.LOST) activeBg else inactiveBg)
        binding.filterLost.setTextColor(if (filter == FilterType.LOST) activeColor else inactiveColor)

        binding.filterFound.setBackgroundResource(if (filter == FilterType.FOUND) activeBg else inactiveBg)
        binding.filterFound.setTextColor(if (filter == FilterType.FOUND) activeColor else inactiveColor)

        loadRecentReports(filter)
    }

    private fun loadRecentReports(filter: FilterType) {
        val container = binding.containerRecentReports
        container.removeAllViews()

        val allAds = LostAndFoundRepository.getAllAds(this)

        val filtered = allAds
            .filter { ad -> !ad.isEnded && !ad.isReunited && ad.getRemainingDays() > 0 }
            .filter { ad ->
                when (filter) {
                    FilterType.ALL -> true
                    FilterType.LOST -> ad.category.equals("PERDIDO", ignoreCase = true)
                    FilterType.FOUND -> ad.category.equals("ENCONTRADO", ignoreCase = true)
                }
            }
            .sortedByDescending { it.publicationTimestamp }

        if (filtered.isEmpty()) {
            val emptyMsg = when (filter) {
                FilterType.ALL -> "Nenhum relato disponível no momento."
                FilterType.LOST -> "Nenhum animal perdido foi registrado recentemente."
                FilterType.FOUND -> "Nenhum animal encontrado foi registrado recentemente."
            }
            val emptyTv = TextView(this).apply {
                text = emptyMsg
                textSize = 13f
                setTextColor(getColor(R.color.muted))
                setPadding(0, 16, 0, 16)
            }
            container.addView(emptyTv)
            return
        }

        for (ad in filtered) {
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
                    val intent = Intent(this@LostAndFoundActivity, PublicAdDetailActivity::class.java).apply {
                        putExtra(PublicAdDetailActivity.EXTRA_AD_ID, ad.id)
                    }
                    startActivity(intent)
                }
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(48, 48)
                if (ad.photoUris.isNotEmpty()) {
                    try {
                        setImageURI(Uri.parse(ad.photoUris.first()))
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    } catch (e: Exception) {
                        setImageResource(R.drawable.ic_paw_print)
                        setColorFilter(
                            getColor(if (ad.category.equals("PERDIDO", ignoreCase = true)) R.color.terracotta else R.color.pine800)
                        )
                    }
                } else {
                    setImageResource(R.drawable.ic_paw_print)
                    setColorFilter(
                        getColor(if (ad.category.equals("PERDIDO", ignoreCase = true)) R.color.terracotta else R.color.pine800)
                    )
                }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 24
                }
            }

            val titleTv = TextView(this).apply {
                text = ad.getDisplayTitle()
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
            }

            val subTitleTv = TextView(this).apply {
                text = "${ad.getDisplayLocation()} · ${ad.getRelativeTimeText()}"
                textSize = 12f
                setTextColor(getColor(R.color.muted))
                setPadding(0, 2, 0, 0)
            }

            infoLayout.addView(titleTv)
            infoLayout.addView(subTitleTv)

            val statusBadge = TextView(this).apply {
                text = if (ad.category.equals("PERDIDO", ignoreCase = true)) "Perdido" else ad.currentSituation
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(20, 8, 20, 8)
                setBackgroundResource(R.drawable.bg_capsule_unselected)
                setTextColor(getColor(R.color.pine800))
            }

            card.addView(icon)
            card.addView(infoLayout)
            card.addView(statusBadge)

            container.addView(card)
        }
    }
}
