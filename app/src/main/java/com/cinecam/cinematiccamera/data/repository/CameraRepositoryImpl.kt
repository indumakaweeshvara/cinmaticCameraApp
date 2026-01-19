package com.cinecam.cinematiccamera.data.repository

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.domain.repository.CameraRepository
import com.cinecam.cinematiccamera.processing.camera.CameraManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera Repository Implementation
 * 
 * Bridges the domain layer with the camera processing layer.
 */
@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraManager: CameraManager
) : CameraRepository {
    
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    
    override val cameraState: StateFlow<CameraState>
        get() = cameraManager.cameraState
    
    override val recordingState: StateFlow<RecordingState>
        get() = cameraManager.recordingState
    
    override val settings: StateFlow<CameraSettings>
        get() = cameraManager.settings
    
    override val recordingDuration: Flow<Long>
        get() = cameraManager.recordingDuration
    
    override suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        cameraManager.initializeCamera(lifecycleOwner, previewView)
    }
    
    override suspend fun startRecording(): Result<String> {
        return cameraManager.startRecording()
    }
    
    override suspend fun stopRecording(): Result<VideoMetadata> {
        return cameraManager.stopRecording()
    }
    
    override suspend fun updateSettings(settings: CameraSettings) {
        cameraManager.updateSettings(settings)
    }
    
    override suspend fun setFocusDistance(distance: Float) {
        cameraManager.setFocusDistance(distance)
    }
    
    override suspend fun setIso(iso: Int) {
        cameraManager.setIso(iso)
    }
    
    override suspend fun setShutterSpeed(shutterSpeed: ShutterSpeed) {
        cameraManager.setShutterSpeed(shutterSpeed)
    }
    
    override suspend fun setExposureCompensation(ev: Float) {
        cameraManager.setExposureCompensation(ev)
    }
    
    override suspend fun setBokehEnabled(enabled: Boolean) {
        cameraManager.setBokehEnabled(enabled)
    }
    
    override suspend fun setBokehIntensity(intensity: Float) {
        cameraManager.setBokehIntensity(intensity)
    }
    
    override suspend fun setLut(lutId: String?) {
        cameraManager.setLut(lutId)
    }
    
    override suspend fun switchCamera() {
        val owner = currentLifecycleOwner
        val view = currentPreviewView
        if (owner != null && view != null) {
            cameraManager.switchCamera(owner, view)
        }
    }
    
    override suspend fun release() {
        cameraManager.release()
        currentLifecycleOwner = null
        currentPreviewView = null
    }
}
