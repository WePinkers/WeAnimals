package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityConfirmReunionBinding
import com.example.weanimals.lostandfound.model.ReunionRequest
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository

class ConfirmReunionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmReunionBinding
    private var reunionRequest: ReunionRequest? = null
    private var selectedYes = false
    private var selectedNo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmReunionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        reunionRequest = LostAndFoundRepository.getReunionForRequest(this, requestId)
        if (reunionRequest == null) {
            Toast.makeText(this, "Solicitação não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBackConfirmReunion.setOnClickListener { finish() }

        binding.cardConfirmYes.setOnClickListener {
            selectedYes = true
            selectedNo = false
            binding.radioConfirmYes.isChecked = true
            binding.radioConfirmNo.isChecked = false
        }

        binding.cardConfirmNo.setOnClickListener {
            selectedYes = false
            selectedNo = true
            binding.radioConfirmYes.isChecked = false
            binding.radioConfirmNo.isChecked = true
        }

        binding.btnConfirmReunionAction.setOnClickListener {
            if (!selectedYes && !selectedNo) {
                Toast.makeText(this, "Selecione uma das opções", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val r = reunionRequest ?: return@setOnClickListener
            if (selectedYes) {
                LostAndFoundRepository.updateReunionStatus(this, r.reunionId, "CONFIRMED")
                Toast.makeText(this, "Reencontro confirmado!", Toast.LENGTH_LONG).show()
            } else {
                LostAndFoundRepository.updateReunionStatus(this, r.reunionId, "NOT_CONFIRMED")
                Toast.makeText(this, "Resposta registrada: ainda não aconteceu.", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
