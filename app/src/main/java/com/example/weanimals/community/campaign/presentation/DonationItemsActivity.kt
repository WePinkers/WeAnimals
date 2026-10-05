package com.example.weanimals.community.campaign.presentation

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.campaign.domain.CampaignDonationInfo
import com.example.weanimals.core.location.domain.Coordinates
import com.example.weanimals.databinding.ActivityDonationItemsBinding
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DonationItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDonationItemsBinding
    private val donationInfo by lazy { CampaignDonationInfo.fromIntent(intent) }
    private val repository by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityCampaignRepository()
    }
    private val selectedItems = linkedSetOf<String>()
    private lateinit var otherItemInput: EditText
    private var itemOptions: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.itemsHeader.headerTitle.setText(R.string.community_items_title)
        binding.itemsHeader.backButton.setOnClickListener { finish() }
        binding.deliveryLocation.text = getString(
            R.string.community_items_delivery_title,
            donationInfo.organization.ifBlank {
                getString(R.string.community_organization_default)
            }
        )
        binding.itemsNotifyButton.setOnClickListener { notifyOrganization() }
        buildItemOptions()
        updateDeliveryDistance(donationInfo.fallbackDistanceKm)
    }

    private fun buildItemOptions() {
        val needs = donationInfo.donationNeeds.ifEmpty {
            listOf(
                getString(R.string.community_items_default_food),
                getString(R.string.community_items_default_blankets)
            )
        }
        itemOptions = needs
        needs.forEach { item -> addItemOption(item, item.contains("urgente", ignoreCase = true)) }
        addItemOption(getString(R.string.community_items_other), false, isOther = true)
        loadDistanceFromUser()
    }

    private fun addItemOption(label: String, urgent: Boolean, isOther: Boolean = false) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(54)).apply {
                bottomMargin = dp(8)
            }
            setCardBackgroundColor(ContextCompat.getColor(this@DonationItemsActivity, R.color.surface))
            strokeColor = ContextCompat.getColor(this@DonationItemsActivity, R.color.border)
            strokeWidth = dp(1)
            radius = dp(12).toFloat()
            isClickable = true
            isFocusable = true
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(10), 0)
        }
        val checkBox = CheckBox(this).apply {
            setButtonDrawable(R.drawable.checkbox_donation_selector)
            buttonTintList = null
            isClickable = false
            contentDescription = label
        }
        val labelView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            text = label
            setTextColor(ContextCompat.getColor(this@DonationItemsActivity, R.color.ink))
            textSize = 12f
        }
        row.addView(checkBox, LinearLayout.LayoutParams(dp(28), dp(48)))
        row.addView(labelView)
        if (urgent) {
            row.addView(TextView(this).apply {
                setText(R.string.community_items_urgent)
                setTextColor(ContextCompat.getColor(this@DonationItemsActivity, R.color.clay600))
                textSize = 10f
            })
        }
        card.addView(row, LinearLayout.LayoutParams(-1, -1))
        card.setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
            if (isOther) {
                if (checkBox.isChecked) selectedItems.add(label) else selectedItems.remove(label)
                showOtherInput(checkBox.isChecked)
            } else if (checkBox.isChecked) selectedItems.add(label) else selectedItems.remove(label)
            binding.itemsError.isVisible = false
        }
        binding.itemOptionsContainer.addView(card)
    }

    private fun showOtherInput(visible: Boolean) {
        if (!::otherItemInput.isInitialized) {
            otherItemInput = EditText(this).apply {
                hint = getString(R.string.community_items_other_hint)
                setSingleLine(true)
                setTextColor(ContextCompat.getColor(this@DonationItemsActivity, R.color.ink900))
                setBackgroundResource(R.drawable.bg_report_card)
                setPadding(dp(12), 0, dp(12), 0)
            }
            binding.itemOptionsContainer.addView(otherItemInput, LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(4)
            })
        }
        otherItemInput.isVisible = visible
    }

    private fun loadDistanceFromUser() {
        lifecycleScope.launch {
            val userLocation = (application as WeAnimalsApplication).appContainer
                .getCurrentLocationForCommunity().invoke().getOrNull()
            val campaignCoordinates = Coordinates.fromOrNull(
                donationInfo.locationLatitude,
                donationInfo.locationLongitude
            )
            val currentCoordinates = userLocation?.let {
                Coordinates.fromOrNull(it.latitude, it.longitude)
            }
            val distance = if (campaignCoordinates != null && currentCoordinates != null) {
                com.example.weanimals.core.location.interactor.CalculateDistanceInteractor()(
                    currentCoordinates,
                    campaignCoordinates
                )
            } else {
                donationInfo.fallbackDistanceKm
            }
            updateDeliveryDistance(distance)
        }
    }

    private fun updateDeliveryDistance(distanceKm: Double?) {
        val location = donationInfo.receivingLocation.ifBlank {
            getString(R.string.community_location_region_unknown)
        }
        binding.deliveryDistance.text = if (distanceKm != null) {
            getString(
                R.string.community_items_distance,
                location,
                formatDistance(distanceKm)
            )
        } else {
            getString(R.string.community_items_distance_unknown)
        }
    }

    private fun notifyOrganization() {
        val otherItem = if (::otherItemInput.isInitialized) {
            otherItemInput.text?.toString().orEmpty().trim()
        } else {
            ""
        }
        val cleanItems = selectedItems.filterNot { it == getString(R.string.community_items_other) }
        if (cleanItems.isEmpty() && otherItem.isBlank()) {
            binding.itemsError.setText(R.string.community_items_required)
            binding.itemsError.isVisible = true
            return
        }
        binding.itemsNotifyButton.isEnabled = false
        lifecycleScope.launch {
            repository.notifyItemDonation(donationInfo.campaignId, cleanItems, otherItem).fold(
                onSuccess = {
                    binding.itemsNotifyButton.setText(R.string.community_items_notify_sent)
                    binding.itemsError.setText(R.string.community_items_notify_success)
                    binding.itemsError.setTextColor(
                        ContextCompat.getColor(this@DonationItemsActivity, R.color.pine800)
                    )
                    binding.itemsError.isVisible = true
                },
                onFailure = {
                    binding.itemsNotifyButton.isEnabled = true
                    binding.itemsError.setText(R.string.community_items_notify_error)
                    binding.itemsError.setTextColor(
                        ContextCompat.getColor(this@DonationItemsActivity, R.color.alert600)
                    )
                    binding.itemsError.isVisible = true
                }
            )
        }
    }

    private fun formatDistance(value: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            minimumFractionDigits = if (value < 10) 1 else 0
            maximumFractionDigits = 1
        }
        return "${format.format(value)} km"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        fun newIntent(context: Context, info: CampaignDonationInfo) =
            info.toIntent(context, DonationItemsActivity::class.java)
    }
}
