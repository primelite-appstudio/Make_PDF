package com.example.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawPoint(
    val offset: Offset,
    val isStart: Boolean = false
)

data class DrawingPath(
    val points: List<Offset>,
    val color: Color = Color.Red,
    val strokeWidth: Float = 5f,
    val isHighlighter: Boolean = false
)

data class TextStamp(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val xRatio: Float, // 0.0 to 1.0 on page
    val yRatio: Float, // 0.0 to 1.0 on page
    val color: Color = Color.Red,
    val isSignature: Boolean = false
)

data class PageAnnotations(
    val pageIndex: Int,
    val paths: List<DrawingPath> = emptyList(),
    val stamps: List<TextStamp> = emptyList()
)
