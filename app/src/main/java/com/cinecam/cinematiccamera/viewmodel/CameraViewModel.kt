package com.cinecam.cinematiccamera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Camera ViewModel - State management for camera UI
 * 
 * Manages UI state, user interactions, and coordinates between
 * the UI layer and domain use cases.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val getCameraState: GetCameraStateUseCase,
    private val getRecordingState: GetRecordingStateUseCase,
    private val getCameraSettings: GetCameraSettingsUseCase,
    private val getRecordingDuration: GetRecordingDurationUseCase,
    private val startRecording: StartRecordingUseCase,
    private val stopRecording: StopRecordingUseCase,
    private val setFocusDistance: SetFocusDistanceUseCase,
    private val setIso: SetIsoUseCase,
    private val setShutterSpeed: SetShutterSpeedUseCase,
    private val setExposureCompensation: SetExposureCompensationUseCase,
    private val setBokehEnabled: SetBokehEnabledUseCase,
    private val setBokehIntensity: SetBokehIntensityUseCase,
    private val setLut: SetLutUseCase,
    private val switchCamera: SwitchCameraUseCase,
    private val calculate180Rule: Calculate180ShutterRuleUseCase
) : ViewModel() {
    
    // Camera state flows
    val cameraState: StateFlow<CameraState> = getCameraState()
    val recordingState: StateFlow<RecordingState> = getRecordingState()
    val settings: StateFlow<CameraSettings> = getCameraSettings()
    val recordingDuration: Flow<Long> = getRecordingDuration()
    
    // UI state
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    
    // Last saved video
    private val _lastSavedVideo = MutableSharedFlow<String>()
    val lastSavedVideo: SharedFlow<String> = _lastSavedVideo.asSharedFlow()
    
    /**
     * Toggle recording start/stop
     */
    fun toggleRecording() {
        viewModelScope.launch {
            val currentState = recordingState.value
            when (currentState) {
                is RecordingState.Idle, is RecordingState.Completed, is RecordingState.Error -> {
                    startRecording().onSuccess { fileName ->
                        _uiState.update { it.copy(message = "Recording: $fileName") }
                    }.onFailure { error ->
                        _uiState.update { it.copy(message = "Error: ${error.message}") }
                    }
                }
                is RecordingState.Recording -> {
                    stopRecording().onSuccess { metadata ->
                        _uiState.update { it.copy(message = "Video saved") }
                        _lastSavedVideo.emit(metadata.filePath)
                    }.onFailure { error ->
                        _uiState.update { it.copy(message = "Error: ${error.message}") }
                    }
                }
                else -> { /* Ignore during Starting/Stopping states */ }
            }
        }
    }
    
    /**
     * Update focus distance
     */
    fun onFocusChanged(distance: Float) {
        viewModelScope.launch {
            setFocusDistance(distance)
        }
    }
    
    /**
     * Update ISO
     */
    fun onIsoChanged(iso: Int) {
        viewModelScope.launch {
            setIso(iso)
        }
    }
    
    /**
     * Update shutter speed
     */
    fun onShutterSpeedChanged(shutterSpeed: ShutterSpeed) {
        viewModelScope.launch {
            setShutterSpeed(shutterSpeed)
        }
    }
    
    /**
     * Update exposure compensation
     */
    fun onExposureChanged(ev: Float) {
        viewModelScope.launch {
            setExposureCompensation(ev)
        }
    }
    
    /**
     * Toggle bokeh effect
     */
    fun onBokehToggle(enabled: Boolean) {
        viewModelScope.launch {
            setBokehEnabled(enabled)
        }
    }
    
    /**
     * Update bokeh intensity
     */
    fun onBokehIntensityChanged(intensity: Float) {
        viewModelScope.launch {
            setBokehIntensity(intensity)
        }
    }
    
    /**
     * Select LUT
     */
    fun onLutSelected(lutId: String?) {
        viewModelScope.launch {
            setLut(lutId)
        }
    }
    
    /**
     * Switch front/back camera
     */
    fun onSwitchCamera() {
        viewModelScope.launch {
            switchCamera()
        }
    }
    
    /**
     * Apply 180-degree shutter rule automatically
     */
    fun apply180ShutterRule() {
        viewModelScope.launch {
            val currentFrameRate = settings.value.frameRate
            val recommendedShutter = calculate180Rule(currentFrameRate)
            setShutterSpeed(recommendedShutter)
            _uiState.update { 
                it.copy(message = "180° Rule: ${recommendedShutter.displayName}")
            }
        }
    }
    
    /**
     * Update frame rate and optionally apply 180° rule
     */
    fun onFrameRateChanged(frameRate: FrameRate, autoApply180Rule: Boolean = true) {
        viewModelScope.launch {
            // Frame rate would be updated through settings
            if (autoApply180Rule) {
                val recommendedShutter = calculate180Rule(frameRate)
                setShutterSpeed(recommendedShutter)
            }
        }
    }
    
    /**
     * Show/hide controls panel
     */
    fun toggleControlsPanel() {
        _uiState.update { it.copy(showControlsPanel = !it.showControlsPanel) }
    }
    
    /**
     * Show/hide settings
     */
    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }
    
    /**
     * Show/hide LUT selector
     */
    fun toggleLutSelector() {
        _uiState.update { it.copy(showLutSelector = !it.showLutSelector) }
    }
    
    /**
     * Clear message
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/**
 * UI State for Camera Screen
 */
data class CameraUiState(
    val showControlsPanel: Boolean = true,
    val showSettings: Boolean = false,
    val showLutSelector: Boolean = false,
    val message: String? = null
)
