package com.cinecam.cinematiccamera.processing.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.processing.segmentation.BlurProcessor
import com.cinecam.cinematiccamera.processing.segmentation.SelfieSegmentationProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera Manager - Core camera functionality using CameraX
 * 
 * Manages camera lifecycle, preview, recording, and integrates
 * with AI segmentation for real-time bokeh effects.
 */
@Singleton
@OptIn(ExperimentalCamera2Interop::class)
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val segmentationProcessor: SelfieSegmentationProcessor,
    private val blurProcessor: BlurProcessor
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "CineCam_yyyyMMdd_HHmmss"
    }
    
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Initializing)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _settings = MutableStateFlow(CameraSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    private val _processedFrame = MutableSharedFlow<Bitmap>(replay = 1)
    val processedFrame: SharedFlow<Bitmap> = _processedFrame.asSharedFlow()
    
    // Camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    
    // Executors
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Current camera selector (back camera by default)
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    
    // Recording timer
    private var recordingStartTime = 0L
    private var durationJob: Job? = null
    
    /**
     * Initialize camera with preview view
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = withContext(Dispatchers.Main) {
        try {
            _cameraState.value = CameraState.Initializing
            
            // Initialize segmentation processor
            segmentationProcessor.initialize()
            
            // Get camera provider
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()
            
            // Build use cases
            bindCameraUseCases(lifecycleOwner, previewView)
            
            _cameraState.value = CameraState.Ready
            Log.d(TAG, "Camera initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed: ${e.message}")
            _cameraState.value = CameraState.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: throw IllegalStateException("Camera not initialized")
        
        // Unbind existing use cases
        provider.unbindAll()
        
        val currentSettings = _settings.value
        
        // Camera selector
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        // Preview use case
        preview = Preview.Builder()
            .setTargetAspectRatio(getAspectRatioInt(currentSettings.aspectRatio))
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image analysis for AI processing
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720)) // Lower res for faster processing
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processFrame(imageProxy)
                }
            }
        
        // Video capture use case
        val qualitySelector = QualitySelector.from(
            getQualityFromResolution(currentSettings.resolution),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
        
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        
        videoCapture = VideoCapture.withOutput(recorder)
        
        // Bind use cases to camera
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer,
            videoCapture
        )
        
        // Apply initial settings
        applyManualControls(currentSettings)
    }
    
    /**
     * Process each camera frame for bokeh effect
     */
    private fun processFrame(imageProxy: ImageProxy) {
        val settings = _settings.value
        
        if (!settings.bokehEnabled) {
            imageProxy.close()
            return
        }
        
        processingScope.launch {
            try {
                val bitmap = imageProxyToBitmap(imageProxy)
                bitmap?.let { frame ->
                    val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000
                    
                    segmentationProcessor.processFrameAsync(frame, timestampMs) { segOutput ->
                        processingScope.launch {
                            val processed = blurProcessor.applyBokehEffect(
                                frame,
                                segOutput,
                                settings.bokehIntensity
                            )
                            _processedFrame.emit(processed)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Frame processing error: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }
    
    /**
     * Convert ImageProxy to Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        val imageBytes = out.toByteArray()
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        // Rotate bitmap if needed
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Start video recording
     */
    @SuppressWarnings("MissingPermission")
    suspend fun startRecording(): Result<String> = withContext(Dispatchers.Main) {
        try {
            val videoCapture = videoCapture ?: return@withContext Result.failure(
                IllegalStateException("Video capture not initialized")
            )
            
            _recordingState.value = RecordingState.Starting
            
            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CineCam")
                }
            }
            
            val mediaStoreOutputOptions = MediaStoreOutputOptions
                .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()
            
            recording = videoCapture.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    handleRecordingEvent(recordEvent)
                }
            
            recordingStartTime = System.currentTimeMillis()
            startDurationTimer()
            
            Result.success(name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            _recordingState.value = RecordingState.Error(e.message ?: "Recording failed")
            Result.failure(e)
        }
    }
    
    private fun handleRecordingEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                _recordingState.value = RecordingState.Recording()
                Log.d(TAG, "Recording started")
            }
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    Log.e(TAG, "Recording error: ${event.error}")
                    _recordingState.value = RecordingState.Error("Recording failed: ${event.error}")
                } else {
                    val uri = event.outputResults.outputUri
                    _recordingState.value = RecordingState.Completed(uri.toString())
                    Log.d(TAG, "Recording saved: $uri")
                }
            }
            is VideoRecordEvent.Status -> {
                val stats = event.recordingStats
                _recordingState.value = RecordingState.Recording(
                    durationMs = stats.recordedDurationNanos / 1_000_000
                )
            }
        }
    }
    
    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = processingScope.launch {
            while (isActive) {
                val duration = System.currentTimeMillis() - recordingStartTime
                _recordingDuration.value = duration
                delay(100)
            }
        }
    }
    
    /**
     * Stop recording
     */
    suspend fun stopRecording(): Result<VideoMetadata> = withContext(Dispatchers.Main) {
        try {
            _recordingState.value = RecordingState.Stopping
            durationJob?.cancel()
            
            recording?.stop()
            recording = null
            
            val duration = _recordingDuration.value
            _recordingDuration.value = 0
            _recordingState.value = RecordingState.Idle
            
            Result.success(
                VideoMetadata(
                    filePath = "",
                    duration = duration,
                    width = _settings.value.resolution.width,
                    height = _settings.value.resolution.height,
                    bitrate = _settings.value.bitrate.bitsPerSecond,
                    frameRate = _settings.value.frameRate.fps,
                    codec = "H.264"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update camera settings
     */
    fun updateSettings(newSettings: CameraSettings) {
        _settings.value = newSettings
        applyManualControls(newSettings)
    }
    
    /**
     * Apply manual controls to camera
     */
    private fun applyManualControls(settings: CameraSettings) {
        camera?.let { cam ->
            val cameraControl = cam.cameraControl
            
            // Exposure compensation
            val exposureIndex = (settings.exposureCompensation * 6).toInt() // Assuming range of ±2 EV
            cameraControl.setExposureCompensationIndex(exposureIndex)
            
            // Focus distance (if supported)
            if (settings.focusDistance > 0f) {
                // Camera2Interop would be used here for manual focus
                // This requires more complex Camera2 extension code
            }
        }
    }
    
    /**
     * Set focus distance
     */
    fun setFocusDistance(distance: Float) {
        _settings.update { it.copy(focusDistance = distance) }
    }
    
    /**
     * Set ISO
     */
    fun setIso(iso: Int) {
        _settings.update { it.copy(iso = iso) }
    }
    
    /**
     * Set shutter speed
     */
    fun setShutterSpeed(shutterSpeed: ShutterSpeed) {
        _settings.update { it.copy(shutterSpeed = shutterSpeed) }
    }
    
    /**
     * Set exposure compensation
     */
    fun setExposureCompensation(ev: Float) {
        _settings.update { it.copy(exposureCompensation = ev) }
        applyManualControls(_settings.value)
    }
    
    /**
     * Toggle bokeh
     */
    fun setBokehEnabled(enabled: Boolean) {
        _settings.update { it.copy(bokehEnabled = enabled) }
    }
    
    /**
     * Set bokeh intensity
     */
    fun setBokehIntensity(intensity: Float) {
        _settings.update { it.copy(bokehIntensity = intensity) }
    }
    
    /**
     * Set LUT
     */
    fun setLut(lutId: String?) {
        _settings.update { it.copy(selectedLut = lutId) }
    }
    
    /**
     * Switch camera (front/back)
     */
    suspend fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases(lifecycleOwner, previewView)
    }
    
    /**
     * Release resources
     */
    fun release() {
        recording?.stop()
        durationJob?.cancel()
        processingScope.cancel()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        segmentationProcessor.release()
    }
    
    // Helper functions
    private fun getAspectRatioInt(ratio: AspectRatio): Int {
        return when (ratio) {
            AspectRatio.RATIO_16_9 -> AspectRatio.RATIO_16_9.ordinal
            AspectRatio.RATIO_1_1 -> AspectRatio.RATIO_1_1.ordinal
            else -> AspectRatio.RATIO_16_9.ordinal
        }
    }
    
    private fun getQualityFromResolution(resolution: Resolution): Quality {
        return when (resolution) {
            Resolution.HD_720P -> Quality.HD
            Resolution.FHD_1080P -> Quality.FHD
            Resolution.UHD_4K -> Quality.UHD
        }
    }
}
