package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageFormatOptions

@Composable
fun FormatToolbar(
    options: PageFormatOptions,
    onOptionsChange: (PageFormatOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("format_toolbar_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Document Formatting & Page Adjustments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Font Family Selector
            Text(
                text = "Font Family",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SansSerif", "Serif", "Monospace").forEach { family ->
                    FilterChip(
                        selected = options.fontFamily == family,
                        onClick = { onOptionsChange(options.copy(fontFamily = family)) },
                        label = { Text(family) },
                        modifier = Modifier.testTag("font_chip_$family")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Font Size & Alignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Font Size: ${options.fontSize.toInt()} pt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = options.fontSize,
                        onValueChange = { onOptionsChange(options.copy(fontSize = it)) },
                        valueRange = 10f..28f,
                        steps = 18,
                        modifier = Modifier.testTag("font_size_slider")
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onOptionsChange(options.copy(textAlignment = "Left")) },
                        modifier = Modifier.testTag("align_left_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatAlignLeft,
                            contentDescription = "Left Align",
                            tint = if (options.textAlignment == "Left") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onOptionsChange(options.copy(textAlignment = "Center")) },
                        modifier = Modifier.testTag("align_center_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatAlignCenter,
                            contentDescription = "Center Align",
                            tint = if (options.textAlignment == "Center") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onOptionsChange(options.copy(textAlignment = "Right")) },
                        modifier = Modifier.testTag("align_right_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatAlignRight,
                            contentDescription = "Right Align",
                            tint = if (options.textAlignment == "Right") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Paper Background Colors
            Text(
                text = "Paper Style & Background Color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Pair("#FFFFFF", "White"),
                    Pair("#FBF0D9", "Sepia"),
                    Pair("#111827", "Charcoal"),
                    Pair("#1E293B", "Slate")
                ).forEach { (hex, name) ->
                    val isSelected = options.paperColorHex == hex
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val textHex = if (hex == "#FFFFFF" || hex == "#FBF0D9") "#0F172A" else "#F8FAFC"
                                onOptionsChange(options.copy(paperColorHex = hex, textColorHex = textHex))
                            }
                            .testTag("paper_color_$name"),
                        color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (hex == "#FFFFFF" || hex == "#FBF0D9") Color.DarkGray else Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                color = if (hex == "#FFFFFF" || hex == "#FBF0D9") Color.Black else Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Watermark & Orientation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = options.watermarkText,
                    onValueChange = { onOptionsChange(options.copy(watermarkText = it)) },
                    label = { Text("Watermark Text") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("watermark_text_input"),
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { onOptionsChange(options.copy(isLandscape = !options.isLandscape)) },
                    modifier = Modifier.testTag("orientation_toggle_button")
                ) {
                    Icon(
                        imageVector = if (options.isLandscape) Icons.Default.Landscape else Icons.Default.Portrait,
                        contentDescription = "Toggle Orientation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
