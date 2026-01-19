package com.cinecam.cinematiccamera.processing.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selfie Segmentation Processor
 * 
 * Uses MediaPipe's Image Segmenter to separate foreground (person)
 * from background in real-time for bokeh effect application.
 */
@Singleton
class SelfieSegmentationProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SelfieSegmentation"
        private const val MODEL_PATH = "selfie_segmenter.tflite"
    }
    
    private var imageSegmenter: ImageSegmenter? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    
    private val _processingTimeMs = MutableStateFlow(0L)
    val processingTimeMs: StateFlow<Long> = _processingTimeMs
    
    // Callback for live stream mode
    private var resultCallback: ((SegmentationOutput) -> Unit)? = null
    
    /**
     * Initialize the segmenter with MediaPipe
     */
    suspend fun initialize(runningMode: RunningMode = RunningMode.LIVE_STREAM) = withContext(Dispatchers.IO) {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.GPU) // Use GPU for faster processing
                .setModelAssetPath(MODEL_PATH)
                .build()
            
            val options = ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(runningMode)
                .setOutputCategoryMask(true)
                .setOutputConfidenceMasks(true)
                .apply {
                    if (runningMode == RunningMode.LIVE_STREAM) {
                        setResultListener { result, input ->
                            handleSegmentationResult(result, input)
                        }
                        setErrorListener { error ->
                            Log.e(TAG, "Segmentation error: ${error.message}")
                        }
                    }
                }
                .build()
            
            imageSegmenter = ImageSegmenter.createFromOptions(context, options)
            _isInitialized.value = true
            Log.d(TAG, "Segmenter initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize segmenter: ${e.message}")
            _isInitialized.value = false
        }
    }
    
    /**
     * Process a single frame for segmentation (VIDEO mode)
     */
    suspend fun processFrame(bitmap: Bitmap, timestampMs: Long): SegmentationOutput? = withContext(Dispatchers.Default) {
        if (!_isInitialized.value || imageSegmenter == null) {
            return@withContext null
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = imageSegmenter?.segmentForVideo(mpImage, timestampMs)
            
            val processingTime = System.currentTimeMillis() - startTime
            _processingTimeMs.value = processingTime
            
            result?.let { createOutput(it, processingTime) }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
            null
        }
    }
    
    /**
     * Process frame asynchronously (LIVE_STREAM mode)
     */
    fun processFrameAsync(bitmap: Bitmap, timestampMs: Long, callback: (SegmentationOutput) -> Unit) {
        if (!_isInitialized.value || imageSegmenter == null) {
            return
        }
        
        resultCallback = callback
        
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            imageSegmenter?.segmentAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error in async processing: ${e.message}")
        }
    }
    
    private fun handleSegmentationResult(result: ImageSegmenterResult, input: MPImage) {
        val startTime = System.currentTimeMillis()
        val output = createOutput(result, System.currentTimeMillis() - startTime)
        output?.let { resultCallback?.invoke(it) }
    }
    
    private fun createOutput(result: ImageSegmenterResult, processingTimeMs: Long): SegmentationOutput? {
        val confidenceMasks = result.confidenceMasks()
        if (!confidenceMasks.isPresent || confidenceMasks.get().isEmpty()) {
            return null
        }
        
        // First mask is typically the person/foreground mask
        val personMask = confidenceMasks.get()[0]
        val maskWidth = personMask.width
        val maskHeight = personMask.height
        
        // Convert FloatBuffer to float array for mask data
        val floatBuffer = personMask.getAsFloatBuffer()
        val maskData = FloatArray(maskWidth * maskHeight)
        floatBuffer.get(maskData)
        
        return SegmentationOutput(
            maskData = maskData,
            width = maskWidth,
            height = maskHeight,
            processingTimeMs = processingTimeMs
        )
    }
    
    /**
     * Release resources
     */
    fun release() {
        imageSegmenter?.close()
        imageSegmenter = null
        _isInitialized.value = false
        resultCallback = null
    }
}

/**
 * Output from segmentation processing
 */
data class SegmentationOutput(
    val maskData: FloatArray,
    val width: Int,
    val height: Int,
    val processingTimeMs: Long
) {
    /**
     * Convert mask data to a Bitmap for visualization or processing
     */
    fun toMaskBitmap(): Bitmap {
        val pixels = IntArray(width * height)
        for (i in maskData.indices) {
            val alpha = (maskData[i] * 255).toInt().coerceIn(0, 255)
            pixels[i] = (alpha shl 24) or 0x00FFFFFF // White with varying alpha
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SegmentationOutput
        return maskData.contentEquals(other.maskData) &&
               width == other.width &&
               height == other.height
    }
    
    override fun hashCode(): Int {
        var result = maskData.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
