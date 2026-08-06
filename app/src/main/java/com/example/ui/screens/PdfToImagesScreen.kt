package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ConvertedDocument
import com.example.data.model.DocumentPage
import com.example.util.ImageDpiResolution
import com.example.util.ImageExportFormat
import com.example.viewmodel.PdfViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImagesScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportFile by viewModel.exportPdfFile.collectAsStateWithLifecycle()
    val exportPages by viewModel.exportPdfPages.collectAsStateWithLifecycle()
    val selectedPages by viewModel.selectedExportPages.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.exportFormat.collectAsStateWithLifecycle()
    val selectedResolution by viewModel.exportResolution.collectAsStateWithLifecycle()
    val exportedFiles by viewModel.exportedFiles.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val historyDocuments by viewModel.historyDocuments.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setExportPdfFileFromUri(it) }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PDF to High-Res Images",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Save specific pages or full document as JPEG or PNG",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pdf_to_images_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F2FA)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF7F2FA)
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. SELECT SOURCE PDF
            item {
                Text(
                    text = "1. Select PDF Document",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (exportFile == null) {
                    SelectPdfForImageExportCard(
                        onPickPdf = { pdfPickerLauncher.launch("application/pdf") },
                        recentPdfs = historyDocuments.filter { it.filePath.endsWith(".pdf", ignoreCase = true) },
                        onSelectRecent = { doc ->
                            val file = File(doc.filePath)
                            if (file.exists()) {
                                viewModel.setExportPdfFile(file)
                            } else {
                                pdfPickerLauncher.launch("application/pdf")
                            }
                        }
                    )
                } else {
                    SelectedPdfCard(
                        file = exportFile!!,
                        pageCount = exportPages.size,
                        onChangeFile = { pdfPickerLauncher.launch("application/pdf") },
                        onRemove = { viewModel.resetPdfToImagesExport() }
                    )
                }
            }

            // 2. PAGE SELECTION GRID
            if (exportFile != null && exportPages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Select Pages to Export (${selectedPages.size}/${exportPages.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1C1B1F)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select All",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF6750A4),
                                modifier = Modifier
                                    .clickable { viewModel.selectAllExportPages() }
                                    .padding(4.dp)
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8C1D18),
                                modifier = Modifier
                                    .clickable { viewModel.deselectAllExportPages() }
                                    .padding(4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    PageSelectionGrid(
                        pages = exportPages,
                        selectedIndices = selectedPages,
                        onTogglePage = { viewModel.togglePageSelectionForExport(it) }
                    )
                }
            }

            // 3. EXPORT OPTIONS (FORMAT & RESOLUTION)
            if (exportFile != null) {
                item {
                    Text(
                        text = "3. Export Options",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Format choice
                            Text(
                                text = "Image Format",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ImageExportFormat.entries.forEach { format ->
                                    val isSelected = selectedFormat == format
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF6750A4) else Color(0xFFE7E0EC),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.setExportFormat(format) },
                                        color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF7F2FA)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.setExportFormat(format) },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6750A4))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = format.label,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) Color(0xFF21005D) else Color(0xFF1C1B1F)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Resolution choice
                            Text(
                                text = "Resolution & Detail Level",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ImageDpiResolution.entries.forEach { res ->
                                    val isSelected = selectedResolution == res
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF6750A4) else Color(0xFFE7E0EC),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.setExportResolution(res) },
                                        color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF7F2FA)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { viewModel.setExportResolution(res) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6750A4))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = res.label,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) Color(0xFF21005D) else Color(0xFF1C1B1F)
                                                    )
                                                    Text(
                                                        text = res.description,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color(0xFF21005D).copy(alpha = 0.8f) else Color(0xFF49454F)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. EXPORT ACTION BUTTON & PROGRESS
            if (exportFile != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isProcessing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val (current, total) = exportProgress ?: Pair(0, 1)
                            val fraction = if (total > 0) current.toFloat() / total.toFloat() else 0f

                            Text(
                                text = "Exporting High-Res Image $current of $total...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF6750A4),
                                trackColor = Color(0xFFEADDFF)
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.exportSelectedPagesAsImages() },
                            enabled = selectedPages.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("export_pages_as_images_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6750A4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export ${selectedPages.size} Page(s) as ${selectedFormat.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. EXPORTED RESULTS GRID & ACTIONS
            item {
                AnimatedVisibility(
                    visible = exportedFiles.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ExportedImagesResultCard(
                        files = exportedFiles,
                        format = selectedFormat,
                        onPreview = { file -> onNavigateToViewer(file.absolutePath) },
                        onShareAll = { shareMultipleFiles(context, exportedFiles, selectedFormat.mimeType) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SelectPdfForImageExportCard(
    onPickPdf: () -> Unit,
    recentPdfs: List<ConvertedDocument>,
    onSelectRecent: (ConvertedDocument) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 2.dp,
                color = Color(0xFF6750A4).copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onPickPdf() },
        color = Color(0xFFEADDFF),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Upload PDF",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Choose PDF Document to Extract Images",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF21005D)
            )

            Text(
                text = "Select any PDF to extract pages as individual PNG or JPEG files",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF21005D).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            if (recentPdfs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Or choose from recent conversions:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF21005D)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentPdfs.take(5)) { doc ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectRecent(doc) },
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF6750A4),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    color = Color(0xFF1C1B1F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedPdfCard(
    file: File,
    pageCount: Int,
    onChangeFile: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEADDFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Color(0xFF21005D),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$pageCount Total Pages • ${formatBytes(file.length())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F)
                )
            }

            IconButton(onClick = onChangeFile) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Change File",
                    tint = Color(0xFF6750A4)
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove File",
                    tint = Color(0xFF49454F)
                )
            }
        }
    }
}

@Composable
private fun PageSelectionGrid(
    pages: List<DocumentPage>,
    selectedIndices: Set<Int>,
    onTogglePage: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pages, key = { it.id }) { page ->
                val isSelected = selectedIndices.contains(page.pageIndex)

                Box(
                    modifier = Modifier
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFE7E0EC),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onTogglePage(page.pageIndex) }
                ) {
                    if (page.bitmapPath != null) {
                        AsyncImage(
                            model = File(page.bitmapPath),
                            contentDescription = "Page ${page.pageIndex + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF7F2FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Page ${page.pageIndex + 1}")
                        }
                    }

                    // Checkbox badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF6750A4) else Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Bottom page label badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Page ${page.pageIndex + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportedImagesResultCard(
    files: List<File>,
    format: ImageExportFormat,
    onPreview: (File) -> Unit,
    onShareAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = Color.White,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Export Complete!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "${files.size} high-res ${format.name} image(s) saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thumbnails row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(files) { imgFile ->
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                            .clickable { onPreview(imgFile) }
                    ) {
                        AsyncImage(
                            model = imgFile,
                            contentDescription = imgFile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = imgFile.nameWithoutExtension.takeLast(6),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { if (files.isNotEmpty()) onPreview(files.first()) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_first_image_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview")
                }

                OutlinedButton(
                    onClick = onShareAll,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_all_images_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6750A4)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share All")
                }
            }
        }
    }
}

private fun shareMultipleFiles(context: Context, files: List<File>, mimeType: String) {
    try {
        val uris = ArrayList<Uri>()
        files.forEach { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            uris.add(uri)
        }

        val intent = Intent().apply {
            action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            type = mimeType
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Exported Images"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}
