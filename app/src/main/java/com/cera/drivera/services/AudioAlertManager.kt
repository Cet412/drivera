package com.cera.drivera.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.cera.drivera.R

class AudioAlertManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Inject system AudioManager to take over device volume control
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val TAG = "AudioAlertManager"

    init {
        setupVibrator()
    }

    private fun setupVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun playAlarm() {
        // 1. CUSTOM AUDIO EXECUTION & VOLUME TAKEOVER
        if (mediaPlayer?.isPlaying != true) {
            try {
                // Force ALARM channel volume to the maximum (100%) invisibly (without UI flags)
                val maxAlarmVolume = systemAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                systemAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)

                // Manual MediaPlayer instantiation (Not using .create())
                mediaPlayer = MediaPlayer().apply {
                    // Define attributes BEFORE setting the data source
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(audioAttributes)

                    // Open raw resource file descriptor manually
                    val afd = context.resources.openRawResourceFd(R.raw.dms_alarm)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close() // Must be closed to prevent file descriptor memory leaks

                    isLooping = true
                    prepare() // Prepare engine once routing is set
                    start()   // Start audio playback
                }
                Log.d(TAG, "Alarm audio triggered on ALARM channel with max volume.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play custom audio", e)
            }
        }

        // 2. LOOPING VIBRATION EXECUTION
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                val pattern = longArrayOf(0, 500, 200, 500)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, 1)
                    v.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, 1)
                }
            }
        }
    }

    fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
    }
}