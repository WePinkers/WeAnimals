package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityLostAndFoundBinding
import com.example.weanimals.databinding.ItemLostFoundReportCardBinding
import com.example.weanimals.user.home.presenter.HomeContract
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.example.weanimals.user.map.overview.presentation.MapActivity
import com.example.weanimals.user.reporting.report.domain.Report
import com.example.weanimals.user.reporting.report.domain.ReportStatus
import com.example.weanimals.user.reporting.tracking.presentation.TrackingActivity

class LostAndFoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostAndFoundBinding
    private var currentFilter = FilterType.ALL
    private var allReports: List<Report> = emptyList()

    private val reportsPresenter: HomeContract.Presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createHomePresenter()
    }

    private val reportsView = object : HomeContract.View {
        override fun showGreeting(name: String?) = Unit

        override fun showLoading() = Unit

        override fun showReports(reports: List<Report>) {
            allReports = reports
            updateIndicators()
            loadRecentReports(currentFilter)
        }

        override fun showReportsError(error: Throwable) {
            allReports = emptyList()
            updateIndicators()
            loadRecentReports(currentFilter)
        }

        override fun openReport(report: Report) = Unit
    }

    private enum class FilterType { ALL, LOST, FOUND }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostAndFoundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MainNavigation.bind(this, binding.mainNavigation, null)

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        reportsPresenter.attachView(reportsView)
        reportsPresenter.loadReports()
    }

    override fun onStop() {
        reportsPresenter.detachView()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        updateIndicators()
        loadRecentReports(currentFilter)
    }

    private fun updateIndicators() {
        val summary = LostAndFoundRepository.getIndicatorsSummary(this)
        val remoteLost = allReports.count { it.isLostReport() }
        val remoteFound = allReports.count { !it.isLostReport() && it.status != ReportStatus.CLOSED }
        val remoteReunited = allReports.count {
            it.status == ReportStatus.RESCUED || it.status == ReportStatus.CLOSED
        }
        binding.statLostCount.text = (summary.lostCount + remoteLost).toString()
        binding.statFoundCount.text = (summary.foundCount + remoteFound).toString()
        binding.statReunitedCount.text = (summary.reunitedCount + remoteReunited).toString()
    }

    private fun setupListeners() {
        binding.btnLostPet.setOnClickListener {
            startActivity(Intent(this, LostPetFormActivity::class.java))
        }

        binding.btnFoundPet.setOnClickListener {
            startActivity(Intent(this, FoundPetFormActivity::class.java))
        }

        binding.btnMyAds.setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        binding.btnMyRequests.setOnClickListener {
            startActivity(Intent(this, MyRequestsActivity::class.java))
        }

        binding.btnMySightingReports.setOnClickListener {
            startActivity(Intent(this, MySightingReportsActivity::class.java))
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

        val filteredReports = allReports
            .filter { report ->
                when (filter) {
                    FilterType.ALL -> true
                    FilterType.LOST -> report.isLostReport()
                    FilterType.FOUND -> !report.isLostReport()
                }
            }
            .sortedByDescending { it.createdAtMillis }

        if (filtered.isEmpty() && filteredReports.isEmpty()) {
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

        filteredReports.forEach { report ->
            addReportCard(report, container)
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
                setImageResource(R.drawable.ic_paw_print)
                setColorFilter(
                    getColor(if (ad.category.equals("PERDIDO", ignoreCase = true)) R.color.terracotta else R.color.pine800)
                )
            }

            if (ad.photoUris.isNotEmpty()) {
                val photoUri = ad.photoUris.first()
                lifecycleScope.launch {
                    val bitmap = com.example.weanimals.core.utils.ImageThumbnailLoader.loadThumbnail(
                        this@LostAndFoundActivity,
                        photoUri,
                        120,
                        120
                    )
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

    private fun addReportCard(report: Report, container: LinearLayout) {
        val cardBinding = ItemLostFoundReportCardBinding.inflate(layoutInflater, container, false)
        val isLost = report.isLostReport()
        val animalLabel = when (report.animalType) {
            Report.ANIMAL_CAT -> "Gato"
            Report.ANIMAL_OTHER -> "Animal"
            else -> "Cão"
        }
        val title = if (report.description.isNotBlank()) {
            "$animalLabel — ${report.description.take(28)}"
        } else {
            "$animalLabel (#${report.protocolNumber})"
        }
        val status = when (report.status) {
            ReportStatus.RESCUED -> "Resgatado"
            ReportStatus.CLOSED -> "Reencontrado"
            ReportStatus.IN_PROGRESS -> "Em atendimento"
            else -> if (isLost) "Perdido" else "Encontrado"
        }

        cardBinding.reportTitle.text = title
        cardBinding.reportSubtitle.text = "${report.address.ifBlank { "Localização registrada" }} · Relato da comunidade"
        cardBinding.statusBadge.text = status
        val accent = ContextCompat.getColor(this, if (isLost) R.color.alert600 else R.color.pine700)
        cardBinding.indicatorBorder.setBackgroundColor(accent)
        cardBinding.statusBadge.setTextColor(accent)
        cardBinding.cardRoot.setOnClickListener {
            if (isLost) {
                startActivity(TrackingActivity.newIntent(this, report.id))
            } else {
                startActivity(
                    Intent(this, FoundAnimalPublicDetailActivity::class.java)
                        .putExtra("extra_report_id", report.id)
                )
            }
        }
        container.addView(cardBinding.root)
    }

    private fun Report.isLostReport(): Boolean =
        urgency == Report.URGENCY_HIGH || status == ReportStatus.NEW
}
