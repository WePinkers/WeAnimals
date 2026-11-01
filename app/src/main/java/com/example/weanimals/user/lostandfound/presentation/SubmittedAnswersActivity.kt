package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivitySubmittedAnswersBinding
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository

class SubmittedAnswersActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubmittedAnswersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmittedAnswersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeHeader.headerTitleTv.text = "Relato enviado"
        binding.includeHeader.headerBackBtn.setOnClickListener { finish() }

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val request = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (request == null) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val ad = LostAndFoundRepository.getAdById(this, request.adId)

        binding.tvAnswerName.text = "Anúncio: ${ad?.getDisplayTitle() ?: "Animal"}"
        binding.tvAnswerMark.text = "Informações: ${request.specialMark}"

        val statusUpper = request.status.uppercase()
        when (statusUpper) {
            "APROVADO" -> {
                binding.tvSubmittedStatusBadge.text = "Aprovada"
                binding.tvSubmittedStatusBadge.setBackgroundResource(R.drawable.bg_btn_dark_green)
                binding.tvSubmittedStatusBadge.setTextColor(getColor(R.color.white))
            }
            "RECUSADO", "RECUSADA" -> {
                binding.tvSubmittedStatusBadge.text = "Recusada"
                binding.tvSubmittedStatusBadge.setBackgroundResource(R.drawable.bg_bar_red)
                binding.tvSubmittedStatusBadge.setTextColor(getColor(R.color.alert600))
            }
            else -> {
                binding.tvSubmittedStatusBadge.text = "Em análise"
                binding.tvSubmittedStatusBadge.setBackgroundResource(R.drawable.bg_bar_yellow)
                binding.tvSubmittedStatusBadge.setTextColor(getColor(R.color.ink))
            }
        }

        if (!request.photoUri.isNullOrBlank()) {
            binding.layoutPhotoAttached.visibility = View.VISIBLE
        } else {
            binding.layoutPhotoAttached.visibility = View.GONE
        }

        val ownTestRequest = LostAndFoundRepository.canTestOwnAnnouncement(this) &&
            request.ownerUserId == request.requesterUserId &&
            request.ownerUserId == (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "local_user")
        binding.btnReviewTestRequest.visibility = if (ownTestRequest && request.status.equals("PENDENTE", true))
            View.VISIBLE else View.GONE
        binding.btnReviewTestRequest.setOnClickListener {
            val destination = if (ad?.category.equals("PERDIDO", true))
                SightingReportDetailActivity::class.java else TutorRequestDetailActivity::class.java
            startActivity(android.content.Intent(this, destination).putExtra(EXTRA_REQUEST_ID, request.id))
            finish()
        }

        binding.btnBackToMyAds.setOnClickListener {
            finish()
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
