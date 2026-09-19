package com.example.weanimals.community.feed.presentation

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.databinding.ItemCommunityCampaignBinding
import com.example.weanimals.databinding.ItemCommunityPostBinding
import java.text.NumberFormat
import java.util.Locale

class CommunityFeedAdapter(
    private val onCampaignClick: (CommunityFeedItem.Campaign) -> Unit,
    private val onCampaignActionClick: (CommunityFeedItem.Campaign) -> Unit,
    private val onPostCommentClick: (CommunityFeedItem.Post) -> Unit,
    private val onPostLikeClick: (CommunityFeedItem.Post) -> Unit
) : ListAdapter<CommunityFeedItem, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is CommunityFeedItem.Campaign -> VIEW_CAMPAIGN
        is CommunityFeedItem.Post -> VIEW_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_CAMPAIGN) {
            CampaignHolder(ItemCommunityCampaignBinding.inflate(inflater, parent, false))
        } else {
            PostHolder(ItemCommunityPostBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CampaignHolder -> holder.bind(getItem(position) as CommunityFeedItem.Campaign)
            is PostHolder -> holder.bind(getItem(position) as CommunityFeedItem.Post)
        }
    }

    private inner class CampaignHolder(private val binding: ItemCommunityCampaignBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CommunityFeedItem.Campaign) {
            val context = binding.root.context
            val category = when (item.category) {
                CommunityCategory.NEUTERING -> context.getString(R.string.community_filter_neutering)
                CommunityCategory.VACCINATION -> context.getString(R.string.community_filter_vaccination)
                CommunityCategory.FOUND -> context.getString(R.string.community_filter_found)
                CommunityCategory.QUESTION -> context.getString(R.string.community_category_question)
                CommunityCategory.NOTICE -> context.getString(R.string.community_category_notice)
                CommunityCategory.ADOPTION -> context.getString(R.string.community_campaign_adoption)
                CommunityCategory.DONATION -> context.getString(R.string.community_campaign_donation)
                CommunityCategory.OTHER -> context.getString(R.string.community_filter_campaigns)
            }
            binding.campaignLabel.text = context.getString(
                R.string.community_campaign_label, category.uppercase(), item.organization.uppercase()
            )
            binding.campaignTitle.text = item.title
            binding.campaignDate.text = item.dateText
            binding.campaignLocation.text = item.locationText
            binding.campaignAvailability.text = if (
                item.category == CommunityCategory.ADOPTION && item.confirmedAnimalsCount != null
            ) {
                context.getString(
                    R.string.community_campaign_animals_count,
                    item.confirmedAnimalsCount
                )
            } else {
                item.availabilityText
            }
            val slotsProgress = item.category == CommunityCategory.NEUTERING
                && item.totalSlots != null
                && item.filledSlots != null
            val donationProgress = item.category == CommunityCategory.DONATION
                && item.donationGoalCents != null
                && item.donationRaisedCents != null
            val hasProgress = slotsProgress || donationProgress
            binding.campaignAvailability.isVisible = !hasProgress
            binding.campaignProgressGroup.isVisible = hasProgress
            if (hasProgress) {
                val progress = if (slotsProgress) {
                    val total = item.totalSlots!!.coerceAtLeast(1)
                    val filled = item.filledSlots!!.coerceIn(0, total)
                    binding.campaignProgressLabel.setText(R.string.community_campaign_slots_progress_label)
                    binding.campaignProgressDetail.text = context.getString(
                        R.string.community_campaign_slots_progress,
                        filled,
                        total,
                        total - filled
                    )
                    (filled * 100 / total)
                } else {
                    val goal = item.donationGoalCents!!.coerceAtLeast(1L)
                    val raised = item.donationRaisedCents!!.coerceIn(0L, goal)
                    binding.campaignProgressLabel.setText(R.string.community_campaign_donation_progress_label)
                    binding.campaignProgressDetail.text = context.getString(
                        R.string.community_campaign_donation_progress,
                        formatCurrency(raised),
                        formatCurrency(goal)
                    )
                    ((raised * 100L) / goal).toInt()
                }
                binding.campaignProgress.progress = progress
            }
            val icon = when (item.category) {
                CommunityCategory.VACCINATION -> R.drawable.ic_community_vaccination
                CommunityCategory.ADOPTION -> R.drawable.ic_adoption_heart
                CommunityCategory.DONATION -> R.drawable.ic_community_donation
                else -> R.drawable.ic_community_neutering
            }
            val iconBackground = when (item.category) {
                CommunityCategory.VACCINATION -> R.color.community_vaccination_bg
                CommunityCategory.ADOPTION -> R.color.soft_gold
                CommunityCategory.DONATION -> R.color.soft_red
                else -> R.color.soft_red
            }
            val iconTint = when (item.category) {
                CommunityCategory.VACCINATION -> R.color.community_vaccination_tint
                else -> R.color.clay600
            }
            binding.campaignIcon.setImageResource(icon)
            binding.campaignIcon.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, iconBackground)
            )
            binding.campaignIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, iconTint)
            )
            val canParticipate = item.category == CommunityCategory.NEUTERING
                || item.category == CommunityCategory.VACCINATION
                || item.category == CommunityCategory.ADOPTION
            val isParticipating = canParticipate && item.participatingByCurrentUser
            binding.campaignJoin.setText(
                when {
                    isParticipating -> R.string.community_campaign_confirmed
                    item.category == CommunityCategory.VACCINATION
                        || item.category == CommunityCategory.ADOPTION ->
                        R.string.community_campaign_confirm_presence
                    item.category == CommunityCategory.DONATION -> R.string.community_campaign_donate
                    else -> R.string.community_join
                }
            )
            binding.campaignJoin.icon = if (isParticipating) {
                ContextCompat.getDrawable(context, R.drawable.ic_check)
            } else {
                null
            }
            binding.campaignJoin.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.white)
            )
            binding.campaignJoin.isEnabled = !isParticipating
            binding.root.setOnClickListener { onCampaignClick(item) }
            binding.campaignJoin.setOnClickListener { onCampaignActionClick(item) }
        }
    }

    private inner class PostHolder(private val binding: ItemCommunityPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CommunityFeedItem.Post) {
            val context = binding.root.context
            val author = item.author.ifBlank { context.getString(R.string.community_post_author_default) }
            binding.postAvatar.text = author.take(1).uppercase()
            binding.postAuthor.text = author
            binding.postMeta.text = context.getString(
                R.string.community_post_meta, item.timeText, item.neighborhood.orEmpty()
            )
            binding.postBody.text = item.body
            val bitmap = item.photoData?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            binding.postPhoto.setImageBitmap(bitmap)
            binding.postPhoto.visibility = if (bitmap == null) android.view.View.GONE else android.view.View.VISIBLE
            binding.postLikes.text = item.likes.toString()
            binding.postComments.text = item.comments.toString()
            binding.postLikeIcon.setImageResource(
                if (item.likedByCurrentUser) R.drawable.ic_favorite_filled
                else R.drawable.ic_heart_outline
            )
            binding.postLikeIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (item.likedByCurrentUser) R.color.terracotta else R.color.muted
                )
            )
            binding.root.setOnClickListener { onPostCommentClick(item) }
            binding.postLikeAction.setOnClickListener { onPostLikeClick(item) }
            binding.postCommentAction.setOnClickListener { onPostCommentClick(item) }
        }
    }

    private companion object {
        const val VIEW_CAMPAIGN = 1
        const val VIEW_POST = 2
        val DIFF = object : DiffUtil.ItemCallback<CommunityFeedItem>() {
            override fun areItemsTheSame(oldItem: CommunityFeedItem, newItem: CommunityFeedItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CommunityFeedItem, newItem: CommunityFeedItem) =
                oldItem == newItem
        }

        fun formatCurrency(cents: Long): String = NumberFormat.getCurrencyInstance(
            Locale("pt", "BR")
        ).format(cents / 100.0)
    }
}
