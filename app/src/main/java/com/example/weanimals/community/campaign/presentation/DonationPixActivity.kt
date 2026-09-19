package com.example.weanimals.community.campaign.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.graphics.drawable.ColorDrawable
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.campaign.domain.CampaignDonationInfo
import com.example.weanimals.databinding.ActivityDonationPixBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class DonationPixActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDonationPixBinding
    private val donationInfo by lazy { CampaignDonationInfo.fromIntent(intent) }
    private val repository by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityCampaignRepository()
    }
    private var amountCents = DEFAULT_AMOUNT_CENTS
    private var customAmountSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationPixBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pixHeader.headerTitle.setText(R.string.community_pix_title)
        binding.pixHeader.backButton.setOnClickListener { finish() }
        binding.pixKeyText.text = donationInfo.pixKey.ifBlank {
            getString(R.string.community_pix_missing_key)
        }
        bindAmounts()
        binding.copyPixButton.setOnClickListener { copyPixKey() }
        binding.pixDoneButton.setOnClickListener { confirmPixPayment() }
    }

    private fun bindAmounts() {
        val chips = listOf(
            binding.pixAmount5 to 500L,
            binding.pixAmount10 to 1_000L,
            binding.pixAmount20 to 2_000L,
            binding.pixAmount50 to 5_000L,
            binding.pixAmount100 to 10_000L
        )
        chips.forEach { (chip, value) ->
            styleAmountChip(chip, selected = value == amountCents)
            chip.setOnClickListener {
                customAmountSelected = false
                amountCents = value
                styleAllAmountChips()
            }
        }
        binding.pixAmountOther.setOnClickListener { showCustomAmountSheet() }
        styleAmountChip(binding.pixAmountOther, selected = false)
    }

    private fun styleAllAmountChips() {
        styleAmountChip(binding.pixAmount5, amountCents == 500L && !customAmountSelected)
        styleAmountChip(binding.pixAmount10, amountCents == 1_000L && !customAmountSelected)
        styleAmountChip(binding.pixAmount20, amountCents == 2_000L && !customAmountSelected)
        styleAmountChip(binding.pixAmount50, amountCents == 5_000L && !customAmountSelected)
        styleAmountChip(binding.pixAmount100, amountCents == 10_000L && !customAmountSelected)
        styleAmountChip(binding.pixAmountOther, customAmountSelected)
    }

    private fun styleAmountChip(chip: Chip, selected: Boolean) {
        chip.isChecked = selected
        chip.setChipBackgroundColorResource(
            if (selected) R.color.pine800 else R.color.background
        )
        chip.setChipStrokeColorResource(R.color.border)
        chip.chipStrokeWidth = resources.displayMetrics.density
        chip.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.ink900))
    }

    private fun showCustomAmountSheet() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_donation_sheet)
            setPadding(dp(24), dp(12), dp(24), dp(24))
        }
        val handle = TextView(this).apply {
            text = "—"
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@DonationPixActivity, R.color.border))
            textSize = 28f
        }
        val title = TextView(this).apply {
            text = getString(R.string.community_pix_amount_title)
            setTextColor(ContextCompat.getColor(this@DonationPixActivity, R.color.ink))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val input = EditText(this).apply {
            hint = getString(R.string.community_pix_amount_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundResource(R.drawable.bg_report_card)
        }
        val confirm = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.community_pix_amount_confirm)
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ContextCompat.getColorStateList(this@DonationPixActivity, R.color.clay600)
            cornerRadius = dp(14)
            insetTop = 0
            insetBottom = 0
        }
        content.addView(handle, LinearLayout.LayoutParams(-1, dp(24)))
        content.addView(title, LinearLayout.LayoutParams(-1, dp(32)))
        content.addView(input, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })
        content.addView(confirm, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(14) })
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(content)
        confirm.setOnClickListener {
            val parsed = parseMoneyCents(input.text?.toString().orEmpty())
            if (parsed == null) {
                input.error = getString(R.string.community_pix_amount_invalid)
                return@setOnClickListener
            }
            amountCents = parsed
            customAmountSelected = true
            styleAllAmountChips()
            dialog.dismiss()
        }
        dialog.show()
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.backgroundTintList = null
        bottomSheet?.background = ContextCompat.getDrawable(this, R.drawable.bg_donation_sheet)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun copyPixKey() {
        if (donationInfo.pixKey.isBlank()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pix", donationInfo.pixKey))
        Toast.makeText(this, R.string.community_pix_copied, Toast.LENGTH_SHORT).show()
    }

    private fun confirmPixPayment() {
        if (donationInfo.campaignId.isBlank()) return
        binding.pixDoneButton.isEnabled = false
        lifecycleScope.launch {
            repository.donate(donationInfo.campaignId, amountCents).fold(
                onSuccess = {
                    startActivity(
                        DonationConfirmationActivity.newIntent(
                            this@DonationPixActivity,
                            donationInfo,
                            amountCents
                        )
                    )
                    finish()
                },
                onFailure = {
                    binding.pixDoneButton.isEnabled = true
                    Toast.makeText(
                        this@DonationPixActivity,
                        R.string.community_pix_confirmation_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_AMOUNT_CENTS = 5_000L

        fun newIntent(context: Context, info: CampaignDonationInfo) =
            info.toIntent(context, DonationPixActivity::class.java)
    }
}
