package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivitySightingReportDetailBinding
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import kotlinx.coroutines.launch

class SightingReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySightingReportDetailBinding
    private var currentRequest: TutorVerificationRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySightingReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Relato não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentRequest = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (currentRequest == null) {
            Toast.makeText(this, "Relato não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.includeHeader.headerTitleTv.text = "Relato de encontro"
        binding.includeHeader.headerBackBtn.setOnClickListener { finish() }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val req = currentRequest ?: return
        val name = req.requesterName?.takeIf { it.isNotBlank() } ?: "Usuário"

        binding.tvSightingRequesterName.text = name
        binding.tvSightingRequesterInitial.text = name.firstOrNull()?.uppercase() ?: "U"
        binding.tvSightingStatusBadge.text = when (req.status.uppercase()) {
            "APROVADO" -> "Aprovado"
            "RECUSADO", "RECUSADA" -> "Recusado"
            else -> "Em análise"
        }

        binding.tvSightingSituation.text = req.petName
        binding.tvSightingDetails.text = req.specialMark

        if (!req.photoUri.isNullOrBlank()) {
            binding.layoutSightingPhotoContainer.visibility = View.VISIBLE
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(
                    this@SightingReportDetailActivity,
                    req.photoUri,
                    200,
                    200
                )
                if (bitmap != null) {
                    binding.ivSightingPhotoThumb.setImageBitmap(bitmap)
                }
            }
        } else {
            binding.layoutSightingPhotoContainer.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.layoutSightingPhotoContainer.setOnClickListener {
            val photoUri = currentRequest?.photoUri ?: return@setOnClickListener
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(photoUri), "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível abrir a imagem", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRejectSighting.setOnClickListener {
            showRejectConfirmationDialog()
        }

        binding.btnApproveSighting.setOnClickListener {
            showApproveConfirmationDialog()
        }
    }

    private fun showRejectConfirmationDialog() {
        val req = currentRequest ?: return
        AlertDialog.Builder(this)
            .setTitle("Não é seu animal?")
            .setMessage("Deseja marcar este relato como recusado?")
            .setPositiveButton("Sim, recusar") { _, _ ->
                LostAndFoundRepository.updateRequestStatus(this, req.id, "RECUSADO")
                req.status = "RECUSADO"
                setupUI()
                Toast.makeText(this, "Relato marcado como recusado.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showApproveConfirmationDialog() {
        val req = currentRequest ?: return
        AlertDialog.Builder(this)
            .setTitle("Aprovar relato?")
            .setMessage("Deseja aceitar este relato e liberar a conversa pelo chat?")
            .setPositiveButton("Pode ser, abrir chat") { _, _ ->
                LostAndFoundRepository.updateRequestStatus(this, req.id, "APROVADO")
                val intent = Intent(this, LostAndFoundChatActivity::class.java).apply {
                    putExtra(LostAndFoundChatActivity.EXTRA_REQUEST_ID, req.id)
                    putExtra(LostAndFoundChatActivity.EXTRA_VIEWER_ROLE, LostAndFoundChatActivity.ROLE_FOUNDER)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
