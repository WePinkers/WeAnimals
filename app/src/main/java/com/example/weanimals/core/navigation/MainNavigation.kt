package com.example.weanimals.core.navigation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.weanimals.R
import com.example.weanimals.adoption.listing.presentation.AdoptionActivity
import com.example.weanimals.databinding.ViewMainNavigationBinding
import com.example.weanimals.home.presentation.HomeActivity
import com.example.weanimals.community.feed.presentation.CommunityActivity
import com.example.weanimals.profile.overview.presentation.ProfileActivity
import com.example.weanimals.map.overview.presentation.MapActivity

object MainNavigation {
    enum class Destination { HOME, MAP, COMMUNITY, ADOPTION, PROFILE }

    fun bind(
        activity: Activity,
        binding: ViewMainNavigationBinding,
        selected: Destination? = null
    ) {
        val active = ContextCompat.getColor(activity, R.color.adoption_primary)
        val muted = ContextCompat.getColor(activity, R.color.muted)
        ImageViewCompat.setImageTintList(
            binding.navHomeIcon, ColorStateList.valueOf(if (selected == Destination.HOME) active else muted)
        )
        binding.navHomeLabel.setTextColor(if (selected == Destination.HOME) active else muted)
        ImageViewCompat.setImageTintList(
            binding.navAdoptionIcon, ColorStateList.valueOf(if (selected == Destination.ADOPTION) active else muted)
        )
        binding.navAdoptionLabel.setTextColor(if (selected == Destination.ADOPTION) active else muted)
        ImageViewCompat.setImageTintList(
            binding.navProfileIcon, ColorStateList.valueOf(if (selected == Destination.PROFILE) active else muted)
        )
        binding.navProfileLabel.setTextColor(if (selected == Destination.PROFILE) active else muted)
        ImageViewCompat.setImageTintList(
            binding.navMapIcon, ColorStateList.valueOf(if (selected == Destination.MAP) active else muted)
        )
        binding.navMapLabel.setTextColor(if (selected == Destination.MAP) active else muted)
        ImageViewCompat.setImageTintList(
            binding.navCommunityIcon, ColorStateList.valueOf(if (selected == Destination.COMMUNITY) active else muted)
        )
        binding.navCommunityLabel.setTextColor(if (selected == Destination.COMMUNITY) active else muted)
        binding.navHome.isSelected = selected == Destination.HOME
        binding.navAdoption.isSelected = selected == Destination.ADOPTION
        binding.navProfile.isSelected = selected == Destination.PROFILE
        binding.navMap.isSelected = selected == Destination.MAP
        binding.navCommunity.isSelected = selected == Destination.COMMUNITY
        // The navigation background consumes taps that are not handled by a destination.
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
        binding.navCommunity.setOnClickListener {
            if (selected != Destination.COMMUNITY) activity.startActivity(
                Intent(activity, CommunityActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }
}
