package com.cera.drivera.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.pow
import kotlin.math.sqrt
import java.util.LinkedList

class FaceAnalyzer(
    private val context: Context,
    private val onEarCalculated: (Double) -> Unit
) {
    private var faceLandmarker: FaceLandmarker? = null

    // Shock absorber (Moving Average) to prevent over-sensitivity
    private val earHistory = LinkedList<Double>()
    private val HISTORY_SIZE = 5

    // OPTIMASI: Frame throttling untuk mengurangi beban CPU/GPU
    // Hanya proses setiap 3 frame untuk hemat baterai dan mengurangi panas
    private var frameCounter = 0
    private val FRAMES_TO_SKIP = 2 // Proses setiap frame ke-3 (0, 3, 6, ...)

    // OPTIMASI: Mode adaptif berdasarkan kondisi deteksi
    private var isCriticalMode = false
    private val FRAMES_TO_SKIP_NORMAL = 3  // Saat normal: proses setiap frame ke-4
    private val FRAMES_TO_SKIP_CRITICAL = 1 // Saat kritis: proses setiap frame ke-2 (lebih responsif)

    init {
        setupFaceLandmarker()
    }

    private fun setupFaceLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setResultListener { result, _ -> processLandmarks(result) }
            .setErrorListener { error -> Log.e("FaceAnalyzer", "AI Error: ${error.message}") }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    fun analyze(imageProxy: ImageProxy) {
        try {
            // OPTIMASI: Frame throttling - skip frame jika belum saatnya proses
            frameCounter++
            val currentSkipThreshold = if (isCriticalMode) FRAMES_TO_SKIP_CRITICAL else FRAMES_TO_SKIP_NORMAL
            
            if (frameCounter <= currentSkipThreshold) {
                imageProxy.close()
                return
            }
            frameCounter = 0 // Reset counter

            // Capture frames safely from rotation distortion
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()

            val timestampMs = SystemClock.uptimeMillis()
            faceLandmarker?.detectAsync(mpImage, timestampMs)
            
            // OPTIMASI: Recycle bitmap segera setelah digunakan untuk mengurangi memory pressure
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e("FaceAnalyzer", "Failed to process AI frame: ${e.message}")
        } finally {
            // Pastikan imageProxy selalu ditutup
            try {
                imageProxy.close()
            } catch (e: Exception) {
                Log.w("FaceAnalyzer", "Error closing imageProxy: ${e.message}")
            }
        }
    }

    private fun processLandmarks(result: FaceLandmarkerResult) {
        if (result.faceLandmarks().isEmpty()) return

        val landmarks = result.faceLandmarks()[0]

        val rightEye = listOf(33, 160, 158, 133, 153, 144).map { landmarks[it] }
        val leftEye = listOf(362, 385, 387, 263, 373, 380).map { landmarks[it] }

        // 1. Calculate horizontal distance (h) for each eye
        val rightH = calculateDistance(rightEye[0], rightEye[3])
        val leftH = calculateDistance(leftEye[0], leftEye[3])

        // 2. Calculate EAR by injecting parameter 'h' modularly
        val rightEAR = calculateEAR(rightEye, rightH)
        val leftEAR = calculateEAR(leftEye, leftH)

        val rawEAR: Double

        // 3. DOMINANT EYE LOGIC (10% Distortion Tolerance for tilted angles)
        if (rightH > leftH * 1.10) {
            rawEAR = rightEAR // Right eye dominates the frame (closer), isolate left eye
        } else if (leftH > rightH * 1.10) {
            rawEAR = leftEAR  // Left eye dominates the frame (closer), isolate right eye
        } else {
            rawEAR = (rightEAR + leftEAR) / 2.0 // Face position is straight forward
        }

        // 4. MOVING AVERAGE FILTER IMPLEMENTATION
        earHistory.addLast(rawEAR)
        if (earHistory.size > HISTORY_SIZE) {
            earHistory.removeFirst()
        }

        val smoothedEAR = earHistory.average()

        // OPTIMASI: Update mode adaptif berdasarkan EAR
        // Jika EAR sangat rendah (< 0.18), aktifkan mode kritis untuk deteksi lebih responsif
        isCriticalMode = smoothedEAR < 0.18

        onEarCalculated(smoothedEAR)
    }

    // Utility functions broken down so distance computation is not redundant
    private fun calculateDistance(
        p1: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        p2: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): Double {
        return sqrt((p1.x() - p2.x()).toDouble().pow(2.0) + (p1.y() - p2.y()).toDouble().pow(2.0))
    }

    private fun calculateEAR(
        eye: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        h: Double
    ): Double {
        val v1 = calculateDistance(eye[1], eye[5])
        val v2 = calculateDistance(eye[2], eye[4])
        return (v1 + v2) / (2.0 * h)
    }

    fun clear() {
        faceLandmarker?.close()
        faceLandmarker = null
    }
}