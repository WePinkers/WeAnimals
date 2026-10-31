package com.example.weanimals.user.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityAccountIssueSentBinding

class AccountIssueSentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountIssueSentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityAccountIssueSentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDataAndInteractions()
    }

    private fun setupDataAndInteractions() {
        val issueType = intent.getStringExtra("extra_issue_type") ?: "Outro problema"
        binding.txtReportedIssue.text = issueType

        binding.btnBackToSupport.setOnClickListener {
            val intent = Intent(this, HelpAndSupportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
