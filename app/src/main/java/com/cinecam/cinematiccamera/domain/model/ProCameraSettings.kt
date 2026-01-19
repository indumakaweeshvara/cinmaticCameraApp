package com.cinecam.cinematiccamera.domain.model

import com.cinecam.cinematiccamera.processing.effects.FocusPeakingProcessor
import com.cinecam.cinematiccamera.processing.effects.GridOverlayProcessor
import com.cinecam.cinematiccamera.processing.recording.RecordingMode

/**
 * Extended Camera Settings with all Pro features
 */
data class ProCameraSettings(
    // Base settings
    val baseSettings: CameraSettings = CameraSettings(),
    
    // Recording Mode
    val recordingMode: RecordingMode = RecordingMode.NORMAL,
    
    // Slow Motion settings
    val slowMotionFps: Int = 120,
    
    // Time-Lapse settings
    val timeLapseIntervalSeconds: Float = 1f,
    val timeLapseOutputFps: Int = 30,
    
    // Hyperlapse settings
    val hyperlapseSpeed: Int = 8,
    
    // Focus Peaking
    val focusPeakingEnabled: Boolean = false,
    val focusPeakingColor: FocusPeakingProcessor.PeakingColor = FocusPeakingProcessor.PeakingColor.RED,
    val focusPeakingSensitivity: Float = 0.5f,
    
    // Zebra Pattern
    val zebraEnabled: Boolean = false,
    val zebraThreshold: Int = 95,
    
    // Grid Overlay
    val gridType: GridOverlayProcessor.GridType = GridOverlayProcessor.GridType.NONE,
    
    // Histogram
    val histogramEnabled: Boolean = false,
    val histogramShowRGB: Boolean = true,
    
    // Audio Meter
    val audioMeterEnabled: Boolean = true,
    
    // Film Grain
    val filmGrainEnabled: Boolean = false,
    val filmGrainIntensity: Float = 0.3f,
    val filmGrainSize: Int = 1,
    
    // Anamorphic
    val anamorphicEnabled: Boolean = false,
    val anamorphicFlareIntensity: Float = 0.5f,
    
    // Timecode
    val timecodeEnabled: Boolean = false,
    val timecodeDropFrame: Boolean = false,
    
    // Waveform
    val waveformEnabled: Boolean = false,
    
    // Safe Area guides
    val safeAreaEnabled: Boolean = false,
    val safeAreaType: SafeAreaType = SafeAreaType.TITLE_SAFE
)

/**
 * Safe Area Types for broadcast
 */
enum class SafeAreaType(val percentage: Float, val displayName: String) {
    ACTION_SAFE(0.93f, "Action Safe (93%)"),
    TITLE_SAFE(0.90f, "Title Safe (90%)"),
    SOCIAL_MEDIA(0.85f, "Social Media (85%)")
}

/**
 * Pro Features Bundle for quick enable/disable
 */
data class ProFeaturesBundle(
    val monitoringTools: Boolean = true,  // Histogram, Waveform, Audio
    val focusAssist: Boolean = true,       // Focus Peaking, Zebra
    val cinematicEffects: Boolean = true,  // Film Grain, Anamorphic
    val compositionGuides: Boolean = true  // Grids, Safe Areas
) {
    companion object {
        val ALL_ENABLED = ProFeaturesBundle(true, true, true, true)
        val MINIMAL = ProFeaturesBundle(false, false, false, false)
        val FOCUS_ONLY = ProFeaturesBundle(false, true, false, true)
    }
}
