package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityReportChatBinding
import com.example.weanimals.lostandfound.model.ChatReport
import com.example.weanimals.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth

class ReportChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportChatBinding
    private var verificationRequest: TutorVerificationRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Conversa não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        verificationRequest = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (verificationRequest == null) {
            Toast.makeText(this, "Conversa não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBackReport.setOnClickListener { finish() }

        binding.btnSubmitReport.setOnClickListener {
            val checkedId = binding.rgReportReasons.checkedRadioButtonId
            if (checkedId == -1) {
                Toast.makeText(this, "Selecione um motivo para a denúncia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRadio = findViewById<RadioButton>(checkedId)
            val reasonText = selectedRadio.text.toString()
            val descriptionText = binding.etReportDescription.text.toString().trim()

            val req = verificationRequest ?: return@setOnClickListener
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
            val reportedUserId = if (currentUserId == req.ownerUserId) req.requesterUserId else req.ownerUserId

            val report = ChatReport(
                conversationId = req.id,
                announcementId = req.adId,
                reporterUserId = currentUserId,
                reportedUserId = reportedUserId,
                reason = reasonText,
                description = descriptionText.ifBlank { null }
            )

            LostAndFoundRepository.saveChatReport(this, report)

            val intent = Intent(this, ReportSubmittedActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
