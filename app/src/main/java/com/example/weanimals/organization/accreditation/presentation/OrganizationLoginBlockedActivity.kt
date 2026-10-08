package com.example.weanimals.organization.accreditation.presentation

import android.content.Intent
import android.os.Bundle
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityOrganizationLoginBlockedBinding
import com.example.weanimals.user.entry.presentation.EntryActivity

class OrganizationLoginBlockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizationLoginBlockedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOrganizationLoginBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.blockedHeader.headerTitle.setText(R.string.login_blocked_header)
        binding.blockedHeader.backButton.setOnClickListener { openEntry() }
        binding.blockedResubmitButton.setOnClickListener { openAccreditation() }
        binding.blockedBackToEntry.paintFlags =
            binding.blockedBackToEntry.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.blockedBackToEntry.setOnClickListener { openEntry() }
    }

    private fun openAccreditation() {
        startActivity(Intent(this, OrganizationAccreditationActivity::class.java))
    }

    private fun openEntry() {
        startActivity(
            Intent(this, EntryActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}