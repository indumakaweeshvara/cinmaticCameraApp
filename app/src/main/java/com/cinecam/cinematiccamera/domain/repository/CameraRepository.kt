package com.cinecam.cinematiccamera.domain.repository

import com.cinecam.cinematiccamera.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Camera Repository Interface
 * 
 * Defines the contract for camera operations including
 * initialization, recording, and settings management.
 */
interface CameraRepository {
    
    /** Current camera state */
    val cameraState: StateFlow<CameraState>
    
    /** Current recording state */
    val recordingState: StateFlow<RecordingState>
    
    /** Current camera settings */
    val settings: StateFlow<CameraSettings>
    
    /** Recording duration in milliseconds */
    val recordingDuration: Flow<Long>
    
    /**
     * Initialize the camera with the specified settings
     */
    suspend fun initializeCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: androidx.camera.view.PreviewView
    )
    
    /**
     * Start video recording
     */
    suspend fun startRecording(): Result<String>
    
    /**
     * Stop current recording
     */
    suspend fun stopRecording(): Result<VideoMetadata>
    
    /**
     * Update camera settings
     */
    suspend fun updateSettings(settings: CameraSettings)
    
    /**
     * Set manual focus distance (0.0 = infinity, 1.0 = closest)
     */
    suspend fun setFocusDistance(distance: Float)
    
    /**
     * Set ISO sensitivity
     */
    suspend fun setIso(iso: Int)
    
    /**
     * Set shutter speed
     */
    suspend fun setShutterSpeed(shutterSpeed: ShutterSpeed)
    
    /**
     * Set exposure compensation
     */
    suspend fun setExposureCompensation(ev: Float)
    
    /**
     * Toggle bokeh effect
     */
    suspend fun setBokehEnabled(enabled: Boolean)
    
    /**
     * Set bokeh blur intensity
     */
    suspend fun setBokehIntensity(intensity: Float)
    
    /**
     * Apply LUT color grading
     */
    suspend fun setLut(lutId: String?)
    
    /**
     * Switch camera (front/back)
     */
    suspend fun switchCamera()
    
    /**
     * Release camera resources
     */
    suspend fun release()
}
