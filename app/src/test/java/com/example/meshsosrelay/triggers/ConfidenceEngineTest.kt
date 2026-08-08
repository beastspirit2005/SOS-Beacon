package com.example.meshsosrelay.triggers

import com.example.meshsosrelay.sensors.FallEvent
import com.example.meshsosrelay.sensors.ScreamEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceEngineTest {

    @Test
    fun testManualTriggerYieldsMaxConfidenceAndCriticalSeverity() {
        val score = ConfidenceEngine.calculateScore(manualTrigger = true)

        assertEquals(1.0f, score.confidence, 0.001f)
        assertEquals("critical", score.severity)
        assertEquals("manual", score.triggerType)
    }

    @Test
    fun testFallEventConfidenceCalculation() {
        val fallEvent = FallEvent(
            gForcePeak = 3.5f,
            stillnessDurationMs = 2500L,
            confidence = 0.85f
        )
        val score = ConfidenceEngine.calculateScore(fallEvent = fallEvent)

        assertEquals(0.85f, score.confidence, 0.001f)
        assertEquals("critical", score.severity)
        assertEquals("fall_detection", score.triggerType)
    }

    @Test
    fun testFallAndScreamFusionBoostsConfidence() {
        val fallEvent = FallEvent(gForcePeak = 3.0f, stillnessDurationMs = 2000L, confidence = 0.80f)
        val screamEvent = ScreamEvent(decibels = 85f, confidence = 0.85f)

        val score = ConfidenceEngine.calculateScore(
            fallEvent = fallEvent,
            screamEvent = screamEvent
        )

        assertTrue(score.confidence >= 0.90f)
        assertEquals("critical", score.severity)
        assertEquals("fall_scream_fusion", score.triggerType)
    }

    @Test
    fun testDeadManTimerYieldsWarnSeverity() {
        val score = ConfidenceEngine.calculateScore(isDeadManTimer = true)

        assertEquals(0.75f, score.confidence, 0.001f)
        assertEquals("warn", score.severity)
        assertEquals("dead_man_timer", score.triggerType)
    }
}
