package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class PdfQuality(val label: String, val maxDimension: Int, val compressQuality: Int, val description: String) {
    COMPACT("Compact (WhatsApp)", 1200, 60, "Smallest size, easy to share"),
    BALANCED("Balanced", 1800, 80, "Great balance of clarity & size"),
    HIGH("High Definition", 2400, 92, "Crisp comic text & artwork"),
    ORIGINAL("Maximum Lossless", 4000, 98, "Full original resolution")
}

data class PdfExportConfig(
    val title: String,
    val quality: PdfQuality = PdfQuality.HIGH,
    val includePageNumbers: Boolean = false,
    val fitToOriginalAspect: Boolean = true
)

data class ExportResult(
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long
)

class PdfExporter(private val context: Context) {

    suspend fun generatePdf(
        book: BookEntity,
        pages: List<BookPageEntity>,
        config: PdfExportConfig,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): ExportResult = withContext(Dispatchers.IO) {
        val sanitizedTitle = book.title
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "ComicBook" }
        val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.pdf"

        // Create cache file first
        val cacheFile = File(context.cacheDir, fileName)
        val pdfDocument = PdfDocument()

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val textBgPaint = Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        try {
            val total = pages.size
            pages.forEachIndexed { index, pageEntity ->
                onProgress(index + 1, total)

                val imageFile = File(pageEntity.imagePath)
                if (!imageFile.exists()) return@forEachIndexed

                // Step 1: Decode image with sub-sampling to fit quality maxDimension
                val decodedBitmap = decodeSampledBitmap(imageFile, config.quality.maxDimension)
                    ?: return@forEachIndexed

                val imgWidth = decodedBitmap.width
                val imgHeight = decodedBitmap.height

                // Step 2: Determine PDF page dimensions in 72 DPI points
                // For comic layout: standard base width around 595 pt (A4 width)
                val targetPdfWidth: Int
                val targetPdfHeight: Int

                if (config.fitToOriginalAspect) {
                    targetPdfWidth = 595
                    targetPdfHeight = ((595f * imgHeight) / imgWidth).toInt().coerceAtLeast(100)
                } else {
                    // Standard A4: 595 x 842 pt
                    targetPdfWidth = 595
                    targetPdfHeight = 842
                }

                val pageInfo = PdfDocument.PageInfo.Builder(targetPdfWidth, targetPdfHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = pdfPage.canvas

                // Clear background
                canvas.drawColor(Color.WHITE)

                // Compute destination rect
                val destRect: RectF
                if (config.fitToOriginalAspect) {
                    destRect = RectF(0f, 0f, targetPdfWidth.toFloat(), targetPdfHeight.toFloat())
                } else {
                    // Center fit inside A4 page
                    val scale = minOf(
                        targetPdfWidth.toFloat() / imgWidth.toFloat(),
                        targetPdfHeight.toFloat() / imgHeight.toFloat()
                    )
                    val scaledW = imgWidth * scale
                    val scaledH = imgHeight * scale
                    val left = (targetPdfWidth - scaledW) / 2f
                    val top = (targetPdfHeight - scaledH) / 2f
                    destRect = RectF(left, top, left + scaledW, top + scaledH)
                }

                // Draw bitmap onto page canvas
                val srcRect = Rect(0, 0, imgWidth, imgHeight)
                val bitmapPaint = Paint().apply { isFilterBitmap = true }
                canvas.drawBitmap(decodedBitmap, srcRect, destRect, bitmapPaint)

                // Optional page number watermark badge
                if (config.includePageNumbers) {
                    val pageStr = "${index + 1}"
                    val padding = 8f
                    val textW = textPaint.measureText(pageStr)
                    val textH = 14f
                    val badgeRight = targetPdfWidth - 16f
                    val badgeBottom = targetPdfHeight - 16f
                    val badgeRect = RectF(
                        badgeRight - textW - padding * 2,
                        badgeBottom - textH - padding,
                        badgeRight,
                        badgeBottom + padding / 2
                    )
                    canvas.drawRoundRect(badgeRect, 6f, 6f, textBgPaint)
                    canvas.drawText(pageStr, badgeRight - padding, badgeBottom, textPaint)
                }

                pdfDocument.finishPage(pdfPage)
                decodedBitmap.recycle()
            }

            // Write PDF document to local cache
            FileOutputStream(cacheFile).use { output ->
                pdfDocument.writeTo(output)
            }
        } finally {
            pdfDocument.close()
        }

        // Copy to MediaStore Downloads so it is saved permanently and visible to user
        val publicUri = copyToDownloads(cacheFile, fileName)

        // Generate FileProvider uri for instant opening and sharing
        val shareableUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        ExportResult(
            uri = shareableUri,
            filePath = cacheFile.absolutePath,
            fileName = fileName,
            fileSizeBytes = cacheFile.length()
        )
    }

    private fun decodeSampledBitmap(file: File, maxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        var halfW = origW
        var halfH = origH
        while (halfW > maxDimension || halfH > maxDimension) {
            sampleSize *= 2
            halfW /= 2
            halfH /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun copyToDownloads(sourceFile: File, fileName: String): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ComicMaker")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    return uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val comicDir = File(downloadsDir, "ComicMaker")
                if (!comicDir.exists()) comicDir.mkdirs()
                val destFile = File(comicDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                return Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
