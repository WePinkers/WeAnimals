package com.example.weanimals.lostandfound.presentation

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
import com.example.weanimals.databinding.ActivityPublicAdDetailBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
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
        binding.tvSubtitlePublic.text = "${ad.getDisplayLocation()} · $verb $pubDateStr"

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
            binding.layoutMarksSection.visibility = View.VISIBLE
            binding.tvMarksTextPublic.text = marks
        } else {
            binding.layoutMarksSection.visibility = View.GONE
        }

        // Section: LOCALIZAÇÃO DO ANIMAL
        binding.tvLocationTitlePublic.text = if (isLost) "ONDE PROCURAR" else "ONDE FOI ENCONTRADO"

        val addressLines = mutableListOf<String>()
        val streetStr = ad.street?.trim().orEmpty()
        val numStr = ad.number?.trim().orEmpty()
        if (streetStr.isNotBlank()) {
            val line1 = if (numStr.isNotBlank() && !numStr.equals("Sem número", ignoreCase = true)) {
                "$streetStr, $numStr"
            } else {
                streetStr
            }
            addressLines.add(line1)
        }

        val neighStr = ad.neighborhood?.trim().orEmpty()
        val cityStr = ad.city?.trim().orEmpty()
        val stateStr = ad.state?.trim().orEmpty()
        val locParts = listOfNotNull(
            neighStr.takeIf { it.isNotBlank() },
            listOfNotNull(cityStr.takeIf { it.isNotBlank() }, stateStr.takeIf { it.isNotBlank() })
                .joinToString(" - ")
                .takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        if (locParts.isNotBlank()) {
            addressLines.add(locParts)
        }

        if (addressLines.isNotEmpty()) {
            binding.tvLocationTextPublic.text = addressLines.joinToString("\n")
            binding.tvLocationTextPublic.visibility = View.VISIBLE
        } else {
            binding.tvLocationTextPublic.text = ad.getDisplayLocation()
            binding.tvLocationTextPublic.visibility = View.VISIBLE
        }

        val complement = ad.complement?.trim().orEmpty()
        if (complement.isNotBlank()) {
            binding.tvComplementTextPublic.text = complement
            binding.tvComplementTextPublic.visibility = View.VISIBLE
        } else {
            binding.tvComplementTextPublic.visibility = View.GONE
        }

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
            Toast.makeText(this, "Fluxo de denúncia em desenvolvimento.", Toast.LENGTH_SHORT).show()
        }

        binding.btnMainActionPublic.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val intent = Intent(this, TutorVerificationActivity::class.java).apply {
                putExtra(TutorVerificationActivity.EXTRA_AD_ID, ad.id)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
