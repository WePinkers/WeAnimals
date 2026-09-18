package com.example.weanimals.core.navigation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.weanimals.R
import com.example.weanimals.adoption.listing.presentation.AdoptionActivity
import com.example.weanimals.databinding.ViewMainNavigationBinding
import com.example.weanimals.home.presentation.HomeActivity
import com.example.weanimals.profile.overview.presentation.ProfileActivity
import com.example.weanimals.map.overview.presentation.MapActivity

object MainNavigation {
    enum class Destination { HOME, ADOPTION, PROFILE, MAP }

    fun bind(
        activity: Activity,
        binding: ViewMainNavigationBinding,
        selected: Destination? = null
    ) {
        val activeColor = ContextCompat.getColor(activity, R.color.pine800)
        val mutedColor = ContextCompat.getColor(activity, R.color.muted)

        fun setupItem(
            indicator: FrameLayout,
            icon: ImageView,
            label: TextView,
            isCurrent: Boolean
        ) {
            if (isCurrent) {
                indicator.setBackgroundResource(R.drawable.bg_nav_active_indicator)
                indicator.animate().scaleX(1.05f).scaleY(1.05f).alpha(1.0f).setDuration(150).start()
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(activeColor))
                label.setTextColor(activeColor)
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                label.setTypeface(null, Typeface.BOLD)
            } else {
                indicator.setBackgroundResource(0)
                indicator.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(150).start()
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(mutedColor))
                label.setTextColor(mutedColor)
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                label.setTypeface(null, Typeface.NORMAL)
            }
        }

        setupItem(binding.navHomeIndicator, binding.navHomeIcon, binding.navHomeLabel, selected == Destination.HOME)
        setupItem(binding.navAdoptionIndicator, binding.navAdoptionIcon, binding.navAdoptionLabel, selected == Destination.ADOPTION)
        setupItem(binding.navProfileIndicator, binding.navProfileIcon, binding.navProfileLabel, selected == Destination.PROFILE)
        setupItem(binding.navMapIndicator, binding.navMapIcon, binding.navMapLabel, selected == Destination.MAP)

        binding.navHome.isSelected = selected == Destination.HOME
        binding.navAdoption.isSelected = selected == Destination.ADOPTION
        binding.navProfile.isSelected = selected == Destination.PROFILE
        binding.navMap.isSelected = selected == Destination.MAP

        binding.root.setOnClickListener { }

        binding.navHome.setOnClickListener {
            if (selected != Destination.HOME) activity.startActivity(
                Intent(activity, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        binding.navAdoption.setOnClickListener {
            if (selected != Destination.ADOPTION) activity.startActivity(
                Intent(activity, AdoptionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        binding.navProfile.setOnClickListener {
            if (selected != Destination.PROFILE) activity.startActivity(
                Intent(activity, ProfileActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        binding.navMap.setOnClickListener {
            if (selected != Destination.MAP) activity.startActivity(
                Intent(activity, MapActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }
}
