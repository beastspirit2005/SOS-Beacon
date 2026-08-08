package com.example.meshsosrelay.mesh

import com.example.meshsosrelay.contract.SosDraft
import com.example.meshsosrelay.contract.MeshState
import com.example.meshsosrelay.contract.DeliveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshSosControllerTest {

    @Test
    fun testRealMeshControllerTriggerFlow() {
        val controller = MeshSosController()
        
        // Assert initial state is Idle
        assertEquals(MeshState.Idle, controller.meshState.value)
        assertEquals(DeliveryState.Idle, controller.deliveryState.value)

        // Trigger SOS draft
        val draft = SosDraft(severity = "critical", payload = "Test mesh emergency message")
        controller.trigger(draft)

        // Assert state transitions to real mesh path (InFlight / Searching)
        val finalMeshState = controller.meshState.value
        assertTrue(finalMeshState is MeshState.InFlight)
        assertEquals(3, (finalMeshState as MeshState.InFlight).peers)
        assertEquals(2, finalMeshState.hops)

        assertEquals(DeliveryState.Pending, controller.deliveryState.value)
    }

    @Test
    fun testRealMeshControllerRoleCycling() {
        val controller = MeshSosController()
        assertEquals("observer", controller.deviceRole.value)

        controller.cycleDeviceRole()
        assertEquals("relay", controller.deviceRole.value)

        controller.cycleDeviceRole()
        assertEquals("gateway", controller.deviceRole.value)

        controller.cycleDeviceRole()
        assertEquals("victim", controller.deviceRole.value)

        controller.cycleDeviceRole()
        assertEquals("observer", controller.deviceRole.value)
    }
}
