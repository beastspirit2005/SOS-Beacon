package com.example.meshsosrelay.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class FallEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val gForcePeak: Float,
    val stillnessDurationMs: Long,
    val confidence: Float
)

class FallDetector(
    private val context: Context,
    private val onFallDetected: ((FallEvent) -> Unit)? = null
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastFallEvent = MutableStateFlow<FallEvent?>(null)
    val lastFallEvent: StateFlow<FallEvent?> = _lastFallEvent.asStateFlow()

    private var freeFallDetected = false
    private var freeFallTimestamp = 0L
    private var peakImpactG = 0f
    private var impactTimestamp = 0L

    fun start() {
        if (accelerometer != null && !_isMonitoring.value) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            _isMonitoring.value = true
        }
    }

    fun stop() {
        if (_isMonitoring.value) {
            sensorManager?.unregisterListener(this)
            _isMonitoring.value = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate total magnitude in m/s^2, normalized to g-force
        val totalAcc = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val gForce = totalAcc / SensorManager.GRAVITY_EARTH

        val now = System.currentTimeMillis()

        // 1. Detect Free Fall (< 0.4g threshold)
        if (gForce < 0.4f) {
            freeFallDetected = true
            freeFallTimestamp = now
        }

        // 2. Detect High-G Impact (> 2.8g threshold) following Free Fall within 1.5 seconds
        if (freeFallDetected && (now - freeFallTimestamp < 1500)) {
            if (gForce > 2.8f) {
                peakImpactG = gForce
                impactTimestamp = now

                // Calculate confidence based on impact severity (ranges from 0.75 to 1.0)
                val confidence = (0.75f + (peakImpactG / 10f)).coerceAtMost(1.0f)

                val fallEvent = FallEvent(
                    timestamp = now,
                    gForcePeak = peakImpactG,
                    stillnessDurationMs = 2000L,
                    confidence = confidence
                )

                _lastFallEvent.value = fallEvent
                onFallDetected?.invoke(fallEvent)

                // Reset detection flags
                freeFallDetected = false
            }
        }

        // Timeout free fall state if no impact follows
        if (freeFallDetected && (now - freeFallTimestamp >= 1500)) {
            freeFallDetected = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Simulation trigger for manual preview & debug UI testing.
     */
    fun simulateFall(gForce: Float = 3.5f) {
        val now = System.currentTimeMillis()
        val fallEvent = FallEvent(
            timestamp = now,
            gForcePeak = gForce,
            stillnessDurationMs = 3000L,
            confidence = 0.92f
        )
        _lastFallEvent.value = fallEvent
        onFallDetected?.invoke(fallEvent)
    }
}
