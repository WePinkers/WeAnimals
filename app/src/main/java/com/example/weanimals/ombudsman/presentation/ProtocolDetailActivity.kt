package com.example.weanimals.ombudsman.presentation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityProtocolDetailBinding
import com.example.weanimals.databinding.ItemTrackingStepBinding
import com.example.weanimals.ombudsman.domain.ManifestationStatus
import com.example.weanimals.ombudsman.domain.OmbudsmanProtocol
import com.example.weanimals.ombudsman.repository.OmbudsmanRepository

class ProtocolDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProtocolDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityProtocolDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDataAndInteractions()
    }

    private fun setupDataAndInteractions() {
        val protocolNumber = intent.getStringExtra("extra_protocol_number") ?: "WA-2026-000123"
        val protocol = OmbudsmanRepository.getProtocolByNumber(protocolNumber)
            ?: OmbudsmanRepository.getProtocols().first()

        binding.btnBack.setOnClickListener { finish() }

        binding.txtProtocolNumber.text = protocol.protocolNumber
        binding.txtMetaSubtitle.text = "${protocol.dateFormatted} · ${protocol.type.displayName}"
        binding.txtSubjectVal.text = protocol.subject
        binding.txtDescriptionVal.text = protocol.description
        binding.badgeStatus.text = protocol.status.displayName

        when (protocol.status) {
            ManifestationStatus.EM_ANALISE -> {
                binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_status_amber)
                binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.brass500))
            }
            ManifestationStatus.AGUARDANDO_INFORMACOES -> {
                binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_status_terracotta)
                binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.clay600))
            }
            ManifestationStatus.EM_PROVIDENCIA -> {
                binding.badgeStatus.setBackgroundResource(R.drawable.bg_chip_unselected)
                binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.pine800))
            }
            ManifestationStatus.CONCLUIDA -> {
                binding.badgeStatus.setBackgroundResource(R.drawable.bg_badge_status_green)
                binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.pine700))
            }
        }

        renderTimeline(protocol)

        binding.btnAddInfo.setOnClickListener {
            Toast.makeText(this, "Adicionar informações complementares ao protocolo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderTimeline(protocol: OmbudsmanProtocol) {
        binding.timelineContainer.removeAllViews()
        protocol.timelineEvents.forEachIndexed { index, event ->
            val stepBinding = ItemTrackingStepBinding.inflate(
                layoutInflater,
                binding.timelineContainer,
                false
            )

            stepBinding.trackingStepTitle.text = event.title
            stepBinding.trackingStepSubtitle.text = event.subtitle
            stepBinding.trackingStepConnector.visibility =
                if (index == protocol.timelineEvents.lastIndex) View.GONE else View.VISIBLE

            if (event.isCompleted) {
                stepBinding.trackingStepMarker.setBackgroundResource(R.drawable.bg_tracking_marker_complete)
                stepBinding.trackingStepMarker.text = "✓"
                stepBinding.trackingStepMarker.setTextColor(ContextCompat.getColor(this, R.color.white))
                stepBinding.trackingStepTitle.setTextColor(ContextCompat.getColor(this, R.color.ink))
            } else {
                stepBinding.trackingStepMarker.setBackgroundResource(R.drawable.bg_tracking_marker_pending)
                stepBinding.trackingStepMarker.text = ""
                stepBinding.trackingStepTitle.setTextColor(ContextCompat.getColor(this, R.color.muted))
            }

            binding.timelineContainer.addView(stepBinding.root)
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
