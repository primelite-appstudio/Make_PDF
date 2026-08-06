package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConversionMode
import com.example.data.model.ConvertedDocument
import com.example.ui.components.DragDropBox
import com.example.viewmodel.PdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PdfViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToReorganizer: () -> Unit,
    onNavigateToCompress: () -> Unit,
    onNavigateToPdfToImages: () -> Unit,
    onNavigateToBatch: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyDocs by viewModel.historyDocuments.collectAsStateWithLifecycle()
    val activeMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val webDavConfig by viewModel.webDavConfig.collectAsStateWithLifecycle()
    val localSyncConfig by viewModel.localSyncConfig.collectAsStateWithLifecycle()
    var showSyncSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PDF Converter Pro",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "High Quality Conversion & Editor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSyncSettings = true },
                        modifier = Modifier.testTag("sync_settings_top_icon")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Cloud & Local Sync Destinations")
                    }
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("history_top_icon")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History & Files")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Drag and Drop Import Banner
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DragDropBox(
                    onFilesPicked = { uris ->
                        viewModel.setImageUris(uris)
                        viewModel.setMode(ConversionMode.IMAGE_TO_PDF)
                        onNavigateToEditor()
                    },
                    onPdfPicked = { uri ->
                        viewModel.loadPdfForReorganizationFromUri(uri)
                        viewModel.setMode(ConversionMode.PDF_ORGANIZER)
                        onNavigateToReorganizer()
                    }
                )
            }

            // Core Conversion Modes Grid
            item {
                Text(
                    text = "Conversion & Editing Tools",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Span 2 Full Width Card - Batch Processing Utility
                    BentoToolCard(
                        title = "Batch Document Utility",
                        subtitle = "Select multiple files to convert, compress, or extract simultaneously",
                        icon = Icons.Default.Layers,
                        containerColor = Color(0xFFEADDFF),
                        contentColor = Color(0xFF21005D),
                        iconBoxBg = Color(0xFF6750A4),
                        iconTint = Color.White,
                        borderColor = Color(0xFFD0BCFF),
                        onClick = {
                            viewModel.setMode(ConversionMode.BATCH_CONVERT)
                            onNavigateToBatch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_card_batch_processing")
                    )

                    // Span 2 Full Width Card - Edit Text / Doc to PDF
                    BentoToolCard(
                        title = "Edit & Format Text",
                        subtitle = "Modify text, add rich typography & formatting",
                        icon = Icons.Default.TextSnippet,
                        containerColor = Color.White,
                        contentColor = Color(0xFF1C1B1F),
                        iconBoxBg = Color(0xFFF3EDF7),
                        iconTint = Color(0xFF6750A4),
                        borderColor = Color(0xFFE7E0EC),
                        onClick = {
                            viewModel.setMode(ConversionMode.TEXT_TO_PDF)
                            onNavigateToEditor()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_card_text_to_pdf")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Soft Blue Bento Card - Reorganize
                        BentoCompactCard(
                            title = "Organize",
                            subtitle = "Merge, split, rotate",
                            icon = Icons.Default.Reorder,
                            containerColor = Color(0xFFD0E4FF),
                            contentColor = Color(0xFF001D35),
                            onClick = {
                                viewModel.setMode(ConversionMode.PDF_ORGANIZER)
                                onNavigateToReorganizer()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_card_reorganize")
                        )

                        // Soft Rose Bento Card - Images to PDF
                        BentoCompactCard(
                            title = "Images to PDF",
                            subtitle = "JPG/PNG gallery scan",
                            icon = Icons.Default.AddPhotoAlternate,
                            containerColor = Color(0xFFF2B8B5),
                            contentColor = Color(0xFF410E0B),
                            onClick = {
                                viewModel.setMode(ConversionMode.IMAGE_TO_PDF)
                                onNavigateToEditor()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_card_images_to_pdf")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Soft Lavender Bento Card - PDF to Images
                        BentoCompactCard(
                            title = "PDF to Images",
                            subtitle = "Extract high-res pages",
                            icon = Icons.Default.Image,
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D),
                            onClick = {
                                viewModel.setMode(ConversionMode.PDF_TO_IMAGES)
                                onNavigateToPdfToImages()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_card_pdf_to_images")
                            )

                        // Soft Coral Bento Card - Compress PDF
                        BentoCompactCard(
                            title = "Compress PDF",
                            subtitle = "Reduce PDF file size",
                            icon = Icons.Default.Compress,
                            containerColor = Color(0xFFFFDADA),
                            contentColor = Color(0xFF410002),
                            onClick = {
                                viewModel.setMode(ConversionMode.COMPRESS_PDF)
                                onNavigateToCompress()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tool_card_compress_pdf")
                        )
                    }

                    // Soft Neutral Bento Card - Files History
                    BentoToolCard(
                        title = "Files & History",
                        subtitle = "Access all converted documents and exports",
                        icon = Icons.Default.History,
                        containerColor = Color.White,
                        contentColor = Color(0xFF1C1B1F),
                        iconBoxBg = Color(0xFFF3EDF7),
                        iconTint = Color(0xFF6750A4),
                        borderColor = Color(0xFFE7E0EC),
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_card_history")
                    )
                }
            }

            // Quick Starter Templates
            item {
                Text(
                    text = "Quick Sample Templates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TemplateChip(
                        title = "Resume / CV",
                        icon = Icons.Default.Article,
                        onClick = {
                            viewModel.loadSampleTemplate("Resume")
                            viewModel.setMode(ConversionMode.TEXT_TO_PDF)
                            onNavigateToEditor()
                        },
                        tag = "template_resume"
                    )

                    TemplateChip(
                        title = "Business Invoice",
                        icon = Icons.Default.Receipt,
                        onClick = {
                            viewModel.loadSampleTemplate("Invoice")
                            viewModel.setMode(ConversionMode.TEXT_TO_PDF)
                            onNavigateToEditor()
                        },
                        tag = "template_invoice"
                    )

                    TemplateChip(
                        title = "Meeting Sync Notes",
                        icon = Icons.Default.Description,
                        onClick = {
                            viewModel.loadSampleTemplate("Notes")
                            viewModel.setMode(ConversionMode.TEXT_TO_PDF)
                            onNavigateToEditor()
                        },
                        tag = "template_notes"
                    )
                }
            }

            // Recent Converted Files
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Converted Files",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (historyDocs.isNotEmpty()) {
                        Text(
                            text = "View All (${historyDocs.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onNavigateToHistory() }
                                .testTag("view_all_history_text")
                        )
                    }
                }
            }

            if (historyDocs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No converted files yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Convert text, images or reorganize PDF pages to see history here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(historyDocs.take(3)) { doc ->
                    RecentFileItem(
                        doc = doc,
                        onClick = { onNavigateToViewer(doc.filePath) }
                    )
                }
            }
        }
    }

    if (showSyncSettings) {
        com.example.ui.components.SyncSettingsBottomSheet(
            webDavConfig = webDavConfig,
            localSyncConfig = localSyncConfig,
            onSaveWebDav = { viewModel.updateWebDavConfig(it) },
            onSaveLocalSync = { viewModel.updateLocalSyncConfig(it) },
            onTestWebDav = { viewModel.testWebDavConnection(it) },
            onDismiss = { showSyncSettings = false }
        )
    }
}

@Composable
fun BentoToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    iconBoxBg: Color,
    iconTint: Color,
    borderColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (borderColor != null) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(24.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        color = containerColor,
        shape = RoundedCornerShape(24.dp)
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBoxBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BentoCompactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (borderColor != null) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(24.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        color = containerColor,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier
                    .size(28.dp)
                    .padding(bottom = 8.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TemplateChip(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag),
        color = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RecentFileItem(
    doc: ConvertedDocument,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFFFDADA),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF410002),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${doc.originalType} → ${doc.targetType} • ${doc.pageCount} pages • ${doc.fileSizeFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "View File",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
