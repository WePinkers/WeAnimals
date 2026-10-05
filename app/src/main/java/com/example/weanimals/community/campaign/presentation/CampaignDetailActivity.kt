package com.example.weanimals.community.campaign.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.campaign.domain.CampaignDonationInfo
import com.example.weanimals.community.campaign.domain.CommunityCampaignDonationResult
import com.example.weanimals.community.campaign.domain.CommunityCampaignParticipationResult
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.databinding.ActivityCampaignDetailBinding
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class CampaignDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCampaignDetailBinding

    private val campaignId by lazy { intent.getStringExtra(EXTRA_ID).orEmpty() }
    private val category by lazy {
        intent.getStringExtra(EXTRA_CATEGORY)?.let {
            runCatching { CommunityCategory.valueOf(it) }.getOrNull()
        } ?: CommunityCategory.OTHER
    }
    private val organization by lazy { intent.getStringExtra(EXTRA_ORGANIZATION).orEmpty() }
    private val title by lazy { intent.getStringExtra(EXTRA_TITLE).orEmpty() }
    private val dateText by lazy { intent.getStringExtra(EXTRA_DATE).orEmpty() }
    private val locationText by lazy { intent.getStringExtra(EXTRA_LOCATION).orEmpty() }
    private val availabilityText by lazy {
        intent.getStringExtra(EXTRA_AVAILABILITY).orEmpty()
    }
    private val audienceText by lazy { intent.getStringExtra(EXTRA_AUDIENCE).orEmpty() }
    private val description by lazy { intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty() }
    private val highlightText by lazy { intent.getStringExtra(EXTRA_HIGHLIGHT).orEmpty() }
    private val totalSlots by lazy { intent.getIntExtra(EXTRA_TOTAL_SLOTS, -1).takeIf { it > 0 } }
    private var currentFilledSlots: Int? = null
    private val donationGoalCents by lazy {
        intent.getLongExtra(EXTRA_DONATION_GOAL_CENTS, -1L).takeIf { it > 0L }
    }
    private var currentDonationRaisedCents: Long? = null
    private val donationNeeds by lazy {
        intent.getStringArrayListExtra(EXTRA_DONATION_NEEDS).orEmpty()
    }
    private val pixKey by lazy { intent.getStringExtra(EXTRA_PIX_KEY).orEmpty() }
    private val locationLatitude by lazy {
        intent.getDoubleExtra(EXTRA_LOCATION_LATITUDE, Double.NaN).takeIf(Double::isFinite)
    }
    private val locationLongitude by lazy {
        intent.getDoubleExtra(EXTRA_LOCATION_LONGITUDE, Double.NaN).takeIf(Double::isFinite)
    }
    private val distanceKm by lazy {
        intent.getDoubleExtra(EXTRA_DISTANCE_KM, Double.NaN).takeIf(Double::isFinite)
    }
    private var isParticipating = false
    private val confirmedAnimalsCount by lazy {
        intent.getIntExtra(EXTRA_CONFIRMED_ANIMALS_COUNT, -1).takeIf { it >= 0 }
    }
    private val campaignRepository by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityCampaignRepository()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentFilledSlots = intent.getIntExtra(EXTRA_FILLED_SLOTS, -1).takeIf { it >= 0 }
        currentDonationRaisedCents = intent
            .getLongExtra(EXTRA_DONATION_RAISED_CENTS, -1L)
            .takeIf { it >= 0L }
        isParticipating = intent.getBooleanExtra(EXTRA_PARTICIPATING, false)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCampaignDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindCampaign()
        binding.backButton.setOnClickListener { finish() }
        binding.shareButton.setOnClickListener { shareCampaign() }
        binding.confirmedAnimalsCard.setOnClickListener {
            startActivity(CampaignAnimalsActivity.newIntent(this, campaignId, title))
        }
        binding.actionButton.setOnClickListener { handleAction() }
        if (isParticipationCategory()) {
            if (isParticipating) showParticipatingState()
            refreshParticipationState()
        }
    }

    private fun bindCampaign() = with(binding) {
        val context = root.context
        val categoryName = categoryLabel(category)
        campaignHero.setBackgroundColor(ContextCompat.getColor(context, heroColor(category)))
        heroIcon.setImageResource(heroIcon(category))
        heroIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)

        campaignEyebrow.text = getString(
            R.string.community_campaign_detail_eyebrow,
            categoryName.uppercase(),
        )
        campaignTitle.text = title
        organizationName.text = organization.ifBlank {
            getString(R.string.community_organization_default)
        }
        organizationAvatar.text = initials(organization)
        campaignDate.text = dateText
        campaignDate.isVisible = dateText.isNotBlank()
        campaignLocation.text = locationText
        campaignLocation.isVisible = locationText.isNotBlank()
        audienceRow.isVisible = audienceText.isNotBlank()
        campaignAudience.text = audienceText

        confirmedAnimalsCard.isVisible = category == CommunityCategory.ADOPTION
        if (category == CommunityCategory.ADOPTION && confirmedAnimalsCount != null) {
            confirmedAnimalsSubtitle.text = getString(
                R.string.community_campaign_animals_count,
                confirmedAnimalsCount
            )
        }
        renderProgress()

        val campaignHighlight = this@CampaignDetailActivity.highlightText
        highlightCard.isVisible = campaignHighlight.isNotBlank()
        if (category == CommunityCategory.VACCINATION && campaignHighlight.isNotBlank()) {
            highlightCard.setBackgroundResource(R.drawable.bg_campaign_vaccination_note)
            highlightLabel.isVisible = true
            highlightLabel.setText(R.string.community_campaign_no_scheduling)
            highlightText.text = getString(R.string.community_campaign_arrive_anytime)
            highlightBody.isVisible = true
            highlightBody.text = campaignHighlight
        } else {
            highlightCard.setBackgroundResource(R.drawable.bg_campaign_info)
            highlightLabel.isVisible = false
            highlightText.text = campaignHighlight
            highlightBody.isVisible = false
        }
        donationNeedsGroup.isVisible = category == CommunityCategory.DONATION && donationNeeds.isNotEmpty()
        donationNeedsText.text = donationNeeds.joinToString("\n") { "✓  $it" }
        aboutText.text = description.ifBlank {
            getString(R.string.community_campaign_description_unavailable)
        }
        if (isParticipating) showParticipatingState() else actionButton.setText(actionLabel(category))
    }

    private fun renderProgress() = with(binding) {
        val slotsProgress = category == CommunityCategory.NEUTERING
            && totalSlots != null
            && currentFilledSlots != null
        val donationProgress = category == CommunityCategory.DONATION
            && donationGoalCents != null
            && currentDonationRaisedCents != null
        val hasProgress = slotsProgress || donationProgress
        campaignAvailabilityText.text = availabilityText
        campaignAvailabilityLabel.text = when (category) {
            CommunityCategory.DONATION -> getString(R.string.community_campaign_donation_info)
            CommunityCategory.ADOPTION -> getString(R.string.community_campaign_adoption_info)
            else -> getString(R.string.community_campaign_availability_label)
        }
        campaignAvailabilityText.isVisible = availabilityText.isNotBlank() && !hasProgress
        campaignAvailabilityLabel.isVisible = availabilityText.isNotBlank() && !hasProgress
        progressLabel.isVisible = hasProgress
        campaignProgress.isVisible = hasProgress
        progressDetail.isVisible = hasProgress
        if (slotsProgress) {
            val total = totalSlots!!.coerceAtLeast(1)
            val filled = currentFilledSlots!!.coerceIn(0, total)
            progressLabel.setText(R.string.community_campaign_slots_progress_label)
            campaignProgress.progress = filled * 100 / total
            progressDetail.text = getString(
                R.string.community_campaign_slots_progress,
                filled,
                total,
                total - filled
            )
        } else if (donationProgress) {
            val goal = donationGoalCents!!.coerceAtLeast(1L)
            val raised = currentDonationRaisedCents!!.coerceIn(0L, goal)
            progressLabel.setText(R.string.community_campaign_donation_progress_label)
            campaignProgress.progress = ((raised * 100L) / goal).toInt()
            progressDetail.text = getString(
                R.string.community_campaign_donation_progress,
                formatCurrency(raised),
                formatCurrency(goal)
            )
        }
    }

    private fun handleAction() {
        when (category) {
            CommunityCategory.NEUTERING,
            CommunityCategory.VACCINATION,
            CommunityCategory.ADOPTION -> participateInCampaign()
            CommunityCategory.DONATION -> openDonationHelp()
            else -> Toast.makeText(
                this,
                R.string.community_campaign_action_soon,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openDonationHelp() {
        val info = CampaignDonationInfo(
            campaignId = campaignId,
            organization = organization,
            campaignTitle = title,
            donationNeeds = donationNeeds,
            receivingLocation = locationText,
            pixKey = pixKey,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude,
            fallbackDistanceKm = distanceKm,
            dateText = dateText,
            availabilityText = availabilityText,
            audienceText = audienceText,
            description = description,
            highlightText = highlightText,
            donationGoalCents = donationGoalCents,
            donationRaisedCents = currentDonationRaisedCents
        )
        startActivity(DonationHelpActivity.newIntent(this, info))
    }

    private fun participateInCampaign() {
        if (isParticipating) return
        binding.actionButton.isEnabled = false
        lifecycleScope.launch {
            campaignRepository.participate(campaignId).fold(
                onSuccess = ::showParticipationResult,
                onFailure = {
                    binding.actionButton.isEnabled = true
                    Toast.makeText(
                        this@CampaignDetailActivity,
                        R.string.community_campaign_participation_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun showParticipationResult(result: CommunityCampaignParticipationResult) {
        currentFilledSlots = result.filledSlots
        isParticipating = true
        showParticipatingState()
        renderProgress()
    }

    private fun showParticipatingState() {
        binding.actionButton.setText(R.string.community_campaign_confirmed)
        binding.actionButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_check)
        binding.actionButton.iconTint = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.white)
        )
        binding.actionButton.isEnabled = false
    }

    private fun refreshParticipationState() {
        lifecycleScope.launch {
            campaignRepository.isParticipating(campaignId)
                .getOrNull()
                ?.takeIf { it && !isParticipating }
                ?.let {
                    isParticipating = true
                    showParticipatingState()
                }
        }
    }

    private fun isParticipationCategory(): Boolean = category == CommunityCategory.NEUTERING
        || category == CommunityCategory.VACCINATION
        || category == CommunityCategory.ADOPTION

    private fun showDonationDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.community_campaign_donation_amount_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setPadding(0, 0, 0, 0)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.community_campaign_donation_title)
            .setView(input)
            .setNegativeButton(R.string.adoption_tracking_cancel, null)
            .setPositiveButton(R.string.community_campaign_donate, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amountCents = parseMoneyCents(input.text?.toString().orEmpty())
                if (amountCents == null) {
                    input.error = getString(R.string.community_campaign_donation_invalid_amount)
                    return@setOnClickListener
                }
                dialog.dismiss()
                submitDonation(amountCents)
            }
        }
        dialog.show()
    }

    private fun submitDonation(amountCents: Long) {
        binding.actionButton.isEnabled = false
        lifecycleScope.launch {
            campaignRepository.donate(campaignId, amountCents).fold(
                onSuccess = ::showDonationResult,
                onFailure = {
                    binding.actionButton.isEnabled = true
                    Toast.makeText(
                        this@CampaignDetailActivity,
                        R.string.community_campaign_donation_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun showDonationResult(result: CommunityCampaignDonationResult) {
        currentDonationRaisedCents = result.raisedCents
        binding.actionButton.isEnabled = true
        renderProgress()
        Toast.makeText(this, R.string.community_campaign_donation_success, Toast.LENGTH_SHORT).show()
    }

    private fun parseMoneyCents(value: String): Long? = runCatching {
        val normalized = value
            .replace("R$", "", ignoreCase = true)
            .replace(" ", "")
            .let { if (it.contains(',')) it.replace(".", "").replace(',', '.') else it }
        BigDecimal(normalized)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
            .takeIf { it in 100..10_000_000_000L }
    }.getOrNull()

    private fun shareCampaign() {
        binding.shareButton.isEnabled = false
        lifecycleScope.launch {
            val confirmedAnimals = if (category == CommunityCategory.ADOPTION) {
                campaignRepository.getConfirmedAnimals(campaignId).getOrNull().orEmpty()
            } else {
                emptyList()
            }
            val shareText = buildShareText(confirmedAnimals)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                getString(R.string.community_campaign_share)
            ))
            binding.shareButton.isEnabled = true
        }
    }

    private fun buildShareText(
        confirmedAnimals: List<com.example.weanimals.community.campaign.domain.CommunityCampaignAnimal>
    ): String = buildString {
        appendLine("$title | WeAnimals")
        appendLine()
        appendLine("Tipo: ${categoryLabel(category)}")
        appendLine("Organização: ${organization.ifBlank { getString(R.string.community_organization_default) }}")
        appendLine(getString(R.string.community_organization_verified))
        if (dateText.isNotBlank()) appendLine("Data e horário: $dateText")
        if (locationText.isNotBlank()) appendLine("Local: $locationText")
        if (audienceText.isNotBlank()) appendLine("Público/animais atendidos: $audienceText")
        if (availabilityText.isNotBlank()) appendLine("Vagas/meta: $availabilityText")
        if (category == CommunityCategory.NEUTERING && totalSlots != null && currentFilledSlots != null) {
            appendLine(
                "Vagas preenchidas: ${currentFilledSlots!!.coerceIn(0, totalSlots!!)} de $totalSlots"
            )
        }
        if (category == CommunityCategory.DONATION
            && donationGoalCents != null
            && currentDonationRaisedCents != null
        ) {
            appendLine(
                "Arrecadado: ${formatCurrency(currentDonationRaisedCents!!)} de "
                    + formatCurrency(donationGoalCents!!)
            )
        }
        if (donationNeeds.isNotEmpty()) {
            appendLine("O que é mais necessário: ${donationNeeds.joinToString(", ")}")
        }
        if (highlightText.isNotBlank()) {
            appendLine("Informação importante: $highlightText")
        }
        if (confirmedAnimals.isNotEmpty()) {
            appendLine()
            appendLine("Animais confirmados:")
            confirmedAnimals.forEach { animal ->
                val meta = listOf(animal.breed, animal.ageText)
                    .filter(String::isNotBlank)
                    .joinToString(" · ")
                appendLine("• ${animal.name}${if (meta.isNotBlank()) " — $meta" else ""}")
            }
        }
        if (description.isNotBlank()) {
            appendLine()
            appendLine("Sobre a campanha:")
            appendLine(description)
        }
    }

    private fun categoryLabel(value: CommunityCategory): String = when (value) {
        CommunityCategory.NEUTERING -> getString(R.string.community_filter_neutering)
        CommunityCategory.VACCINATION -> getString(R.string.community_filter_vaccination)
        CommunityCategory.ADOPTION -> getString(R.string.community_campaign_adoption)
        CommunityCategory.DONATION -> getString(R.string.community_campaign_donation)
        else -> getString(R.string.community_filter_campaigns)
    }

    private fun heroIcon(value: CommunityCategory): Int = when (value) {
        CommunityCategory.NEUTERING -> R.drawable.ic_community_neutering
        CommunityCategory.VACCINATION -> R.drawable.ic_community_vaccination
        CommunityCategory.ADOPTION -> R.drawable.ic_adoption_heart
        CommunityCategory.DONATION -> R.drawable.ic_community_donation
        else -> R.drawable.ic_community
    }

    private fun heroColor(value: CommunityCategory): Int = when (value) {
        CommunityCategory.NEUTERING -> R.color.gold
        CommunityCategory.VACCINATION -> R.color.pine800
        CommunityCategory.ADOPTION -> R.color.gold
        CommunityCategory.DONATION -> R.color.clay600
        else -> R.color.clay600
    }

    private fun actionLabel(value: CommunityCategory): Int = when (value) {
        CommunityCategory.NEUTERING -> R.string.community_campaign_join
        CommunityCategory.VACCINATION -> R.string.community_campaign_confirm_presence
        CommunityCategory.ADOPTION -> R.string.community_campaign_confirm_presence
        CommunityCategory.DONATION -> R.string.community_campaign_donate
        else -> R.string.community_campaign_join
    }

    private fun initials(value: String): String = value.trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "ONG" }

    companion object {
        private const val EXTRA_CATEGORY = "campaign_category"
        private const val EXTRA_ID = "campaign_id"
        private const val EXTRA_ORGANIZATION = "campaign_organization"
        private const val EXTRA_TITLE = "campaign_title"
        private const val EXTRA_DATE = "campaign_date"
        private const val EXTRA_LOCATION = "campaign_location"
        private const val EXTRA_AVAILABILITY = "campaign_availability"
        private const val EXTRA_AUDIENCE = "campaign_audience"
        private const val EXTRA_DESCRIPTION = "campaign_description"
        private const val EXTRA_HIGHLIGHT = "campaign_highlight"
        private const val EXTRA_TOTAL_SLOTS = "campaign_total_slots"
        private const val EXTRA_FILLED_SLOTS = "campaign_filled_slots"
        private const val EXTRA_DONATION_GOAL_CENTS = "campaign_donation_goal_cents"
        private const val EXTRA_DONATION_RAISED_CENTS = "campaign_donation_raised_cents"
        private const val EXTRA_DONATION_NEEDS = "campaign_donation_needs"
        private const val EXTRA_PIX_KEY = "campaign_pix_key"
        private const val EXTRA_LOCATION_LATITUDE = "campaign_location_latitude"
        private const val EXTRA_LOCATION_LONGITUDE = "campaign_location_longitude"
        private const val EXTRA_DISTANCE_KM = "campaign_distance_km"
        private const val EXTRA_PARTICIPATING = "campaign_participating"
        private const val EXTRA_CONFIRMED_ANIMALS_COUNT = "campaign_confirmed_animals_count"

        fun newIntent(context: Context, item: CommunityFeedItem.Campaign) =
            Intent(context, CampaignDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, item.id)
                putExtra(EXTRA_CATEGORY, item.category.name)
                putExtra(EXTRA_ORGANIZATION, item.organization)
                putExtra(EXTRA_TITLE, item.title)
                putExtra(EXTRA_DATE, item.dateText)
                putExtra(EXTRA_LOCATION, item.locationText)
                putExtra(EXTRA_AVAILABILITY, item.availabilityText)
                putExtra(EXTRA_AUDIENCE, item.audienceText)
                putExtra(EXTRA_DESCRIPTION, item.description)
                putExtra(EXTRA_HIGHLIGHT, item.highlightText)
                item.totalSlots?.let { putExtra(EXTRA_TOTAL_SLOTS, it) }
                item.filledSlots?.let { putExtra(EXTRA_FILLED_SLOTS, it) }
                item.donationGoalCents?.let { putExtra(EXTRA_DONATION_GOAL_CENTS, it) }
                item.donationRaisedCents?.let { putExtra(EXTRA_DONATION_RAISED_CENTS, it) }
                putStringArrayListExtra(EXTRA_DONATION_NEEDS, ArrayList(item.donationNeeds))
                putExtra(EXTRA_PIX_KEY, item.pixKey)
                item.locationLatitude?.let { putExtra(EXTRA_LOCATION_LATITUDE, it) }
                item.locationLongitude?.let { putExtra(EXTRA_LOCATION_LONGITUDE, it) }
                item.distanceKm?.let { putExtra(EXTRA_DISTANCE_KM, it) }
                putExtra(EXTRA_PARTICIPATING, item.participatingByCurrentUser)
                item.confirmedAnimalsCount?.let { putExtra(EXTRA_CONFIRMED_ANIMALS_COUNT, it) }
            }

        private fun formatCurrency(cents: Long): String = NumberFormat.getCurrencyInstance(
            Locale("pt", "BR")
        ).format(cents / 100.0)
    }
}
