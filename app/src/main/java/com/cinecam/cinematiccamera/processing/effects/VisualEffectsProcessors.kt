package com.cinecam.cinematiccamera.processing.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

/**
 * Focus Peaking Processor
 * 
 * Highlights in-focus areas with colored outlines for precise manual focusing.
 * Uses edge detection to find sharp areas in the frame.
 */
@Singleton
class FocusPeakingProcessor @Inject constructor() {
    
    enum class PeakingColor(val color: Int) {
        RED(Color.RED),
        GREEN(Color.GREEN),
        BLUE(Color.BLUE),
        YELLOW(Color.YELLOW),
        WHITE(Color.WHITE)
    }
    
    private var isEnabled = false
    private var peakingColor = PeakingColor.RED
    private var sensitivity = 0.5f // 0.0 to 1.0
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun setColor(color: PeakingColor) {
        peakingColor = color
    }
    
    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(0f, 1f)
    }
    
    /**
     * Apply focus peaking overlay to the frame
     */
    suspend fun applyFocusPeaking(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isEnabled) return@withContext bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val edgePixels = IntArray(width * height)
        
        // Sobel edge detection threshold based on sensitivity
        val threshold = ((1f - sensitivity) * 100 + 20).toInt()
        
        // Apply Sobel edge detection
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = calculateSobelX(pixels, x, y, width)
                val gy = calculateSobelY(pixels, x, y, width)
                val magnitude = kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toInt()
                
                if (magnitude > threshold) {
                    edgePixels[y * width + x] = peakingColor.color
                }
            }
        }
        
        // Overlay edges on result
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        val canvas = Canvas(result)
        val paint = Paint()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (edgePixels[y * width + x] != 0) {
                    paint.color = edgePixels[y * width + x]
                    canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                }
            }
        }
        
        result
    }
    
    private fun calculateSobelX(pixels: IntArray, x: Int, y: Int, width: Int): Int {
        val tl = getLuminance(pixels[(y - 1) * width + (x - 1)])
        val ml = getLuminance(pixels[y * width + (x - 1)])
        val bl = getLuminance(pixels[(y + 1) * width + (x - 1)])
        val tr = getLuminance(pixels[(y - 1) * width + (x + 1)])
        val mr = getLuminance(pixels[y * width + (x + 1)])
        val br = getLuminance(pixels[(y + 1) * width + (x + 1)])
        
        return (-tl - 2 * ml - bl + tr + 2 * mr + br)
    }
    
    private fun calculateSobelY(pixels: IntArray, x: Int, y: Int, width: Int): Int {
        val tl = getLuminance(pixels[(y - 1) * width + (x - 1)])
        val tm = getLuminance(pixels[(y - 1) * width + x])
        val tr = getLuminance(pixels[(y - 1) * width + (x + 1)])
        val bl = getLuminance(pixels[(y + 1) * width + (x - 1)])
        val bm = getLuminance(pixels[(y + 1) * width + x])
        val br = getLuminance(pixels[(y + 1) * width + (x + 1)])
        
        return (-tl - 2 * tm - tr + bl + 2 * bm + br)
    }
    
    private fun getLuminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}

/**
 * Zebra Pattern Processor
 * 
 * Shows diagonal stripes on overexposed areas to prevent clipping.
 */
@Singleton
class ZebraPatternProcessor @Inject constructor() {
    
    private var isEnabled = false
    private var threshold = 95 // Percentage (0-100)
    private var stripeWidth = 4
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun setThreshold(value: Int) {
        threshold = value.coerceIn(70, 100)
    }
    
    /**
     * Apply zebra pattern to overexposed areas
     */
    suspend fun applyZebraPattern(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isEnabled) return@withContext bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val thresholdValue = (threshold / 100f * 255).toInt()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Check if any channel is overexposed
                if (r > thresholdValue || g > thresholdValue || b > thresholdValue) {
                    // Create diagonal stripe pattern
                    if ((x + y) % (stripeWidth * 2) < stripeWidth) {
                        // Black stripe
                        pixels[y * width + x] = Color.BLACK
                    }
                    // Keep original for non-stripe areas
                }
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }
}

/**
 * Film Grain Effect Processor
 * 
 * Adds vintage film grain texture for cinematic look.
 */
@Singleton
class FilmGrainProcessor @Inject constructor() {
    
    private var isEnabled = false
    private var intensity = 0.3f // 0.0 to 1.0
    private var grainSize = 1 // 1 = fine, 2 = medium, 3 = coarse
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun setIntensity(value: Float) {
        intensity = value.coerceIn(0f, 1f)
    }
    
    fun setGrainSize(size: Int) {
        grainSize = size.coerceIn(1, 3)
    }
    
    /**
     * Apply film grain effect
     */
    suspend fun applyFilmGrain(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isEnabled) return@withContext bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val grainAmount = (intensity * 50).toInt()
        
        for (y in 0 until height step grainSize) {
            for (x in 0 until width step grainSize) {
                val noise = Random.nextInt(-grainAmount, grainAmount + 1)
                
                for (dy in 0 until grainSize) {
                    for (dx in 0 until grainSize) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < width && ny < height) {
                            val idx = ny * width + nx
                            val pixel = pixels[idx]
                            
                            var r = ((pixel shr 16) and 0xFF) + noise
                            var g = ((pixel shr 8) and 0xFF) + noise
                            var b = (pixel and 0xFF) + noise
                            
                            r = r.coerceIn(0, 255)
                            g = g.coerceIn(0, 255)
                            b = b.coerceIn(0, 255)
                            
                            pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }
}

/**
 * Anamorphic Lens Effect Processor
 * 
 * Simulates anamorphic lens characteristics:
 * - Horizontal lens flares
 * - Oval bokeh (stretched vertically)
 * - Subtle barrel distortion
 */
@Singleton
class AnamorphicProcessor @Inject constructor() {
    
    private var isEnabled = false
    private var flareIntensity = 0.5f
    private var flareColor = Color.argb(100, 100, 180, 255) // Blue-ish
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun setFlareIntensity(value: Float) {
        flareIntensity = value.coerceIn(0f, 1f)
    }
    
    /**
     * Apply anamorphic lens flare effect
     */
    suspend fun applyAnamorphicFlare(bitmap: Bitmap, brightSpots: List<Pair<Int, Int>>): Bitmap = withContext(Dispatchers.Default) {
        if (!isEnabled || brightSpots.isEmpty()) return@withContext bitmap
        
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val paint = Paint().apply {
            color = flareColor
            alpha = (flareIntensity * 150).toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        
        // Draw horizontal flares at bright spots
        brightSpots.take(5).forEach { (x, y) ->
            val flareWidth = bitmap.width * 0.8f
            val flareHeight = 4f + (flareIntensity * 8f)
            
            // Draw horizontal flare line
            canvas.drawRect(
                x - flareWidth / 2,
                y - flareHeight / 2,
                x + flareWidth / 2,
                y + flareHeight / 2,
                paint
            )
        }
        
        result
    }
    
    /**
     * Find bright spots in the image for flare placement
     */
    fun findBrightSpots(bitmap: Bitmap, threshold: Int = 240): List<Pair<Int, Int>> {
        val spots = mutableListOf<Pair<Int, Int>>()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Sample every 20 pixels for performance
        for (y in 0 until height step 20) {
            for (x in 0 until width step 20) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                if (r > threshold && g > threshold && b > threshold) {
                    spots.add(Pair(x, y))
                }
            }
        }
        
        return spots
    }
}
