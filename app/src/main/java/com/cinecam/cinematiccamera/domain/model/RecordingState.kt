package com.cinecam.cinematiccamera.domain.model

/**
 * Recording State - Represents the current recording status
 */
sealed class RecordingState {
    data object Idle : RecordingState()
    data object Starting : RecordingState()
    data class Recording(
        val durationMs: Long = 0L,
        val filePath: String = ""
    ) : RecordingState()
    data object Stopping : RecordingState()
    data class Completed(val filePath: String) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

/**
 * Camera State - Overall camera status
 */
sealed class CameraState {
    data object Initializing : CameraState()
    data object Ready : CameraState()
    data class Error(val message: String) : CameraState()
    data object PermissionDenied : CameraState()
}

/**
 * Segmentation Result - Output from AI bokeh processing
 */
data class SegmentationResult(
    val maskBitmap: android.graphics.Bitmap?,
    val confidenceScore: Float,
    val processingTimeMs: Long
)

/**
 * Video Metadata - Information about recorded video
 */
data class VideoMetadata(
    val filePath: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val frameRate: Int,
    val codec: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * LUT (Look-Up Table) definition for color grading
 */
data class LutDefinition(
    val id: String,
    val name: String,
    val assetPath: String,
    val thumbnailPath: String? = null
)

/**
 * Built-in LUTs for cinematic color grading
 */
object BuiltInLuts {
    val available = listOf(
        LutDefinition("none", "No LUT", ""),
        LutDefinition("cinematic_orange_teal", "Cinematic Orange & Teal", "luts/cinematic_orange_teal.cube"),
        LutDefinition("film_kodak", "Kodak Film Emulation", "luts/film_kodak.cube"),
        LutDefinition("noir", "Film Noir", "luts/noir.cube"),
        LutDefinition("vibrant", "Vibrant Colors", "luts/vibrant.cube"),
        LutDefinition("log_to_rec709", "Log to Rec.709", "luts/log_to_rec709.cube")
    )
}
