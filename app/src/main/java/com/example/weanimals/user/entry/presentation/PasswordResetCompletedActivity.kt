package com.example.weanimals.user.entry.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PasswordResetCompletedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isOrganization = intent?.data?.getQueryParameter("account") == "organization"
        startActivity(
            LoginActivity.newPasswordResetSuccessIntent(this, isOrganization).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }
}
