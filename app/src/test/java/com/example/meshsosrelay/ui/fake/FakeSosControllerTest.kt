package com.example.meshsosrelay.ui.fake

import com.example.meshsosrelay.contract.DeliveryState
import com.example.meshsosrelay.contract.MeshState
import com.example.meshsosrelay.contract.SosDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeSosControllerTest {

    @Test
    fun testInitialState() = runBlocking {
        val controller = FakeSosController()
        assertEquals(MeshState.Idle, controller.meshState.value)
        assertEquals(DeliveryState.Idle, controller.deliveryState.value)
    }

    @Test
    fun testTriggerStateTransitions() = runBlocking {
        val controller = FakeSosController()
        controller.trigger(SosDraft("critical", "Test message"))
        
        // Sleep real time to allow Dispatchers.Default coroutine to execute
        Thread.sleep(200)
        
        assertEquals(DeliveryState.Pending, controller.deliveryState.value)
        assertEquals(MeshState.Searching(peers = 2), controller.meshState.value)
    }
}
