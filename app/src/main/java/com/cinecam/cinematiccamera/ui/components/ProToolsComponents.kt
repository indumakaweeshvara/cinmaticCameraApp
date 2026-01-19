package com.cinecam.cinematiccamera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinecam.cinematiccamera.processing.effects.*
import com.cinecam.cinematiccamera.processing.recording.*
import com.cinecam.cinematiccamera.ui.theme.CineCamColors

/**
 * Pro Tools Panel - Advanced monitoring and effects
 */
@Composable
fun ProToolsPanel(
    // Focus Peaking
    focusPeakingEnabled: Boolean,
    onFocusPeakingToggle: (Boolean) -> Unit,
    focusPeakingColor: FocusPeakingProcessor.PeakingColor,
    onFocusPeakingColorChange: (FocusPeakingProcessor.PeakingColor) -> Unit,
    
    // Zebra
    zebraEnabled: Boolean,
    onZebraToggle: (Boolean) -> Unit,
    zebraThreshold: Int,
    onZebraThresholdChange: (Int) -> Unit,
    
    // Grid
    gridType: GridOverlayProcessor.GridType,
    onGridTypeChange: (GridOverlayProcessor.GridType) -> Unit,
    
    // Film Grain
    filmGrainEnabled: Boolean,
    onFilmGrainToggle: (Boolean) -> Unit,
    filmGrainIntensity: Float,
    onFilmGrainIntensityChange: (Float) -> Unit,
    
    // Anamorphic
    anamorphicEnabled: Boolean,
    onAnamorphicToggle: (Boolean) -> Unit,
    
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "PRO TOOLS",
            style = MaterialTheme.typography.titleSmall,
            color = CineCamColors.Primary,
            fontWeight = FontWeight.Bold
        )
        
        // Focus Peaking Row
        ProToolRow(
            icon = Icons.Outlined.CenterFocusStrong,
            label = "Focus Peaking",
            isEnabled = focusPeakingEnabled,
            onToggle = onFocusPeakingToggle
        ) {
            // Color selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(FocusPeakingProcessor.PeakingColor.entries) { color ->
                    ColorDot(
                        color = Color(color.color),
                        isSelected = color == focusPeakingColor,
                        onClick = { onFocusPeakingColorChange(color) }
                    )
                }
            }
        }
        
        // Zebra Row
        ProToolRow(
            icon = Icons.Outlined.Texture,
            label = "Zebra Pattern",
            isEnabled = zebraEnabled,
            onToggle = onZebraToggle
        ) {
            Text(
                text = "$zebraThreshold%",
                style = MaterialTheme.typography.labelSmall,
                color = CineCamColors.OnSurface
            )
            Slider(
                value = zebraThreshold.toFloat(),
                onValueChange = { onZebraThresholdChange(it.toInt()) },
                valueRange = 70f..100f,
                modifier = Modifier.width(100.dp),
                colors = SliderDefaults.colors(
                    thumbColor = CineCamColors.Primary,
                    activeTrackColor = CineCamColors.Primary
                )
            )
        }
        
        // Grid Overlay Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Grid3x3,
                    contentDescription = "Grid",
                    tint = CineCamColors.OnSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Grid Overlay",
                    style = MaterialTheme.typography.bodySmall,
                    color = CineCamColors.OnBackground
                )
            }
            
            GridTypeSelector(
                selectedType = gridType,
                onTypeSelected = onGridTypeChange
            )
        }
        
        Divider(color = CineCamColors.ButtonBorder)
        
        // Film Grain Row
        ProToolRow(
            icon = Icons.Outlined.Grain,
            label = "Film Grain",
            isEnabled = filmGrainEnabled,
            onToggle = onFilmGrainToggle
        ) {
            Slider(
                value = filmGrainIntensity,
                onValueChange = onFilmGrainIntensityChange,
                valueRange = 0f..1f,
                modifier = Modifier.width(100.dp),
                colors = SliderDefaults.colors(
                    thumbColor = CineCamColors.Primary,
                    activeTrackColor = CineCamColors.Primary
                )
            )
        }
        
        // Anamorphic Row
        ProToolRow(
            icon = Icons.Outlined.AutoAwesome,
            label = "Anamorphic Flares",
            isEnabled = anamorphicEnabled,
            onToggle = onAnamorphicToggle
        ) {}
    }
}

@Composable
private fun ProToolRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    extraContent: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isEnabled) CineCamColors.Primary else CineCamColors.OnSurface,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = CineCamColors.OnBackground
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            extraContent()
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CineCamColors.Primary,
                    checkedTrackColor = CineCamColors.Primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ColorDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = Color.White,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun GridTypeSelector(
    selectedType: GridOverlayProcessor.GridType,
    onTypeSelected: (GridOverlayProcessor.GridType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = when (selectedType) {
                    GridOverlayProcessor.GridType.NONE -> "Off"
                    GridOverlayProcessor.GridType.RULE_OF_THIRDS -> "3rds"
                    GridOverlayProcessor.GridType.GOLDEN_RATIO -> "Golden"
                    GridOverlayProcessor.GridType.CENTER_CROSS -> "Center"
                    GridOverlayProcessor.GridType.DIAGONAL -> "Diag"
                    GridOverlayProcessor.GridType.GRID_3X3 -> "3×3"
                    GridOverlayProcessor.GridType.GRID_4X4 -> "4×4"
                },
                color = CineCamColors.Primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GridOverlayProcessor.GridType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace("_", " ")) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Recording Mode Selector
 */
@Composable
fun RecordingModeSelector(
    currentMode: RecordingMode,
    onModeSelected: (RecordingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(RecordingMode.entries) { mode ->
            RecordingModeChip(
                mode = mode,
                isSelected = mode == currentMode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun RecordingModeChip(
    mode: RecordingMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (icon, label) = when (mode) {
        RecordingMode.NORMAL -> Icons.Outlined.Videocam to "Normal"
        RecordingMode.SLOW_MOTION -> Icons.Outlined.SlowMotionVideo to "Slow-Mo"
        RecordingMode.TIME_LAPSE -> Icons.Outlined.Timer to "Time-Lapse"
        RecordingMode.HYPERLAPSE -> Icons.Outlined.DirectionsRun to "Hyperlapse"
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CineCamColors.Primary else CineCamColors.ButtonBackground,
        label = "modeColor"
    )
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else CineCamColors.OnSurface,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.White else CineCamColors.OnSurface
        )
    }
}

/**
 * Slow Motion Speed Selector
 */
@Composable
fun SlowMotionSpeedSelector(
    currentSpeed: SlowMotionRecorder.SlowMotionSpeed,
    availableSpeeds: List<SlowMotionRecorder.SlowMotionSpeed>,
    onSpeedSelected: (SlowMotionRecorder.SlowMotionSpeed) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        availableSpeeds.forEach { speed ->
            val isSelected = speed == currentSpeed
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) CineCamColors.Primary else Color.Transparent
                    )
                    .clickable { onSpeedSelected(speed) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = speed.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else CineCamColors.OnSurface
                )
            }
        }
    }
}

/**
 * Time-Lapse Interval Selector
 */
@Composable
fun TimeLapseIntervalSelector(
    currentInterval: TimeLapseRecorder.TimeLapseInterval,
    onIntervalSelected: (TimeLapseRecorder.TimeLapseInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TimeLapseRecorder.TimeLapseInterval.entries.take(5).forEach { interval ->
            val isSelected = interval == currentInterval
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) CineCamColors.Primary else Color.Transparent
                    )
                    .clickable { onIntervalSelected(interval) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = interval.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else CineCamColors.OnSurface,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Live Histogram View
 */
@Composable
fun HistogramView(
    histogramData: HistogramProcessor.HistogramData?,
    modifier: Modifier = Modifier
) {
    if (histogramData == null) return
    
    Canvas(
        modifier = modifier
            .size(width = 160.dp, height = 60.dp)
            .background(
                color = CineCamColors.Background.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        val width = size.width
        val height = size.height
        
        val maxValue = maxOf(
            histogramData.luminance.maxOrNull() ?: 1,
            1
        )
        
        val scaleX = width / 256f
        val scaleY = height / maxValue.toFloat()
        
        // Draw luminance histogram
        val path = Path()
        path.moveTo(0f, height)
        
        histogramData.luminance.forEachIndexed { index, value ->
            val x = index * scaleX
            val y = height - (value * scaleY)
            path.lineTo(x, y)
        }
        
        path.lineTo(width, height)
        path.close()
        
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Audio Level Meter View
 */
@Composable
fun AudioMeterView(
    level: Float,
    peakLevel: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(width = 120.dp, height = 20.dp)
            .background(
                color = CineCamColors.Background.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(2.dp)
    ) {
        val width = size.width
        val height = size.height
        
        // Background segments
        drawRect(
            color = Color(0xFF1A1A1A),
            size = Size(width, height)
        )
        
        // Green zone (0-70%)
        val greenWidth = minOf(level * width, width * 0.7f)
        if (greenWidth > 0) {
            drawRect(
                color = Color(0xFF00C853),
                size = Size(greenWidth, height)
            )
        }
        
        // Yellow zone (70-90%)
        if (level > 0.7f) {
            val yellowStart = width * 0.7f
            val yellowWidth = minOf(level * width, width * 0.9f) - yellowStart
            if (yellowWidth > 0) {
                drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(yellowStart, 0f),
                    size = Size(yellowWidth, height)
                )
            }
        }
        
        // Red zone (90-100%)
        if (level > 0.9f) {
            val redStart = width * 0.9f
            val redWidth = level * width - redStart
            if (redWidth > 0) {
                drawRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(redStart, 0f),
                    size = Size(redWidth, height)
                )
            }
        }
        
        // Peak indicator
        drawLine(
            color = Color.White,
            start = Offset(peakLevel * width, 0f),
            end = Offset(peakLevel * width, height),
            strokeWidth = 2f
        )
    }
}
