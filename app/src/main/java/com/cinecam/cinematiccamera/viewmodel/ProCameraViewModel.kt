package com.cinecam.cinematiccamera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.processing.effects.*
import com.cinecam.cinematiccamera.processing.recording.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pro Camera ViewModel - Extended state management for advanced features
 */
@HiltViewModel
class ProCameraViewModel @Inject constructor(
    private val focusPeakingProcessor: FocusPeakingProcessor,
    private val zebraPatternProcessor: ZebraPatternProcessor,
    private val filmGrainProcessor: FilmGrainProcessor,
    private val anamorphicProcessor: AnamorphicProcessor,
    private val histogramProcessor: HistogramProcessor,
    private val audioLevelProcessor: AudioLevelProcessor,
    private val gridOverlayProcessor: GridOverlayProcessor,
    private val slowMotionRecorder: SlowMotionRecorder,
    private val timeLapseRecorder: TimeLapseRecorder,
    private val hyperlapseRecorder: HyperlapseRecorder
) : ViewModel() {
    
    // Pro Settings State
    private val _proSettings = MutableStateFlow(ProCameraSettings())
    val proSettings: StateFlow<ProCameraSettings> = _proSettings.asStateFlow()
    
    // Recording Mode
    private val _recordingMode = MutableStateFlow(RecordingMode.NORMAL)
    val recordingMode: StateFlow<RecordingMode> = _recordingMode.asStateFlow()
    
    // Histogram Data
    private val _histogramData = MutableStateFlow<HistogramProcessor.HistogramData?>(null)
    val histogramData: StateFlow<HistogramProcessor.HistogramData?> = _histogramData.asStateFlow()
    
    // Audio Level
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()
    
    private val _audioPeakLevel = MutableStateFlow(0f)
    val audioPeakLevel: StateFlow<Float> = _audioPeakLevel.asStateFlow()
    
    // Slow Motion Speeds
    private val _availableSlowMotionSpeeds = MutableStateFlow<List<SlowMotionRecorder.SlowMotionSpeed>>(emptyList())
    val availableSlowMotionSpeeds: StateFlow<List<SlowMotionRecorder.SlowMotionSpeed>> = _availableSlowMotionSpeeds.asStateFlow()
    
    // Time-Lapse
    val timeLapseFrameCount: StateFlow<Int> = timeLapseRecorder.capturedFrames
    val timeLapseEstimatedDuration: StateFlow<Long> = timeLapseRecorder.estimatedDuration
    
    // UI State
    private val _proToolsPanelVisible = MutableStateFlow(false)
    val proToolsPanelVisible: StateFlow<Boolean> = _proToolsPanelVisible.asStateFlow()
    
    // ============ Recording Mode ============
    
    fun setRecordingMode(mode: RecordingMode) {
        _recordingMode.value = mode
        _proSettings.update { it.copy(recordingMode = mode) }
    }
    
    // ============ Focus Peaking ============
    
    fun setFocusPeakingEnabled(enabled: Boolean) {
        focusPeakingProcessor.setEnabled(enabled)
        _proSettings.update { it.copy(focusPeakingEnabled = enabled) }
    }
    
    fun setFocusPeakingColor(color: FocusPeakingProcessor.PeakingColor) {
        focusPeakingProcessor.setColor(color)
        _proSettings.update { it.copy(focusPeakingColor = color) }
    }
    
    fun setFocusPeakingSensitivity(sensitivity: Float) {
        focusPeakingProcessor.setSensitivity(sensitivity)
        _proSettings.update { it.copy(focusPeakingSensitivity = sensitivity) }
    }
    
    // ============ Zebra Pattern ============
    
    fun setZebraEnabled(enabled: Boolean) {
        zebraPatternProcessor.setEnabled(enabled)
        _proSettings.update { it.copy(zebraEnabled = enabled) }
    }
    
    fun setZebraThreshold(threshold: Int) {
        zebraPatternProcessor.setThreshold(threshold)
        _proSettings.update { it.copy(zebraThreshold = threshold) }
    }
    
    // ============ Grid Overlay ============
    
    fun setGridType(type: GridOverlayProcessor.GridType) {
        gridOverlayProcessor.setGridType(type)
        _proSettings.update { it.copy(gridType = type) }
    }
    
    // ============ Film Grain ============
    
    fun setFilmGrainEnabled(enabled: Boolean) {
        filmGrainProcessor.setEnabled(enabled)
        _proSettings.update { it.copy(filmGrainEnabled = enabled) }
    }
    
    fun setFilmGrainIntensity(intensity: Float) {
        filmGrainProcessor.setIntensity(intensity)
        _proSettings.update { it.copy(filmGrainIntensity = intensity) }
    }
    
    // ============ Anamorphic ============
    
    fun setAnamorphicEnabled(enabled: Boolean) {
        anamorphicProcessor.setEnabled(enabled)
        _proSettings.update { it.copy(anamorphicEnabled = enabled) }
    }
    
    fun setAnamorphicFlareIntensity(intensity: Float) {
        anamorphicProcessor.setFlareIntensity(intensity)
        _proSettings.update { it.copy(anamorphicFlareIntensity = intensity) }
    }
    
    // ============ Histogram ============
    
    fun setHistogramEnabled(enabled: Boolean) {
        _proSettings.update { it.copy(histogramEnabled = enabled) }
    }
    
    fun updateHistogram(bitmap: android.graphics.Bitmap) {
        if (_proSettings.value.histogramEnabled) {
            viewModelScope.launch {
                _histogramData.value = histogramProcessor.calculateHistogram(bitmap)
            }
        }
    }
    
    // ============ Audio Meter ============
    
    fun setAudioMeterEnabled(enabled: Boolean) {
        _proSettings.update { it.copy(audioMeterEnabled = enabled) }
    }
    
    fun updateAudioLevel(level: Float) {
        audioLevelProcessor.updateLevel(level)
        _audioLevel.value = audioLevelProcessor.getCurrentLevel()
        _audioPeakLevel.value = audioLevelProcessor.getPeakLevel()
    }
    
    // ============ Slow Motion ============
    
    fun setSlowMotionSpeed(speed: SlowMotionRecorder.SlowMotionSpeed) {
        slowMotionRecorder.setSpeed(speed)
        _proSettings.update { it.copy(slowMotionFps = speed.fps) }
    }
    
    fun checkSlowMotionSupport(cameraInfo: androidx.camera.core.CameraInfo) {
        _availableSlowMotionSpeeds.value = slowMotionRecorder.checkSupport(cameraInfo)
    }
    
    // ============ Time-Lapse ============
    
    fun setTimeLapseInterval(interval: TimeLapseRecorder.TimeLapseInterval) {
        timeLapseRecorder.setInterval(interval)
        _proSettings.update { it.copy(timeLapseIntervalSeconds = interval.seconds) }
    }
    
    fun startTimeLapse() {
        timeLapseRecorder.startRecording()
    }
    
    fun stopTimeLapse(): Int {
        return timeLapseRecorder.stopRecording()
    }
    
    fun captureTimeLapseFrame() {
        timeLapseRecorder.captureFrame()
    }
    
    // ============ Hyperlapse ============
    
    fun setHyperlapseSpeed(speed: HyperlapseRecorder.HyperlapseSpeed) {
        hyperlapseRecorder.setSpeed(speed)
        _proSettings.update { it.copy(hyperlapseSpeed = speed.multiplier) }
    }
    
    // ============ UI ============
    
    fun toggleProToolsPanel() {
        _proToolsPanelVisible.value = !_proToolsPanelVisible.value
    }
    
    fun showProToolsPanel() {
        _proToolsPanelVisible.value = true
    }
    
    fun hideProToolsPanel() {
        _proToolsPanelVisible.value = false
    }
    
    // ============ Presets ============
    
    fun applyPreset(bundle: ProFeaturesBundle) {
        if (bundle.focusAssist) {
            setFocusPeakingEnabled(true)
            setZebraEnabled(true)
        } else {
            setFocusPeakingEnabled(false)
            setZebraEnabled(false)
        }
        
        if (bundle.cinematicEffects) {
            setFilmGrainEnabled(true)
            setFilmGrainIntensity(0.2f)
        } else {
            setFilmGrainEnabled(false)
        }
        
        if (bundle.compositionGuides) {
            setGridType(GridOverlayProcessor.GridType.RULE_OF_THIRDS)
        } else {
            setGridType(GridOverlayProcessor.GridType.NONE)
        }
        
        setHistogramEnabled(bundle.monitoringTools)
        setAudioMeterEnabled(bundle.monitoringTools)
    }
    
    /**
     * Reset all pro settings to defaults
     */
    fun resetToDefaults() {
        _proSettings.value = ProCameraSettings()
        _recordingMode.value = RecordingMode.NORMAL
        
        // Reset all processors
        focusPeakingProcessor.setEnabled(false)
        zebraPatternProcessor.setEnabled(false)
        filmGrainProcessor.setEnabled(false)
        anamorphicProcessor.setEnabled(false)
        gridOverlayProcessor.setGridType(GridOverlayProcessor.GridType.NONE)
    }
}
