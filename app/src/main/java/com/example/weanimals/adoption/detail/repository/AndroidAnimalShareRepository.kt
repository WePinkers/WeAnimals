package com.example.weanimals.adoption.detail.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.weanimals.R
import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.domain.AnimalShareDocument
import com.example.weanimals.adoption.detail.domain.EnergyLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

class AndroidAnimalShareRepository(context: Context) : AnimalShareRepository {

    private val applicationContext = context.applicationContext
    private val resources = applicationContext.resources
    private val distanceFormat = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("pt-BR")
    ).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    override suspend fun createPdf(
        details: AnimalDetails,
        distanceKm: Double?
    ): Result<AnimalShareDocument> = withContext(Dispatchers.IO) {
        try {
            val cover = loadCover(details.photoUrls.firstOrNull())
            val directory = File(applicationContext.cacheDir, SHARE_DIRECTORY).apply {
                check(exists() || mkdirs()) { "Could not create PDF cache directory." }
            }
            val displayName = "WeAnimals_${safeFilePart(details.name)}_${safeFilePart(details.id)}.pdf"
            val outputFile = File(directory, displayName)
            createDocument(details, distanceKm, cover, outputFile)
            Result.success(
                AnimalShareDocument(
                    absolutePath = outputFile.absolutePath,
                    displayName = displayName,
                    mimeType = PDF_MIME_TYPE
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun loadCover(photoUrl: String?): Bitmap? {
        if (photoUrl.isNullOrBlank()) return null
        return try {
            val request = ImageRequest.Builder(applicationContext)
                .data(photoUrl)
                .allowHardware(false)
                .size(PHOTO_REQUEST_WIDTH, PHOTO_REQUEST_HEIGHT)
                .build()
            val result = applicationContext.imageLoader.execute(request) as? SuccessResult
            result?.drawable?.toBitmap(
                width = PHOTO_REQUEST_WIDTH,
                height = PHOTO_REQUEST_HEIGHT,
                config = Bitmap.Config.ARGB_8888
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun createDocument(
        details: AnimalDetails,
        distanceKm: Double?,
        cover: Bitmap?,
        outputFile: File
    ) {
        val document = PdfDocument()
        try {
            val writer = PdfWriter(document, details.name)
            writer.drawBrandTitle(resources.getString(R.string.adoption_detail_share_pdf_title))
            writer.drawCover(cover)
            writer.drawAnimalName(details.name)

            val months = waitingMonths(details.createdAtMillis)
            writer.drawHighlightedText(
                resources.getQuantityString(
                    R.plurals.adoption_detail_waiting_months,
                    months,
                    months
                )
            )

            val basicInformation = buildList {
                if (details.breed.isNotBlank()) {
                    add(resources.getString(R.string.adoption_detail_share_breed, details.breed))
                }
                if (details.ageText.isNotBlank()) {
                    add(resources.getString(R.string.adoption_detail_share_age, details.ageText))
                }
                if (details.size.isNotBlank()) {
                    add(resources.getString(R.string.adoption_detail_share_size, details.size))
                }
                val care = buildList {
                    if (details.vaccinated) {
                        add(resources.getString(R.string.adoption_detail_vaccinated))
                    }
                    if (details.neutered) {
                        add(resources.getString(R.string.adoption_detail_neutered))
                    }
                }
                if (care.isNotEmpty()) {
                    add(resources.getString(
                        R.string.adoption_detail_share_health,
                        care.joinToString(" · ")
                    ))
                }
            }
            writer.drawLines(basicInformation)

            val compatibility = buildList {
                details.goodWithChildren?.let { goodWithChildren ->
                    add(resources.getString(
                        if (goodWithChildren) R.string.adoption_detail_good_with_children
                        else R.string.adoption_detail_not_good_with_children
                    ))
                }
                details.goodWithOtherAnimals?.let { goodWithAnimals ->
                    add(resources.getString(
                        if (goodWithAnimals) R.string.adoption_detail_good_with_animals
                        else R.string.adoption_detail_not_good_with_animals
                    ))
                }
                details.energyLevel?.let { energyLevel ->
                    add(resources.getString(when (energyLevel) {
                        EnergyLevel.LOW -> R.string.adoption_detail_energy_low
                        EnergyLevel.MODERATE -> R.string.adoption_detail_energy_moderate
                        EnergyLevel.HIGH -> R.string.adoption_detail_energy_high
                    }))
                }
            }
            writer.drawSection(
                resources.getString(R.string.adoption_detail_compatibility),
                compatibility
            )

            if (details.description.isNotBlank()) {
                writer.drawSection(
                    resources.getString(
                        R.string.adoption_detail_about,
                        details.name.uppercase(Locale.forLanguageTag("pt-BR"))
                    ),
                    listOf(details.description)
                )
            }

            writer.drawSection(
                resources.getString(R.string.adoption_detail_requirements),
                details.adoptionRequirements.map { requirement ->
                    resources.getString(R.string.adoption_detail_share_list_item, requirement)
                }
            )

            val shelterInformation = buildList {
                details.shelter?.name?.takeIf(String::isNotBlank)?.let(::add)
                details.shelter?.address?.takeIf(String::isNotBlank)?.let(::add)
                distanceKm?.let { distance ->
                    add(resources.getString(
                        R.string.adoption_detail_share_distance,
                        formatDistance(distance)
                    ))
                }
            }
            writer.drawSection(
                resources.getString(R.string.adoption_detail_shelter),
                shelterInformation
            )
            writer.drawFooter(resources.getString(R.string.adoption_detail_share_footer))
            writer.finish()

            FileOutputStream(outputFile).use(document::writeTo)
        } finally {
            document.close()
            cover?.recycle()
        }
    }

    private fun formatDistance(distanceKm: Double) = resources.getString(
        R.string.adoption_distance,
        distanceFormat.format(distanceKm)
    )

    private fun waitingMonths(createdAtMillis: Long): Int {
        if (createdAtMillis <= 0L) return 1
        return max(1, ((System.currentTimeMillis() - createdAtMillis) / MONTH_MILLIS).toInt())
    }

    private fun safeFilePart(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS, "")
        .replace(NON_FILE_CHARACTERS, "_")
        .trim('_')
        .take(MAX_FILE_PART_LENGTH)
        .ifBlank { "animal" }

    private inner class PdfWriter(
        private val document: PdfDocument,
        private val animalName: String
    ) {
        private val ink = ContextCompat.getColor(applicationContext, R.color.ink)
        private val muted = ContextCompat.getColor(applicationContext, R.color.muted)
        private val terracotta = ContextCompat.getColor(applicationContext, R.color.terracotta)
        private val border = ContextCompat.getColor(applicationContext, R.color.border)
        private val softCream = ContextCompat.getColor(applicationContext, R.color.soft_cream)
        private val background = ContextCompat.getColor(applicationContext, R.color.background)
        private val pageMargin = pdfDimension(R.dimen.adoption_share_pdf_page_margin)
        private val contentWidth = PAGE_WIDTH - pageMargin * 2
        private val coverHeight = pdfDimension(R.dimen.adoption_share_pdf_cover_height)
        private val coverRadius = pdfDimension(R.dimen.adoption_share_pdf_cover_radius)
        private val borderWidth = pdfDimension(R.dimen.adoption_share_pdf_border_width)
        private val titleLineHeight = pdfDimension(R.dimen.adoption_share_pdf_title_line_height)
        private val bodyLineHeight = pdfDimension(R.dimen.adoption_share_pdf_body_line_height)
        private val sectionTitleLineHeight = pdfDimension(
            R.dimen.adoption_share_pdf_section_title_line_height
        )
        private val footerLineHeight = pdfDimension(R.dimen.adoption_share_pdf_footer_line_height)
        private val smallGap = pdfDimension(R.dimen.adoption_share_pdf_small_gap)
        private val bodyGap = pdfDimension(R.dimen.adoption_share_pdf_body_gap)
        private val sectionGap = pdfDimension(R.dimen.adoption_share_pdf_section_gap)
        private val titleTextSize = pdfTextSize(R.dimen.text_size_adoption_share_pdf_title)
        private val brandTextSize = pdfTextSize(R.dimen.text_size_adoption_share_pdf_brand)
        private val bodyTextSize = pdfTextSize(R.dimen.text_size_adoption_share_pdf_body)
        private val sectionTitleTextSize = pdfTextSize(
            R.dimen.text_size_adoption_share_pdf_section_title
        )
        private val footerTextSize = pdfTextSize(R.dimen.text_size_adoption_share_pdf_footer)
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var cursorY = pageMargin

        init {
            startPage()
        }

        fun drawBrandTitle(text: String) {
            drawTextLine(text, textPaint(terracotta, brandTextSize, bold = true))
            cursorY += smallGap
        }

        fun drawCover(bitmap: Bitmap?) {
            ensureSpace(coverHeight + sectionGap)
            val bounds = RectF(
                pageMargin,
                cursorY,
                PAGE_WIDTH - pageMargin,
                cursorY + coverHeight
            )
            val path = Path().apply {
                addRoundRect(bounds, coverRadius, coverRadius, Path.Direction.CW)
            }
            val checkpoint = canvas.save()
            canvas.clipPath(path)
            canvas.drawColor(softCream)
            if (bitmap != null) {
                drawCenterCrop(bitmap, bounds)
            } else {
                canvas.drawRect(bounds, Paint().apply { color = softCream })
                val placeholder = resources.getString(R.string.adoption_detail_share_photo_unavailable)
                val paint = textPaint(muted, bodyTextSize)
                val textX = bounds.centerX() - paint.measureText(placeholder) / 2f
                canvas.drawText(placeholder, textX, bounds.centerY(), paint)
            }
            canvas.restoreToCount(checkpoint)
            canvas.drawRoundRect(
                bounds,
                coverRadius,
                coverRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = border
                    style = Paint.Style.STROKE
                    strokeWidth = borderWidth
                }
            )
            cursorY = bounds.bottom + sectionGap
        }

        fun drawAnimalName(name: String) {
            drawWrappedTextFromTop(
                name,
                textPaint(ink, titleTextSize, bold = true),
                titleLineHeight
            )
            cursorY += smallGap
        }

        fun drawHighlightedText(text: String) {
            drawWrappedText(
                text,
                textPaint(terracotta, bodyTextSize, bold = true),
                bodyLineHeight
            )
            cursorY += bodyGap
        }

        fun drawLines(lines: List<String>) {
            lines.forEach { line ->
                drawWrappedText(line, textPaint(ink, bodyTextSize), bodyLineHeight)
            }
            if (lines.isNotEmpty()) cursorY += sectionGap
        }

        fun drawSection(title: String, lines: List<String>) {
            if (lines.isEmpty()) return
            ensureSpace(sectionTitleLineHeight + bodyLineHeight)
            drawTextLine(title, textPaint(terracotta, sectionTitleTextSize, bold = true))
            cursorY += smallGap
            lines.forEach { line ->
                drawWrappedText(line, textPaint(ink, bodyTextSize), bodyLineHeight)
                cursorY += bodyGap
            }
            cursorY += sectionGap
        }

        fun drawFooter(text: String) {
            val footerBaseline = PAGE_HEIGHT - pageMargin
            val dividerY = footerBaseline - footerLineHeight
            if (cursorY + smallGap > dividerY) {
                page?.let(document::finishPage)
                page = null
                startPage()
            }
            canvas.drawLine(
                pageMargin,
                dividerY,
                PAGE_WIDTH - pageMargin,
                dividerY,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = border
                    strokeWidth = borderWidth
                }
            )
            canvas.drawText(
                text,
                pageMargin,
                footerBaseline,
                textPaint(muted, footerTextSize)
            )
            cursorY = footerBaseline + footerLineHeight
        }

        fun finish() {
            page?.let(document::finishPage)
            page = null
        }

        private fun startPage() {
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = requireNotNull(page).canvas
            canvas.drawColor(background)
            cursorY = pageMargin
            if (pageNumber > 1) {
                val continuation = resources.getString(
                    R.string.adoption_detail_share_pdf_continuation,
                    animalName
                )
                drawTextLine(continuation, textPaint(terracotta, brandTextSize, bold = true))
                cursorY += sectionGap
            }
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (cursorY + requiredHeight <= PAGE_HEIGHT - pageMargin) return
            page?.let(document::finishPage)
            page = null
            startPage()
        }

        private fun drawWrappedText(text: String, paint: Paint, lineHeight: Float) {
            text.lines().forEach { paragraph ->
                if (paragraph.isBlank()) {
                    cursorY += lineHeight
                    return@forEach
                }
                var remaining = paragraph.trim()
                while (remaining.isNotEmpty()) {
                    ensureSpace(lineHeight)
                    var characterCount = paint.breakText(remaining, true, contentWidth, null)
                        .coerceAtLeast(1)
                    if (characterCount < remaining.length) {
                        val lastSpace = remaining.lastIndexOf(' ', characterCount - 1)
                        if (lastSpace > 0) characterCount = lastSpace
                    }
                    val line = remaining.take(characterCount).trim()
                    canvas.drawText(line, pageMargin, cursorY, paint)
                    cursorY += lineHeight
                    remaining = remaining.drop(characterCount).trimStart()
                }
            }
        }

        private fun drawWrappedTextFromTop(text: String, paint: Paint, lineHeight: Float) {
            val baselineOffset = -paint.fontMetrics.ascent
            ensureSpace(baselineOffset + lineHeight)
            cursorY += baselineOffset
            drawWrappedText(text, paint, lineHeight)
            cursorY -= baselineOffset
        }

        private fun drawTextLine(text: String, paint: Paint) {
            ensureSpace(bodyLineHeight)
            canvas.drawText(text, pageMargin, cursorY, paint)
            cursorY += bodyLineHeight
        }

        private fun drawCenterCrop(bitmap: Bitmap, destination: RectF) {
            val destinationRatio = destination.width() / destination.height()
            val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val source = if (sourceRatio > destinationRatio) {
                val width = (bitmap.height * destinationRatio).toInt()
                val left = (bitmap.width - width) / 2
                Rect(left, 0, left + width, bitmap.height)
            } else {
                val height = (bitmap.width / destinationRatio).toInt()
                val top = (bitmap.height - height) / 2
                Rect(0, top, bitmap.width, top + height)
            }
            canvas.drawBitmap(bitmap, source, destination, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        private fun textPaint(colorValue: Int, size: Float, bold: Boolean = false) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorValue
                textSize = size
                typeface = Typeface.create(
                    if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT,
                    Typeface.NORMAL
                )
            }

        private fun pdfDimension(dimensionRes: Int): Float =
            resourceValue(dimensionRes)

        private fun pdfTextSize(dimensionRes: Int): Float =
            resourceValue(dimensionRes)

        private fun resourceValue(dimensionRes: Int): Float {
            val value = TypedValue()
            resources.getValue(dimensionRes, value, true)
            return TypedValue.complexToFloat(value.data)
        }
    }

    private companion object {
        const val SHARE_DIRECTORY = "shared_animals"
        const val PDF_MIME_TYPE = "application/pdf"
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val PHOTO_REQUEST_WIDTH = 1200
        const val PHOTO_REQUEST_HEIGHT = 720
        const val MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val MAX_FILE_PART_LENGTH = 48
        val NON_SPACING_MARKS = "\\p{Mn}+".toRegex()
        val NON_FILE_CHARACTERS = "[^A-Za-z0-9_-]+".toRegex()
    }
}
