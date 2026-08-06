package com.example.data.model

data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val authKey: String = "",
    val remotePath: String = "/PDFConverter/Backup/",
    val isEnabled: Boolean = false
)

data class LocalFolderSyncConfig(
    val customFolderPath: String = "",
    val isAutoExportEnabled: Boolean = true
)

data class OcrScanResult(
    val totalPages: Int,
    val extractedText: String,
    val confidenceScore: Float = 0.95f,
    val isSearchablePdfCreated: Boolean = false,
    val pdfPath: String? = null
)
