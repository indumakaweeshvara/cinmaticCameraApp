package com.cinecam.cinematiccamera.processing.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Histogram Calculator
 * 
 * Calculates RGB and Luminance histograms for exposure monitoring.
 */
@Singleton
class HistogramProcessor @Inject constructor() {
    
    data class HistogramData(
        val red: IntArray,
        val green: IntArray,
        val blue: IntArray,
        val luminance: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HistogramData) return false
            return red.contentEquals(other.red) &&
                   green.contentEquals(other.green) &&
                   blue.contentEquals(other.blue) &&
                   luminance.contentEquals(other.luminance)
        }
        
        override fun hashCode(): Int {
            var result = red.contentHashCode()
            result = 31 * result + green.contentHashCode()
            result = 31 * result + blue.contentHashCode()
            result = 31 * result + luminance.contentHashCode()
            return result
        }
    }
    
    /**
     * Calculate histogram from bitmap
     * Returns normalized histogram data (0-255 bins)
     */
    fun calculateHistogram(bitmap: Bitmap, sampleRate: Int = 4): HistogramData {
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val luminance = IntArray(256)
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Sample pixels for performance
        for (y in 0 until height step sampleRate) {
            for (x in 0 until width step sampleRate) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = ((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt().coerceIn(0, 255)
                
                red[r]++
                green[g]++
                blue[b]++
                luminance[lum]++
            }
        }
        
        return HistogramData(red, green, blue, luminance)
    }
    
    /**
     * Render histogram to a bitmap for display
     */
    fun renderHistogram(
        data: HistogramData,
        width: Int = 256,
        height: Int = 100,
        showRGB: Boolean = true,
        showLuminance: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background
        canvas.drawColor(Color.argb(180, 0, 0, 0))
        
        // Find max value for normalization
        val maxValue = maxOf(
            data.red.maxOrNull() ?: 1,
            data.green.maxOrNull() ?: 1,
            data.blue.maxOrNull() ?: 1,
            data.luminance.maxOrNull() ?: 1
        )
        
        val scaleX = width / 256f
        val scaleY = height / maxValue.toFloat()
        
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = 100
        }
        
        if (showRGB) {
            // Draw Red channel
            paint.color = Color.RED
            drawHistogramChannel(canvas, data.red, scaleX, scaleY, height, paint)
            
            // Draw Green channel
            paint.color = Color.GREEN
            drawHistogramChannel(canvas, data.green, scaleX, scaleY, height, paint)
            
            // Draw Blue channel
            paint.color = Color.BLUE
            drawHistogramChannel(canvas, data.blue, scaleX, scaleY, height, paint)
        }
        
        if (showLuminance) {
            // Draw Luminance
            paint.color = Color.WHITE
            paint.alpha = 150
            drawHistogramChannel(canvas, data.luminance, scaleX, scaleY, height, paint)
        }
        
        return bitmap
    }
    
    private fun drawHistogramChannel(
        canvas: Canvas,
        data: IntArray,
        scaleX: Float,
        scaleY: Float,
        height: Int,
        paint: Paint
    ) {
        val path = Path()
        path.moveTo(0f, height.toFloat())
        
        for (i in data.indices) {
            val x = i * scaleX
            val y = height - (data[i] * scaleY)
            path.lineTo(x, y)
        }
        
        path.lineTo(256 * scaleX, height.toFloat())
        path.close()
        
        canvas.drawPath(path, paint)
    }
}

/**
 * Audio Level Meter
 * 
 * Visualizes audio input levels for monitoring.
 */
@Singleton
class AudioLevelProcessor @Inject constructor() {
    
    private var currentLevel = 0f
    private var peakLevel = 0f
    private var peakHoldTime = 0L
    private val peakHoldDuration = 1500L // ms
    
    /**
     * Update audio level (0.0 to 1.0)
     */
    fun updateLevel(level: Float) {
        currentLevel = level.coerceIn(0f, 1f)
        
        if (level > peakLevel || System.currentTimeMillis() - peakHoldTime > peakHoldDuration) {
            peakLevel = level
            peakHoldTime = System.currentTimeMillis()
        }
    }
    
    /**
     * Get current level
     */
    fun getCurrentLevel(): Float = currentLevel
    
    /**
     * Get peak level
     */
    fun getPeakLevel(): Float = peakLevel
    
    /**
     * Convert level to dB (-60 to 0)
     */
    fun levelToDb(level: Float): Float {
        if (level <= 0) return -60f
        return (20 * kotlin.math.log10(level.toDouble())).toFloat().coerceIn(-60f, 0f)
    }
    
    /**
     * Check if audio is clipping (too loud)
     */
    fun isClipping(): Boolean = currentLevel > 0.95f
    
    /**
     * Render audio meter to bitmap
     */
    fun renderMeter(width: Int = 200, height: Int = 30): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background
        val bgPaint = Paint().apply {
            color = Color.argb(180, 30, 30, 30)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Level bar with gradient
        val levelWidth = width * currentLevel
        val paint = Paint()
        
        // Green zone (0-70%)
        val greenWidth = minOf(levelWidth, width * 0.7f)
        paint.color = Color.rgb(0, 200, 0)
        canvas.drawRect(0f, 4f, greenWidth, height - 4f, paint)
        
        // Yellow zone (70-90%)
        if (levelWidth > width * 0.7f) {
            val yellowWidth = minOf(levelWidth, width * 0.9f)
            paint.color = Color.rgb(255, 200, 0)
            canvas.drawRect(width * 0.7f, 4f, yellowWidth, height - 4f, paint)
        }
        
        // Red zone (90-100%)
        if (levelWidth > width * 0.9f) {
            paint.color = Color.rgb(255, 50, 50)
            canvas.drawRect(width * 0.9f, 4f, levelWidth, height - 4f, paint)
        }
        
        // Peak indicator
        paint.color = Color.WHITE
        val peakX = width * peakLevel
        canvas.drawRect(peakX - 2, 2f, peakX + 2, height - 2f, paint)
        
        // dB markers
        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("-40", width * 0.1f, height - 2f, paint)
        canvas.drawText("-20", width * 0.4f, height - 2f, paint)
        canvas.drawText("-12", width * 0.6f, height - 2f, paint)
        canvas.drawText("0", width * 0.95f, height - 2f, paint)
        
        return bitmap
    }
}

/**
 * Grid Overlay Generator
 * 
 * Generates composition guide overlays.
 */
@Singleton
class GridOverlayProcessor @Inject constructor() {
    
    enum class GridType {
        NONE,
        RULE_OF_THIRDS,
        GOLDEN_RATIO,
        CENTER_CROSS,
        DIAGONAL,
        GRID_3X3,
        GRID_4X4
    }
    
    private var currentGrid = GridType.NONE
    private var gridColor = Color.argb(128, 255, 255, 255)
    private var lineWidth = 1f
    
    fun setGridType(type: GridType) {
        currentGrid = type
    }
    
    fun setColor(color: Int) {
        gridColor = color
    }
    
    /**
     * Render grid overlay
     */
    fun renderGrid(width: Int, height: Int): Bitmap? {
        if (currentGrid == GridType.NONE) return null
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = gridColor
            strokeWidth = lineWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        when (currentGrid) {
            GridType.RULE_OF_THIRDS -> drawRuleOfThirds(canvas, width, height, paint)
            GridType.GOLDEN_RATIO -> drawGoldenRatio(canvas, width, height, paint)
            GridType.CENTER_CROSS -> drawCenterCross(canvas, width, height, paint)
            GridType.DIAGONAL -> drawDiagonals(canvas, width, height, paint)
            GridType.GRID_3X3 -> drawGrid(canvas, width, height, 3, paint)
            GridType.GRID_4X4 -> drawGrid(canvas, width, height, 4, paint)
            else -> {}
        }
        
        return bitmap
    }
    
    private fun drawRuleOfThirds(canvas: Canvas, width: Int, height: Int, paint: Paint) {
        val w = width.toFloat()
        val h = height.toFloat()
        
        // Vertical lines
        canvas.drawLine(w / 3, 0f, w / 3, h, paint)
        canvas.drawLine(2 * w / 3, 0f, 2 * w / 3, h, paint)
        
        // Horizontal lines
        canvas.drawLine(0f, h / 3, w, h / 3, paint)
        canvas.drawLine(0f, 2 * h / 3, w, 2 * h / 3, paint)
        
        // Intersection points (power points)
        val pointPaint = Paint(paint).apply {
            style = Paint.Style.FILL
        }
        val radius = 4f
        canvas.drawCircle(w / 3, h / 3, radius, pointPaint)
        canvas.drawCircle(2 * w / 3, h / 3, radius, pointPaint)
        canvas.drawCircle(w / 3, 2 * h / 3, radius, pointPaint)
        canvas.drawCircle(2 * w / 3, 2 * h / 3, radius, pointPaint)
    }
    
    private fun drawGoldenRatio(canvas: Canvas, width: Int, height: Int, paint: Paint) {
        val phi = 1.618f
        val w = width.toFloat()
        val h = height.toFloat()
        
        // Golden ratio divisions
        val x1 = w / (1 + phi)
        val x2 = w - x1
        val y1 = h / (1 + phi)
        val y2 = h - y1
        
        // Vertical lines
        canvas.drawLine(x1, 0f, x1, h, paint)
        canvas.drawLine(x2, 0f, x2, h, paint)
        
        // Horizontal lines
        canvas.drawLine(0f, y1, w, y1, paint)
        canvas.drawLine(0f, y2, w, y2, paint)
    }
    
    private fun drawCenterCross(canvas: Canvas, width: Int, height: Int, paint: Paint) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2
        val size = minOf(w, h) * 0.1f
        
        // Center cross
        canvas.drawLine(cx - size, cy, cx + size, cy, paint)
        canvas.drawLine(cx, cy - size, cx, cy + size, paint)
        
        // Center circle
        canvas.drawCircle(cx, cy, size / 2, paint)
    }
    
    private fun drawDiagonals(canvas: Canvas, width: Int, height: Int, paint: Paint) {
        val w = width.toFloat()
        val h = height.toFloat()
        
        canvas.drawLine(0f, 0f, w, h, paint)
        canvas.drawLine(w, 0f, 0f, h, paint)
    }
    
    private fun drawGrid(canvas: Canvas, width: Int, height: Int, divisions: Int, paint: Paint) {
        val w = width.toFloat()
        val h = height.toFloat()
        
        for (i in 1 until divisions) {
            val x = w * i / divisions
            val y = h * i / divisions
            canvas.drawLine(x, 0f, x, h, paint)
            canvas.drawLine(0f, y, w, y, paint)
        }
    }
}
