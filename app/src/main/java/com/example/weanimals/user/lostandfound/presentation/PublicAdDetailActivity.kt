package com.example.weanimals.user.lostandfound.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.core.utils.ImageThumbnailLoader
import kotlinx.coroutines.launch
import com.example.weanimals.R
import com.example.weanimals.user.map.overview.presentation.PublicAnimalDetails
import com.example.weanimals.databinding.ActivityPublicAdDetailBinding
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PublicAdDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublicAdDetailBinding
    private var currentAd: FoundPetAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicAdDetailBinding.inflate(layoutInflater)
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
        val isLost = ad.category.equals("PERDIDO", ignoreCase = true)

        // Status Badge
        binding.tvStatusBadgePublic.text = if (isLost) "Perdido" else "Encontrado"

        // Title
        binding.tvTitlePublic.text = ad.getDisplayTitle()

        // Subtitle
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val pubDateStr = dateFormat.format(Date(ad.publicationTimestamp))
        val verb = if (isLost) "desaparecido em" else "encontrado em"
        binding.tvSubtitlePublic.text = "${PublicAnimalDetails.location(ad)} · $verb ${PublicAnimalDetails.date(ad)}"

        // Photo or Paw
        if (ad.photoUris.isNotEmpty()) {
            val photoUri = ad.photoUris.first()
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(this@PublicAdDetailActivity, photoUri, 600, 400)
                if (bitmap != null) {
                    binding.ivPetPhotoPublic.setImageBitmap(bitmap)
                    binding.ivPetPhotoPublic.visibility = View.VISIBLE
                    binding.containerPawPublic.visibility = View.GONE
                } else {
                    binding.ivPetPhotoPublic.visibility = View.GONE
                    binding.containerPawPublic.visibility = View.VISIBLE
                }
            }
        } else {
            binding.ivPetPhotoPublic.visibility = View.GONE
            binding.containerPawPublic.visibility = View.VISIBLE
        }

        // Capsules
        renderCapsules(ad)

        // Section: MARCAS QUE DIFERENCIAM
        val marks = ad.marks?.trim()
        if (!marks.isNullOrBlank()) {
            val cleanPhysicalMarks = PublicAnimalDetails.marks(ad)
            if (cleanPhysicalMarks.isNotBlank()) {
                binding.layoutMarksSection.visibility = View.VISIBLE
                binding.tvMarksTextPublic.text = cleanPhysicalMarks
            } else {
                binding.layoutMarksSection.visibility = View.GONE
            }
        } else {
            binding.layoutMarksSection.visibility = View.GONE
        }

        // Section: LOCALIZAÇÃO DO ANIMAL
        binding.tvLocationTitlePublic.text = if (isLost) "ONDE PROCURAR" else "ONDE FOI ENCONTRADO"

        binding.tvLocationTextPublic.text = PublicAnimalDetails.location(ad)
        binding.tvLocationTextPublic.visibility = View.VISIBLE
        binding.tvComplementTextPublic.visibility = View.GONE

        val situationSubtext = when (ad.currentSituation.lowercase().trim()) {
            "comigo" -> "Última situação informada: está com quem encontrou"
            "ainda na rua" -> "Última situação informada: ainda na rua"
            else -> "Última situação informada: ${ad.currentSituation}"
        }
        binding.tvSituationTextPublic.text = situationSubtext

        // Section: PUBLICADO POR / TUTOR
        binding.tvPublisherTitlePublic.text = if (isLost) "TUTOR" else "PUBLICADO POR"
        val authorName = ad.publisherName?.takeIf { it.isNotBlank() } ?: "Usuário"
        binding.tvPublisherTextPublic.text = "$authorName · publicou em $pubDateStr"

        // Footer Main Button Text
        binding.btnMainActionPublic.text = if (isLost) "Acho que encontrei" else "Acho que é meu"
    }

    override fun onResume() {
        super.onResume()
        val ad = currentAd?.let { LostAndFoundRepository.getAdById(this, it.id) } ?: return
        currentAd = ad
        val action = AdActionNavigator.action(this, ad)
        binding.btnMainActionPublic.isEnabled =
            action != AdActionNavigator.Action.OWN_AD && action != AdActionNavigator.Action.INACTIVE
        binding.btnMainActionPublic.setText(when (action) {
            AdActionNavigator.Action.OWN_AD -> R.string.map_animal_own_ad
            AdActionNavigator.Action.STATUS -> R.string.map_animal_request_status
            AdActionNavigator.Action.CHAT -> R.string.map_animal_chat
            else -> if (ad.category.equals("PERDIDO", true)) R.string.map_animal_sighting
                else R.string.map_animal_verification
        })
    }

    private fun renderCapsules(ad: FoundPetAd) {
        val container = binding.containerCapsulesPublic
        container.removeAllViews()

        val items = listOfNotNull(
            ad.species.takeIf { it.isNotBlank() },
            ad.size?.takeIf { it.isNotBlank() }?.let { "Porte $it" },
            ad.ageApprox?.takeIf { it.isNotBlank() },
            ad.condition?.takeIf { it.isNotBlank() }
        )

        for (item in items) {
            val tv = TextView(this).apply {
                text = item
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
                setBackgroundResource(R.drawable.bg_capsule_unselected)
                setPadding(28, 10, 28, 10)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 12, 0)
                layoutParams = params
            }
            container.addView(tv)
        }
    }

    private fun setupListeners() {
        binding.btnBackPublic.setOnClickListener { finish() }
        binding.btnBackFooterPublic.setOnClickListener { finish() }

        binding.btnSharePublic.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val shareText = "Confira este anúncio no WeAnimals: ${ad.getDisplayTitle()} - ${ad.getDisplayLocation()}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "Compartilhar anúncio"))
        }

        binding.btnCopyLinkPublic.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val url = "https://weanimals-642a9.web.app/ad/${ad.id}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Link do Anúncio", url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Link do anúncio copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
        }

        binding.tvReportAdPublic.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val intent = Intent(this, ReportAdActivity::class.java).apply {
                putExtra(ReportAdActivity.EXTRA_AD_ID, ad.id)
            }
            startActivity(intent)
        }

        binding.btnMainActionPublic.setOnClickListener {
            currentAd?.let { AdActionNavigator.open(this, it.id) }
        }
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
