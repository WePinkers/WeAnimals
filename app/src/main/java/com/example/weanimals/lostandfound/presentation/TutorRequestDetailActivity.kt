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
import com.example.weanimals.databinding.ActivityTutorRequestDetailBinding
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import kotlinx.coroutines.launch

class TutorRequestDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorRequestDetailBinding
    private var currentRequest: TutorVerificationRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorRequestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentRequest = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (currentRequest == null) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val req = currentRequest ?: return
        val name = req.requesterName?.takeIf { it.isNotBlank() } ?: "Usuário"

        binding.tvRequesterName.text = name
        binding.tvRequesterInitial.text = name.firstOrNull()?.uppercase() ?: "U"
        binding.tvRequestStatusBadge.text = when (req.status.uppercase()) {
            "APROVADO" -> "Aprovado"
            "RECUSADO", "RECUSADA" -> "Recusado"
            else -> "Em análise"
        }

        binding.tvReqName.text = "Nome informado: ${req.petName}"
        binding.tvReqMark.text = "Marca informada: ${req.specialMark}"

        if (!req.photoUri.isNullOrBlank()) {
            binding.layoutReqPhotoContainer.visibility = View.VISIBLE
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(
                    this@TutorRequestDetailActivity,
                    req.photoUri,
                    200,
                    200
                )
                if (bitmap != null) {
                    binding.ivReqPhotoThumb.setImageBitmap(bitmap)
                }
            }
        } else {
            binding.layoutReqPhotoContainer.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnBackRequestDetail.setOnClickListener { finish() }

        binding.layoutReqPhotoContainer.setOnClickListener {
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

        binding.tvReportRequest.setOnClickListener {
            Toast.makeText(this, "Fluxo de denúncia em desenvolvimento.", Toast.LENGTH_SHORT).show()
        }

        binding.btnRejectRequest.setOnClickListener {
            showRejectConfirmationDialog()
        }

        binding.btnApproveRequest.setOnClickListener {
            showApproveConfirmationDialog()
        }
    }

    private fun showRejectConfirmationDialog() {
        val req = currentRequest ?: return
        AlertDialog.Builder(this)
            .setTitle("Não confere?")
            .setMessage("Deseja marcar esta solicitação como recusada?")
            .setPositiveButton("Sim, recusar") { _, _ ->
                LostAndFoundRepository.updateRequestStatus(this, req.id, "RECUSADO")
                req.status = "RECUSADO"
                setupUI()
                Toast.makeText(this, "Solicitação marcada como recusada.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showApproveConfirmationDialog() {
        val req = currentRequest ?: return
        AlertDialog.Builder(this)
            .setTitle("Aprovar pedido?")
            .setMessage("Confere! Deseja aprovar este pedido e liberar o chat com o tutor?")
            .setPositiveButton("Aprovar e abrir chat") { _, _ ->
                LostAndFoundRepository.updateRequestStatus(this, req.id, "APROVADO")
                req.status = "APROVADO"
                setupUI()
                Toast.makeText(this, "Solicitação aprovada! Chat liberado.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
