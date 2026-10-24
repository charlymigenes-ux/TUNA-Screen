package com.nopal.tunascreen.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MachineDecodingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `los segundos con decimales no tumban el estado de la maquina`() {
        // Caso real: el servidor mandó "elapsed_s": 18404.524883615995 y la app se quedó sin máquinas.
        val machine = json.decodeFromString<Machine>(
            """{"id":"klipper:7125","name":"ET4","type":"printer","online":true,
                "status":{"state":"completed","job":{"filename":"pieza.gcode","percent":100,
                "elapsed_s":18404.524883615995,"remaining_s":0.4}}}""",
        )

        val job = machine.status.job!!
        assertEquals(18404L, job.elapsedS)
        assertEquals(0L, job.remainingS)
    }

    @Test
    fun `los segundos enteros y los ausentes siguen funcionando`() {
        val job = json.decodeFromString<JobProgress>("""{"filename":"a.gcode","percent":5,"elapsed_s":90}""")
        assertEquals(90L, job.elapsedS)
        assertNull(job.remainingS)
    }

    @Test
    fun `homed_axes llega como texto`() {
        val machine = json.decodeFromString<Machine>(
            """{"id":"klipper:7125","name":"ET4","type":"printer","status":{"homed_axes":"xyz"}}""",
        )
        assertEquals("xyz", machine.status.homedAxes)
    }
}
