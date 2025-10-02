package com.kasirtoko.ui.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kasirtoko.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Advanced Barcode Analyzer with audio/haptic feedback
 */
class BarcodeAnalyzerWithFeedback(
    private val context: Context,
    private val onBarcodeDetected: (BarcodeResult) -> Unit,
    private val enableSound: Boolean = true,
    private val enableVibration: Boolean = true
) : ImageAnalysis.Analyzer {
    
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_DATA_MATRIX
            )
            .build()
    )
    
    private var soundPool: SoundPool? = null
    private var beepSoundId: Int = 0
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    
    private var lastAnalyzedTimestamp = 0L
    private val throttleTimeMs = 1000L
    private var lastDetectedBarcode: String? = null
    private var lastBarcodeTimestamp = 0L
    private val isProcessing = AtomicBoolean(false)
    private val duplicateTimeout = 3000L
    
    init {
        if (enableSound) {
            initSoundPool()
        }
    }
    
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // You can add a beep sound file in res/raw/beep_sound.mp3
        // beepSoundId = soundPool?.load(context, R.raw.beep_sound, 1) ?: 0
    }
    
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        
        if (isProcessing.get() || currentTimestamp - lastAnalyzedTimestamp < throttleTimeMs) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isProcessing.set(true)
            
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    processBarcodes(barcodes, currentTimestamp)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Barcode scanning failed", exception)
                }
                .addOnCompleteListener {
                    isProcessing.set(false)
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    private fun processBarcodes(barcodes: List<Barcode>, timestamp: Long) {
        for (barcode in barcodes) {
            val rawValue = barcode.rawValue ?: continue
            
            if (rawValue.isEmpty() || rawValue.length < 3) {
                continue
            }
            
            if (rawValue == lastDetectedBarcode && 
                timestamp - lastBarcodeTimestamp < duplicateTimeout) {
                continue
            }
            
            lastAnalyzedTimestamp = timestamp
            lastDetectedBarcode = rawValue
            lastBarcodeTimestamp = timestamp
            
            // Provide feedback
            provideFeedback()
            
            val result = BarcodeResult(
                rawValue = rawValue,
                displayValue = barcode.displayValue ?: rawValue,
                format = barcode.format,
                formatName = getBarcodeTypeName(barcode.format),
                valueType = barcode.valueType,
                boundingBox = barcode.boundingBox,
                cornerPoints = barcode.cornerPoints,
                timestamp = timestamp
            )
            
            Log.d(TAG, "Barcode detected: ${result.formatName} - ${result.rawValue}")
            onBarcodeDetected(result)
            
            return
        }
    }
    
    private fun provideFeedback() {
        // Play sound
        if (enableSound && beepSoundId != 0) {
            soundPool?.play(beepSoundId, 1f, 1f, 1, 0, 1f)
        }
        
        // Vibrate
        if (enableVibration && vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }
    
    private fun getBarcodeTypeName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            else -> "UNKNOWN"
        }
    }
    
    fun reset() {
        lastDetectedBarcode = null
        lastBarcodeTimestamp = 0L
        lastAnalyzedTimestamp = 0L
    }
    
    fun release() {
        scanner.close()
        soundPool?.release()
        soundPool = null
    }
    
    companion object {
        private const val TAG = "BarcodeAnalyzer"
    }
}
```

## BarcodeAnalyzer Builder Pattern

```kotlin
package com.kasirtoko.ui.scanner

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Builder for creating customized BarcodeAnalyzer instances
 */
class BarcodeAnalyzerBuilder(private val context: Context) {
    
    private var onBarcodeDetected: ((BarcodeResult) -> Unit)? = null
    private var onError: ((Exception) -> Unit)? = null
    private var supportedFormats: IntArray = DEFAULT_FORMATS
    private var enableSound: Boolean = true
    private var enableVibration: Boolean = true
    private var throttleTimeMs: Long = 1000L
    private var duplicateTimeout: Long = 3000L
    
    fun setOnBarcodeDetected(callback: (BarcodeResult) -> Unit): BarcodeAnalyzerBuilder {
        this.onBarcodeDetected = callback
        return this
    }
    
    fun setOnError(callback: (Exception) -> Unit): BarcodeAnalyzerBuilder {
        this.onError = callback
        return this
    }
    
    fun setSupportedFormats(formats: IntArray): BarcodeAnalyzerBuilder {
        this.supportedFormats = formats
        return this
    }
    
    fun enableSound(enable: Boolean): BarcodeAnalyzerBuilder {
        this.enableSound = enable
        return this
    }
    
    fun enableVibration(enable: Boolean): BarcodeAnalyzerBuilder {
        this.enableVibration = enable
        return this
    }
    
    fun setThrottleTime(timeMs: Long): BarcodeAnalyzerBuilder {
        this.throttleTimeMs = timeMs
        return this
    }
    
    fun setDuplicateTimeout(timeMs: Long): BarcodeAnalyzerBuilder {
        this.duplicateTimeout = timeMs
        return this
    }
    
    fun build(): BarcodeAnalyzerWithFeedback {
        require(onBarcodeDetected != null) { "onBarcodeDetected callback must be set" }
        
        return BarcodeAnalyzerWithFeedback(
            context = context,
            onBarcodeDetected = onBarcodeDetected!!,
            enableSound = enableSound,
            enableVibration = enableVibration
        )
    }
    
    companion object {
        val DEFAULT_FORMATS = intArrayOf(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39
        )
        
        val ALL_FORMATS = intArrayOf(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_DATA_MATRIX
        )
    }
}
