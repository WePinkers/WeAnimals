package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityRegisterReunionBinding
import com.example.weanimals.user.lostandfound.model.ReunionRequest
import com.example.weanimals.user.lostandfound.model.TutorVerificationRequest
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.google.firebase.auth.FirebaseAuth

class RegisterReunionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterReunionBinding
    private var verificationRequest: TutorVerificationRequest? = null
    private var selectedOptionYes = false
    private var selectedOptionNo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterReunionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenHeader.headerTitle.text = "Registrar reencontro"

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        verificationRequest = LostAndFoundRepository.getVerificationRequestById(this, requestId)
        if (verificationRequest == null) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.cardOptionYes.setOnClickListener {
            selectedOptionYes = true
            selectedOptionNo = false
            binding.radioYes.isChecked = true
            binding.radioNo.isChecked = false
        }

        binding.cardOptionNo.setOnClickListener {
            selectedOptionYes = false
            selectedOptionNo = true
            binding.radioYes.isChecked = false
            binding.radioNo.isChecked = true
        }

        binding.btnContinueRegisterReunion.setOnClickListener {
            if (!selectedOptionYes && !selectedOptionNo) {
                Toast.makeText(this, "Selecione uma das opções", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedOptionNo) {
                finish()
                return@setOnClickListener
            }

            val req = verificationRequest ?: return@setOnClickListener
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
            val confirmingUserId = if (currentUserId == req.ownerUserId) req.requesterUserId else req.ownerUserId

            val reunion = ReunionRequest(
                announcementId = req.adId,
                requestId = req.id,
                initiatedByUserId = currentUserId,
                confirmingUserId = confirmingUserId,
                status = "PENDING_CONFIRMATION"
            )

            LostAndFoundRepository.saveReunionRequest(this, reunion)

            val intent = Intent(this, ReunionSubmittedActivity::class.java).apply {
                putExtra(ReunionSubmittedActivity.EXTRA_REQUEST_ID, req.id)
            }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
