package com.nopal.tunascreen.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTrackerTest {

    @Test
    fun `se reconoce el home completo`() {
        assertTrue(isFullyHomed("xyz"))
        assertFalse(isFullyHomed("xy"))
        assertFalse(isFullyHomed(""))
        assertFalse(isFullyHomed(null))
    }

    @Test
    fun `sin eventos no hay fase`() {
        val tracker = HomeTracker()
        assertNull(tracker.update("xyz", 0))
        assertNull(tracker.update("xyz", 1_000))
    }

    @Test
    fun `perder los ejes es haciendo home y recuperarlos es completado`() {
        val tracker = HomeTracker()
        tracker.update("xyz", 0)
        assertEquals(HomePhase.HOMING, tracker.update("", 1_000))
        assertEquals(HomePhase.HOMING, tracker.update("xy", 5_000))
        assertEquals(HomePhase.DONE, tracker.update("xyz", 9_000))
        assertEquals(HomePhase.DONE, tracker.update("xyz", 12_000))
        assertNull(tracker.update("xyz", 15_000))
    }

    @Test
    fun `al pedir home se muestra en curso aunque aun no se vea el cambio`() {
        val tracker = HomeTracker()
        tracker.update("xyz", 0)
        tracker.onRequest(1_000)
        assertEquals(HomePhase.HOMING, tracker.update("xyz", 1_500))
        // Klipper vacía los ejes al empezar el G28 y los repone al terminar.
        assertEquals(HomePhase.HOMING, tracker.update("", 2_000))
        assertEquals(HomePhase.DONE, tracker.update("xyz", 20_000))
    }

    @Test
    fun `si el home pedido no cambia nada se da por completado tras un rato`() {
        val tracker = HomeTracker()
        tracker.update("xyz", 0)
        tracker.onRequest(1_000)
        assertEquals(HomePhase.HOMING, tracker.update("xyz", 3_000))
        assertEquals(HomePhase.DONE, tracker.update("xyz", 5_500))
    }

    @Test
    fun `homing desde ejes sin referenciar termina en completado`() {
        val tracker = HomeTracker()
        tracker.update("", 0)
        tracker.onRequest(500)
        assertEquals(HomePhase.HOMING, tracker.update("", 1_000))
        assertEquals(HomePhase.DONE, tracker.update("xyz", 8_000))
    }

    @Test
    fun `una maquina sin homed_axes no genera fases`() {
        val tracker = HomeTracker()
        tracker.onRequest(0)
        assertNull(tracker.update(null, 1_000))
    }
}
