package com.cinecam.cinematiccamera.processing.segmentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Blur Processor - Applies Gaussian blur to background regions
 * 
 * Uses the segmentation mask to separate foreground from background
 * and applies a configurable blur effect to create bokeh.
 */
@Singleton
class BlurProcessor @Inject constructor() {
    
    companion object {
        private const val MAX_BLUR_RADIUS = 25f
        private const val MIN_BLUR_RADIUS = 1f
    }
    
    // Paint objects for compositing
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    
    private val srcInPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
    
    private val dstOverPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
    }
    
    /**
     * Apply bokeh effect to an image using the segmentation mask
     * 
     * @param original The original camera frame
     * @param segmentationOutput The segmentation result with foreground mask
     * @param blurIntensity Blur strength from 0.0 to 1.0
     * @return Processed image with blurred background
     */
    suspend fun applyBokehEffect(
        original: Bitmap,
        segmentationOutput: SegmentationOutput,
        blurIntensity: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = original.width
        val height = original.height
        
        // Create the blur radius based on intensity
        val blurRadius = MIN_BLUR_RADIUS + (MAX_BLUR_RADIUS - MIN_BLUR_RADIUS) * blurIntensity
        
        // Create blurred version of the entire image
        val blurredBackground = createBlurredBitmap(original, blurRadius)
        
        // Create mask bitmap scaled to match original
        val scaledMask = scaleMaskToFrame(segmentationOutput, width, height)
        
        // Composite: foreground (sharp) + background (blurred)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Draw blurred background first
        canvas.drawBitmap(blurredBackground, 0f, 0f, null)
        
        // Create foreground with mask applied
        val foreground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val fgCanvas = Canvas(foreground)
        
        // Draw the mask as alpha
        fgCanvas.drawBitmap(scaledMask, 0f, 0f, null)
        
        // Draw the original where mask is present
        fgCanvas.drawBitmap(original, 0f, 0f, srcInPaint)
        
        // Composite foreground over blurred background
        canvas.drawBitmap(foreground, 0f, 0f, null)
        
        // Clean up temporary bitmaps
        blurredBackground.recycle()
        scaledMask.recycle()
        foreground.recycle()
        
        result
    }
    
    /**
     * Create a Gaussian blurred version of the bitmap
     * 
     * For API 31+, we use a simple box blur approximation
     * since RenderScript is deprecated.
     */
    private fun createBlurredBitmap(source: Bitmap, radius: Float): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        
        // Simple stack blur for cross-version compatibility
        return stackBlur(output, radius.toInt().coerceIn(1, 25))
    }
    
    /**
     * Stack blur implementation (fast approximation of Gaussian blur)
     */
    private fun stackBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        
        val vmin = IntArray(maxOf(w, h))
        
        var divsum = (div + 1) shr 1
        divsum *= divsum
        
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) {
            dv[i] = i / divsum
        }
        
        yi = 0
        yw = 0
        
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        
        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            
            i = -radius
            while (i <= radius) {
                p = pixels[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                
                if (y == 0) {
                    vmin[x] = minOf(x + radius + 1, wm)
                }
                p = pixels[yw + vmin[x]]
                
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                
                yi++
                x++
            }
            yw += w
            y++
        }
        
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            
            yp = -radius * w
            
            i = -radius
            while (i <= radius) {
                yi = maxOf(0, yp) + x
                
                sir = stack[i + radius]
                
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                
                rbs = r1 - kotlin.math.abs(i)
                
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            
            y = 0
            while (y < h) {
                pixels[yi] = (0xff000000.toInt() and pixels[yi]) or
                        (dv[rsum] shl 16) or
                        (dv[gsum] shl 8) or
                        dv[bsum]
                
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                
                if (x == 0) {
                    vmin[y] = minOf(y + r1, hm) * w
                }
                p = x + vmin[y]
                
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                
                yi += w
                y++
            }
            x++
        }
        
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }
    
    /**
     * Scale the segmentation mask to match the frame dimensions
     */
    private fun scaleMaskToFrame(
        segmentationOutput: SegmentationOutput,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val maskBitmap = segmentationOutput.toMaskBitmap()
        return Bitmap.createScaledBitmap(maskBitmap, targetWidth, targetHeight, true).also {
            if (it !== maskBitmap) maskBitmap.recycle()
        }
    }
    
    /**
     * Apply edge feathering to smooth the mask edges
     */
    fun applyEdgeFeathering(mask: Bitmap, featherRadius: Int = 5): Bitmap {
        return stackBlur(mask.copy(Bitmap.Config.ARGB_8888, true), featherRadius)
    }
}
