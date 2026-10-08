package com.example.weanimals.profile.overview.presentation

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.repository.CommunityRepositoryFactory
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityProfileBinding
import com.example.weanimals.databinding.DialogEditProfileBinding
import com.example.weanimals.databinding.DialogLogoutConfirmationBinding
import com.example.weanimals.databinding.DialogPhotoOptionsBinding
import com.example.weanimals.entry.presentation.EntryActivity
import com.example.weanimals.profile.campaigns.presentation.CampaignsActivity
import com.example.weanimals.profile.favorites.presentation.FavoritesActivity
import com.example.weanimals.profile.overview.presenter.ProfileContract
import com.example.weanimals.profile.reports.presentation.MyReportsActivity
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity(), ProfileContract.View {
    private lateinit var binding: ActivityProfileBinding
    private var logoutDialog: Dialog? = null
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createProfilePresenter()
    }
    private val communityRepository by lazy { CommunityRepositoryFactory.create() }

    private var nomeAtual = "Usuário"
    private var bioAtual: String? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            showCropDialog(uri)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val tempFile = File(cacheDir, "temp_camera_photo.jpg")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val uri = Uri.fromFile(tempFile)
                showCropDialog(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCropDialog(imageUri: Uri) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }

        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }
            scaleType = ImageView.ScaleType.MATRIX
            setImageURI(imageUri)
        }

        val matrix = Matrix()
        val savedMatrix = Matrix()
        var mode = 0
        var startX = 0f
        var startY = 0f
        var oldDist = 1f

        imageView.setOnTouchListener { v, event ->
            val view = v as ImageView
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    startX = event.x
                    startY = event.y
                    mode = 1
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        mode = 2
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = 0
                    v.performClick()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == 1) {
                        matrix.set(savedMatrix)
                        matrix.postTranslate(event.x - startX, event.y - startY)
                    } else if (mode == 2) {
                        val newDist = spacing(event)
                        if (newDist > 10f) {
                            matrix.set(savedMatrix)
                            val scale = newDist / oldDist
                            matrix.postScale(scale, scale, view.width / 2f, view.height / 2f)
                        }
                    }
                }
            }
            view.imageMatrix = matrix
            true
        }

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.BLACK)
        }

        val cancelButton = Button(this).apply {
            text = "Cancelar"
            setOnClickListener { dialog.dismiss() }
        }

        val saveButton = Button(this).apply {
            text = "Salvar"
            setOnClickListener {
                if (imageView.width > 0 && imageView.height > 0) {
                    val bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    imageView.draw(canvas)
                    saveProfilePicture(bitmap)
                }
                dialog.dismiss()
            }
        }

        buttonLayout.addView(cancelButton)
        buttonLayout.addView(saveButton)
        layout.addView(imageView)
        layout.addView(buttonLayout)
        dialog.setContentView(layout)
        dialog.show()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.PROFILE)
        setupProfileActions()
        loadSavedProfilePicture()
    }

    private fun setupProfileActions() {
        binding.profileContent.profileHeader.profileSettings.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Editar Perfil")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        showEditProfileDialog()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        binding.profileContent.profileHeader.profileBio.setOnClickListener {
            showEditProfileDialog()
        }
        binding.profileContent.profileHeader.profileEditPhoto.setOnClickListener {
            showPhotoOptions()
        }
        binding.profileContent.itemMyReports.root.setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }
        binding.profileContent.itemCampaigns.root.setOnClickListener {
            startActivity(Intent(this, CampaignsActivity::class.java))
        }
        binding.profileContent.itemFavorites.root.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        binding.profileContent.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
        setupAccountSection()
        updateHeader()
    }

    private fun setupAccountSection() {
        val hasPasswordProvider = FirebaseAuth.getInstance().currentUser?.providerData?.any {
            it.providerId == "password"
        } == true

        if (hasPasswordProvider) {
            binding.profileContent.sectionAccount.visibility = View.VISIBLE
            binding.profileContent.itemChangePassword.rootChangePassword.setOnClickListener {
                startActivity(Intent(this, ChangePasswordActivity::class.java))
            }
        } else {
            binding.profileContent.sectionAccount.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start()
        loadCampaignSummary()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showIdentity(name: String?) {
        if (!name.isNullOrBlank()) {
            nomeAtual = name
            updateHeader()
        }
    }

    override fun showReportCount(count: Int) {
        binding.profileContent.reportCount.text = count.toString()
    }

    override fun showReportsError() {
        binding.profileContent.reportCount.text = "—"
    }

    private fun updateHeader() {
        binding.profileContent.profileHeader.profileName.text = nomeAtual
        binding.profileContent.profileHeader.profileAvatarInitial.text =
            nomeAtual.firstOrNull()?.uppercase() ?: "U"
        binding.profileContent.profileHeader.profileBio.text =
            bioAtual?.takeIf(String::isNotBlank) ?: "Toque para adicionar bio"
    }





    private fun rotateBitmapIfNeeded(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun saveProfilePicture(bitmap: Bitmap) {
        try {
            val file = File(filesDir, "profile_picture.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
            prefs.edit().putString("profile_picture_path", file.absolutePath).apply()

            val orientedBitmap = rotateBitmapIfNeeded(bitmap, file.absolutePath)
            binding.profileContent.profileHeader.profileAvatarImage.setImageBitmap(orientedBitmap)
            binding.profileContent.profileHeader.profileAvatarImage.visibility = View.VISIBLE
            binding.profileContent.profileHeader.profileAvatarInitial.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveProfilePicture(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_picture.jpg")
            FileOutputStream(file).use { out ->
                inputStream?.copyTo(out)
            }
            val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
            prefs.edit().putString("profile_picture_path", file.absolutePath).apply()

            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
            val bitmap = if (rawBitmap != null) rotateBitmapIfNeeded(rawBitmap, file.absolutePath) else null

            if (bitmap != null) {
                binding.profileContent.profileHeader.profileAvatarImage.setImageBitmap(bitmap)
            } else {
                binding.profileContent.profileHeader.profileAvatarImage.setImageURI(uri)
            }
            binding.profileContent.profileHeader.profileAvatarImage.visibility = View.VISIBLE
            binding.profileContent.profileHeader.profileAvatarInitial.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeProfilePicture() {
        val file = File(filesDir, "profile_picture.jpg")
        if (file.exists()) {
            file.delete()
        }
        val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
        prefs.edit().remove("profile_picture_path").apply()

        binding.profileContent.profileHeader.profileAvatarImage.setImageDrawable(null)
        binding.profileContent.profileHeader.profileAvatarImage.visibility = View.GONE
        binding.profileContent.profileHeader.profileAvatarInitial.visibility = View.VISIBLE
    }

    private fun loadSavedProfilePicture() {
        val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
        val path = prefs.getString("profile_picture_path", null)
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists()) {
                val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (rawBitmap != null) {
                    val bitmap = rotateBitmapIfNeeded(rawBitmap, file.absolutePath)
                    binding.profileContent.profileHeader.profileAvatarImage.setImageBitmap(bitmap)
                    binding.profileContent.profileHeader.profileAvatarImage.visibility = View.VISIBLE
                    binding.profileContent.profileHeader.profileAvatarInitial.visibility = View.GONE
                }
            }
        }
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        dialogBinding.editName.setText(nomeAtual)
        dialogBinding.editBio.setText(bioAtual.orEmpty())
        AlertDialog.Builder(this)
            .setTitle("Editar perfil")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val newName = dialogBinding.editName.text.toString().trim()
                if (newName.isNotBlank()) nomeAtual = newName
                bioAtual = dialogBinding.editBio.text.toString().trim().takeIf(String::isNotBlank)
                updateHeader()
            }
            .show()
    }

    private fun showPhotoOptions() {
        val dialog = Dialog(this)
        val dialogBinding = DialogPhotoOptionsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setTitle("Foto do perfil")
        dialogBinding.takePhotoButton.setOnClickListener {
            dialog.dismiss()
            cameraLauncher.launch(null)
        }
        dialogBinding.choosePhotoButton.setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch("image/*")
        }
        dialogBinding.removePhotoButton.setOnClickListener {
            dialog.dismiss()
            removeProfilePicture()
        }
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.86f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }
        dialog.show()
    }

    private fun loadCampaignSummary() {
        lifecycleScope.launch {
            communityRepository.getFeed().fold(
                onSuccess = { items ->
                    val count = items
                        .filterIsInstance<CommunityFeedItem.Campaign>()
                        .count { it.participatingByCurrentUser }
                    binding.profileContent.campaignCount.text = count.toString()
                    binding.profileContent.itemCampaigns.campaignsDescription.text = if (count == 0) {
                        getString(R.string.profile_activity_campaigns_desc)
                    } else {
                        resources.getQuantityString(
                            R.plurals.profile_campaigns_participation_count,
                            count,
                            count
                        )
                    }
                },
                onFailure = {
                    binding.profileContent.campaignCount.text = "—"
                    binding.profileContent.itemCampaigns.campaignsDescription.setText(
                        R.string.profile_campaigns_count_error
                    )
                }
            )
        }
    }

    private fun showLogoutConfirmation() {
        logoutDialog?.dismiss()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogLogoutConfirmationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialogBinding.dialogLogoutCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.dialogLogoutConfirm.setOnClickListener {
            dialog.dismiss()
            signOut()
        }
        dialog.setOnDismissListener {
            if (logoutDialog === dialog) logoutDialog = null
        }
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.86f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { dimAmount = 0.58f }
            }
        }
        logoutDialog = dialog
        dialog.show()
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
            .signOut()
            .addOnCompleteListener { openEntry() }
    }

    private fun openEntry() {
        startActivity(
            Intent(this, EntryActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }
}
