package com.example.weanimals.user.entry.presentation

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.session.SessionAccountType
import com.example.weanimals.databinding.ActivityEntryBinding
import com.example.weanimals.organization.accreditation.OrganizationAccreditationDraft
import com.example.weanimals.organization.dashboard.presentation.OrganizationReportsActivity
import com.example.weanimals.user.entry.auth.repository.FirebaseAuthRepository
import com.example.weanimals.user.home.presentation.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser?.isAnonymous == false) {
            routeAuthenticatedSession(currentUser)
            return
        }
        SessionAccountType.clear(this)

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

    private fun routeAuthenticatedSession(user: FirebaseUser) {
        when (SessionAccountType.get(this)) {
            SessionAccountType.Type.ORGANIZATION -> openOrganizationPanel()
            SessionAccountType.Type.CITIZEN -> openApp()
            null -> lifecycleScope.launch {
                val isOrganization = withTimeoutOrNull(SESSION_LOOKUP_TIMEOUT_MS) {
                    detectOrganizationSession(user)
                } ?: false
                if (isOrganization) {
                    SessionAccountType.rememberOrganization(this@EntryActivity)
                    openOrganizationPanel()
                } else {
                    SessionAccountType.rememberCitizen(this@EntryActivity)
                    openApp()
                }
            }
        }
    }

    private suspend fun detectOrganizationSession(user: FirebaseUser): Boolean {
        val localDraftEmail = OrganizationAccreditationDraft.load(this)?.email
        if (localDraftEmail.equals(user.email, ignoreCase = true)) return true

        return runCatching {
            val firestore = FirebaseFirestore.getInstance()
            val identifier = firestore
                .collection(FirebaseAuthRepository.ORGANIZATION_IDENTIFIERS_COLLECTION)
                .whereEqualTo("userId", user.uid)
                .limit(1)
                .get()
                .await()
            if (!identifier.isEmpty) return@runCatching true

            firestore
                .collection(FirebaseAuthRepository.ORGANIZATION_VERIFICATION_REQUESTS_COLLECTION)
                .whereEqualTo("requesterId", user.uid)
                .limit(1)
                .get()
                .await()
                .isEmpty
                .not()
        }.getOrDefault(false)
    }

    private fun openApp() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun openOrganizationPanel() {
        startActivity(
            Intent(this, OrganizationReportsActivity::class.java).addFlags(
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

    companion object {
        private const val SESSION_LOOKUP_TIMEOUT_MS = 5_000L
    }
}
