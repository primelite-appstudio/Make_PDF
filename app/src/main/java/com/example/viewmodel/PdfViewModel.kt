package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BatchFileItem
import com.example.data.model.BatchItemStatus
import com.example.data.model.BatchOperationType
import com.example.data.model.ConversionMode
import com.example.data.model.ConvertedDocument
import com.example.data.model.DocumentPage
import com.example.data.model.PageFormatOptions
import com.example.data.repository.DocumentRepository
import com.example.util.PdfEngine
import com.example.util.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    val historyDocuments: StateFlow<List<ConvertedDocument>>

    private val _currentMode = MutableStateFlow(ConversionMode.TEXT_TO_PDF)
    val currentMode: StateFlow<ConversionMode> = _currentMode.asStateFlow()

    private val _editorTitle = MutableStateFlow("Document_Conversion_1")
    val editorTitle: StateFlow<String> = _editorTitle.asStateFlow()

    private val _editorText = MutableStateFlow(SampleData.SAMPLE_RESUME)
    val editorText: StateFlow<String> = _editorText.asStateFlow()

    private val _formatOptions = MutableStateFlow(PageFormatOptions())
    val formatOptions: StateFlow<PageFormatOptions> = _formatOptions.asStateFlow()

    private val _imageUris = MutableStateFlow<List<Uri>>(emptyList())
    val imageUris: StateFlow<List<Uri>> = _imageUris.asStateFlow()

    private val _reorganizerPages = MutableStateFlow<List<DocumentPage>>(emptyList())
    val reorganizerPages: StateFlow<List<DocumentPage>> = _reorganizerPages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastExportedDocument = MutableStateFlow<ConvertedDocument?>(null)
    val lastExportedDocument: StateFlow<ConvertedDocument?> = _lastExportedDocument.asStateFlow()

    private val _extractedImagePaths = MutableStateFlow<List<String>>(emptyList())
    val extractedImagePaths: StateFlow<List<String>> = _extractedImagePaths.asStateFlow()

    private val _compressFile = MutableStateFlow<File?>(null)
    val compressFile: StateFlow<File?> = _compressFile.asStateFlow()

    private val _compressionLevel = MutableStateFlow(com.example.util.CompressionLevel.RECOMMENDED)
    val compressionLevel: StateFlow<com.example.util.CompressionLevel> = _compressionLevel.asStateFlow()

    private val _compressionResult = MutableStateFlow<com.example.util.CompressionResult?>(null)
    val compressionResult: StateFlow<com.example.util.CompressionResult?> = _compressionResult.asStateFlow()

    private val _compressionProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val compressionProgress: StateFlow<Pair<Int, Int>?> = _compressionProgress.asStateFlow()

    private val _exportPdfFile = MutableStateFlow<File?>(null)
    val exportPdfFile: StateFlow<File?> = _exportPdfFile.asStateFlow()

    private val _exportPdfPages = MutableStateFlow<List<DocumentPage>>(emptyList())
    val exportPdfPages: StateFlow<List<DocumentPage>> = _exportPdfPages.asStateFlow()

    private val _selectedExportPages = MutableStateFlow<Set<Int>>(emptySet())
    val selectedExportPages: StateFlow<Set<Int>> = _selectedExportPages.asStateFlow()

    private val _exportFormat = MutableStateFlow(com.example.util.ImageExportFormat.PNG)
    val exportFormat: StateFlow<com.example.util.ImageExportFormat> = _exportFormat.asStateFlow()

    private val _exportResolution = MutableStateFlow(com.example.util.ImageDpiResolution.HIGH)
    val exportResolution: StateFlow<com.example.util.ImageDpiResolution> = _exportResolution.asStateFlow()

    private val _exportedFiles = MutableStateFlow<List<File>>(emptyList())
    val exportedFiles: StateFlow<List<File>> = _exportedFiles.asStateFlow()

    private val _exportProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val exportProgress: StateFlow<Pair<Int, Int>?> = _exportProgress.asStateFlow()

    private val _batchOperation = MutableStateFlow(BatchOperationType.COMPRESS_PDFS)
    val batchOperation: StateFlow<BatchOperationType> = _batchOperation.asStateFlow()

    private val _batchQueue = MutableStateFlow<List<BatchFileItem>>(emptyList())
    val batchQueue: StateFlow<List<BatchFileItem>> = _batchQueue.asStateFlow()

    private val _batchProgress = MutableStateFlow<Pair<Int, Int>?>(null) // Pair(completed, total)
    val batchProgress: StateFlow<Pair<Int, Int>?> = _batchProgress.asStateFlow()

    private val _batchCompletedFiles = MutableStateFlow<List<File>>(emptyList())
    val batchCompletedFiles: StateFlow<List<File>> = _batchCompletedFiles.asStateFlow()

    private val _ocrScanResult = MutableStateFlow<com.example.data.model.OcrScanResult?>(null)
    val ocrScanResult: StateFlow<com.example.data.model.OcrScanResult?> = _ocrScanResult.asStateFlow()

    private val _webDavConfig = MutableStateFlow(com.example.data.model.WebDavConfig())
    val webDavConfig: StateFlow<com.example.data.model.WebDavConfig> = _webDavConfig.asStateFlow()

    private val _localSyncConfig = MutableStateFlow(com.example.data.model.LocalFolderSyncConfig())
    val localSyncConfig: StateFlow<com.example.data.model.LocalFolderSyncConfig> = _localSyncConfig.asStateFlow()

    private val _syncStatusLog = MutableStateFlow<String?>(null)
    val syncStatusLog: StateFlow<String?> = _syncStatusLog.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).documentDao()
        repository = DocumentRepository(dao)

        val docsFlow = MutableStateFlow<List<ConvertedDocument>>(emptyList())
        historyDocuments = docsFlow.asStateFlow()

        viewModelScope.launch {
            repository.allDocuments.collect { list ->
                docsFlow.value = list
            }
        }

        // Initialize sample pages for instant reorganization preview
        loadInitialSamplePages()
        loadSyncConfigs()
    }

    fun setMode(mode: ConversionMode) {
        _currentMode.value = mode
    }

    fun updateEditorTitle(title: String) {
        _editorTitle.value = title.ifBlank { "Untitled" }
    }

    fun updateEditorText(text: String) {
        _editorText.value = text
    }

    fun updateFormatOptions(options: PageFormatOptions) {
        _formatOptions.value = options
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setImageUris(uris: List<Uri>) {
        _imageUris.value = uris
        _statusMessage.value = "${uris.size} image(s) selected for PDF creation"
    }

    fun addImageUri(uri: Uri) {
        _imageUris.value = _imageUris.value + uri
        _statusMessage.value = "Image added"
    }

    fun removeImageUri(uri: Uri) {
        _imageUris.value = _imageUris.value.filter { it != uri }
    }

    fun loadSampleTemplate(type: String) {
        when (type) {
            "Resume" -> {
                _editorTitle.value = "Software_Engineer_Resume"
                _editorText.value = SampleData.SAMPLE_RESUME
            }
            "Invoice" -> {
                _editorTitle.value = "Invoice_2026_084"
                _editorText.value = SampleData.SAMPLE_INVOICE
            }
            "Notes" -> {
                _editorTitle.value = "Project_Sync_Notes"
                _editorText.value = SampleData.SAMPLE_MEETING_NOTES
            }
        }
        _statusMessage.value = "$type template loaded"
    }

    fun convertTextToPdf() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${_editorTitle.value.replace(" ", "_")}_$timestamp.pdf"
                val outFile = File(getExportDir(context), fileName)

                val pageCount = PdfEngine.generatePdfFromText(
                    context,
                    _editorTitle.value,
                    _editorText.value,
                    _formatOptions.value,
                    outFile
                )

                val doc = ConvertedDocument(
                    title = _editorTitle.value,
                    originalType = "TEXT/MD",
                    targetType = "PDF",
                    filePath = outFile.absolutePath,
                    fileSizeFormatted = formatFileSize(outFile.length()),
                    pageCount = pageCount
                )

                val id = repository.saveDocument(doc)
                val savedDoc = doc.copy(id = id)
                _lastExportedDocument.value = savedDoc
                _statusMessage.value = "Successfully converted to PDF ($pageCount pages)"

            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error converting text to PDF: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun convertImagesToPdf() {
        if (_imageUris.value.isEmpty()) {
            _statusMessage.value = "Please select at least one image"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${_editorTitle.value.replace(" ", "_")}_images_$timestamp.pdf"
                val outFile = File(getExportDir(context), fileName)

                val pageCount = PdfEngine.generatePdfFromImages(
                    context,
                    _imageUris.value,
                    _formatOptions.value,
                    outFile
                )

                val doc = ConvertedDocument(
                    title = "${_editorTitle.value}_Images",
                    originalType = "JPEG/PNG",
                    targetType = "PDF",
                    filePath = outFile.absolutePath,
                    fileSizeFormatted = formatFileSize(outFile.length()),
                    pageCount = pageCount
                )

                val id = repository.saveDocument(doc)
                _lastExportedDocument.value = doc.copy(id = id)
                _statusMessage.value = "Successfully created PDF from ${_imageUris.value.size} images!"

            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error converting images: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loadPdfForReorganizationFromUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val cacheFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.pdf")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val pages = PdfEngine.renderPdfPagesToBitmaps(context, cacheFile)
                _reorganizerPages.value = pages
                _statusMessage.value = "Loaded ${pages.size} PDF pages for reorganization"

            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error opening PDF file: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val list = _reorganizerPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _reorganizerPages.value = list
        }
    }

    fun rotatePage(pageId: String) {
        val list = _reorganizerPages.value.map { page ->
            if (page.id == pageId) {
                val newRotation = (page.rotationDegrees + 90f) % 360f
                page.copy(rotationDegrees = newRotation)
            } else page
        }
        _reorganizerPages.value = list
    }

    fun deletePage(pageId: String) {
        _reorganizerPages.value = _reorganizerPages.value.filter { it.id != pageId }
        _statusMessage.value = "Page removed"
    }

    fun duplicatePage(pageId: String) {
        val list = _reorganizerPages.value.toMutableList()
        val index = list.indexOfFirst { it.id == pageId }
        if (index != -1) {
            val page = list[index]
            val duplicate = page.copy(id = UUID.randomUUID().toString())
            list.add(index + 1, duplicate)
            _reorganizerPages.value = list
            _statusMessage.value = "Page duplicated"
        }
    }

    fun addBlankPage() {
        val newPage = DocumentPage(
            id = UUID.randomUUID().toString(),
            pageIndex = _reorganizerPages.value.size,
            isBlankPage = true,
            textContent = "Notes Page ${_reorganizerPages.value.size + 1}"
        )
        _reorganizerPages.value = _reorganizerPages.value + newPage
        _statusMessage.value = "Blank page added"
    }

    fun exportReorganizedPdf() {
        if (_reorganizerPages.value.isEmpty()) {
            _statusMessage.value = "No pages to export"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "Reorganized_${_editorTitle.value.replace(" ", "_")}_$timestamp.pdf"
                val outFile = File(getExportDir(context), fileName)

                val pageCount = PdfEngine.saveReorganizedPdf(
                    context,
                    _reorganizerPages.value,
                    _formatOptions.value,
                    outFile
                )

                val doc = ConvertedDocument(
                    title = "Reorganized_${_editorTitle.value}",
                    originalType = "PDF/Pages",
                    targetType = "PDF",
                    filePath = outFile.absolutePath,
                    fileSizeFormatted = formatFileSize(outFile.length()),
                    pageCount = pageCount
                )

                val id = repository.saveDocument(doc)
                _lastExportedDocument.value = doc.copy(id = id)
                _statusMessage.value = "Reorganized PDF exported successfully ($pageCount pages)!"

            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error exporting reorganized PDF: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun extractPdfToImages(pdfFile: File) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val pages = PdfEngine.renderPdfPagesToBitmaps(context, pdfFile, scaleFactor = 2.5f)
                val paths = pages.mapNotNull { it.bitmapPath }
                _extractedImagePaths.value = paths

                if (paths.isNotEmpty()) {
                    val doc = ConvertedDocument(
                        title = "Extracted_${pdfFile.nameWithoutExtension}",
                        originalType = "PDF",
                        targetType = "IMAGES",
                        filePath = paths.first(),
                        fileSizeFormatted = formatFileSize(pdfFile.length()),
                        pageCount = paths.size
                    )
                    repository.saveDocument(doc)
                    _statusMessage.value = "Extracted ${paths.size} high-res page image(s)"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error extracting PDF images: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun setCompressionLevel(level: com.example.util.CompressionLevel) {
        _compressionLevel.value = level
    }

    fun setCompressFile(file: File) {
        _compressFile.value = file
        _compressionResult.value = null
        _compressionProgress.value = null
        _statusMessage.value = "Selected ${file.name} (${formatFileSize(file.length())}) for compression"
    }

    fun setCompressFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val cacheFile = File(context.cacheDir, "compress_source_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    setCompressFile(cacheFile)
                } else {
                    _statusMessage.value = "Unable to read selected PDF file"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error opening PDF: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun compressSelectedPdf() {
        val sourceFile = _compressFile.value
        if (sourceFile == null || !sourceFile.exists()) {
            _statusMessage.value = "Please select a valid PDF file to compress"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _compressionProgress.value = Pair(0, 1)
            try {
                val context = getApplication<Application>()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val cleanTitle = sourceFile.nameWithoutExtension.removePrefix("compress_source_")
                val fileName = "Compressed_${cleanTitle}_$timestamp.pdf"
                val outFile = File(getExportDir(context), fileName)

                val result = PdfEngine.compressPdf(
                    context = context,
                    inputFile = sourceFile,
                    level = _compressionLevel.value,
                    outputFile = outFile,
                    onProgress = { current, total ->
                        _compressionProgress.value = Pair(current, total)
                    }
                )

                _compressionResult.value = result

                val doc = ConvertedDocument(
                    title = "Compressed_${cleanTitle}",
                    originalType = "PDF (${formatFileSize(result.originalSize)})",
                    targetType = "PDF",
                    filePath = outFile.absolutePath,
                    fileSizeFormatted = formatFileSize(result.compressedSize),
                    pageCount = result.pageCount
                )

                val id = repository.saveDocument(doc)
                _lastExportedDocument.value = doc.copy(id = id)

                val savedPercentage = result.reductionPercentage
                _statusMessage.value = if (savedPercentage > 0) {
                    "PDF Compressed! Saved $savedPercentage% (${formatFileSize(result.originalSize)} -> ${formatFileSize(result.compressedSize)})"
                } else {
                    "PDF processed cleanly (${formatFileSize(result.compressedSize)})"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error compressing PDF: ${e.message}"
            } finally {
                _isProcessing.value = false
                _compressionProgress.value = null
            }
        }
    }

    fun resetCompression() {
        _compressFile.value = null
        _compressionResult.value = null
        _compressionProgress.value = null
    }

    fun setExportPdfFile(file: File) {
        _exportPdfFile.value = file
        _exportedFiles.value = emptyList()
        _exportProgress.value = null
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val pages = PdfEngine.renderPdfPagesToBitmaps(context, file, scaleFactor = 1.5f)
                _exportPdfPages.value = pages
                _selectedExportPages.value = pages.map { it.pageIndex }.toSet()
                _statusMessage.value = "Loaded ${pages.size} page(s) from ${file.name}"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error reading PDF pages: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun setExportPdfFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context = getApplication<Application>()
                val cacheFile = File(context.cacheDir, "export_source_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    setExportPdfFile(cacheFile)
                } else {
                    _statusMessage.value = "Unable to read selected PDF file"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error opening PDF: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun togglePageSelectionForExport(pageIndex: Int) {
        val current = _selectedExportPages.value.toMutableSet()
        if (current.contains(pageIndex)) {
            current.remove(pageIndex)
        } else {
            current.add(pageIndex)
        }
        _selectedExportPages.value = current
    }

    fun selectAllExportPages() {
        _selectedExportPages.value = _exportPdfPages.value.map { it.pageIndex }.toSet()
    }

    fun deselectAllExportPages() {
        _selectedExportPages.value = emptySet()
    }

    fun setExportFormat(format: com.example.util.ImageExportFormat) {
        _exportFormat.value = format
    }

    fun setExportResolution(resolution: com.example.util.ImageDpiResolution) {
        _exportResolution.value = resolution
    }

    fun exportSelectedPagesAsImages() {
        val pdfFile = _exportPdfFile.value
        if (pdfFile == null || !pdfFile.exists()) {
            _statusMessage.value = "Please select a PDF file first"
            return
        }

        val pages = _selectedExportPages.value
        if (pages.isEmpty()) {
            _statusMessage.value = "Please select at least one page to export"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _exportProgress.value = Pair(0, pages.size)
            try {
                val context = getApplication<Application>()
                val files = PdfEngine.exportPdfPagesAsImages(
                    context = context,
                    pdfFile = pdfFile,
                    targetPageIndices = pages,
                    format = _exportFormat.value,
                    resolution = _exportResolution.value,
                    onProgress = { current, total ->
                        _exportProgress.value = Pair(current, total)
                    }
                )

                _exportedFiles.value = files

                if (files.isNotEmpty()) {
                    val cleanTitle = pdfFile.nameWithoutExtension.removePrefix("export_source_")
                    val totalBytes = files.sumOf { it.length() }
                    val doc = ConvertedDocument(
                        title = "Exported_${cleanTitle}_${files.size}Imgs",
                        originalType = "PDF (${pages.size} pages)",
                        targetType = _exportFormat.value.name,
                        filePath = files.first().absolutePath,
                        fileSizeFormatted = formatFileSize(totalBytes),
                        pageCount = files.size
                    )
                    val id = repository.saveDocument(doc)
                    _lastExportedDocument.value = doc.copy(id = id)
                    _statusMessage.value = "Successfully exported ${files.size} page image(s) as ${_exportFormat.value.label}!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Error exporting images: ${e.message}"
            } finally {
                _isProcessing.value = false
                _exportProgress.value = null
            }
        }
    }

    fun resetPdfToImagesExport() {
        _exportPdfFile.value = null
        _exportPdfPages.value = emptyList()
        _selectedExportPages.value = emptySet()
        _exportedFiles.value = emptyList()
        _exportProgress.value = null
    }

    fun setBatchOperation(operation: BatchOperationType) {
        _batchOperation.value = operation
        _batchQueue.value = emptyList()
        _batchCompletedFiles.value = emptyList()
        _batchProgress.value = null
    }

    fun addBatchUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val currentQueue = _batchQueue.value.toMutableList()

            for (uri in uris) {
                try {
                    val name = getUriFileName(context, uri) ?: "Document_${System.currentTimeMillis()}"
                    val tempFile = File(context.cacheDir, "batch_in_${System.currentTimeMillis()}_${name}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        currentQueue.add(
                            BatchFileItem(
                                name = name,
                                sizeBytes = tempFile.length(),
                                uri = uri,
                                localFile = tempFile
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _batchQueue.value = currentQueue
            _statusMessage.value = "Added ${uris.size} document(s) to batch queue"
        }
    }

    fun addBatchFiles(files: List<File>) {
        val currentQueue = _batchQueue.value.toMutableList()
        for (file in files) {
            if (file.exists() && file.length() > 0) {
                currentQueue.add(
                    BatchFileItem(
                        name = file.name,
                        sizeBytes = file.length(),
                        localFile = file
                    )
                )
            }
        }
        _batchQueue.value = currentQueue
        _statusMessage.value = "Added ${files.size} file(s) to batch queue"
    }

    fun removeBatchItem(id: String) {
        _batchQueue.value = _batchQueue.value.filter { it.id != id }
    }

    fun clearBatchQueue() {
        _batchQueue.value = emptyList()
        _batchCompletedFiles.value = emptyList()
        _batchProgress.value = null
    }

    fun processBatchQueue() {
        val queue = _batchQueue.value.filter { it.status != BatchItemStatus.COMPLETED }
        if (queue.isEmpty()) {
            _statusMessage.value = "Batch queue is empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val totalCount = _batchQueue.value.size
            var completedCount = _batchQueue.value.count { it.status == BatchItemStatus.COMPLETED }
            _batchProgress.value = Pair(completedCount, totalCount)

            val context = getApplication<Application>()
            val operation = _batchOperation.value
            val completedList = _batchCompletedFiles.value.toMutableList()

            val updatedQueue = _batchQueue.value.toMutableList()

            for (i in updatedQueue.indices) {
                val item = updatedQueue[i]
                if (item.status == BatchItemStatus.COMPLETED) continue

                val input = item.localFile
                if (input == null || !input.exists()) {
                    updatedQueue[i] = item.copy(status = BatchItemStatus.FAILED, errorMessage = "File not found")
                    _batchQueue.value = updatedQueue.toList()
                    continue
                }

                updatedQueue[i] = item.copy(status = BatchItemStatus.PROCESSING, progressMessage = "Processing...")
                _batchQueue.value = updatedQueue.toList()

                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val cleanTitle = input.nameWithoutExtension.removePrefix("batch_in_")

                    when (operation) {
                        BatchOperationType.COMPRESS_PDFS -> {
                            val outFile = File(getExportDir(context), "BatchCompressed_${cleanTitle}_$timestamp.pdf")
                            val result = PdfEngine.compressPdf(
                                context = context,
                                inputFile = input,
                                level = _compressionLevel.value,
                                outputFile = outFile
                            )

                            val doc = ConvertedDocument(
                                title = "BatchCompressed_${cleanTitle}",
                                originalType = "PDF (${formatFileSize(result.originalSize)})",
                                targetType = "PDF",
                                filePath = outFile.absolutePath,
                                fileSizeFormatted = formatFileSize(result.compressedSize),
                                pageCount = result.pageCount
                            )
                            val id = repository.saveDocument(doc)

                            updatedQueue[i] = item.copy(
                                status = BatchItemStatus.COMPLETED,
                                progressMessage = "Saved ${result.reductionPercentage}% (${formatFileSize(result.compressedSize)})",
                                outputFile = outFile
                            )
                            completedList.add(outFile)
                        }

                        BatchOperationType.PDF_TO_IMAGES -> {
                            val imgFiles = PdfEngine.exportPdfPagesAsImages(
                                context = context,
                                pdfFile = input,
                                targetPageIndices = emptySet(), // all pages
                                format = _exportFormat.value,
                                resolution = _exportResolution.value
                            )
                            if (imgFiles.isNotEmpty()) {
                                val totalBytes = imgFiles.sumOf { it.length() }
                                val doc = ConvertedDocument(
                                    title = "BatchImages_${cleanTitle}",
                                    originalType = "PDF",
                                    targetType = _exportFormat.value.name,
                                    filePath = imgFiles.first().absolutePath,
                                    fileSizeFormatted = formatFileSize(totalBytes),
                                    pageCount = imgFiles.size
                                )
                                repository.saveDocument(doc)

                                updatedQueue[i] = item.copy(
                                    status = BatchItemStatus.COMPLETED,
                                    progressMessage = "Extracted ${imgFiles.size} image(s)",
                                    outputFile = imgFiles.first()
                                )
                                completedList.addAll(imgFiles)
                            } else {
                                updatedQueue[i] = item.copy(status = BatchItemStatus.FAILED, errorMessage = "No images extracted")
                            }
                        }

                        BatchOperationType.PDF_TO_TEXT -> {
                            val pages = PdfEngine.renderPdfPagesToBitmaps(context, input)
                            val extractedText = pages.joinToString("\n\n--- Page Break ---\n\n") { "Page ${it.pageIndex + 1}:\n" + it.textContent }
                            val txtFile = File(getExportDir(context), "BatchExtracted_${cleanTitle}_$timestamp.txt")
                            FileOutputStream(txtFile).use { it.write(extractedText.toByteArray()) }

                            val doc = ConvertedDocument(
                                title = "BatchExtracted_${cleanTitle}",
                                originalType = "PDF",
                                targetType = "TXT",
                                filePath = txtFile.absolutePath,
                                fileSizeFormatted = formatFileSize(txtFile.length()),
                                pageCount = pages.size.coerceAtLeast(1)
                            )
                            repository.saveDocument(doc)

                            updatedQueue[i] = item.copy(
                                status = BatchItemStatus.COMPLETED,
                                progressMessage = "Extracted ${extractedText.length} characters",
                                outputFile = txtFile
                            )
                            completedList.add(txtFile)
                        }

                        BatchOperationType.TEXT_TO_PDF -> {
                            val textContent = input.readText()
                            val pdfFile = File(getExportDir(context), "BatchPDF_${cleanTitle}_$timestamp.pdf")
                            val pageCount = PdfEngine.generatePdfFromText(
                                context = context,
                                title = cleanTitle,
                                text = textContent,
                                options = _formatOptions.value,
                                outputFile = pdfFile
                            )

                            val doc = ConvertedDocument(
                                title = "BatchPDF_${cleanTitle}",
                                originalType = "TXT",
                                targetType = "PDF",
                                filePath = pdfFile.absolutePath,
                                fileSizeFormatted = formatFileSize(pdfFile.length()),
                                pageCount = pageCount
                            )
                            repository.saveDocument(doc)

                            updatedQueue[i] = item.copy(
                                status = BatchItemStatus.COMPLETED,
                                progressMessage = "Converted to $pageCount PDF page(s)",
                                outputFile = pdfFile
                            )
                            completedList.add(pdfFile)
                        }

                        BatchOperationType.IMAGES_TO_PDF -> {
                            val pdfFile = File(getExportDir(context), "BatchPDF_${cleanTitle}_$timestamp.pdf")
                            val pageCount = PdfEngine.generatePdfFromImages(
                                context = context,
                                imageUris = listOf(Uri.fromFile(input)),
                                options = _formatOptions.value,
                                outputFile = pdfFile
                            )

                            val doc = ConvertedDocument(
                                title = "BatchPDF_${cleanTitle}",
                                originalType = "IMAGE",
                                targetType = "PDF",
                                filePath = pdfFile.absolutePath,
                                fileSizeFormatted = formatFileSize(pdfFile.length()),
                                pageCount = pageCount
                            )
                            repository.saveDocument(doc)

                            updatedQueue[i] = item.copy(
                                status = BatchItemStatus.COMPLETED,
                                progressMessage = "Converted image to PDF",
                                outputFile = pdfFile
                            )
                            completedList.add(pdfFile)
                        }
                    }

                    completedCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    updatedQueue[i] = item.copy(status = BatchItemStatus.FAILED, errorMessage = e.message ?: "Error during processing")
                }

                _batchQueue.value = updatedQueue.toList()
                _batchCompletedFiles.value = completedList.toList()
                _batchProgress.value = Pair(completedCount, totalCount)
            }

            _isProcessing.value = false
            _statusMessage.value = "Batch processing complete! Converted $completedCount of $totalCount items."
        }
    }

    private fun getUriFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) return it.getString(nameIdx)
                }
            }
            uri.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            _statusMessage.value = "Document deleted from history"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _statusMessage.value = "History cleared"
        }
    }

    private fun loadInitialSamplePages() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val sampleImg1 = SampleData.createSampleImageFile(context, "sample_p1", "PAGE 1: Executive Summary")
            val sampleImg2 = SampleData.createSampleImageFile(context, "sample_p2", "PAGE 2: Project Specifications")
            val sampleImg3 = SampleData.createSampleImageFile(context, "sample_p3", "PAGE 3: Budget & Timeline")

            val initialPages = listOf(
                DocumentPage(UUID.randomUUID().toString(), 0, sampleImg1.absolutePath, "Executive Summary"),
                DocumentPage(UUID.randomUUID().toString(), 1, sampleImg2.absolutePath, "Specifications"),
                DocumentPage(UUID.randomUUID().toString(), 2, sampleImg3.absolutePath, "Budget & Timeline"),
                DocumentPage(UUID.randomUUID().toString(), 3, null, "Blank Appendix Page", isBlankPage = true)
            )
            _reorganizerPages.value = initialPages
        }
    }

    private fun getExportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "ConvertedPDFs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 KB"
        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    /* -------------------------- OCR & SCANNING -------------------------- */

    fun runOcrScan(file: File) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Scanning text with ML Kit OCR..."
            val context = getApplication<Application>()

            try {
                val result = if (file.extension.equals("pdf", true)) {
                    com.example.util.PdfEngine.performOcrOnPdf(context, file)
                } else {
                    com.example.util.PdfEngine.performOcrOnImageFile(context, file)
                }

                _ocrScanResult.value = result
                _statusMessage.value = "OCR Scan complete! (${result.totalPages} page(s))"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "OCR failed: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearOcrResult() {
        _ocrScanResult.value = null
    }

    /* -------------------------- PDF ANNOTATION & SIGNATURE -------------------------- */

    fun saveAnnotatedDocument(
        originalFile: File,
        annotationsMap: Map<Int, com.example.data.model.PageAnnotations>
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Saving PDF annotations & signatures..."
            val context = getApplication<Application>()

            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val cleanName = originalFile.nameWithoutExtension.replace(" ", "_")
                val outputFile = File(getExportDir(context), "Annotated_${cleanName}_$timestamp.pdf")

                com.example.util.PdfEngine.saveAnnotatedPdf(
                    context = context,
                    originalPdfFile = originalFile,
                    annotationsMap = annotationsMap,
                    outputFile = outputFile
                )

                val doc = ConvertedDocument(
                    title = "Annotated ${originalFile.name}",
                    originalType = "PDF",
                    targetType = "PDF",
                    filePath = outputFile.absolutePath,
                    fileSizeFormatted = formatFileSize(outputFile.length()),
                    pageCount = annotationsMap.size.coerceAtLeast(1)
                )
                repository.saveDocument(doc)

                // Auto-sync
                triggerAutoSync(outputFile)

                _statusMessage.value = "Annotated PDF saved successfully!"
                _lastExportedDocument.value = doc
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Failed to save annotations: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /* -------------------------- CLOUD & WEBDAV SYNC -------------------------- */

    private fun loadSyncConfigs() {
        val context = getApplication<Application>()
        _webDavConfig.value = com.example.util.SyncManager.getWebDavConfig(context)
        _localSyncConfig.value = com.example.util.SyncManager.getLocalSyncConfig(context)
    }

    fun updateWebDavConfig(config: com.example.data.model.WebDavConfig) {
        val context = getApplication<Application>()
        com.example.util.SyncManager.saveWebDavConfig(context, config)
        _webDavConfig.value = config
        _statusMessage.value = "WebDAV settings saved"
    }

    fun updateLocalSyncConfig(config: com.example.data.model.LocalFolderSyncConfig) {
        val context = getApplication<Application>()
        com.example.util.SyncManager.saveLocalSyncConfig(context, config)
        _localSyncConfig.value = config
        _statusMessage.value = "Local auto-export settings saved"
    }

    fun testWebDavConnection(config: com.example.data.model.WebDavConfig) {
        viewModelScope.launch {
            _statusMessage.value = "Testing WebDAV connection..."
            val result = com.example.util.SyncManager.testWebDavConnection(config)
            _statusMessage.value = result.second
        }
    }

    fun syncDocumentNow(file: File) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _statusMessage.value = "Syncing ${file.name}..."
            val result = com.example.util.SyncManager.syncDocument(context, file)
            _syncStatusLog.value = result.second
            _statusMessage.value = if (result.first) "Sync completed!" else "Sync finished with warnings"
        }
    }

    private fun triggerAutoSync(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val result = com.example.util.SyncManager.syncDocument(context, file)
            _syncStatusLog.value = result.second
        }
    }

    fun clearSyncStatusLog() {
        _syncStatusLog.value = null
    }
}

