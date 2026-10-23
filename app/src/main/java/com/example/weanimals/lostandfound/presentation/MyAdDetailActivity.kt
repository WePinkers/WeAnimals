package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityMyAdDetailBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAdDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAdDetailBinding
    private var currentAd: FoundPetAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAdDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adId = intent.getStringExtra(EXTRA_AD_ID)
        if (adId.isNullOrBlank()) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentAd = LostAndFoundRepository.getAdById(this, adId)
        if (currentAd == null) {
            Toast.makeText(this, "Anúncio não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val ad = currentAd ?: return

        binding.tvStatusBadge.text = ad.currentSituation

        binding.tvAdTitle.text = ad.getDisplayTitle()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val pubDateStr = dateFormat.format(Date(ad.publicationTimestamp))
        binding.tvAdSubtitle.text = "${ad.getDisplayLocation()} · publicado em $pubDateStr"

        // Card 1: Remaining Days
        val remainingDays = ad.getRemainingDays()
        binding.tvRemainingDays.text = ad.getRemainingDaysText()
        binding.pbDaysRemaining.progress = remainingDays

        // Card 2: Tutors
        val tutors = ad.possibleTutorsCount
        binding.tvTutorsTitle.text = when (tutors) {
            0 -> "0 possíveis tutores"
            1 -> "1 possível tutor"
            else -> "$tutors possíveis tutores"
        }

        binding.tvTutorsPhrase.text = when (tutors) {
            0 -> "Ainda não há possíveis tutores."
            1 -> "Já existe 1 possível tutor interessado."
            else -> "Já existem $tutors possíveis tutores interessados."
        }
    }

    private fun setupListeners() {
        binding.btnBackDetail.setOnClickListener { finish() }

        binding.btnViewTutors.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val intent = Intent(this, PossibleTutorsActivity::class.java).apply {
                putExtra(PossibleTutorsActivity.EXTRA_AD_ID, ad.id)
            }
            startActivity(intent)
        }

        binding.btnUpdateSituation.setOnClickListener {
            showUpdateSituationDialog()
        }

        binding.btnEndAd.setOnClickListener {
            showEndAdConfirmationDialog()
        }
    }

    private fun showUpdateSituationDialog() {
        val ad = currentAd ?: return
        val options = arrayOf("Comigo", "Ainda na rua", "Entregue ao tutor", "Encerrado")
        val currentIndex = options.indexOf(ad.currentSituation).let { if (it >= 0) it else 0 }

        AlertDialog.Builder(this)
            .setTitle("Atualizar situação")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val newSituation = options[which]
                LostAndFoundRepository.updateAdSituation(this, ad.id, newSituation)
                ad.currentSituation = newSituation
                setupUI()
                Toast.makeText(this, "Situação atualizada para: $newSituation", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEndAdConfirmationDialog() {
        val ad = currentAd ?: return
        AlertDialog.Builder(this)
            .setTitle("Encerrar anúncio?")
            .setMessage("Deseja realmente encerrar este anúncio? Esta ação alterará o status para Encerrado.")
            .setPositiveButton("Encerrar") { _, _ ->
                LostAndFoundRepository.endAd(this, ad.id)
                ad.isEnded = true
                ad.currentSituation = "Encerrado"
                setupUI()
                Toast.makeText(this, "Anúncio encerrado com sucesso", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
