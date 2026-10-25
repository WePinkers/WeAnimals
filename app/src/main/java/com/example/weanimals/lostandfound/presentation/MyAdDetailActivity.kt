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

        // Card 2: Possíveis Encontros / Possíveis Tutores
        if (ad.category.equals("PERDIDO", ignoreCase = true)) {
            val requests = LostAndFoundRepository.getVerificationRequestsForAd(this, ad.id)
            val sightingCount = requests.size

            binding.tvTutorsTitle.text = when (sightingCount) {
                0 -> "0 possíveis encontros"
                1 -> "1 possível encontro"
                else -> "$sightingCount possíveis encontros"
            }

            binding.tvTutorsPhrase.text = if (sightingCount > 0) {
                "Relatos enviados por pessoas que acreditam ter encontrado seu animal."
            } else {
                "Ninguém relatou ter encontrado este animal ainda."
            }

            binding.btnViewTutors.text = "Ver possíveis encontros"
        } else {
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
            binding.btnViewTutors.text = "Ver possíveis tutores"
        }
    }

    private fun setupListeners() {
        binding.btnBackDetail.setOnClickListener { finish() }

        binding.btnViewTutors.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            if (ad.category.equals("PERDIDO", ignoreCase = true)) {
                val intent = Intent(this, PossibleSightingsActivity::class.java).apply {
                    putExtra(PossibleSightingsActivity.EXTRA_AD_ID, ad.id)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, PossibleTutorsActivity::class.java).apply {
                    putExtra(PossibleTutorsActivity.EXTRA_AD_ID, ad.id)
                }
                startActivity(intent)
            }
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
        if (ad.category.equals("PERDIDO", ignoreCase = true)) {
            showUpdateSituationForLostAd(ad)
        } else {
            showUpdateSituationForFoundAd(ad)
        }
    }

    private fun showUpdateSituationForFoundAd(ad: FoundPetAd) {
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

    private fun showUpdateSituationForLostAd(ad: FoundPetAd) {
        val options = arrayOf(
            "Ainda procurando\n(Meu animal continua desaparecido)",
            "Possivelmente localizado\n(Encontrei um possível animal correspondente, mas ainda não confirmei)",
            "Encontrei meu animal\n(Meu animal voltou para mim. Registrar reencontro)",
            "Encerrar anúncio\n(Quero encerrar a busca por outro motivo)"
        )

        val keys = arrayOf("Ainda procurando", "Possivelmente localizado", "Encontrei meu animal", "Encerrar anúncio")
        val currentIndex = keys.indexOf(ad.currentSituation).let { if (it >= 0) it else 0 }

        AlertDialog.Builder(this)
            .setTitle("Atualizar situação")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> {
                        LostAndFoundRepository.updateAdSituation(this, ad.id, "Ainda procurando")
                        ad.currentSituation = "Ainda procurando"
                        setupUI()
                        Toast.makeText(this, "Situação atualizada: Ainda procurando", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        LostAndFoundRepository.updateAdSituation(this, ad.id, "Possivelmente localizado")
                        ad.currentSituation = "Possivelmente localizado"
                        setupUI()
                        Toast.makeText(this, "Situação atualizada: Possivelmente localizado", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        handleFoundMyPetOption(ad)
                    }
                    3 -> {
                        showEndAdConfirmationDialog()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleFoundMyPetOption(ad: FoundPetAd) {
        val choices = arrayOf(
            "Reencontro através do WeAnimals\n(Confirmar entrega com quem encontrou pelo chat)",
            "Encontrei por conta própria\n(O animal voltou sem utilizar o chat do app)"
        )

        AlertDialog.Builder(this)
            .setTitle("Como você encontrou seu animal?")
            .setItems(choices) { _, which ->
                if (which == 0) {
                    val requests = LostAndFoundRepository.getVerificationRequestsForAd(this, ad.id)
                    val approvedReq = requests.firstOrNull { it.status.equals("APROVADO", ignoreCase = true) }
                    if (approvedReq != null) {
                        val intent = Intent(this, LostAndFoundChatActivity::class.java).apply {
                            putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, approvedReq.id)
                            putExtra(LostAndFoundChatActivity.EXTRA_VIEWER_ROLE, LostAndFoundChatActivity.ROLE_TUTOR)
                        }
                        startActivity(intent)
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("Nenhuma conversa aprovada")
                            .setMessage("Não encontramos nenhuma conversa aprovada no app para este anúncio. Deseja registrar o reencontro por conta própria?")
                            .setPositiveButton("Sim, por conta própria") { _, _ ->
                                confirmSelfFoundReunion(ad)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                } else {
                    confirmSelfFoundReunion(ad)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmSelfFoundReunion(ad: FoundPetAd) {
        AlertDialog.Builder(this)
            .setTitle("Encontrei meu animal")
            .setMessage("Você confirma que encontrou seu animal e que ele está em segurança?")
            .setPositiveButton("Sim, confirmar reencontro") { _, _ ->
                LostAndFoundRepository.updateAdSituation(this, ad.id, "Reencontrado")
                ad.currentSituation = "Reencontrado"
                ad.isReunited = true
                ad.isEnded = true
                setupUI()
                Toast.makeText(this, "Reencontro confirmado! Anúncio atualizado como Reencontrado.", Toast.LENGTH_LONG).show()
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
