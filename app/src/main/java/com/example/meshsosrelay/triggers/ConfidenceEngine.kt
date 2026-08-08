package com.example.meshsosrelay.triggers

import com.example.meshsosrelay.sensors.FallEvent
import com.example.meshsosrelay.sensors.ScreamEvent

data class ConfidenceScore(
    val confidence: Float,       // 0.0f to 1.0f
    val severity: String,         // "info" | "warn" | "critical"
    val triggerType: String       // "manual" | "fall_detection" | "scream_detection" | "dead_man_timer"
)

object ConfidenceEngine {

    /**
     * Calculates the on-device confidence score based on sensor inputs and trigger origin.
     */
    fun calculateScore(
        manualTrigger: Boolean = false,
        fallEvent: FallEvent? = null,
        screamEvent: ScreamEvent? = null,
        isDeadManTimer: Boolean = false,
        payloadLength: Int = 0
    ): ConfidenceScore {

        var rawConfidence = 0.0f
        var triggerType = "manual"
        var severity = "info"

        if (manualTrigger) {
            rawConfidence = 1.00f
            triggerType = "manual"
            severity = "critical"
        } else if (fallEvent != null && screamEvent != null) {
            // Multi-sensor fusion (Fall + Scream combined = extremely high confidence)
            rawConfidence = (fallEvent.confidence * 0.6f + screamEvent.confidence * 0.4f + 0.15f).coerceAtMost(1.0f)
            triggerType = "fall_scream_fusion"
            severity = "critical"
        } else if (fallEvent != null) {
            rawConfidence = fallEvent.confidence
            triggerType = "fall_detection"
            severity = if (fallEvent.gForcePeak > 3.2f) "critical" else "warn"
        } else if (screamEvent != null) {
            rawConfidence = screamEvent.confidence
            triggerType = "scream_detection"
            severity = if (screamEvent.decibels > 82f) "critical" else "warn"
        } else if (isDeadManTimer) {
            rawConfidence = 0.75f
            triggerType = "dead_man_timer"
            severity = "warn"
        }

        // Adjust for non-empty text input payload
        if (payloadLength > 0 && !manualTrigger) {
            rawConfidence = (rawConfidence + 0.1f).coerceAtMost(1.0f)
        }

        val clampedConfidence = rawConfidence.coerceIn(0.0f, 1.0f)

        return ConfidenceScore(
            confidence = clampedConfidence,
            severity = severity,
            triggerType = triggerType
        )
    }
}
