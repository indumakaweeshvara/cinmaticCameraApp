package com.cinecam.cinematiccamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.domain.repository.CameraRepository
import com.cinecam.cinematiccamera.ui.components.*
import com.cinecam.cinematiccamera.ui.theme.CineCamColors
import com.cinecam.cinematiccamera.ui.theme.CinematicCameraTheme
import com.cinecam.cinematiccamera.viewmodel.CameraViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Camera Activity - Main camera interface
 * 
 * Full-screen camera view with cinematic controls overlay.
 */
@AndroidEntryPoint
class CameraActivity : ComponentActivity() {
    
    private val viewModel: CameraViewModel by viewModels()
    
    @Inject
    lateinit var cameraRepository: CameraRepository
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted, camera will initialize via Composable
        } else {
            Toast.makeText(this, "Camera permissions required", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Check permissions
        checkAndRequestPermissions()
        
        setContent {
            CinematicCameraTheme {
                CameraScreen(
                    viewModel = viewModel,
                    cameraRepository = cameraRepository
                )
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up will be handled by ViewModel
    }
}

/**
 * Camera Screen Composable
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    cameraRepository: CameraRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // State collection
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle(0L)
    
    // Preview view reference
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    // Initialize camera when preview is ready
    LaunchedEffect(previewView) {
        previewView?.let { view ->
            cameraRepository.initializeCamera(lifecycleOwner, view)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineCamColors.Background)
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    preview.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    preview.scaleType = PreviewView.ScaleType.FIT_CENTER
                    previewView = preview
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Cinematic letterbox overlay for aspect ratios
        if (settings.aspectRatio != AspectRatio.RATIO_16_9) {
            CinematicLetterbox(aspectRatio = settings.aspectRatio)
        }
        
        // Top Status Bar
        CameraStatusBar(
            settings = settings,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Recording indicator
        if (recordingState is RecordingState.Recording) {
            RecordingTimer(
                durationMs = recordingDuration,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )
        }
        
        // Left side - Focus control
        if (uiState.showControlsPanel) {
            FocusSlider(
                value = settings.focusDistance,
                onValueChange = { viewModel.onFocusChanged(it) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            )
        }
        
        // Right side - Quick controls
        if (uiState.showControlsPanel) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ControlButton(
                    icon = Icons.Outlined.FlipCameraAndroid,
                    label = "Flip",
                    onClick = { viewModel.onSwitchCamera() }
                )
                
                ControlButton(
                    icon = if (settings.bokehEnabled) Icons.Filled.BlurOn else Icons.Outlined.BlurOff,
                    label = "Bokeh",
                    onClick = { viewModel.onBokehToggle(!settings.bokehEnabled) },
                    isActive = settings.bokehEnabled
                )
                
                ControlButton(
                    icon = Icons.Outlined.Tune,
                    label = "Settings",
                    onClick = { viewModel.toggleSettings() }
                )
                
                ControlButton(
                    icon = Icons.Outlined.ColorLens,
                    label = "LUT",
                    onClick = { viewModel.toggleLutSelector() }
                )
            }
        }
        
        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings panel
            if (uiState.showSettings) {
                SettingsPanel(
                    settings = settings,
                    onIsoChanged = { viewModel.onIsoChanged(it) },
                    onFpsChanged = { viewModel.onFrameRateChanged(it) },
                    onAspectRatioChanged = { /* Update through repository */ },
                    onBokehToggle = { viewModel.onBokehToggle(it) },
                    onBokehIntensityChanged = { viewModel.onBokehIntensityChanged(it) },
                    on180RuleApply = { viewModel.apply180ShutterRule() }
                )
            }
            
            // Exposure control
            if (uiState.showControlsPanel) {
                ExposureWheel(
                    value = settings.exposureCompensation,
                    onValueChange = { viewModel.onExposureChanged(it) }
                )
            }
            
            // Record button row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Gallery button
                ControlButton(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "Gallery",
                    onClick = { /* Open gallery */ }
                )
                
                // Record button
                RecordButton(
                    isRecording = recordingState is RecordingState.Recording,
                    onClick = { viewModel.toggleRecording() }
                )
                
                // Show/Hide controls
                ControlButton(
                    icon = if (uiState.showControlsPanel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    label = "Controls",
                    onClick = { viewModel.toggleControlsPanel() }
                )
            }
        }
        
        // Loading overlay
        if (cameraState is CameraState.Initializing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CineCamColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = CineCamColors.Primary)
                    Text(
                        text = "Initializing Camera...",
                        color = CineCamColors.OnBackground
                    )
                }
            }
        }
        
        // Error display
        if (cameraState is CameraState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CineCamColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (cameraState as CameraState.Error).message,
                    color = CineCamColors.Error
                )
            }
        }
        
        // Toast messages
        uiState.message?.let { message ->
            LaunchedEffect(message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }
}

/**
 * Cinematic letterbox overlay
 */
@Composable
private fun CinematicLetterbox(aspectRatio: AspectRatio) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val screenRatio = screenWidth / screenHeight
        
        if (aspectRatio.ratio < screenRatio.value) {
            // Add letterbox bars on sides (pillarbox)
            val contentWidth = screenHeight * aspectRatio.ratio
            val barWidth = (screenWidth - contentWidth) / 2
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(barWidth)
                    .align(Alignment.CenterStart)
                    .background(CineCamColors.Background)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(barWidth)
                    .align(Alignment.CenterEnd)
                    .background(CineCamColors.Background)
            )
        } else {
            // Add letterbox bars on top/bottom
            val contentHeight = screenWidth / aspectRatio.ratio
            val barHeight = (screenHeight - contentHeight.dp) / 2
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.TopCenter)
                    .background(CineCamColors.Background)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.BottomCenter)
                    .background(CineCamColors.Background)
            )
        }
    }
}

/**
 * Settings Panel
 */
@Composable
private fun SettingsPanel(
    settings: CameraSettings,
    onIsoChanged: (Int) -> Unit,
    onFpsChanged: (FrameRate) -> Unit,
    onAspectRatioChanged: (AspectRatio) -> Unit,
    onBokehToggle: (Boolean) -> Unit,
    onBokehIntensityChanged: (Float) -> Unit,
    on180RuleApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                color = CineCamColors.Surface.copy(alpha = 0.9f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ISO
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("ISO", color = CineCamColors.OnSurface, style = MaterialTheme.typography.labelMedium)
            IsoSelector(
                selectedIso = settings.iso,
                onIsoSelected = onIsoChanged
            )
        }
        
        // Frame Rate
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("FPS", color = CineCamColors.OnSurface, style = MaterialTheme.typography.labelMedium)
            FrameRateSelector(
                selectedFps = settings.frameRate,
                onFpsSelected = onFpsChanged
            )
            
            TextButton(onClick = on180RuleApply) {
                Text("180° Rule", color = CineCamColors.Primary)
            }
        }
        
        // Aspect Ratio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ratio", color = CineCamColors.OnSurface, style = MaterialTheme.typography.labelMedium)
            AspectRatioSelector(
                selectedRatio = settings.aspectRatio,
                onRatioSelected = onAspectRatioChanged
            )
        }
        
        // Bokeh Control
        BokehControl(
            isEnabled = settings.bokehEnabled,
            intensity = settings.bokehIntensity,
            onToggle = onBokehToggle,
            onIntensityChange = onBokehIntensityChanged
        )
    }
}
