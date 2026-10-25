package com.example.weanimals.ombudsman.presentation

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
import com.example.weanimals.databinding.ActivityMyProtocolsBinding
import com.example.weanimals.databinding.ItemOmbudsmanProtocolCardBinding
import com.example.weanimals.ombudsman.domain.ManifestationStatus
import com.example.weanimals.ombudsman.domain.OmbudsmanProtocol
import com.example.weanimals.ombudsman.repository.OmbudsmanRepository

class MyProtocolsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProtocolsBinding
    private var allProtocols: List<OmbudsmanProtocol> = emptyList()
    private var activeFilter: FilterType = FilterType.ALL

    private enum class FilterType { ALL, IN_PROGRESS, COMPLETED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityMyProtocolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
        setupProtocolsList()
    }

    override fun onResume() {
        super.onResume()
        loadProtocols()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        val pineBg = R.drawable.bg_chip_selected_pine
        val unselectedBg = R.drawable.bg_chip_unselected
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val inkColor = ContextCompat.getColor(this, R.color.ink)

        binding.chipFilterAll.setOnClickListener {
            activeFilter = FilterType.ALL
            binding.chipFilterAll.setBackgroundResource(pineBg)
            binding.chipFilterAll.setTextColor(whiteColor)
            binding.chipFilterInProgress.setBackgroundResource(unselectedBg)
            binding.chipFilterInProgress.setTextColor(inkColor)
            binding.chipFilterCompleted.setBackgroundResource(unselectedBg)
            binding.chipFilterCompleted.setTextColor(inkColor)
            applyFilterAndRender()
        }

        binding.chipFilterInProgress.setOnClickListener {
            activeFilter = FilterType.IN_PROGRESS
            binding.chipFilterAll.setBackgroundResource(unselectedBg)
            binding.chipFilterAll.setTextColor(inkColor)
            binding.chipFilterInProgress.setBackgroundResource(pineBg)
            binding.chipFilterInProgress.setTextColor(whiteColor)
            binding.chipFilterCompleted.setBackgroundResource(unselectedBg)
            binding.chipFilterCompleted.setTextColor(inkColor)
            applyFilterAndRender()
        }

        binding.chipFilterCompleted.setOnClickListener {
            activeFilter = FilterType.COMPLETED
            binding.chipFilterAll.setBackgroundResource(unselectedBg)
            binding.chipFilterAll.setTextColor(inkColor)
            binding.chipFilterInProgress.setBackgroundResource(unselectedBg)
            binding.chipFilterInProgress.setTextColor(inkColor)
            binding.chipFilterCompleted.setBackgroundResource(pineBg)
            binding.chipFilterCompleted.setTextColor(whiteColor)
            applyFilterAndRender()
        }
    }

    private fun setupProtocolsList() {
        binding.rvProtocols.layoutManager = LinearLayoutManager(this)
    }

    private fun loadProtocols() {
        allProtocols = OmbudsmanRepository.getProtocols()
        applyFilterAndRender()
    }

    private fun applyFilterAndRender() {
        val filtered = when (activeFilter) {
            FilterType.ALL -> allProtocols
            FilterType.IN_PROGRESS -> allProtocols.filter { it.status != ManifestationStatus.CONCLUIDA }
            FilterType.COMPLETED -> allProtocols.filter { it.status == ManifestationStatus.CONCLUIDA }
        }

        if (filtered.isEmpty()) {
            binding.rvProtocols.visibility = View.GONE
            binding.emptyProtocolsState.visibility = View.VISIBLE
        } else {
            binding.rvProtocols.visibility = View.VISIBLE
            binding.emptyProtocolsState.visibility = View.GONE
            binding.rvProtocols.adapter = OmbudsmanProtocolAdapter(filtered) { protocol ->
                val intent = Intent(this, ProtocolDetailActivity::class.java).apply {
                    putExtra("extra_protocol_number", protocol.protocolNumber)
                }
                startActivity(intent)
            }
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}

class OmbudsmanProtocolAdapter(
    private val items: List<OmbudsmanProtocol>,
    private val onItemClick: (OmbudsmanProtocol) -> Unit
) : RecyclerView.Adapter<OmbudsmanProtocolAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemOmbudsmanProtocolCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOmbudsmanProtocolCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            txtProtocolNumber.text = item.protocolNumber
            txtSubject.text = item.subject
            txtMetaInfo.text = "${item.type.displayName} · ${item.dateFormatted}"
            badgeStatus.text = item.status.displayName

            when (item.status) {
                ManifestationStatus.EM_ANALISE -> {
                    badgeStatus.setBackgroundResource(R.drawable.bg_badge_status_amber)
                    badgeStatus.setTextColor(ContextCompat.getColor(root.context, R.color.brass500))
                }
                ManifestationStatus.AGUARDANDO_INFORMACOES -> {
                    badgeStatus.setBackgroundResource(R.drawable.bg_badge_status_terracotta)
                    badgeStatus.setTextColor(ContextCompat.getColor(root.context, R.color.clay600))
                }
                ManifestationStatus.EM_PROVIDENCIA -> {
                    badgeStatus.setBackgroundResource(R.drawable.bg_chip_unselected)
                    badgeStatus.setTextColor(ContextCompat.getColor(root.context, R.color.pine800))
                }
                ManifestationStatus.CONCLUIDA -> {
                    badgeStatus.setBackgroundResource(R.drawable.bg_badge_status_green)
                    badgeStatus.setTextColor(ContextCompat.getColor(root.context, R.color.pine700))
                }
            }

            cardProtocolRoot.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size
}
