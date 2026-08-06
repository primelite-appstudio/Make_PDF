package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "converted_documents")
data class ConvertedDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalType: String, // "TXT", "IMAGE", "PDF", "HTML", "DOCX"
    val targetType: String,   // "PDF", "IMAGES", "TXT", "MD"
    val filePath: String,
    val fileSizeFormatted: String,
    val pageCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null
)

data class DocumentPage(
    val id: String,
    val pageIndex: Int,
    val bitmapPath: String? = null,
    val textContent: String = "",
    val rotationDegrees: Float = 0f,
    val isBlankPage: Boolean = false
)

data class PageFormatOptions(
    val fontSize: Float = 14f,
    val fontFamily: String = "SansSerif", // "SansSerif", "Serif", "Monospace"
    val lineSpacingMultiplier: Float = 1.3f,
    val textAlignment: String = "Left", // "Left", "Center", "Right", "Justify"
    val marginPt: Int = 36, // 18, 36, 54
    val paperColorHex: String = "#FFFFFF",
    val textColorHex: String = "#0F172A",
    val headerText: String = "",
    val footerText: String = "Page {page} of {total}",
    val watermarkText: String = "",
    val watermarkOpacity: Float = 0.15f,
    val isLandscape: Boolean = false,
    val pageSizeName: String = "A4"
)

enum class ConversionMode(val displayName: String, val description: String) {
    TEXT_TO_PDF("Text / Doc to PDF", "Convert TXT, Markdown, Notes to structured PDF with custom formatting"),
    IMAGE_TO_PDF("Images to PDF", "Combine JPEG, PNG images into a high quality PDF"),
    PDF_TO_IMAGES("PDF to Images", "Extract all PDF pages into sharp standalone PNG/JPG images"),
    PDF_TO_TEXT("PDF to Text", "Extract pure editable text and markdown from any PDF document"),
    PDF_ORGANIZER("Reorganize & Merge", "Reorder pages, rotate, delete, insert blank pages, or merge PDFs"),
    COMPRESS_PDF("Compress PDF", "Reduce PDF file size while maintaining document quality"),
    BATCH_CONVERT("Batch Converter", "Select multiple documents and convert them to your target format simultaneously")
}

enum class BatchOperationType(
    val label: String,
    val description: String,
    val mimeType: String
) {
    COMPRESS_PDFS("Compress PDFs", "Batch compress multiple PDFs to reduce file size", "application/pdf"),
    PDF_TO_IMAGES("PDFs to Images", "Extract high-resolution JPEG/PNG pages from multiple PDFs", "application/pdf"),
    PDF_TO_TEXT("PDFs to Text", "Extract pure text and markdown from multiple PDFs", "application/pdf"),
    TEXT_TO_PDF("Text Files to PDF", "Convert TXT and Markdown files into styled PDF documents", "text/*"),
    IMAGES_TO_PDF("Images to PDFs", "Convert multiple images into high quality PDF documents", "image/*")
}

enum class BatchItemStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class BatchFileItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val sizeBytes: Long,
    val uri: android.net.Uri? = null,
    val localFile: java.io.File? = null,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val progressMessage: String = "Queued",
    val outputFile: java.io.File? = null,
    val errorMessage: String? = null
)
