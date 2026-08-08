package com.example.meshsosrelay.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.meshsosrelay.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

data class ScreamEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val decibels: Float,
    val confidence: Float
)

class ScreamDetector(
    private val context: Context,
    private val onScreamDetected: ((ScreamEvent) -> Unit)? = null
) {

    private val permissionManager = PermissionManager(context)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var recordingJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _lastScreamEvent = MutableStateFlow<ScreamEvent?>(null)
    val lastScreamEvent: StateFlow<ScreamEvent?> = _lastScreamEvent.asStateFlow()

    @SuppressLint("MissingPermission")
    fun start() {
        if (!permissionManager.hasAudioPermission() || _isListening.value) return

        _isListening.value = true
        recordingJob = scope.launch {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                _isListening.value = false
                return@launch
            }

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    _isListening.value = false
                    return@launch
                }

                audioRecord.startRecording()
                val buffer = ShortArray(minBufferSize)

                while (_isListening.value) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readSize)
                        val db = if (rms > 0) (20 * log10(rms)).toFloat() else 0f
                        _currentDb.value = db

                        // Scream threshold (> 78 dB threshold for high-amplitude acoustic distress)
                        if (db > 78f) {
                            val confidence = ((db - 78f) / 30f + 0.70f).coerceAtMost(0.98f)
                            val screamEvent = ScreamEvent(
                                timestamp = System.currentTimeMillis(),
                                decibels = db,
                                confidence = confidence
                            )
                            _lastScreamEvent.value = screamEvent
                            onScreamDetected?.invoke(screamEvent)
                        }
                    }
                }
            } catch (e: Exception) {
                _isListening.value = false
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
            }
        }
    }

    fun stop() {
        _isListening.value = false
        recordingJob?.cancel()
        recordingJob = null
    }

    fun simulateScream(db: Float = 85f) {
        val screamEvent = ScreamEvent(
            timestamp = System.currentTimeMillis(),
            decibels = db,
            confidence = 0.88f
        )
        _lastScreamEvent.value = screamEvent
        onScreamDetected?.invoke(screamEvent)
    }
}
