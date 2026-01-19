package com.cinecam.cinematiccamera.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.ui.theme.CineCamColors

/**
 * Record Button - Animated circular button for recording
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 0.6f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "recordScale"
    )
    
    val cornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 35.dp,
        animationSpec = tween(200),
        label = "cornerRadius"
    )
    
    // Pulsing animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = modifier
            .size(70.dp)
            .scale(if (isRecording) pulseScale else 1f)
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CineCamColors.OnBackground,
                        CineCamColors.OnSurface
                    )
                ),
                shape = CircleShape
            )
            .padding(6.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp * scale)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    if (isRecording) CineCamColors.RecordingRed
                    else CineCamColors.Primary
                )
        )
    }
}

/**
 * Recording Timer Display
 */
@Composable
fun RecordingTimer(
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000 / 60) % 60
    val hours = durationMs / 1000 / 60 / 60
    
    val timeText = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
    
    Row(
        modifier = modifier
            .background(
                color = CineCamColors.RecordingRed.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Blinking dot
        val infiniteTransition = rememberInfiniteTransition(label = "blink")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blinkAlpha"
        )
        
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = Color.White.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
        
        Text(
            text = timeText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

/**
 * Control Button - Circular icon button
 */
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 48.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .background(
                    color = if (isActive) CineCamColors.Primary.copy(alpha = 0.2f)
                            else CineCamColors.ButtonBackground.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isActive) CineCamColors.Primary
                            else CineCamColors.ButtonBorder,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CineCamColors.Primary else CineCamColors.OnBackground,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CineCamColors.OnSurface
        )
    }
}

/**
 * Focus Slider - Vertical slider for manual focus
 */
@Composable
fun FocusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Landscape,
            contentDescription = "Far",
            tint = CineCamColors.OnSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        
        // Vertical slider (rotated horizontal slider)
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(200.dp)
                .rotate(-90f)
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier.width(200.dp),
                colors = SliderDefaults.colors(
                    thumbColor = CineCamColors.Primary,
                    activeTrackColor = CineCamColors.Primary,
                    inactiveTrackColor = CineCamColors.SliderTrack
                )
            )
        }
        
        Icon(
            imageVector = Icons.Outlined.PersonPin,
            contentDescription = "Near",
            tint = CineCamColors.OnSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = "FOCUS",
            style = MaterialTheme.typography.labelSmall,
            color = CineCamColors.OnSurface
        )
    }
}

/**
 * Exposure Wheel - Circular exposure compensation control
 */
@Composable
fun ExposureWheel(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "EV",
            style = MaterialTheme.typography.labelSmall,
            color = CineCamColors.OnSurface
        )
        
        // Exposure value display
        Text(
            text = if (value >= 0) "+%.1f".format(value) else "%.1f".format(value),
            style = MaterialTheme.typography.titleMedium,
            color = CineCamColors.Primary,
            fontWeight = FontWeight.Bold
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -2f..2f,
            steps = 7,
            modifier = Modifier.width(120.dp),
            colors = SliderDefaults.colors(
                thumbColor = CineCamColors.Primary,
                activeTrackColor = CineCamColors.Primary,
                inactiveTrackColor = CineCamColors.SliderTrack
            )
        )
    }
}

/**
 * Aspect Ratio Selector
 */
@Composable
fun AspectRatioSelector(
    selectedRatio: AspectRatio,
    onRatioSelected: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AspectRatio.entries.forEach { ratio ->
            val isSelected = ratio == selectedRatio
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) CineCamColors.Primary
                        else Color.Transparent
                    )
                    .clickable { onRatioSelected(ratio) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ratio.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else CineCamColors.OnSurface
                )
            }
        }
    }
}

/**
 * ISO Selector
 */
@Composable
fun IsoSelector(
    selectedIso: Int,
    onIsoSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IsoValues.available.forEach { iso ->
            val isSelected = iso == selectedIso
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) CineCamColors.Primary
                        else Color.Transparent
                    )
                    .clickable { onIsoSelected(iso) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iso.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else CineCamColors.OnSurface
                )
            }
        }
    }
}

/**
 * Frame Rate Selector
 */
@Composable
fun FrameRateSelector(
    selectedFps: FrameRate,
    onFpsSelected: (FrameRate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FrameRate.entries.forEach { fps ->
            val isSelected = fps == selectedFps
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) CineCamColors.Primary
                        else Color.Transparent
                    )
                    .clickable { onFpsSelected(fps) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fps.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else CineCamColors.OnSurface
                )
            }
        }
    }
}

/**
 * Bokeh Toggle with intensity slider
 */
@Composable
fun BokehControl(
    isEnabled: Boolean,
    intensity: Float,
    onToggle: (Boolean) -> Unit,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.BlurOn,
                contentDescription = "Bokeh",
                tint = if (isEnabled) CineCamColors.Primary else CineCamColors.OnSurface
            )
            Text(
                text = "BOKEH",
                style = MaterialTheme.typography.labelMedium,
                color = CineCamColors.OnBackground
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CineCamColors.Primary,
                    checkedTrackColor = CineCamColors.Primary.copy(alpha = 0.5f)
                )
            )
        }
        
        if (isEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Intensity",
                    style = MaterialTheme.typography.labelSmall,
                    color = CineCamColors.OnSurface
                )
                Slider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.width(150.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CineCamColors.Primary,
                        activeTrackColor = CineCamColors.Primary,
                        inactiveTrackColor = CineCamColors.SliderTrack
                    )
                )
                Text(
                    text = "${(intensity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = CineCamColors.Primary
                )
            }
        }
    }
}

/**
 * Status Bar showing current settings
 */
@Composable
fun CameraStatusBar(
    settings: CameraSettings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CineCamColors.OverlayDark)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Resolution and FPS
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(label = settings.resolution.displayName)
            StatusChip(label = settings.frameRate.displayName)
            if (settings.bokehEnabled) {
                StatusChip(label = "BOKEH", isActive = true)
            }
        }
        
        // Right side - Manual settings
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(label = "ISO ${settings.iso}")
            StatusChip(label = settings.shutterSpeed.displayName)
            StatusChip(
                label = if (settings.exposureCompensation >= 0) 
                    "+${settings.exposureCompensation}" 
                    else settings.exposureCompensation.toString(),
                prefix = "EV"
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    prefix: String? = null,
    isActive: Boolean = false
) {
    Text(
        text = if (prefix != null) "$prefix $label" else label,
        style = MaterialTheme.typography.labelSmall,
        color = if (isActive) CineCamColors.Primary else CineCamColors.OnBackground
    )
}
