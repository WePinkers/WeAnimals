package com.example.weanimals.community.feed.presentation

import android.graphics.Typeface
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.create.presentation.CreateCommunityPostActivity
import com.example.weanimals.community.detail.presentation.CommunityPostDetailActivity
import com.example.weanimals.community.campaign.presentation.CampaignDetailActivity
import com.example.weanimals.community.campaign.repository.CommunityCampaignRepository
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.community.feed.domain.CommunityFilter
import com.example.weanimals.community.feed.presenter.CommunityContract
import com.example.weanimals.community.detail.repository.CommunityPostEngagementRepository
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityCommunityBinding
import com.google.android.material.chip.Chip
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority

class CommunityActivity : AppCompatActivity(), CommunityContract.View {
    private lateinit var binding: ActivityCommunityBinding
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityPresenter()
    }
    private val engagementRepository: CommunityPostEngagementRepository by lazy {
        (application as WeAnimalsApplication).appContainer
            .createCommunityPostEngagementRepository()
    }
    private val campaignRepository: CommunityCampaignRepository by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityCampaignRepository()
    }
    private val feedAdapter = CommunityFeedAdapter(
        onCampaignClick = ::openCampaignDetail,
        onCampaignActionClick = ::handleCampaignAction,
        onPostCommentClick = ::openPostDetail,
        onPostLikeClick = ::toggleLike
    )
    private val engagementOverrides = mutableMapOf<String, EngagementOverride>()
    private var currentItems: List<CommunityFeedItem> = emptyList()
    private var selectedFilterId = R.id.filter_all
    private var contextFilter = CommunityFilter.CAMPAIGNS
    private var contextOptions: List<Pair<Int, CommunityFilter>> = emptyList()
    private var contextFilterAdapter: ContextFilterAdapter? = null
    private var neighborhoodPermissionPending = false
    private val campaignActionsInProgress = mutableSetOf<String>()

    private val postDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val postId = data.getStringExtra(CommunityPostDetailActivity.EXTRA_POST_ID)
            ?: return@registerForActivityResult
        updatePost(
            postId = postId,
            likes = data.getIntExtra(CommunityPostDetailActivity.EXTRA_LIKES, 0),
            comments = data.getIntExtra(CommunityPostDetailActivity.EXTRA_COMMENTS, 0),
            liked = data.getBooleanExtra(CommunityPostDetailActivity.EXTRA_LIKED, false)
        )
    }

    private val createPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) presenter.load()
    }

    private val neighborhoodPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            checkLocationSettingsAndLoadNeighborhood()
        } else {
            neighborhoodPermissionPending = false
            presenter.onFilterSelected(neighborhoodFilter())
        }
    }

    private val neighborhoodSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        neighborhoodPermissionPending = false
        if (result.resultCode == RESULT_OK) {
            presenter.onFilterSelected(neighborhoodFilter())
        } else {
            presenter.onFilterSelected(neighborhoodFilter())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.COMMUNITY)
        binding.feedList.layoutManager = LinearLayoutManager(this)
        binding.feedList.adapter = feedAdapter
        selectedFilterId = savedInstanceState?.getInt(STATE_FILTER) ?: R.id.filter_all
        if (selectedFilterId !in mainFilterIds()) selectedFilterId = R.id.filter_all
        contextFilter = savedInstanceState?.getString(STATE_CONTEXT_FILTER)
            ?.let { runCatching { CommunityFilter.valueOf(it) }.getOrNull() }
            ?: CommunityFilter.CAMPAIGNS
        binding.searchFieldContainer.isVisible =
            savedInstanceState?.getBoolean(STATE_SEARCH_VISIBLE) ?: false
        binding.searchButton.contentDescription = getString(
            if (binding.searchFieldContainer.isVisible) R.string.community_close_search
            else R.string.community_search
        )
        binding.clearSearchButton.isVisible = !binding.searchInput.text.isNullOrEmpty()
        binding.searchButton.setOnClickListener {
            val showing = !binding.searchFieldContainer.isVisible
            binding.searchFieldContainer.isVisible = showing
            binding.searchButton.contentDescription = getString(
                if (showing) R.string.community_close_search else R.string.community_search
            )
            if (showing) {
                binding.searchInput.requestFocus()
                binding.searchInput.post {
                    WindowCompat.getInsetsController(window, binding.searchInput)
                        .show(WindowInsetsCompat.Type.ime())
                }
            } else {
                binding.searchInput.text?.clear()
                WindowCompat.getInsetsController(window, binding.searchInput)
                    .hide(WindowInsetsCompat.Type.ime())
            }
        }
        binding.createButton.setOnClickListener {
            createPostLauncher.launch(Intent(this, CreateCommunityPostActivity::class.java))
        }
        filterChips().forEach { chip ->
            chip.setOnClickListener {
                binding.contextFilterDropdown.clearFocus()
                selectedFilterId = chip.id
                updateContextFilter()
                updateFilterAppearance()
                binding.feedList.scrollToPosition(0)
                selectCurrentFilter()
            }
        }
        binding.contextFilterDropdown.setOnItemClickListener { _, _, position, _ ->
            contextOptions.getOrNull(position)?.second?.let { selectedContextFilter ->
                contextFilter = selectedContextFilter
                contextFilterAdapter?.selectedPosition = position
                contextFilterAdapter?.notifyDataSetChanged()
                binding.contextFilterDropdown.setListSelection(position)
                binding.contextFilterDropdown.clearFocus()
                binding.feedList.scrollToPosition(0)
                selectCurrentFilter()
            }
        }
        binding.contextFilterDropdown.setOnClickListener {
            binding.contextFilterDropdown.showDropDown()
        }
        binding.contextFilterDropdown.setOnTouchListener { view, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                view.post { binding.contextFilterDropdown.showDropDown() }
            }
            false
        }
        binding.contextFilterDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.contextFilterDropdown.post {
                binding.contextFilterDropdown.showDropDown()
            }
        }
        binding.contextFilterLayout.setOnClickListener {
            binding.contextFilterDropdown.showDropDown()
        }
        binding.contextFilterDropdown.setOnDismissListener {
            binding.contextFilterDropdown.clearFocus()
        }
        binding.searchInput.doAfterTextChanged {
            binding.clearSearchButton.isVisible = !it.isNullOrEmpty()
            presenter.onSearchChanged(it?.toString().orEmpty())
        }
        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.text?.clear()
            binding.searchInput.requestFocus()
        }
        updateContextFilter()
        updateFilterAppearance()
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        if (selectedFilter().requiresNeighborhood()) {
            selectCurrentFilter()
        } else {
            presenter.onFilterSelected(selectedFilter())
        }
        presenter.onSearchChanged(binding.searchInput.text?.toString().orEmpty())
        presenter.load()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        binding.feedList.adapter = null
        presenter.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_FILTER, selectedFilterId)
        outState.putString(STATE_CONTEXT_FILTER, contextFilter.name)
        outState.putBoolean(STATE_SEARCH_VISIBLE, binding.searchFieldContainer.isVisible)
        super.onSaveInstanceState(outState)
    }

    private fun updateFilterAppearance() {
        filterChips().forEach { chip ->
            val active = chip.id == selectedFilterId
            chip.isChecked = active
            chip.typeface = Typeface.create("sans-serif", if (active) Typeface.BOLD else Typeface.NORMAL)
            chip.setChipBackgroundColorResource(if (active) R.color.pine800 else R.color.surface)
            chip.setChipStrokeColorResource(R.color.border)
            chip.chipStrokeWidth = if (active) 0f else resources.displayMetrics.density
            chip.setTextColor(ContextCompat.getColor(this, if (active) R.color.white else R.color.ink900))
        }
    }

    private fun filterChips(): List<Chip> = listOf(
        binding.filterAll, binding.filterCampaigns, binding.filterPosts
    )

    override fun showLoading() {
        binding.feedList.isVisible = false
        binding.feedState.isVisible = true
        binding.stateTitle.setText(R.string.community_title)
        binding.stateMessage.setText(R.string.community_loading)
    }

    override fun showNeighborhoodLoading() {
        binding.feedList.isVisible = false
        binding.feedState.isVisible = true
        binding.stateTitle.setText(R.string.community_empty_title)
        binding.stateMessage.setText(R.string.community_neighborhood_loading)
    }

    override fun showItems(items: List<CommunityFeedItem>) {
        currentItems = items.map { item ->
            if (item is CommunityFeedItem.Post) {
                engagementOverrides[item.id]?.let { override ->
                    item.copy(
                        likes = override.likes,
                        comments = override.comments,
                        likedByCurrentUser = override.liked
                    )
                } ?: item
            } else {
                item
            }
        }
        feedAdapter.submitList(currentItems)
        binding.feedState.isVisible = false
        binding.feedList.isVisible = true
    }

    override fun showEmpty(filter: CommunityFilter, query: String) {
        currentItems = emptyList()
        feedAdapter.submitList(emptyList())
        binding.feedList.isVisible = false
        binding.feedState.isVisible = true
        binding.stateTitle.setText(R.string.community_empty_title)
        binding.stateMessage.text = if (filter.requiresNeighborhood()) {
            getString(R.string.community_empty_neighborhood)
        } else if (query.isNotEmpty()) {
            getString(R.string.community_empty_search, query)
        } else {
            getString(when (filter) {
                CommunityFilter.CAMPAIGNS -> R.string.community_empty_campaigns
                CommunityFilter.POSTS -> R.string.community_empty_posts
                CommunityFilter.FOUND -> R.string.community_empty_found
                CommunityFilter.QUESTION -> R.string.community_empty_question
                CommunityFilter.NOTICE -> R.string.community_empty_notice
                CommunityFilter.NEUTERING -> R.string.community_empty_neutering
                CommunityFilter.VACCINATION -> R.string.community_empty_vaccination
                CommunityFilter.ADOPTION -> R.string.community_empty_adoption
                CommunityFilter.DONATION -> R.string.community_empty_donation
                else -> R.string.community_empty_message
            })
        }
    }

    override fun showNeighborhoodUnavailable() {
        binding.feedList.isVisible = false
        binding.feedState.isVisible = true
        binding.stateTitle.setText(R.string.community_empty_title)
        binding.stateMessage.setText(R.string.community_neighborhood_unavailable)
    }

    override fun showError() {
        binding.feedList.isVisible = false
        binding.feedState.isVisible = true
        binding.stateTitle.setText(R.string.community_empty_title)
        binding.stateMessage.setText(R.string.community_load_error)
    }

    private fun openPostDetail(item: CommunityFeedItem.Post) {
        val intent = Intent(this, CommunityPostDetailActivity::class.java)
            .putExtra(CommunityPostDetailActivity.EXTRA_POST_ID, item.id)
            .putExtra(CommunityPostDetailActivity.EXTRA_AUTHOR, item.author)
            .putExtra(CommunityPostDetailActivity.EXTRA_TIME, item.timeText)
            .putExtra(CommunityPostDetailActivity.EXTRA_NEIGHBORHOOD, item.neighborhood.orEmpty())
            .putExtra(CommunityPostDetailActivity.EXTRA_BODY, item.body)
            .putExtra(CommunityPostDetailActivity.EXTRA_LIKES, item.likes)
            .putExtra(CommunityPostDetailActivity.EXTRA_COMMENTS, item.comments)
            .putExtra(CommunityPostDetailActivity.EXTRA_LIKED, item.likedByCurrentUser)
        item.photoData?.let { intent.putExtra(CommunityPostDetailActivity.EXTRA_PHOTO, it) }
        postDetailLauncher.launch(intent)
    }

    private fun openCampaignDetail(item: CommunityFeedItem.Campaign) {
        startActivity(CampaignDetailActivity.newIntent(this, item))
    }

    private fun handleCampaignAction(item: CommunityFeedItem.Campaign) {
        if (item.category == CommunityCategory.DONATION) {
            openCampaignDetail(item)
            return
        }
        if (!campaignActionsInProgress.add(item.id)) return
        lifecycleScope.launch {
            campaignRepository.participate(item.id).fold(
                onSuccess = { result ->
                    updateCampaignParticipation(item.id, result.filledSlots, result.totalSlots)
                },
                onFailure = {
                    Toast.makeText(
                        this@CommunityActivity,
                        R.string.community_campaign_participation_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            campaignActionsInProgress.remove(item.id)
        }
    }

    private fun updateCampaignParticipation(campaignId: String, filledSlots: Int, totalSlots: Int?) {
        currentItems = currentItems.map { item ->
            if (item is CommunityFeedItem.Campaign && item.id == campaignId) {
                item.copy(
                    filledSlots = filledSlots,
                    totalSlots = totalSlots ?: item.totalSlots,
                    participatingByCurrentUser = true
                )
            } else {
                item
            }
        }
        feedAdapter.submitList(currentItems)
    }

    private fun toggleLike(item: CommunityFeedItem.Post) {
        lifecycleScope.launch {
            engagementRepository.toggleLike(item.id, !item.likedByCurrentUser)
                .onSuccess { engagement ->
                    updatePost(
                        postId = item.id,
                        likes = engagement.likes,
                        comments = engagement.comments,
                        liked = engagement.likedByCurrentUser
                    )
                }
                .onFailure {
                    Toast.makeText(
                        this@CommunityActivity,
                        R.string.community_like_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun updatePost(postId: String, likes: Int, comments: Int, liked: Boolean) {
        engagementOverrides[postId] = EngagementOverride(likes, comments, liked)
        currentItems = currentItems.map { item ->
            if (item is CommunityFeedItem.Post && item.id == postId) {
                item.copy(
                    likes = likes,
                    comments = comments,
                    likedByCurrentUser = liked
                )
            } else {
                item
            }
        }
        feedAdapter.submitList(currentItems)
    }

    private data class EngagementOverride(
        val likes: Int,
        val comments: Int,
        val liked: Boolean
    )

    private fun selectedFilter() = when (selectedFilterId) {
        R.id.filter_campaigns -> contextFilter.takeIf(::isCampaignContextFilter)
            ?: CommunityFilter.CAMPAIGNS
        R.id.filter_posts -> contextFilter.takeIf(::isPostContextFilter)
            ?: CommunityFilter.POSTS
        R.id.filter_found -> CommunityFilter.FOUND
        R.id.filter_neutering -> CommunityFilter.NEUTERING
        R.id.filter_vaccination -> CommunityFilter.VACCINATION
        R.id.filter_neighborhood -> CommunityFilter.MY_NEIGHBORHOOD
        else -> CommunityFilter.ALL
    }

    private fun selectCurrentFilter() {
        val filter = selectedFilter()
        if (!filter.requiresNeighborhood()) {
            presenter.onFilterSelected(filter)
            return
        }
        if (hasLocationPermission()) {
            checkLocationSettingsAndLoadNeighborhood()
        } else if (!neighborhoodPermissionPending) {
            neighborhoodPermissionPending = true
            neighborhoodPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun checkLocationSettingsAndLoadNeighborhood() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L
        ).setMinUpdateIntervalMillis(5_000L).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()
        LocationServices.getSettingsClient(this)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                neighborhoodPermissionPending = false
                presenter.onFilterSelected(neighborhoodFilter())
            }
            .addOnFailureListener { error ->
                neighborhoodPermissionPending = false
                if (error is ResolvableApiException
                    && error.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED
                ) {
                    neighborhoodSettingsLauncher.launch(
                        IntentSenderRequest.Builder(error.resolution).build()
                    )
                } else {
                    presenter.onFilterSelected(neighborhoodFilter())
                }
            }
    }

    private fun updateContextFilter() {
        contextOptions = when (selectedFilterId) {
            R.id.filter_campaigns -> listOf(
                R.string.community_filter_context_all to CommunityFilter.CAMPAIGNS,
                R.string.community_filter_vaccination to CommunityFilter.VACCINATION,
                R.string.community_campaign_adoption to CommunityFilter.ADOPTION,
                R.string.community_campaign_donation to CommunityFilter.DONATION,
                R.string.community_filter_neutering to CommunityFilter.NEUTERING,
                R.string.community_filter_neighborhood to CommunityFilter.CAMPAIGN_NEIGHBORHOOD
            )
            R.id.filter_posts -> listOf(
                R.string.community_filter_context_all to CommunityFilter.POSTS,
                R.string.community_filter_found to CommunityFilter.FOUND,
                R.string.community_filter_question to CommunityFilter.QUESTION,
                R.string.community_filter_notice to CommunityFilter.NOTICE,
                R.string.community_filter_neighborhood to CommunityFilter.POST_NEIGHBORHOOD
            )
            else -> emptyList()
        }
        val isContextual = contextOptions.isNotEmpty()
        binding.contextFilterContainer.isVisible = isContextual
        if (!isContextual) return

        if (contextOptions.none { it.second == contextFilter }) {
            contextFilter = contextOptions.first().second
        }
        val selectedPosition = contextOptions.indexOfFirst { it.second == contextFilter }
            .coerceAtLeast(0)
        contextFilterAdapter = ContextFilterAdapter(
            contextOptions.map { getString(it.first) },
            selectedPosition
        )
        binding.contextFilterDropdown.setAdapter(
            contextFilterAdapter
        )
        binding.contextFilterDropdown.setDropDownBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.bg_community_filter_dropdown)
        )
        binding.contextFilterDropdown.dropDownVerticalOffset = 6
        binding.contextFilterDropdown.setListSelection(selectedPosition)
        binding.contextFilterDropdown.setText(
            getString(contextOptions[selectedPosition].first),
            false
        )
        binding.contextFilterDropdown.clearFocus()
    }

    private inner class ContextFilterAdapter(
        labels: List<String>,
        var selectedPosition: Int
    ) : ArrayAdapter<String>(
        this@CommunityActivity,
        R.layout.item_community_filter_option,
        labels
    ) {
        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = super.getDropDownView(position, convertView, parent)
            applySelectionState(view, position)
            return view
        }

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view = super.getView(position, convertView, parent)
            applySelectionState(view, position)
            return view
        }

        private fun applySelectionState(view: View, position: Int) {
            val selected = position == selectedPosition
            view.isActivated = selected
            view.isSelected = selected
        }
    }

    private fun mainFilterIds() = setOf(
        R.id.filter_all,
        R.id.filter_campaigns,
        R.id.filter_posts
    )

    private fun isCampaignContextFilter(filter: CommunityFilter) = filter in setOf(
        CommunityFilter.CAMPAIGNS,
        CommunityFilter.VACCINATION,
        CommunityFilter.ADOPTION,
        CommunityFilter.DONATION,
        CommunityFilter.NEUTERING,
        CommunityFilter.CAMPAIGN_NEIGHBORHOOD
    )

    private fun isPostContextFilter(filter: CommunityFilter) = filter in setOf(
        CommunityFilter.POSTS,
        CommunityFilter.FOUND,
        CommunityFilter.QUESTION,
        CommunityFilter.NOTICE,
        CommunityFilter.POST_NEIGHBORHOOD
    )

    private fun neighborhoodFilter(): CommunityFilter = selectedFilter()

    private fun CommunityFilter.requiresNeighborhood(): Boolean = this ==
        CommunityFilter.MY_NEIGHBORHOOD || this == CommunityFilter.CAMPAIGN_NEIGHBORHOOD ||
        this == CommunityFilter.POST_NEIGHBORHOOD

    private companion object {
        const val STATE_FILTER = "selected_community_filter"
        const val STATE_CONTEXT_FILTER = "selected_community_context_filter"
        const val STATE_SEARCH_VISIBLE = "community_search_visible"
    }
}
