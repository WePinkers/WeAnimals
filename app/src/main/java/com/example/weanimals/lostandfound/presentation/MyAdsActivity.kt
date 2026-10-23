package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.core.utils.ImageThumbnailLoader
import kotlinx.coroutines.launch
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityMyAdsBinding
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAdsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadAds()
    }

    private fun loadAds() {
        val ads = LostAndFoundRepository.getAllAds(this)
        val container = binding.containerAdsList
        container.removeAllViews()

        if (ads.isEmpty()) {
            binding.tvAdsSummary.text = "Você ainda não possui anúncios publicados."
            return
        }

        binding.tvAdsSummary.text = "Exibindo ${ads.size} ${if (ads.size == 1) "anúncio publicado" else "anúncios publicados"}:"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

        for (ad in ads) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 32, 32, 32)
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setBackgroundResource(R.drawable.bg_card_white_border)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 24)
                layoutParams = params

                setOnClickListener {
                    val intent = Intent(this@MyAdsActivity, MyAdDetailActivity::class.java).apply {
                        putExtra(MyAdDetailActivity.EXTRA_AD_ID, ad.id)
                    }
                    startActivity(intent)
                }
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(48, 48)
                setImageResource(R.drawable.ic_paw_print)
                setColorFilter(getColor(R.color.pine800))
            }

            if (ad.photoUris.isNotEmpty()) {
                val photoUri = ad.photoUris.first()
                lifecycleScope.launch {
                    val bitmap = ImageThumbnailLoader.loadThumbnail(this@MyAdsActivity, photoUri, 120, 120)
                    if (bitmap != null) {
                        icon.clearColorFilter()
                        icon.setImageBitmap(bitmap)
                        icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 24
                }
            }

            val titleTv = TextView(this).apply {
                text = ad.getDisplayTitle()
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
            }

            val pubDateStr = dateFormat.format(Date(ad.publicationTimestamp))
            val subTitleTv = TextView(this).apply {
                text = "${ad.getDisplayLocation()} · $pubDateStr"
                textSize = 12f
                setTextColor(getColor(R.color.muted))
                setPadding(0, 4, 0, 4)
            }

            val statusTv = TextView(this).apply {
                text = "${ad.currentSituation} · ${ad.getRemainingDaysText()}"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(if (ad.isEnded) R.color.muted else R.color.terracotta))
            }

            infoLayout.addView(titleTv)
            infoLayout.addView(subTitleTv)
            infoLayout.addView(statusTv)

            val chevron = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40)
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(getColor(R.color.muted))
            }

            card.addView(icon)
            card.addView(infoLayout)
            card.addView(chevron)

            container.addView(card)
        }
    }
}
