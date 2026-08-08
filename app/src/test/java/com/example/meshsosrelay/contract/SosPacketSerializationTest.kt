package com.example.meshsosrelay.contract

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SosPacketSerializationTest {

    @Test
    fun testSerializationWithDefaultPriority() {
        val packet = SosPacket(
            msg_id = "msg123",
            origin_id = "origin456",
            created_at = 123456789L,
            lat = 12.34,
            lon = 56.78,
            acc = 10.0f,
            severity = "critical",
            confidence = 0.9f,
            trigger_type = "manual",
            ttl = 3,
            hops = 1,
            payload = "Test payload",
            sig = "signature"
        )
        
        // Assert priority defaults to 3
        assertEquals(3, packet.priority)

        // Serialize to JSON
        val json = Json { encodeDefaults = true }
        val jsonString = json.encodeToString(SosPacket.serializer(), packet)
        
        // Assert priority is present in serialized output
        assertTrue("Serialized output should contain priority", jsonString.contains("\"priority\":3"))
    }

    @Test
    fun testDeserializationOfOldPacketWithoutPriority() {
        val oldJson = """
            {
                "msg_id": "msg123",
                "origin_id": "origin456",
                "created_at": 123456789,
                "lat": 12.34,
                "lon": 56.78,
                "acc": 10.0,
                "severity": "critical",
                "confidence": 0.9,
                "trigger_type": "manual",
                "ttl": 3,
                "hops": 1,
                "payload": "Test payload",
                "sig": "signature"
            }
        """.trimIndent()

        val packet = Json.decodeFromString(SosPacket.serializer(), oldJson)

        // Assert priority defaults to 3 during deserialization of old packets
        assertEquals(3, packet.priority)
    }
}
