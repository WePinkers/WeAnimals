package com.example.weanimals.user.entry.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.core.session.SessionAccountType
import com.example.weanimals.databinding.ActivityEntryBinding
import com.example.weanimals.user.home.presentation.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser?.isAnonymous == false) {
            routeAuthenticatedSession()
            return
        }
        SessionAccountType.clear(this)

        configureSystemBars()
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enterAccountButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, CitizenRegistrationActivity::class.java))
        }
    }

    private fun routeAuthenticatedSession() {
        SessionAccountType.rememberCitizen(this)
        openApp()
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
