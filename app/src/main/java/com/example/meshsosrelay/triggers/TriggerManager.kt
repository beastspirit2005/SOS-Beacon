package com.example.meshsosrelay.triggers

import android.content.Context
import com.example.meshsosrelay.contract.SosController
import com.example.meshsosrelay.contract.SosDraft
import com.example.meshsosrelay.sensors.FallDetector
import com.example.meshsosrelay.sensors.FallEvent
import com.example.meshsosrelay.sensors.GpsLocationManager
import com.example.meshsosrelay.sensors.ScreamDetector
import com.example.meshsosrelay.sensors.ScreamEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TriggerManager(
    private val context: Context,
    private val sosController: SosController,
    val gpsLocationManager: GpsLocationManager = GpsLocationManager(context)
) {

    val fallDetector = FallDetector(context) { fallEvent ->
        handleFallDetected(fallEvent)
    }

    val screamDetector = ScreamDetector(context) { screamEvent ->
        handleScreamDetected(screamEvent)
    }

    private val _lastTriggeredScore = MutableStateFlow<ConfidenceScore?>(null)
    val lastTriggeredScore: StateFlow<ConfidenceScore?> = _lastTriggeredScore.asStateFlow()

    private val _activeDraft = MutableStateFlow<SosDraft?>(null)
    val activeDraft: StateFlow<SosDraft?> = _activeDraft.asStateFlow()

    fun startAutomatedSensors() {
        gpsLocationManager.startLocationUpdates()
        fallDetector.start()
        screamDetector.start()
    }

    fun stopAutomatedSensors() {
        gpsLocationManager.stopLocationUpdates()
        fallDetector.stop()
        screamDetector.stop()
    }

    fun triggerManualSos(customMessage: String = "Manual Emergency SOS Triggered") {
        val score = ConfidenceEngine.calculateScore(
            manualTrigger = true,
            payloadLength = customMessage.length
        )
        _lastTriggeredScore.value = score

        val draft = SosDraft(
            severity = score.severity,
            payload = customMessage
        )
        _activeDraft.value = draft
        sosController.trigger(draft)
    }

    private fun handleFallDetected(fallEvent: FallEvent) {
        val score = ConfidenceEngine.calculateScore(
            fallEvent = fallEvent
        )
        _lastTriggeredScore.value = score

        val draft = SosDraft(
            severity = score.severity,
            payload = "Automated Fall Detected! Peak Impact: ${String.format("%.1f", fallEvent.gForcePeak)}g"
        )
        _activeDraft.value = draft
        sosController.trigger(draft)
    }

    private fun handleScreamDetected(screamEvent: ScreamEvent) {
        val score = ConfidenceEngine.calculateScore(
            screamEvent = screamEvent
        )
        _lastTriggeredScore.value = score

        val draft = SosDraft(
            severity = score.severity,
            payload = "Acoustic Distress Scream Detected! Intensity: ${String.format("%.1f", screamEvent.decibels)} dB"
        )
        _activeDraft.value = draft
        sosController.trigger(draft)
    }

    fun triggerDeadManTimer() {
        val score = ConfidenceEngine.calculateScore(
            isDeadManTimer = true
        )
        _lastTriggeredScore.value = score

        val draft = SosDraft(
            severity = score.severity,
            payload = "Automated Missed Check-in / Dead-Man Timer Expired"
        )
        _activeDraft.value = draft
        sosController.trigger(draft)
    }
}
