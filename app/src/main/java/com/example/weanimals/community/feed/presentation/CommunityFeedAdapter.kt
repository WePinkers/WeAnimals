package com.example.weanimals.community.feed.presentation

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.databinding.ItemCommunityCampaignBinding
import com.example.weanimals.databinding.ItemCommunityPostBinding

class CommunityFeedAdapter(
    private val onJoinClick: () -> Unit,
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
                CommunityCategory.OTHER -> context.getString(R.string.community_filter_campaigns)
            }
            binding.campaignLabel.text = context.getString(
                R.string.community_campaign_label, category.uppercase(), item.organization.uppercase()
            )
            binding.campaignTitle.text = item.title
            binding.campaignDate.text = item.dateText
            binding.campaignLocation.text = item.locationText
            binding.campaignAvailability.text = item.availabilityText
            val vaccination = item.category == CommunityCategory.VACCINATION
            binding.campaignIcon.setImageResource(
                if (vaccination) R.drawable.ic_community_vaccination else R.drawable.ic_community_neutering
            )
            binding.campaignIcon.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context, if (vaccination) R.color.community_vaccination_bg else R.color.soft_red
                )
            )
            binding.campaignIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context, if (vaccination) R.color.community_vaccination_tint else R.color.clay600
                )
            )
            binding.campaignJoin.setOnClickListener { onJoinClick() }
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
    }
}
