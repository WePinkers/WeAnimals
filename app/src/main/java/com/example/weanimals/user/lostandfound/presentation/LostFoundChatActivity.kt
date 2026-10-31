package com.example.weanimals.user.lostandfound.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.user.reporting.chat.presentation.CaseChatActivity

/** Entry point kept for the lost-and-found flow, backed by the app's XML/MVP chat. */
class LostFoundChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            CaseChatActivity.newIntent(
                context = this,
                reportId = intent.getStringExtra(EXTRA_REPORT_ID).orEmpty(),
                protocolNumber = intent.getIntExtra(EXTRA_PROTOCOL_NUMBER, 0)
            )
        )
        finish()
    }

    companion object {
        private const val EXTRA_REPORT_ID = "extra_report_id"
        private const val EXTRA_PROTOCOL_NUMBER = "extra_protocol_number"

        fun newIntent(context: Context, reportId: String, protocolNumber: Int): Intent =
            Intent(context, LostFoundChatActivity::class.java).apply {
                putExtra(EXTRA_REPORT_ID, reportId)
                putExtra(EXTRA_PROTOCOL_NUMBER, protocolNumber)
            }
    }
}
