package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityReunionSubmittedBinding

class ReunionSubmittedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReunionSubmittedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReunionSubmittedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackReunionSubmitted.setOnClickListener { finish() }
        binding.btnBackToChat.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}
