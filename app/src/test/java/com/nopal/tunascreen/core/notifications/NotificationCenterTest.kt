package com.nopal.tunascreen.core.notifications

import com.nopal.tunascreen.core.model.JobProgress
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachineStatus
import com.nopal.tunascreen.core.model.MachineType
import com.nopal.tunascreen.core.model.TempPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCenterTest {

    private fun machine(
        state: String = "idle",
        online: Boolean = true,
        hotend: TempPair? = null,
        bed: TempPair? = null,
        job: JobProgress? = null,
        homedAxes: String? = null,
    ) = Machine(
        id = "klipper:7125",
        name = "ET4",
        type = MachineType.PRINTER,
        online = online,
        status = MachineStatus(state = state, hotend = hotend, bed = bed, job = job, homedAxes = homedAxes),
    )

    private fun events(before: Machine, after: Machine) =
        NotificationCenter.alertsFor(before, after).map { it.event }

    @Test
    fun `terminar un trabajo avisa una sola vez`() {
        val before = machine("printing", job = JobProgress("pieza.gcode", 100))
        assertEquals(listOf(AlertEvent.JOB_FINISHED), events(before, machine("completed")))
        assertTrue(events(machine("completed"), machine("completed")).isEmpty())
    }

    @Test
    fun `cancelar a mitad de trabajo no se avisa como terminado`() {
        val before = machine("printing", job = JobProgress("pieza.gcode", 40))
        assertTrue(events(before, machine("idle")).isEmpty())
    }

    @Test
    fun `pausa y error se avisan al cambiar de estado`() {
        assertEquals(listOf(AlertEvent.JOB_PAUSED), events(machine("printing"), machine("paused")))
        assertEquals(listOf(AlertEvent.JOB_ERROR), events(machine("printing"), machine("error")))
    }

    @Test
    fun `calentamiento listo y enfriado se avisan por calentador`() {
        val heating = machine(hotend = TempPair(150.0, 200.0))
        val ready = machine(hotend = TempPair(199.0, 200.0))
        assertEquals(listOf(AlertEvent.HEATING_READY), events(heating, ready))

        val cooling = machine(hotend = TempPair(60.0, 0.0))
        val cold = machine(hotend = TempPair(30.0, 0.0))
        assertEquals(listOf(AlertEvent.COOLED_DOWN), events(cooling, cold))
    }

    @Test
    fun `terminar un home avisa solo al pasar de sin home a todos los ejes`() {
        assertEquals(
            listOf(AlertEvent.HOME_DONE),
            events(machine(homedAxes = ""), machine(homedAxes = "xyz")),
        )
        assertTrue(events(machine(homedAxes = "xyz"), machine(homedAxes = "xyz")).isEmpty())
        // Una máquina que no reporta ejes referenciados no genera el aviso.
        assertTrue(events(machine(), machine(homedAxes = "xyz")).isEmpty())
    }

    @Test
    fun `perder la conexion avisa y no genera otros eventos`() {
        val before = machine("printing")
        assertEquals(listOf(AlertEvent.MACHINE_OFFLINE), events(before, machine("offline", online = false)))
    }
}
