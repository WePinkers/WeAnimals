package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityLostAndFoundBinding
import com.example.weanimals.databinding.ItemLostFoundReportCardBinding
import com.example.weanimals.home.presenter.HomeContract
import com.example.weanimals.map.overview.presentation.MapActivity
import com.example.weanimals.profile.reports.presentation.MyReportsActivity
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.domain.ReportStatus
import com.example.weanimals.reporting.tracking.presentation.TrackingActivity

data class LostFoundReportUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val isLost: Boolean,
    val badgeText: String,
    val report: Report
)

class LostAndFoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostAndFoundBinding
    private var allReports: List<Report> = emptyList()
    private var activeFilter: FilterType = FilterType.ALL

    private enum class FilterType { ALL, LOST, FOUND }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityLostAndFoundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resetMetrics()
        setupInteractions()
        setupRecentReportsList()
        observeReports()
    }

    private fun resetMetrics() {
        binding.metricLostCount.text = "0"
        binding.metricFoundCount.text = "0"
        binding.metricReunitedCount.text = "0"
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardLostPet.setOnClickListener {
            startActivity(Intent(this, ReportLostAnimalActivity::class.java))
        }

        binding.cardFoundPet.setOnClickListener {
            startActivity(Intent(this, ReportLostAnimalActivity::class.java))
        }

        binding.btnMyAds.setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // Chip Filters
        val pineBg = R.drawable.bg_chip_selected_pine
        val unselectedBg = R.drawable.bg_chip_unselected
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val inkColor = ContextCompat.getColor(this, R.color.ink)

        binding.chipFilterAll.setOnClickListener {
            activeFilter = FilterType.ALL
            binding.chipFilterAll.setBackgroundResource(pineBg)
            binding.chipFilterAll.setTextColor(whiteColor)
            binding.chipFilterLost.setBackgroundResource(unselectedBg)
            binding.chipFilterLost.setTextColor(inkColor)
            binding.chipFilterFound.setBackgroundResource(unselectedBg)
            binding.chipFilterFound.setTextColor(inkColor)
            applyFilterAndRender()
        }

        binding.chipFilterLost.setOnClickListener {
            activeFilter = FilterType.LOST
            binding.chipFilterAll.setBackgroundResource(unselectedBg)
            binding.chipFilterAll.setTextColor(inkColor)
            binding.chipFilterLost.setBackgroundResource(pineBg)
            binding.chipFilterLost.setTextColor(whiteColor)
            binding.chipFilterFound.setBackgroundResource(unselectedBg)
            binding.chipFilterFound.setTextColor(inkColor)
            applyFilterAndRender()
        }

        binding.chipFilterFound.setOnClickListener {
            activeFilter = FilterType.FOUND
            binding.chipFilterAll.setBackgroundResource(unselectedBg)
            binding.chipFilterAll.setTextColor(inkColor)
            binding.chipFilterLost.setBackgroundResource(unselectedBg)
            binding.chipFilterLost.setTextColor(inkColor)
            binding.chipFilterFound.setBackgroundResource(pineBg)
            binding.chipFilterFound.setTextColor(whiteColor)
            applyFilterAndRender()
        }
    }

    private fun setupRecentReportsList() {
        binding.rvRecentReports.layoutManager = LinearLayoutManager(this)
    }

    private fun observeReports() {
        val homePresenter = (application as WeAnimalsApplication).appContainer.createHomePresenter()
        val view = object : HomeContract.View {
            override fun showGreeting(name: String?) {}
            override fun showLoading() {}
            override fun showReports(reports: List<Report>) {
                updateData(reports)
            }
            override fun showReportsError(error: Throwable) {
                updateData(emptyList())
            }
            override fun openReport(report: Report) {}
        }
        homePresenter.attachView(view)
        homePresenter.loadReports()
    }

    private fun updateData(reports: List<Report>) {
        allReports = reports

        val lostCount = reports.count { it.urgency == Report.URGENCY_HIGH || it.status == ReportStatus.NEW }
        val foundCount = reports.count { it.status == ReportStatus.UNDER_REVIEW || it.status == ReportStatus.IN_PROGRESS }
        val reunitedCount = reports.count { it.status == ReportStatus.RESCUED || it.status == ReportStatus.CLOSED }

        binding.metricLostCount.text = lostCount.toString()
        binding.metricFoundCount.text = foundCount.toString()
        binding.metricReunitedCount.text = reunitedCount.toString()

        applyFilterAndRender()
    }

    private fun applyFilterAndRender() {
        val uiModels = allReports.map { it.toLostFoundUiModel() }
        val filtered = when (activeFilter) {
            FilterType.ALL -> uiModels
            FilterType.LOST -> uiModels.filter { it.isLost }
            FilterType.FOUND -> uiModels.filter { !it.isLost }
        }

        if (filtered.isEmpty()) {
            binding.rvRecentReports.visibility = View.GONE
            binding.emptyReportsState.visibility = View.VISIBLE
        } else {
            binding.rvRecentReports.visibility = View.VISIBLE
            binding.emptyReportsState.visibility = View.GONE
            binding.rvRecentReports.adapter = LostAndFoundReportAdapter(filtered) { item ->
                if (item.isLost) {
                    startActivity(TrackingActivity.newIntent(this, item.id))
                } else {
                    val intent = Intent(this, FoundAnimalPublicDetailActivity::class.java).apply {
                        putExtra("extra_report_id", item.id)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun Report.toLostFoundUiModel(): LostFoundReportUiModel {
        val isLost = urgency == Report.URGENCY_HIGH || status == ReportStatus.NEW
        val animalLabel = when (animalType) {
            Report.ANIMAL_CAT -> "Gato"
            Report.ANIMAL_OTHER -> "Animal"
            else -> "Cão"
        }
        val titleText = if (description.isNotBlank()) {
            "$animalLabel — ${description.take(28)}"
        } else {
            "$animalLabel (#$protocolNumber)"
        }
        val locationText = address.ifBlank { "Localização registrada" }
        val statusText = when (status) {
            ReportStatus.RESCUED -> "Resgatado"
            ReportStatus.CLOSED -> "Reencontrado"
            ReportStatus.IN_PROGRESS -> "Em atendimento"
            else -> if (isLost) "Perdido" else "Encontrado"
        }
        return LostFoundReportUiModel(
            id = id,
            title = titleText,
            subtitle = "$locationText · Perto de você",
            isLost = isLost,
            badgeText = statusText,
            report = this
        )
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}

class LostAndFoundReportAdapter(
    private val items: List<LostFoundReportUiModel>,
    private val onItemClick: (LostFoundReportUiModel) -> Unit
) : RecyclerView.Adapter<LostAndFoundReportAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLostFoundReportCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLostFoundReportCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            reportTitle.text = item.title
            reportSubtitle.text = item.subtitle
            statusBadge.text = item.badgeText
            if (item.isLost) {
                indicatorBorder.setBackgroundColor(
                    ContextCompat.getColor(root.context, R.color.alert600)
                )
                statusBadge.setTextColor(
                    ContextCompat.getColor(root.context, R.color.alert600)
                )
            } else {
                indicatorBorder.setBackgroundColor(
                    ContextCompat.getColor(root.context, R.color.pine700)
                )
                statusBadge.setTextColor(
                    ContextCompat.getColor(root.context, R.color.pine700)
                )
            }
            cardRoot.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size
}
