package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConversionMode
import com.example.ui.components.FormatToolbar
import com.example.viewmodel.PdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditorScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val title by viewModel.editorTitle.collectAsStateWithLifecycle()
    val text by viewModel.editorText.collectAsStateWithLifecycle()
    val options by viewModel.formatOptions.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val mode by viewModel.currentMode.collectAsStateWithLifecycle()
    val lastExported by viewModel.lastExportedDocument.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (mode == ConversionMode.IMAGE_TO_PDF) "Images to PDF Engine" else "Text & Document Editor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val ocrResult by viewModel.ocrScanResult.collectAsStateWithLifecycle()
                    if (ocrResult != null) {
                        com.example.ui.components.OcrResultBottomSheet(
                            ocrResult = ocrResult!!,
                            onDismiss = { viewModel.clearOcrResult() }
                        )
                    }

                    if (lastExported != null) {
                        IconButton(
                            onClick = { onNavigateToViewer(lastExported!!.filePath) },
                            modifier = Modifier.testTag("editor_view_exported_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "View Converted PDF", tint = Color(0xFF10B981))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${text.split("\\s+".toRegex()).size} Words • ${text.length} Chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "A4 Format • High DPI Output",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            if (mode == ConversionMode.IMAGE_TO_PDF) {
                                viewModel.convertImagesToPdf()
                            } else {
                                viewModel.convertTextToPdf()
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("convert_now_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert to PDF")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row between Editor & Styling Preview
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Edit Document Text") },
                    icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("editor_tab_text")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Page Format & Style") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("editor_tab_format")
                )
            }

            if (selectedTab == 0) {
                // Main Text Editor & Paper Preview
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.updateEditorTitle(it) },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("document_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Live A4 Paper Sheet Preview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = try {
                                Color(android.graphics.Color.parseColor(options.paperColorHex))
                            } catch (e: Exception) {
                                Color.White
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = options.headerText.ifBlank { "LIVE A4 PAGE PREVIEW" },
                                    fontSize = 10.sp,
                                    color = try { Color(android.graphics.Color.parseColor(options.textColorHex)).copy(alpha = 0.6f) } catch (e: Exception) { Color.Gray }
                                )
                                Text(
                                    text = "${options.fontSize.toInt()}pt • ${options.fontFamily}",
                                    fontSize = 10.sp,
                                    color = try { Color(android.graphics.Color.parseColor(options.textColorHex)).copy(alpha = 0.6f) } catch (e: Exception) { Color.Gray }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = text,
                                onValueChange = { viewModel.updateEditorText(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .testTag("document_text_editor"),
                                placeholder = { Text("Type or paste document content here...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = try { Color(android.graphics.Color.parseColor(options.textColorHex)) } catch (e: Exception) { Color.Black },
                                    unfocusedTextColor = try { Color(android.graphics.Color.parseColor(options.textColorHex)) } catch (e: Exception) { Color.Black }
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = options.fontSize.sp,
                                    fontFamily = when (options.fontFamily) {
                                        "Serif" -> FontFamily.Serif
                                        "Monospace" -> FontFamily.Monospace
                                        else -> FontFamily.SansSerif
                                    },
                                    textAlign = when (options.textAlignment) {
                                        "Center" -> TextAlign.Center
                                        "Right" -> TextAlign.Right
                                        else -> TextAlign.Left
                                    }
                                )
                            )

                            if (options.watermarkText.isNotBlank()) {
                                Text(
                                    text = "WATERMARK: ${options.watermarkText}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red.copy(alpha = 0.4f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = options.footerText.replace("{page}", "1").replace("{total}", "1"),
                                fontSize = 10.sp,
                                color = try { Color(android.graphics.Color.parseColor(options.textColorHex)).copy(alpha = 0.6f) } catch (e: Exception) { Color.Gray },
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            } else {
                // Format & Styling Options Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    FormatToolbar(
                        options = options,
                        onOptionsChange = { viewModel.updateFormatOptions(it) }
                    )
                }
            }
        }
    }
}
