package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.DocumentPage
import com.example.data.model.DrawingPath
import com.example.data.model.OcrScanResult
import com.example.data.model.PageAnnotations
import com.example.data.model.PageFormatOptions
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

enum class CompressionLevel(
    val label: String,
    val scaleFactor: Float,
    val jpegQuality: Int,
    val description: String
) {
    EXTREME("Extreme", 1.2f, 50, "Maximum compression (~50-70% smaller)"),
    RECOMMENDED("Recommended", 1.5f, 75, "Balanced quality & file size (~30-50% smaller)"),
    LOW("Low / High Quality", 2.0f, 88, "Sharp text & images (~15-30% smaller)")
}

data class CompressionResult(
    val outputFile: File,
    val originalSize: Long,
    val compressedSize: Long,
    val pageCount: Int,
    val reductionPercentage: Int
)

enum class ImageExportFormat(
    val extension: String,
    val format: Bitmap.CompressFormat,
    val label: String,
    val mimeType: String
) {
    PNG("png", Bitmap.CompressFormat.PNG, "PNG (Lossless)", "image/png"),
    JPEG("jpg", Bitmap.CompressFormat.JPEG, "JPEG (High-Quality)", "image/jpeg")
}

enum class ImageDpiResolution(
    val label: String,
    val scaleFactor: Float,
    val description: String
) {
    STANDARD("200 DPI", 2.0f, "Standard screen clarity"),
    HIGH("300 DPI", 3.0f, "High-resolution print quality"),
    ULTRA("400 DPI", 4.0f, "Ultra HD sharp text & details")
}

object PdfEngine {

    // Standard A4 dimensions in points (1 pt = 1/72 inch)
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    suspend fun generatePdfFromText(
        context: Context,
        title: String,
        text: String,
        options: PageFormatOptions,
        outputFile: File
    ): Int = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        val pageWidth = if (options.isLandscape) A4_HEIGHT else A4_WIDTH
        val pageHeight = if (options.isLandscape) A4_WIDTH else A4_HEIGHT
        val margin = options.marginPt.coerceIn(12, 108)

        val printableWidth = (pageWidth - 2 * margin).coerceAtLeast(200)
        val printableHeight = (pageHeight - 2 * margin - 60).coerceAtLeast(300)

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = options.fontSize
            color = parseColor(options.textColorHex)
            typeface = getTypeface(options.fontFamily)
        }

        // Measure text layout
        val alignment = when (options.textAlignment) {
            "Center" -> Layout.Alignment.ALIGN_CENTER
            "Right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            printableWidth
        ).setAlignment(alignment)
            .setLineSpacing(0f, options.lineSpacingMultiplier)
            .setIncludePad(true)
            .build()

        val totalHeight = staticLayout.height
        val lineCount = staticLayout.lineCount

        // Split lines into pages
        val pageLines = mutableListOf<Pair<Int, Int>>() // Pair(startLine, endLine)
        var currentY = 0
        var startLine = 0

        for (i in 0 until lineCount) {
            val lineBottom = staticLayout.getLineBottom(i)
            if (lineBottom - currentY > printableHeight && i > startLine) {
                pageLines.add(Pair(startLine, i - 1))
                startLine = i
                currentY = staticLayout.getLineTop(i)
            }
        }
        if (startLine < lineCount) {
            pageLines.add(Pair(startLine, lineCount - 1))
        }

        val totalPages = pageLines.size.coerceAtLeast(1)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Fill background
            canvas.drawColor(parseColor(options.paperColorHex))

            // Draw Watermark if present
            if (options.watermarkText.isNotBlank()) {
                drawWatermark(canvas, pageWidth, pageHeight, options.watermarkText, options.watermarkOpacity)
            }

            // Draw Header
            if (options.headerText.isNotBlank()) {
                val headerPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    color = parseColor(options.textColorHex)
                    alpha = 150
                }
                canvas.drawText(options.headerText, margin.toFloat(), (margin / 2 + 10).toFloat(), headerPaint)
                canvas.drawLine(
                    margin.toFloat(),
                    (margin / 2 + 18).toFloat(),
                    (pageWidth - margin).toFloat(),
                    (margin / 2 + 18).toFloat(),
                    headerPaint
                )
            }

            // Draw Page Content
            canvas.save()
            val topOffset = margin.toFloat() + 20f
            canvas.translate(margin.toFloat(), topOffset)

            if (pageLines.isNotEmpty() && pageIndex < pageLines.size) {
                val (lineStart, lineEnd) = pageLines[pageIndex]
                val startY = staticLayout.getLineTop(lineStart)
                val endY = staticLayout.getLineBottom(lineEnd)

                canvas.clipRect(0, 0, printableWidth, endY - startY + 10)
                canvas.translate(0f, -startY.toFloat())
                staticLayout.draw(canvas)
            }
            canvas.restore()

            // Draw Footer
            val footerText = options.footerText
                .replace("{page}", "${pageIndex + 1}")
                .replace("{total}", "$totalPages")

            if (footerText.isNotBlank()) {
                val footerPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    color = parseColor(options.textColorHex)
                    alpha = 180
                }
                val bounds = Rect()
                footerPaint.getTextBounds(footerText, 0, footerText.length, bounds)
                canvas.drawText(
                    footerText,
                    (pageWidth - margin - bounds.width()).toFloat(),
                    (pageHeight - margin / 2).toFloat(),
                    footerPaint
                )
            }

            pdfDocument.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        totalPages
    }

    suspend fun generatePdfFromImages(
        context: Context,
        imageUris: List<Uri>,
        options: PageFormatOptions,
        outputFile: File
    ): Int = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = if (options.isLandscape) A4_HEIGHT else A4_WIDTH
        val pageHeight = if (options.isLandscape) A4_WIDTH else A4_HEIGHT
        val margin = options.marginPt.coerceIn(0, 72)

        val targetW = (pageWidth - 2 * margin).toFloat()
        val targetH = (pageHeight - 2 * margin).toFloat()

        var pageIndex = 0

        for (uri in imageUris) {
            val bitmap = loadBitmapFromUri(context, uri) ?: continue

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Fill background
            canvas.drawColor(parseColor(options.paperColorHex))

            // Scale bitmap to fit target area
            val scale = minOf(targetW / bitmap.width, targetH / bitmap.height)
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale

            val dx = margin + (targetW - scaledWidth) / 2f
            val dy = margin + (targetH - scaledHeight) / 2f

            val rect = RectF(dx, dy, dx + scaledWidth, dy + scaledHeight)
            canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))

            // Watermark if requested
            if (options.watermarkText.isNotBlank()) {
                drawWatermark(canvas, pageWidth, pageHeight, options.watermarkText, options.watermarkOpacity)
            }

            // Footer page number
            val footerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.DKGRAY
            }
            canvas.drawText(
                "Page ${pageIndex + 1} of ${imageUris.size}",
                (pageWidth - margin - 60).toFloat(),
                (pageHeight - 15).toFloat(),
                footerPaint
            )

            pdfDocument.finishPage(page)
            bitmap.recycle()
            pageIndex++
        }

        if (pageIndex == 0) {
            // Fallback empty page if failed
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            pdfDocument.finishPage(page)
            pageIndex = 1
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pageIndex
    }

    suspend fun renderPdfPagesToBitmaps(
        context: Context,
        pdfFile: File,
        scaleFactor: Float = 2.0f
    ): List<DocumentPage> = withContext(Dispatchers.IO) {
        val pagesList = mutableListOf<DocumentPage>()
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext pagesList

        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        val cacheDir = File(context.cacheDir, "pdf_renders_${System.currentTimeMillis()}")
        cacheDir.mkdirs()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val width = (page.width * scaleFactor).toInt().coerceAtLeast(300)
            val height = (page.height * scaleFactor).toInt().coerceAtLeast(400)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Save bitmap file
            val imgFile = File(cacheDir, "page_${i + 1}.png")
            FileOutputStream(imgFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            pagesList.add(
                DocumentPage(
                    id = UUID.randomUUID().toString(),
                    pageIndex = i,
                    bitmapPath = imgFile.absolutePath,
                    textContent = "Page ${i + 1} Content",
                    rotationDegrees = 0f
                )
            )
            bitmap.recycle()
        }

        renderer.close()
        pfd.close()

        pagesList
    }

    suspend fun saveReorganizedPdf(
        context: Context,
        pageList: List<DocumentPage>,
        options: PageFormatOptions,
        outputFile: File
    ): Int = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageWidth = if (options.isLandscape) A4_HEIGHT else A4_WIDTH
        val pageHeight = if (options.isLandscape) A4_WIDTH else A4_HEIGHT

        var pageCount = 0

        for (item in pageList) {
            pageCount++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(parseColor(options.paperColorHex))

            if (item.isBlankPage || item.bitmapPath == null) {
                // Draw blank page content with text
                val paint = TextPaint().apply {
                    isAntiAlias = true
                    textSize = 16f
                    color = parseColor(options.textColorHex)
                }
                val contentText = item.textContent.ifBlank { "Blank Note Page" }
                canvas.drawText(contentText, 50f, 100f, paint)
            } else {
                val bitmap = BitmapFactory.decodeFile(item.bitmapPath)
                if (bitmap != null) {
                    canvas.save()

                    // Rotate if specified
                    if (item.rotationDegrees != 0f) {
                        val matrix = Matrix().apply {
                            postRotate(item.rotationDegrees, pageWidth / 2f, pageHeight / 2f)
                        }
                        canvas.concat(matrix)
                    }

                    val scale = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
                    val sw = bitmap.width * scale
                    val sh = bitmap.height * scale
                    val dx = (pageWidth - sw) / 2f
                    val dy = (pageHeight - sh) / 2f

                    canvas.drawBitmap(bitmap, null, RectF(dx, dy, dx + sw, dy + sh), Paint(Paint.FILTER_BITMAP_FLAG))
                    canvas.restore()
                    bitmap.recycle()
                }
            }

            // Watermark
            if (options.watermarkText.isNotBlank()) {
                drawWatermark(canvas, pageWidth, pageHeight, options.watermarkText, options.watermarkOpacity)
            }

            pdfDocument.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pageCount
    }

    suspend fun compressPdf(
        context: Context,
        inputFile: File,
        level: CompressionLevel,
        outputFile: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): CompressionResult = withContext(Dispatchers.IO) {
        if (!inputFile.exists() || inputFile.length() == 0L) {
            throw IllegalArgumentException("Input PDF file is invalid or empty")
        }

        val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount
        val pdfDocument = PdfDocument()

        for (i in 0 until totalPages) {
            onProgress(i + 1, totalPages)
            val page = renderer.openPage(i)

            val pageW = page.width
            val pageH = page.height

            val bitmapW = (pageW * level.scaleFactor).toInt().coerceAtLeast(100)
            val bitmapH = (pageH * level.scaleFactor).toInt().coerceAtLeast(100)

            val renderBitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(renderBitmap)
            canvas.drawColor(Color.WHITE)

            page.render(renderBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val compressedStream = java.io.ByteArrayOutputStream()
            renderBitmap.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, compressedStream)
            renderBitmap.recycle()

            val compressedBytes = compressedStream.toByteArray()
            val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val pageCanvas = pdfPage.canvas

            if (compressedBitmap != null) {
                val destRect = RectF(0f, 0f, pageW.toFloat(), pageH.toFloat())
                pageCanvas.drawBitmap(compressedBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                compressedBitmap.recycle()
            }

            pdfDocument.finishPage(pdfPage)
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val originalSize = inputFile.length()
        val compressedSize = outputFile.length()
        val reduction = if (originalSize > 0 && compressedSize < originalSize) {
            (((originalSize - compressedSize).toDouble() / originalSize.toDouble()) * 100).toInt()
        } else 0

        CompressionResult(
            outputFile = outputFile,
            originalSize = originalSize,
            compressedSize = compressedSize,
            pageCount = totalPages,
            reductionPercentage = reduction
        )
    }

    suspend fun exportPdfPagesAsImages(
        context: Context,
        pdfFile: File,
        targetPageIndices: Set<Int>,
        format: ImageExportFormat,
        resolution: ImageDpiResolution,
        quality: Int = 95,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<File> = withContext(Dispatchers.IO) {
        val exportedFiles = mutableListOf<File>()
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext exportedFiles

        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPdfPages = renderer.pageCount

        val pagesToProcess = if (targetPageIndices.isEmpty()) {
            (0 until totalPdfPages).toList()
        } else {
            targetPageIndices.filter { it in 0 until totalPdfPages }.sorted()
        }

        val exportDir = File(context.getExternalFilesDir(null), "ConvertedPDFs")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val cleanTitle = pdfFile.nameWithoutExtension.replace(" ", "_")

        for ((index, pageIdx) in pagesToProcess.withIndex()) {
            onProgress(index + 1, pagesToProcess.size)
            val page = renderer.openPage(pageIdx)

            val width = (page.width * resolution.scaleFactor).toInt().coerceAtLeast(200)
            val height = (page.height * resolution.scaleFactor).toInt().coerceAtLeast(200)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val fileName = "${cleanTitle}_Page_${pageIdx + 1}_$timestamp.${format.extension}"
            val imgFile = File(exportDir, fileName)

            FileOutputStream(imgFile).use { out ->
                bitmap.compress(format.format, quality, out)
            }
            bitmap.recycle()

            exportedFiles.add(imgFile)
        }

        renderer.close()
        pfd.close()

        exportedFiles
    }

    private fun drawWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        opacity: Float
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 36f
            color = Color.GRAY
            alpha = (opacity * 255).toInt().coerceIn(10, 255)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.save()
        canvas.rotate(-35f, width / 2f, height / 2f)
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, (width - bounds.width()) / 2f, (height + bounds.height()) / 2f, paint)
        canvas.restore()
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseColor(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.BLACK
        }
    }

    private fun getTypeface(family: String): Typeface {
        return when (family) {
            "Serif" -> Typeface.SERIF
            "Monospace" -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
    }

    /**
     * OCR feature using ML Kit Text Recognition to extract text from scanned PDFs or images.
     */
    suspend fun performOcrOnPdf(
        context: Context,
        pdfFile: File
    ): OcrScanResult = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val renderedPages = renderPdfPagesToBitmaps(context, pdfFile, scaleFactor = 2.0f)

        val fullTextBuilder = StringBuilder()
        var pageCount = 0

        for ((index, page) in renderedPages.withIndex()) {
            pageCount++
            fullTextBuilder.append("--- PAGE ${index + 1} ---\n")

            if (page.bitmapPath != null) {
                val bitmapFile = File(page.bitmapPath)
                if (bitmapFile.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(bitmapFile.absolutePath)
                        if (bitmap != null) {
                            val image = InputImage.fromBitmap(bitmap, 0)
                            val result = Tasks.await(recognizer.process(image))
                            val pageText = result.text
                            if (pageText.isNotBlank()) {
                                fullTextBuilder.append(pageText).append("\n\n")
                            } else {
                                fullTextBuilder.append("[No text detected on page]\n\n")
                            }
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        fullTextBuilder.append("[OCR Error on Page ${index + 1}: ${e.message}]\n\n")
                    }
                }
            }
        }

        recognizer.close()
        OcrScanResult(
            totalPages = pageCount,
            extractedText = fullTextBuilder.toString().trim()
        )
    }

    /**
     * OCR feature for single image file
     */
    suspend fun performOcrOnImageFile(
        context: Context,
        imageFile: File
    ): OcrScanResult = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var text = ""

        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = Tasks.await(recognizer.process(image))
                text = result.text
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            text = "Error scanning image: ${e.message}"
        }

        recognizer.close()
        OcrScanResult(
            totalPages = 1,
            extractedText = text.ifBlank { "No text recognized in image" }
        )
    }

    /**
     * PDF Annotation & Signature feature:
     * Overlays drawn annotations, signatures, and text stamps onto PDF pages.
     */
    suspend fun saveAnnotatedPdf(
        context: Context,
        originalPdfFile: File,
        annotationsMap: Map<Int, PageAnnotations>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(originalPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pdfDocument = PdfDocument()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val pdfPageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
            val pdfPage = pdfDocument.startPage(pdfPageInfo)
            val canvas = pdfPage.canvas

            // 1. Render original PDF page onto canvas bitmap
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            val bitmapCanvas = Canvas(bitmap)
            bitmapCanvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            canvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()

            // 2. Draw annotations if present for this page index
            val pageAnnotation = annotationsMap[i]
            if (pageAnnotation != null) {
                // Draw Paths / Freehand strokes / Highlights
                for (pathData in pageAnnotation.paths) {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        strokeWidth = pathData.strokeWidth
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND

                        val argb = (pathData.color.value shr 32).toInt()
                        if (pathData.isHighlighter) {
                            color = argb
                            alpha = 110 // Translucent highlighter
                        } else {
                            color = argb
                        }
                    }

                    if (pathData.points.size > 1) {
                        val path = android.graphics.Path()
                        path.moveTo(pathData.points.first().x, pathData.points.first().y)
                        for (idx in 1 until pathData.points.size) {
                            val pt = pathData.points[idx]
                            path.lineTo(pt.x, pt.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }

                // Draw Text Stamps / Electronic Signatures
                for (stamp in pageAnnotation.stamps) {
                    val stampX = stamp.xRatio * page.width
                    val stampY = stamp.yRatio * page.height

                    val paint = Paint().apply {
                        isAntiAlias = true
                        textSize = if (stamp.isSignature) 28f else 22f
                        typeface = Typeface.create(
                            if (stamp.isSignature) Typeface.SERIF else Typeface.SANS_SERIF,
                            Typeface.BOLD_ITALIC
                        )
                        color = (stamp.color.value shr 32).toInt()
                    }

                    if (stamp.isSignature) {
                        // Signature box outline
                        val bgPaint = Paint().apply {
                            color = Color.WHITE
                            style = Paint.Style.FILL
                        }
                        val borderPaint = Paint().apply {
                            color = (stamp.color.value shr 32).toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                        }
                        val bounds = Rect()
                        paint.getTextBounds(stamp.text, 0, stamp.text.length, bounds)
                        val rect = RectF(
                            stampX - 8,
                            stampY - bounds.height() - 8,
                            stampX + bounds.width() + 16,
                            stampY + 12
                        )
                        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
                        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
                    }

                    canvas.drawText(stamp.text, stampX, stampY, paint)
                }
            }

            pdfDocument.finishPage(pdfPage)
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        outputFile
    }
}

