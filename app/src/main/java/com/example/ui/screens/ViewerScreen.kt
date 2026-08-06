package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.DocumentPage
import com.example.data.model.DrawingPath
import com.example.data.model.PageAnnotations
import com.example.data.model.TextStamp
import com.example.ui.components.OcrResultBottomSheet
import com.example.ui.components.SignaturePadDialog
import com.example.util.PdfEngine
import com.example.viewmodel.PdfViewModel
import java.io.File

enum class AnnotationMode {
    NONE,
    FREEHAND_PEN,
    HIGHLIGHTER,
    SIGNATURE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    filePath: String,
    viewModel: PdfViewModel? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<DocumentPage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var annotationMode by remember { mutableStateOf(AnnotationMode.NONE) }
    var showSignaturePad by remember { mutableStateOf(false) }

    val pageAnnotations = remember { mutableStateMapOf<Int, PageAnnotations>() }

    val file = remember(filePath) { File(filePath) }

    val ocrResult = viewModel?.ocrScanResult?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(filePath) {
        if (file.exists()) {
            val rendered = PdfEngine.renderPdfPagesToBitmaps(context, file, scaleFactor = 2.0f)
            pages = rendered
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = if (pages.isNotEmpty()) "${pages.size} Page(s) • ${if (annotationMode != AnnotationMode.NONE) "Annotating" else "Viewer"}" else "PDF Document",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // OCR Scanning Action
                    if (viewModel != null) {
                        IconButton(
                            onClick = { viewModel.runOcrScan(file) },
                            modifier = Modifier.testTag("viewer_ocr_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "OCR Scan Text",
                                tint = Color(0xFF6750A4)
                            )
                        }

                        // Sync Action
                        IconButton(
                            onClick = { viewModel.syncDocumentNow(file) },
                            modifier = Modifier.testTag("viewer_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Sync to Destinations",
                                tint = Color(0xFF006A6A)
                            )
                        }
                    }

                    // Share Action
                    IconButton(
                        onClick = { shareFile(context, file) },
                        modifier = Modifier.testTag("viewer_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Document")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (pages.isNotEmpty()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Pen Mode
                            IconButton(
                                onClick = {
                                    annotationMode = if (annotationMode == AnnotationMode.FREEHAND_PEN) AnnotationMode.NONE else AnnotationMode.FREEHAND_PEN
                                },
                                modifier = Modifier
                                    .background(
                                        if (annotationMode == AnnotationMode.FREEHAND_PEN) Color(0xFFE8DEF8) else Color.Transparent,
                                        CircleShape
                                    )
                                    .testTag("annotation_mode_pen")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Red Pen", tint = Color.Red)
                            }

                            // Highlighter Mode
                            IconButton(
                                onClick = {
                                    annotationMode = if (annotationMode == AnnotationMode.HIGHLIGHTER) AnnotationMode.NONE else AnnotationMode.HIGHLIGHTER
                                },
                                modifier = Modifier
                                    .background(
                                        if (annotationMode == AnnotationMode.HIGHLIGHTER) Color(0xFFFFF9C4) else Color.Transparent,
                                        CircleShape
                                    )
                                    .testTag("annotation_mode_highlighter")
                            ) {
                                Icon(Icons.Default.Highlight, contentDescription = "Yellow Highlighter", tint = Color(0xFFFBC02D))
                            }

                            // Signature Mode
                            IconButton(
                                onClick = { showSignaturePad = true },
                                modifier = Modifier.testTag("annotation_mode_signature")
                            ) {
                                Icon(Icons.Default.Draw, contentDescription = "Electronic Signature", tint = Color(0xFF6750A4))
                            }
                        }

                        if (pageAnnotations.isNotEmpty() && viewModel != null) {
                            Button(
                                onClick = {
                                    viewModel.saveAnnotatedDocument(file, pageAnnotations.toMap())
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("save_annotated_pdf_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Annotations")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rendering PDF Pages...")
                }
            }
        } else if (pages.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Document exported successfully to:")
                    Text(
                        text = file.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { shareFile(context, file) },
                        modifier = Modifier.testTag("viewer_empty_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share / Open External")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0F172A)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(pages, key = { index, _ -> index }) { index, page ->
                    val activePageAnno = pageAnnotations[index] ?: PageAnnotations(index)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Page ${index + 1} of ${pages.size}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )

                                    Row {
                                        // Quick Stamps
                                        Text(
                                            text = "+ Stamp: APPROVED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF006A6A),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (page.bitmapPath != null) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = File(page.bitmapPath),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )

                                    // Canvas Overlay for Freehand Drawing & Annotations
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .then(
                                                if (annotationMode != AnnotationMode.NONE) {
                                                    Modifier.pointerInput(annotationMode) {
                                                        detectDragGestures(
                                                            onDragStart = { offset ->
                                                                val isHigh = annotationMode == AnnotationMode.HIGHLIGHTER
                                                                val color = if (isHigh) Color.Yellow else Color.Red
                                                                val stroke = if (isHigh) 28f else 6f

                                                                val newPath = DrawingPath(
                                                                    points = listOf(offset),
                                                                    color = color,
                                                                    strokeWidth = stroke,
                                                                    isHighlighter = isHigh
                                                                )
                                                                val current = pageAnnotations[index] ?: PageAnnotations(index)
                                                                pageAnnotations[index] = current.copy(
                                                                    paths = current.paths + newPath
                                                                )
                                                            },
                                                            onDrag = { change, _ ->
                                                                change.consume()
                                                                val current = pageAnnotations[index] ?: PageAnnotations(index)
                                                                if (current.paths.isNotEmpty()) {
                                                                    val lastPath = current.paths.last()
                                                                    val updatedPath = lastPath.copy(
                                                                        points = lastPath.points + change.position
                                                                    )
                                                                    pageAnnotations[index] = current.copy(
                                                                        paths = current.paths.dropLast(1) + updatedPath
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else Modifier
                                            )
                                    ) {
                                        Canvas(modifier = Modifier.matchParentSize()) {
                                            for (pathData in activePageAnno.paths) {
                                                if (pathData.points.size > 1) {
                                                    val path = Path()
                                                    path.moveTo(pathData.points.first().x, pathData.points.first().y)
                                                    for (ptIdx in 1 until pathData.points.size) {
                                                        path.lineTo(pathData.points[ptIdx].x, pathData.points[ptIdx].y)
                                                    }
                                                    drawPath(
                                                        path = path,
                                                        color = pathData.color,
                                                        alpha = if (pathData.isHighlighter) 0.4f else 1.0f,
                                                        style = Stroke(width = pathData.strokeWidth)
                                                    )
                                                }
                                            }
                                        }

                                        // Render Text Stamps / Electronic Signatures
                                        for (stamp in activePageAnno.stamps) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .align(Alignment.BottomEnd)
                                            ) {
                                                Text(
                                                    text = stamp.text,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = stamp.color,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    }

    // OCR Results Bottom Sheet
    if (ocrResult != null) {
        OcrResultBottomSheet(
            ocrResult = ocrResult,
            onDismiss = { viewModel?.clearOcrResult() }
        )
    }

    // Signature Pad Dialog
    if (showSignaturePad) {
        SignaturePadDialog(
            onSignatureCaptured = { sigLabel ->
                val activeIdx = 0
                val current = pageAnnotations[activeIdx] ?: PageAnnotations(activeIdx)
                val newStamp = TextStamp(
                    text = sigLabel,
                    xRatio = 0.6f,
                    yRatio = 0.85f,
                    color = Color(0xFF6750A4),
                    isSignature = true
                )
                pageAnnotations[activeIdx] = current.copy(
                    stamps = current.stamps + newStamp
                )
                showSignaturePad = false
            },
            onDismiss = { showSignaturePad = false }
        )
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
