package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.user.map.overview.presentation.MapActivity

class LostAndFoundMapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val focusAdId = intent.getStringExtra(EXTRA_FOCUS_AD_ID)
        val nextIntent = MapActivity.newIntent(this, MapActivity.FILTER_LOST_FOUND, focusAdId)
        startActivity(nextIntent)
        finish()
    }

    companion object {
        const val EXTRA_FOCUS_AD_ID = "extra_focus_ad_id"
    }
}
