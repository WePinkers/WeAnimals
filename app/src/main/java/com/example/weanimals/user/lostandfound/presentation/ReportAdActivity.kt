package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivityReportAdBinding
import com.example.weanimals.user.lostandfound.model.AnnouncementReport
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ReportAdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportAdBinding
    private var currentAd: FoundPetAd? = null
    private var evidenceUri: Uri? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                evidenceUri = uri
                loadEvidencePreview(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportAdBinding.inflate(layoutInflater)
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

        binding.tvReportAdTitle.text = ad.getDisplayTitle()
        binding.tvReportAdLocation.text = ad.getDisplayLocation()

        if (ad.photoUris.isNotEmpty()) {
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(this@ReportAdActivity, ad.photoUris.first(), 120, 120)
                if (bitmap != null) {
                    binding.ivReportAdThumb.setImageBitmap(bitmap)
                    binding.ivReportAdThumb.visibility = View.VISIBLE
                    binding.ivReportAdPaw.visibility = View.GONE
                }
            }
        }
    }

    private fun loadEvidencePreview(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = ImageThumbnailLoader.loadThumbnail(this@ReportAdActivity, uri.toString(), 400, 300)
            if (bitmap != null) {
                binding.ivAdReportEvidencePreview.setImageBitmap(bitmap)
                binding.ivAdReportEvidencePreview.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        binding.screenHeader.headerTitle.text = "Denunciar anúncio"
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.layoutAdReportEvidence.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        binding.btnSubmitAdReport.setOnClickListener {
            val ad = currentAd ?: return@setOnClickListener
            val checkedId = binding.rgAdReportReasons.checkedRadioButtonId
            if (checkedId == -1) {
                Toast.makeText(this, "Selecione um motivo para a denúncia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRadio = findViewById<RadioButton>(checkedId)
            val reasonText = selectedRadio.text.toString()
            val descriptionText = binding.etAdReportDescription.text.toString().trim()

            if (reasonText.contains("Outro motivo", ignoreCase = true) && descriptionText.isBlank()) {
                binding.etAdReportDescription.error = "Descreva o problema para este motivo."
                Toast.makeText(this, "Descreva o problema para este motivo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"

            val report = AnnouncementReport(
                announcementId = ad.id,
                announcementCategory = ad.category,
                reporterUserId = currentUserId,
                reportedUserId = ad.ownerUserId,
                reason = reasonText,
                description = descriptionText.ifBlank { null },
                evidenceUri = evidenceUri?.toString()
            )

            LostAndFoundRepository.saveAnnouncementReport(this, report)

            val intent = Intent(this, AdReportSubmittedActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_AD_ID = "extra_ad_id"
    }
}
