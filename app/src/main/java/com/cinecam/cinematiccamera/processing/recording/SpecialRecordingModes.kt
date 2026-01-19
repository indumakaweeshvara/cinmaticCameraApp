package com.cinecam.cinematiccamera.processing.recording

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.video.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recording Mode - Different video recording modes
 */
enum class RecordingMode {
    NORMAL,          // Standard recording
    SLOW_MOTION,     // 120/240 fps recording
    TIME_LAPSE,      // Interval-based capture
    HYPERLAPSE       // Time-lapse with motion
}

/**
 * Slow Motion Recorder
 * 
 * Handles high frame rate recording for slow motion playback.
 */
@Singleton
class SlowMotionRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SlowMotionRecorder"
    }
    
    enum class SlowMotionSpeed(val fps: Int, val playbackFactor: Float, val displayName: String) {
        SLOW_4X(120, 4f, "4x Slow (120fps)"),
        SLOW_8X(240, 8f, "8x Slow (240fps)"),
        SLOW_16X(480, 16f, "16x Slow (480fps)") // If supported
    }
    
    private val _currentSpeed = MutableStateFlow(SlowMotionSpeed.SLOW_4X)
    val currentSpeed: StateFlow<SlowMotionSpeed> = _currentSpeed
    
    private val _isSupported = MutableStateFlow(false)
    val isSupported: StateFlow<Boolean> = _isSupported
    
    /**
     * Check if device supports high frame rate recording
     */
    fun checkSupport(cameraInfo: androidx.camera.core.CameraInfo): List<SlowMotionSpeed> {
        val supportedSpeeds = mutableListOf<SlowMotionSpeed>()
        
        // Check available frame rates from CameraInfo
        // This is a simplified check - actual implementation would query Camera2 characteristics
        
        // Most modern phones support at least 120fps
        supportedSpeeds.add(SlowMotionSpeed.SLOW_4X)
        
        // Flagship phones typically support 240fps
        if (Build.MODEL.contains("Pixel", ignoreCase = true) ||
            Build.MODEL.contains("Galaxy S", ignoreCase = true) ||
            Build.MODEL.contains("iPhone", ignoreCase = true)) {
            supportedSpeeds.add(SlowMotionSpeed.SLOW_8X)
        }
        
        _isSupported.value = supportedSpeeds.isNotEmpty()
        return supportedSpeeds
    }
    
    /**
     * Set slow motion speed
     */
    fun setSpeed(speed: SlowMotionSpeed) {
        _currentSpeed.value = speed
    }
    
    /**
     * Get quality selector for slow motion
     */
    fun getQualitySelector(): QualitySelector {
        return QualitySelector.from(
            Quality.FHD, // Slow motion typically limited to 1080p
            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
        )
    }
    
    /**
     * Get output options for slow motion recording
     */
    fun getOutputOptions(): MediaStoreOutputOptions {
        val name = SimpleDateFormat("CineCam_SlowMo_yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CineCam/SlowMotion")
            }
        }
        
        return MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }
}

/**
 * Time-Lapse Recorder
 * 
 * Handles interval-based capture for time-lapse videos.
 */
@Singleton
class TimeLapseRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TimeLapseRecorder"
    }
    
    enum class TimeLapseInterval(val seconds: Float, val displayName: String) {
        INTERVAL_0_5(0.5f, "0.5 sec"),
        INTERVAL_1(1f, "1 sec"),
        INTERVAL_2(2f, "2 sec"),
        INTERVAL_5(5f, "5 sec"),
        INTERVAL_10(10f, "10 sec"),
        INTERVAL_30(30f, "30 sec"),
        INTERVAL_60(60f, "1 min")
    }
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording
    
    private val _currentInterval = MutableStateFlow(TimeLapseInterval.INTERVAL_1)
    val currentInterval: StateFlow<TimeLapseInterval> = _currentInterval
    
    private val _capturedFrames = MutableStateFlow(0)
    val capturedFrames: StateFlow<Int> = _capturedFrames
    
    private val _estimatedDuration = MutableStateFlow(0L)
    val estimatedDuration: StateFlow<Long> = _estimatedDuration
    
    // Output frame rate (playback speed)
    private var outputFps = 30
    
    /**
     * Set capture interval
     */
    fun setInterval(interval: TimeLapseInterval) {
        _currentInterval.value = interval
        calculateEstimatedDuration()
    }
    
    /**
     * Set output frame rate for playback
     */
    fun setOutputFps(fps: Int) {
        outputFps = fps.coerceIn(24, 60)
        calculateEstimatedDuration()
    }
    
    /**
     * Calculate estimated output video duration
     */
    private fun calculateEstimatedDuration() {
        val frames = _capturedFrames.value
        if (frames > 0) {
            _estimatedDuration.value = (frames * 1000L / outputFps)
        }
    }
    
    /**
     * Start time-lapse recording
     */
    fun startRecording() {
        _isRecording.value = true
        _capturedFrames.value = 0
        Log.d(TAG, "Time-lapse started with interval: ${_currentInterval.value.displayName}")
    }
    
    /**
     * Capture frame (called at interval)
     */
    fun captureFrame() {
        if (_isRecording.value) {
            _capturedFrames.value++
            calculateEstimatedDuration()
        }
    }
    
    /**
     * Stop time-lapse recording
     */
    fun stopRecording(): Int {
        _isRecording.value = false
        val totalFrames = _capturedFrames.value
        Log.d(TAG, "Time-lapse stopped. Total frames: $totalFrames")
        return totalFrames
    }
    
    /**
     * Get output options for time-lapse
     */
    fun getOutputOptions(): MediaStoreOutputOptions {
        val name = SimpleDateFormat("CineCam_TimeLapse_yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CineCam/TimeLapse")
            }
        }
        
        return MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }
    
    /**
     * Calculate speed multiplier for preview
     * Example: 1 second interval at 30fps = 30x speed
     */
    fun getSpeedMultiplier(): Float {
        return _currentInterval.value.seconds * outputFps
    }
}

/**
 * Hyperlapse Recorder
 * 
 * Combines time-lapse with motion stabilization for smooth moving shots.
 */
@Singleton
class HyperlapseRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HyperlapseRecorder"
    }
    
    enum class HyperlapseSpeed(val multiplier: Int, val displayName: String) {
        SPEED_4X(4, "4x"),
        SPEED_8X(8, "8x"),
        SPEED_12X(12, "12x"),
        SPEED_16X(16, "16x"),
        SPEED_32X(32, "32x")
    }
    
    private val _currentSpeed = MutableStateFlow(HyperlapseSpeed.SPEED_8X)
    val currentSpeed: StateFlow<HyperlapseSpeed> = _currentSpeed
    
    private val _stabilizationEnabled = MutableStateFlow(true)
    val stabilizationEnabled: StateFlow<Boolean> = _stabilizationEnabled
    
    /**
     * Set hyperlapse speed
     */
    fun setSpeed(speed: HyperlapseSpeed) {
        _currentSpeed.value = speed
    }
    
    /**
     * Toggle stabilization
     */
    fun setStabilization(enabled: Boolean) {
        _stabilizationEnabled.value = enabled
    }
    
    /**
     * Calculate how long to record for desired output duration
     * @param outputDurationSec Desired output video length in seconds
     * @return Required recording time in seconds
     */
    fun calculateRecordingTime(outputDurationSec: Int): Int {
        return outputDurationSec * _currentSpeed.value.multiplier
    }
    
    /**
     * Get output options for hyperlapse
     */
    fun getOutputOptions(): MediaStoreOutputOptions {
        val name = SimpleDateFormat("CineCam_Hyperlapse_yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CineCam/Hyperlapse")
            }
        }
        
        return MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }
}
