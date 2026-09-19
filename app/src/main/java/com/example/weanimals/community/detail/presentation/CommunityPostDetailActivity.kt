package com.example.weanimals.community.detail.presentation

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.detail.domain.CommunityComment
import com.example.weanimals.databinding.ActivityCommunityPostDetailBinding
import com.example.weanimals.databinding.ItemCommunityCommentBinding
import kotlinx.coroutines.launch

class CommunityPostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityPostDetailBinding
    private val postId by lazy { intent.getStringExtra(EXTRA_POST_ID).orEmpty() }
    private val engagementRepository by lazy {
        (application as WeAnimalsApplication).appContainer
            .createCommunityPostEngagementRepository()
    }

    private var likes = 0
    private var commentsCount = 0
    private var liked = false
    private var comments = emptyList<CommunityComment>()
    private var replyingToCommentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityCommunityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        likes = intent.getIntExtra(EXTRA_LIKES, 0)
        commentsCount = intent.getIntExtra(EXTRA_COMMENTS, 0)
        liked = intent.getBooleanExtra(EXTRA_LIKED, false)
        bindPost()
        setupInteractions()
        loadEngagement()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finishWithResult()
        })
    }

    private fun bindPost() {
        binding.postHeader.headerTitle.setText(R.string.community_post_detail_title)
        binding.postHeader.backButton.setOnClickListener { finishWithResult() }

        val author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
            .ifBlank { getString(R.string.community_post_author_default) }
        val time = intent.getStringExtra(EXTRA_TIME).orEmpty()
        val neighborhood = intent.getStringExtra(EXTRA_NEIGHBORHOOD).orEmpty()

        binding.postAvatar.text = author.take(1).uppercase()
        binding.postAuthor.text = author
        binding.postMeta.text = getString(R.string.community_post_meta, time, neighborhood)
        binding.postBody.text = intent.getStringExtra(EXTRA_BODY).orEmpty()
        intent.getByteArrayExtra(EXTRA_PHOTO)?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.postPhoto.setImageBitmap(bitmap)
            binding.postPhoto.isVisible = bitmap != null
        }
        renderEngagement()
    }

    private fun setupInteractions() {
        binding.postLikeAction.setOnClickListener { toggleLike() }
        binding.postCommentAction.setOnClickListener {
            binding.commentInput.requestFocus()
        }
        binding.sendCommentButton.setOnClickListener { sendComment() }
    }

    private fun loadEngagement() {
        binding.commentsState.isVisible = true
        binding.commentsState.setText(R.string.community_loading)
        lifecycleScope.launch {
            engagementRepository.getEngagement(postId).onSuccess { engagement ->
                likes = engagement.likes
                commentsCount = engagement.comments
                liked = engagement.likedByCurrentUser
                renderEngagement()
            }
            engagementRepository.getComments(postId).onSuccess { loadedComments ->
                comments = loadedComments
                showComments()
            }.onFailure {
                binding.commentsState.isVisible = true
                binding.commentsState.setText(R.string.community_comments_error)
            }
        }
    }

    private fun toggleLike() {
        binding.postLikeAction.isEnabled = false
        lifecycleScope.launch {
            engagementRepository.toggleLike(postId, !liked)
                .onSuccess { engagement ->
                    likes = engagement.likes
                    commentsCount = engagement.comments
                    liked = engagement.likedByCurrentUser
                    renderEngagement()
                }
                .onFailure {
                    Toast.makeText(
                        this@CommunityPostDetailActivity,
                        R.string.community_like_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            binding.postLikeAction.isEnabled = true
        }
    }

    private fun sendComment() {
        val text = binding.commentInput.text?.toString().orEmpty().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.community_comment_required, Toast.LENGTH_SHORT).show()
            return
        }

        binding.sendCommentButton.isEnabled = false
        lifecycleScope.launch {
            engagementRepository.addComment(postId, text, replyingToCommentId)
                .onSuccess { comment ->
                    comments = comments + comment
                    commentsCount += 1
                    replyingToCommentId = null
                    binding.commentInput.text?.clear()
                    showComments()
                }
                .onFailure {
                    Toast.makeText(
                        this@CommunityPostDetailActivity,
                        R.string.community_comment_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            binding.sendCommentButton.isEnabled = true
        }
    }

    private fun renderEngagement() {
        binding.postLikes.text = likes.toString()
        binding.postComments.text = commentsCount.toString()
        binding.commentsTitle.text = getString(
            R.string.community_comments_count,
            commentsCount
        )
        binding.postLikeIcon.setImageResource(
            if (liked) R.drawable.ic_favorite_filled else R.drawable.ic_heart_outline
        )
        binding.postLikeIcon.imageTintList = ContextCompat.getColorStateList(
            this,
            if (liked) R.color.terracotta else R.color.muted
        )
    }

    private fun showComments() {
        binding.commentsContainer.removeAllViews()
        binding.commentsState.isVisible = comments.isEmpty()
        if (comments.isEmpty()) {
            binding.commentsState.setText(R.string.community_comments_empty)
        }
        orderedComments().forEach { comment ->
            val commentBinding = ItemCommunityCommentBinding.inflate(
                layoutInflater,
                binding.commentsContainer,
                false
            )
            commentBinding.root.setPaddingRelative(
                if (comment.parentCommentId == null) 0 else resources.displayMetrics.density.times(24).toInt(),
                0,
                0,
                0
            )
            commentBinding.commentAvatar.text = comment.author.take(1).uppercase()
            commentBinding.commentAuthor.text = comment.author
            commentBinding.commentTime.text = comment.timeText
            commentBinding.commentText.text = comment.text
            commentBinding.commentReply.setOnClickListener {
                replyingToCommentId = comment.id
                val mention = "@${comment.author} "
                binding.commentInput.setText(mention)
                binding.commentInput.setSelection(binding.commentInput.length())
                binding.commentInput.requestFocus()
                binding.detailScroll.post {
                    binding.detailScroll.fullScroll(View.FOCUS_DOWN)
                }
            }
            binding.commentsContainer.addView(commentBinding.root)
        }
        renderEngagement()
    }

    private fun orderedComments(): List<CommunityComment> {
        val byParent = comments.groupBy { it.parentCommentId }
        val ordered = mutableListOf<CommunityComment>()
        val added = mutableSetOf<String>()

        fun addThread(parentId: String?) {
            byParent[parentId].orEmpty().forEach { comment ->
                if (added.add(comment.id)) {
                    ordered += comment
                    addThread(comment.id)
                }
            }
        }

        addThread(null)
        comments.forEach { comment ->
            if (added.add(comment.id)) ordered += comment
        }
        return ordered
    }

    private fun finishWithResult() {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_POST_ID, postId)
                .putExtra(EXTRA_LIKES, likes)
                .putExtra(EXTRA_COMMENTS, commentsCount)
                .putExtra(EXTRA_LIKED, liked)
        )
        finish()
    }

    companion object {
        const val EXTRA_POST_ID = "community_post_id"
        const val EXTRA_AUTHOR = "community_post_author"
        const val EXTRA_TIME = "community_post_time"
        const val EXTRA_NEIGHBORHOOD = "community_post_neighborhood"
        const val EXTRA_BODY = "community_post_body"
        const val EXTRA_LIKES = "community_post_likes"
        const val EXTRA_COMMENTS = "community_post_comments"
        const val EXTRA_LIKED = "community_post_liked"
        const val EXTRA_PHOTO = "community_post_photo"
    }
}
