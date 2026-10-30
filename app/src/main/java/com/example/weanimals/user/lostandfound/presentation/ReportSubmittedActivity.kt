package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityReportSubmittedBinding

class ReportSubmittedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportSubmittedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportSubmittedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackReportSubmitted.setOnClickListener { finish() }
        binding.btnBackToConversation.setOnClickListener { finish() }
    }
}
