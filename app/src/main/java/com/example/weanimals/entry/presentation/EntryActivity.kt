package com.example.weanimals.entry.presentation

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityEntryBinding
import com.example.weanimals.home.presentation.HomeActivity
import com.example.weanimals.organization.accreditation.OrganizationAccreditationDraft
import com.google.firebase.auth.FirebaseAuth

class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hasPendingOrganizationDraft = OrganizationAccreditationDraft.hasPending(this)
        if (!hasPendingOrganizationDraft && FirebaseAuth.getInstance().currentUser?.isAnonymous == false) {
            openApp()
            return
        }
        configureSystemBars()
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.organizationLink.paintFlags =
            binding.organizationLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.enterAccountButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, CitizenRegistrationActivity::class.java))
        }
        binding.organizationLink.setOnClickListener {
            startActivity(LoginActivity.newOrganizationIntent(this))
        }
    }

    private fun openApp() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
