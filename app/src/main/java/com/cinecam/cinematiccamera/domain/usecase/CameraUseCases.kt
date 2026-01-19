package com.cinecam.cinematiccamera.domain.usecase

import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Camera Use Cases - Business logic for camera operations
 */

class GetCameraStateUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    operator fun invoke(): StateFlow<CameraState> = repository.cameraState
}

class GetRecordingStateUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    operator fun invoke(): StateFlow<RecordingState> = repository.recordingState
}

class GetCameraSettingsUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    operator fun invoke(): StateFlow<CameraSettings> = repository.settings
}

class GetRecordingDurationUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    operator fun invoke(): Flow<Long> = repository.recordingDuration
}

class StartRecordingUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(): Result<String> = repository.startRecording()
}

class StopRecordingUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(): Result<VideoMetadata> = repository.stopRecording()
}

class UpdateCameraSettingsUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(settings: CameraSettings) {
        repository.updateSettings(settings)
    }
}

class SetFocusDistanceUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(distance: Float) {
        repository.setFocusDistance(distance.coerceIn(0f, 1f))
    }
}

class SetIsoUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(iso: Int) {
        val validIso = IsoValues.available.minByOrNull { kotlin.math.abs(it - iso) } ?: 100
        repository.setIso(validIso)
    }
}

class SetShutterSpeedUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(shutterSpeed: ShutterSpeed) {
        repository.setShutterSpeed(shutterSpeed)
    }
}

class SetExposureCompensationUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(ev: Float) {
        repository.setExposureCompensation(ev.coerceIn(-2f, 2f))
    }
}

class SetBokehEnabledUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setBokehEnabled(enabled)
    }
}

class SetBokehIntensityUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(intensity: Float) {
        repository.setBokehIntensity(intensity.coerceIn(0f, 1f))
    }
}

class SetLutUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke(lutId: String?) {
        repository.setLut(lutId)
    }
}

class SwitchCameraUseCase @Inject constructor(
    private val repository: CameraRepository
) {
    suspend operator fun invoke() {
        repository.switchCamera()
    }
}

/**
 * Calculate 180-degree shutter rule
 */
class Calculate180ShutterRuleUseCase @Inject constructor() {
    operator fun invoke(frameRate: FrameRate): ShutterSpeed {
        return when (frameRate) {
            FrameRate.FPS_24 -> ShutterSpeed.S_1_50
            FrameRate.FPS_30 -> ShutterSpeed.S_1_60
            FrameRate.FPS_60 -> ShutterSpeed.S_1_120
        }
    }
}
