package com.example.weanimals.organization.accreditation.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityOrganizationVerificationPendingBinding
import com.example.weanimals.user.entry.presentation.EntryActivity
import com.google.firebase.auth.FirebaseAuth

class OrganizationVerificationPendingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizationVerificationPendingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOrganizationVerificationPendingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseAuth.getInstance().signOut()

        binding.verificationHeader.headerTitle.setText(R.string.verification_title)
        binding.verificationHeader.backButton.setOnClickListener { openEntry() }
        binding.verificationBackButton.setOnClickListener { openEntry() }
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
