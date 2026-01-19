package com.cinecam.cinematiccamera.domain.model

/**
 * Camera Settings - All configurable camera parameters
 * 
 * Supports manual control of ISO, shutter speed, focus,
 * aspect ratio, and video quality settings.
 */
data class CameraSettings(
    // Manual Controls
    val iso: Int = 100,
    val shutterSpeed: ShutterSpeed = ShutterSpeed.AUTO,
    val focusDistance: Float = 0f, // 0.0 = infinity, 1.0 = closest
    val exposureCompensation: Float = 0f, // -2.0 to +2.0 EV
    val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
    
    // Video Settings
    val frameRate: FrameRate = FrameRate.FPS_24,
    val resolution: Resolution = Resolution.FHD_1080P,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
    val bitrate: Bitrate = Bitrate.HIGH,
    
    // Effects
    val bokehEnabled: Boolean = true,
    val bokehIntensity: Float = 0.5f, // 0.0 to 1.0
    val selectedLut: String? = null,
    val stabilizationEnabled: Boolean = true,
    val logProfileEnabled: Boolean = false
) {
    /**
     * Calculate proper shutter speed for 180-degree rule
     */
    fun getRecommendedShutterSpeed(): ShutterSpeed {
        return when (frameRate) {
            FrameRate.FPS_24 -> ShutterSpeed.S_1_50
            FrameRate.FPS_30 -> ShutterSpeed.S_1_60
            FrameRate.FPS_60 -> ShutterSpeed.S_1_120
        }
    }
}

/**
 * Shutter Speed options
 */
enum class ShutterSpeed(val value: Long, val displayName: String) {
    AUTO(0L, "Auto"),
    S_1_30(33333333L, "1/30"),
    S_1_50(20000000L, "1/50"),
    S_1_60(16666666L, "1/60"),
    S_1_100(10000000L, "1/100"),
    S_1_120(8333333L, "1/120"),
    S_1_250(4000000L, "1/250"),
    S_1_500(2000000L, "1/500"),
    S_1_1000(1000000L, "1/1000"),
    S_1_2000(500000L, "1/2000");
    
    companion object {
        fun fromNanos(nanos: Long): ShutterSpeed {
            return entries.minByOrNull { kotlin.math.abs(it.value - nanos) } ?: AUTO
        }
    }
}

/**
 * Frame rate options for cinematic recording
 */
enum class FrameRate(val fps: Int, val displayName: String) {
    FPS_24(24, "24 fps"),
    FPS_30(30, "30 fps"),
    FPS_60(60, "60 fps")
}

/**
 * Video resolution options
 */
enum class Resolution(val width: Int, val height: Int, val displayName: String) {
    HD_720P(1280, 720, "720p HD"),
    FHD_1080P(1920, 1080, "1080p Full HD"),
    UHD_4K(3840, 2160, "4K UHD")
}

/**
 * Aspect ratio options for cinematic look
 */
enum class AspectRatio(val ratio: Float, val displayName: String) {
    RATIO_16_9(16f / 9f, "16:9"),
    RATIO_2_35_1(2.35f, "2.35:1 Cinematic"),
    RATIO_1_85_1(1.85f, "1.85:1"),
    RATIO_1_1(1f, "1:1 Square")
}

/**
 * White balance presets
 */
enum class WhiteBalance(val kelvin: Int, val displayName: String) {
    AUTO(0, "Auto"),
    DAYLIGHT(5600, "Daylight"),
    CLOUDY(6500, "Cloudy"),
    TUNGSTEN(3200, "Tungsten"),
    FLUORESCENT(4000, "Fluorescent"),
    SHADE(7500, "Shade")
}

/**
 * Video bitrate quality options
 */
enum class Bitrate(val bitsPerSecond: Int, val displayName: String) {
    STANDARD(35_000_000, "Standard (35 Mbps)"),
    HIGH(65_000_000, "High (65 Mbps)"),
    CINEMA(100_000_000, "Cinema (100 Mbps)")
}

/**
 * ISO sensitivity options
 */
object IsoValues {
    val available = listOf(100, 200, 400, 800, 1600, 3200, 6400)
}
