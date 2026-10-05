package com.example.weanimals.entry.presentation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.weanimals.R
import kotlin.math.abs
import kotlin.math.min

/** Reveals the original dog-and-cat mark in a continuous loading loop. */
class SilhouetteLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val strokeColor = ContextCompat.getColor(context, R.color.brass500)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = strokeColor
    }
    private val maskBitmap = createMask(
        BitmapFactory.decodeResource(resources, R.drawable.weanimals_loading_source)
    )
    private var revealProgress = 0f
    private var animator: ValueAnimator? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = maskBitmap ?: return
        val side = min(width, height).toFloat()
        if (side <= 0f) return

        canvas.save()
        canvas.translate((width - side) / 2f, (height - side) / 2f)
        canvas.scale(side / bitmap.width, side / bitmap.height)

        bitmapPaint.alpha = 45
        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)

        canvas.save()
        canvas.clipRect(0f, 0f, bitmap.width * revealProgress, bitmap.height.toFloat())
        bitmapPaint.alpha = 255
        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
        canvas.restore()
        canvas.restore()
    }

    fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                revealProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
        revealProgress = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    /** Converts the white line art in the reference PNG into a transparent mask. */
    private fun createMask(source: Bitmap?): Bitmap? {
        source ?: return null
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

        val left = source.width * 0.035f
        val right = source.width * 0.965f
        val top = source.height * 0.02f
        val bottom = source.height * 0.95f
        val radius = source.width * 0.15f
        // The reference image has a rounded app-icon frame around the mark.
        // Keep only the central illustration so that frame never reaches the mask.
        val contentLeft = source.width * 0.12f
        val contentRight = source.width * 0.92f
        val contentTop = source.height * 0.20f
        val contentBottom = source.height * 0.86f

        pixels.indices.forEach { index ->
            val x = index % source.width
            val y = index / source.width
            val insideLogoTile = isInsideRoundedRect(
                x.toFloat(), y.toFloat(), left, top, right, bottom, radius
            )
            val insideIllustration = x in contentLeft.toInt()..contentRight.toInt() &&
                y in contentTop.toInt()..contentBottom.toInt()
            val color = pixels[index]
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            val luminance = (red + green + blue) / 3
            val isWhiteLine = insideLogoTile && insideIllustration &&
                luminance > 125 &&
                abs(red - green) < 45 &&
                abs(green - blue) < 45
            val alpha = if (isWhiteLine) ((luminance - 100) * 255 / 155).coerceIn(0, 255) else 0
            pixels[index] = Color.argb(
                alpha,
                Color.red(strokeColor),
                Color.green(strokeColor),
                Color.blue(strokeColor)
            )
        }

        return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        }
    }

    private fun isInsideRoundedRect(
        x: Float,
        y: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float
    ): Boolean {
        if (x in (left + radius)..(right - radius) || y in (top + radius)..(bottom - radius)) {
            return x in left..right && y in top..bottom
        }

        val cornerCenterX = if (x < left + radius) left + radius else right - radius
        val cornerCenterY = if (y < top + radius) top + radius else bottom - radius
        val dx = x - cornerCenterX
        val dy = y - cornerCenterY
        return dx * dx + dy * dy <= radius * radius
    }
}
