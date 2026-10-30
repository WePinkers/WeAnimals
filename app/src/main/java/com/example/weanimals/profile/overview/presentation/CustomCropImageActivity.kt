package com.example.weanimals.profile.overview.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContract
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView

class CustomCropImageActivity : CropImageActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cropImageView = findViewById<CropImageView>(com.canhub.cropper.R.id.cropImageView)
        if (cropImageView != null) {
            val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    resetCropToInitialState(cropImageView)
                    return true
                }
            })

            cropImageView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }
    }

    private fun resetCropToInitialState(cropImageView: CropImageView) {
        cropImageView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        cropImageView.resetCropRect()
        cropImageView.scaleType = CropImageView.ScaleType.FIT_CENTER
    }
}

class CustomCropImageContract : ActivityResultContract<CropImageContractOptions, CropImageView.CropResult>() {
    private val delegateContract = CropImageContract()

    override fun createIntent(context: Context, input: CropImageContractOptions): Intent {
        val intent = delegateContract.createIntent(context, input)
        intent.setClass(context, CustomCropImageActivity::class.java)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CropImageView.CropResult {
        return delegateContract.parseResult(resultCode, intent)
    }
}
