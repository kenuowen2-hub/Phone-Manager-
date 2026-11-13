package com.example.ui.screens.privacy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeColorEngine

private val quickSwatchColors = listOf(
    Color(0xFF0284C7),
    Color(0xFF0D9488),
    Color(0xFF4338CA),
    Color(0xFF7C3AED),
    Color(0xFF059669),
    Color(0xFFD97706),
    Color(0xFFDC2626),
    Color(0xFFDB2777)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorPickerSection(
    mainColor: Color,
    accentColor: Color,
    onMainColorChange: (Color) -> Unit,
    onAccentColorChange: (Color) -> Unit
) {
    var editingMain by remember { mutableStateOf(true) }
    val activeColor = if (editingMain) mainColor else accentColor
    val onActiveColorChange = if (editingMain) onMainColorChange else onAccentColorChange

    val initialHsv = remember(activeColor) { ThemeColorEngine.colorToHsv(activeColor) }
    var hue by remember(editingMain) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(editingMain) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(editingMain) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(activeColor) { mutableStateOf(ThemeColorEngine.colorToHex(activeColor)) }

    // Keep HSV sliders synced if activeColor changed externally (e.g., reset or swatch tap)
    LaunchedEffect(activeColor, editingMain) {
        val currentFromHsv = ThemeColorEngine.hsvToColor(hue, saturation, value)
        if (currentFromHsv != activeColor) {
            val hsv = ThemeColorEngine.colorToHsv(activeColor)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        }
        hexInput = ThemeColorEngine.colorToHex(activeColor)
    }

    val currentHue by rememberUpdatedState(hue)
    val currentSat by rememberUpdatedState(saturation)
    val currentVal by rememberUpdatedState(value)
    val currentCallback by rememberUpdatedState(onActiveColorChange)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Target Selector: Main vs Accent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ColorTargetChip(
                label = "Main Color",
                color = mainColor,
                selected = editingMain,
                onClick = { editingMain = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_main_color_tab")
            )
            ColorTargetChip(
                label = "Accent Color",
                color = accentColor,
                selected = !editingMain,
                onClick = { editingMain = false },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_accent_color_tab")
            )
        }

        // 2D Saturation / Brightness Box
        val pureHueColor = remember(hue) { ThemeColorEngine.hsvToColor(hue, 1f, 1f) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .testTag("sv_color_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(editingMain) {
                        detectTapGestures { offset ->
                            val s = (offset.x / size.width).coerceIn(0f, 1f)
                            val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            saturation = s
                            value = v
                            currentCallback(ThemeColorEngine.hsvToColor(currentHue, s, v))
                        }
                    }
                    .pointerInput(editingMain) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val s = (change.position.x / size.width).coerceIn(0f, 1f)
                            val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            saturation = s
                            value = v
                            currentCallback(ThemeColorEngine.hsvToColor(currentHue, s, v))
                        }
                    }
            ) {
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.White, pureHueColor))
                )
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                )

                val thumbX = saturation.coerceIn(0f, 1f) * size.width
                val thumbY = (1f - value.coerceIn(0f, 1f)) * size.height
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = 12.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 10.dp.toPx(),
                    center = Offset(thumbX, thumbY),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        // Hue Spectrum Bar
        val hueColors = remember {
            listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .testTag("hue_color_slider")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(editingMain) {
                        detectTapGestures { offset ->
                            val newHue = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
                            hue = newHue
                            currentCallback(ThemeColorEngine.hsvToColor(newHue, currentSat, currentVal))
                        }
                    }
                    .pointerInput(editingMain) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newHue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                            hue = newHue
                            currentCallback(ThemeColorEngine.hsvToColor(newHue, currentSat, currentVal))
                        }
                    }
            ) {
                drawRect(brush = Brush.horizontalGradient(hueColors))
                val thumbX = (hue.coerceIn(0f, 360f) / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 11.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
                drawCircle(
                    color = pureHueColor,
                    radius = 7.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
            }
        }

        // Quick Swatches + Hex Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSwatchColors.forEach { swatch ->
                    val isSelected = swatch == activeColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable {
                                val hsv = ThemeColorEngine.colorToHsv(swatch)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                onActiveColorChange(swatch)
                            }
                    )
                }
            }

            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    val filtered = raw.uppercase().take(7)
                    hexInput = filtered
                    ThemeColorEngine.parseHexColor(filtered)?.let { parsed ->
                        val hsv = ThemeColorEngine.colorToHsv(parsed)
                        hue = hsv[0]
                        saturation = hsv[1]
                        value = hsv[2]
                        onActiveColorChange(parsed)
                    }
                },
                label = { Text("Hex") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .width(116.dp)
                    .testTag("custom_hex_input"),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun ColorTargetChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ThemeColorEngine.colorToHex(color),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
