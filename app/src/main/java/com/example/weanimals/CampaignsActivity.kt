package com.example.weanimals

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityCampaignsBinding
import com.example.weanimals.databinding.ItemCampaignBinding

class CampaignsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCampaignsBinding
    private var isActiveExpanded = true
    private var isCompletedExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampaignsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.headerActive.setOnClickListener {
            isActiveExpanded = !isActiveExpanded
            binding.containerActive.visibility = if (isActiveExpanded) View.VISIBLE else View.GONE
            binding.arrowActive.animate().rotation(if (isActiveExpanded) 0f else -90f).setDuration(200).start()
        }

        binding.headerCompleted.setOnClickListener {
            isCompletedExpanded = !isCompletedExpanded
            binding.containerCompleted.visibility = if (isCompletedExpanded) View.VISIBLE else View.GONE
            binding.arrowCompleted.animate().rotation(if (isCompletedExpanded) 0f else -90f).setDuration(200).start()
        }
        
        setupCampaigns()
    }

    private fun setupCampaigns() {
        // Setup Active
        binding.active1.apply {
            campaignTitle.text = getString(R.string.campaign_adoption_title)
            campaignDesc.text = "Participação confirmada para 12/09"
            campaignAddress.text = getString(R.string.campaign_adoption_address)
            campaignContact.text = getString(R.string.campaign_adoption_contact)
            campaignStatus.text = getString(R.string.campaign_status_active)
        }
        binding.active2.apply {
            campaignTitle.text = getString(R.string.campaign_items_title)
            campaignDesc.text = "Coleta ativa em pontos parceiros"
            campaignAddress.text = getString(R.string.campaign_items_address)
            campaignContact.text = getString(R.string.campaign_items_contact)
            campaignStatus.text = getString(R.string.campaign_status_active)
        }

        // Setup Completed
        val completedIncludes = listOf(
            binding.completed1, binding.completed2, binding.completed3, binding.completed4,
            binding.completed5, binding.completed6, binding.completed7, binding.completed8,
            binding.completed9, binding.completed10, binding.completed11, binding.completed12
        )

        completedIncludes.forEachIndexed { index, include ->
            include.campaignTitle.text = if (index % 2 == 0) "Campanha de Vacinação" else "Doação de Rações"
            include.campaignDesc.text = "Finalizada em Agosto 2024"
            include.campaignAddress.text = "Evento encerrado"
            include.campaignContact.text = "Agradecemos sua participação!"
            include.campaignStatus.text = getString(R.string.campaign_status_completed)
            include.campaignStatus.alpha = 0.6f
        }
    }
}
