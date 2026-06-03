package com.cera.drivera.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.cera.drivera.data.FaceAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Foreground Service that handles the Driver Monitoring System (DMS) logic.
 * It manages the camera lifecycle, eye-tracking analysis, and triggers alerts.
 */
class DmsForegroundService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "drivera_safety_channel"
        private const val NOTIFICATION_ID = 101
        private const val TAG = "DmsCameraSystem"
    }

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceAnalyzer: FaceAnalyzer
    private lateinit var audioManager: AudioAlertManager

    // --- STATE MACHINE LOGIC VARIABLES ---
    private val EAR_THRESHOLD = 0.16
    private val CRITICAL_DURATION_MS = 1500L // 1.5 Seconds
    private var timeEyesClosedStart = 0L
    private var isCurrentlyCritical = false

    /**
     * Initializes background executors, audio manager, and the face analyzer.
     * The face analyzer callback triggers the drowsiness logic.
     */
    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        audioManager = AudioAlertManager(this)
        faceAnalyzer = FaceAnalyzer(this) { earValue ->
            Log.d(TAG, "Live EAR: ${String.format("%.3f", earValue)}")
            processDrowsinessLogic(earValue)
        }
    }

    /**
     * Processes the Eye Aspect Ratio (EAR) value to determine if the driver is drowsy.
     *
     * @param earValue The calculated EAR from the FaceAnalyzer.
     */
    private fun processDrowsinessLogic(earValue: Double) {
        val currentTime = System.currentTimeMillis()

        if (earValue < EAR_THRESHOLD) {
            // Eyes starting to close or squinting
            if (timeEyesClosedStart == 0L) {
                // Record the first time eyes are detected as closed
                timeEyesClosedStart = currentTime
                broadcastState("WARNING")
            } else {
                // If already closed, calculate the duration
                val durationClosed = currentTime - timeEyesClosedStart

                if (durationClosed >= CRITICAL_DURATION_MS && !isCurrentlyCritical) {
                    // DURATION EXCEEDED: Trigger critical status!
                    isCurrentlyCritical = true
                    Log.e(TAG, "DANGER: Drowsy Driver! (Duration: ${durationClosed}ms)")
                    broadcastState("CRITICAL")
                    // Play Alarm
                    audioManager.playAlarm()
                }
            }
        } else {
            // Eyes are open again, system back to normal
            if (timeEyesClosedStart != 0L || isCurrentlyCritical) {
                timeEyesClosedStart = 0L
                isCurrentlyCritical = false
                Log.i(TAG, "System returned to normal.")
                broadcastState("SAFE")
                audioManager.stopAlarm()
            }
        }
    }

    /**
     * Starts the foreground service with a notification and initializes the camera.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DRIVERA Sedang Aktif")
            .setContentText("Sistem memantau tingkat kesadaran mengemudi Anda.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        startCamera()

        return START_STICKY
    }

    /**
     * Configures and binds the CameraX CameraProvider to the service's lifecycle.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configure ImageAnalysis for AI processing
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    faceAnalyzer.analyze(imageProxy)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to analyze frame", e)
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                // Bind camera to this LifecycleService
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis
                )
                Log.d(TAG, "Kamera berhasil di-bind ke Background Service")
            } catch (exc: Exception) {
                Log.e(TAG, "Gagal melakukan binding kamera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Creates a Notification Channel for Android O and above to support foreground services.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Driver Monitoring System",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Broadcasts the current monitoring state (SAFE, WARNING, CRITICAL)
     * to other components of the application.
     */
    private fun broadcastState(state: String) {
        val intent = Intent("DMS_STATE_UPDATE")
        intent.putExtra("STATUS", state)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Cleans up resources, shuts down the camera executor, and
     * ensures any active alarms are stopped when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceAnalyzer.clear()
        audioManager.stopAlarm()
    }
}