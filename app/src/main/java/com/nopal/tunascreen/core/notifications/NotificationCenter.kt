package com.nopal.tunascreen.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nopal.tunascreen.MainActivity
import com.nopal.tunascreen.R
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.ThermalPhase
import com.nopal.tunascreen.core.model.thermalPhaseOf
import com.nopal.tunascreen.core.repository.NopalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private val PRINTING_STATES = setOf("printing", "running", "working")

/** Un aviso listo para mostrar. */
data class MachineAlert(val event: AlertEvent, val machineId: String, val title: String, val text: String)

/**
 * Detecta cambios de estado en las máquinas (el snapshot por WebSocket ya llega
 * al repositorio) y los avisa según las preferencias del usuario: notificación
 * visual, sonido y vibración, por tipo de evento.
 *
 * Solo funciona mientras el proceso de la app está vivo: con la app cerrada no
 * hay conexión con NOPAL, así que tampoco avisos.
 */
@Singleton
class NotificationCenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NopalRepository,
    private val store: NotificationStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previous: Map<String, Machine> = emptyMap()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            repository.machines.collect { machines -> handle(machines) }
        }
    }

    /** Aviso de prueba con las preferencias actuales (ignora qué eventos están activos). */
    fun sendTest() {
        scope.launch {
            val prefs = store.prefs.first()
            deliver(
                prefs,
                MachineAlert(AlertEvent.JOB_FINISHED, "test", "Prueba de aviso", "Así se verá un aviso de TUNA-Screen."),
            )
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private suspend fun handle(machines: List<Machine>) {
        val before = previous
        previous = machines.associateBy { it.id }
        // Sin estado previo de esa máquina (primer snapshot) no hay cambio que avisar.
        val changes = machines.flatMap { machine ->
            before[machine.id]?.let { alertsFor(it, machine) }.orEmpty()
        }
        if (changes.isEmpty()) return
        val prefs = store.prefs.first()
        if (!prefs.enabled) return
        changes.filter { it.event in prefs.events }.forEach { deliver(prefs, it) }
    }

    private fun deliver(prefs: NotificationPrefs, alert: MachineAlert) {
        if (prefs.visual) {
            if (hasPermission()) postNotification(prefs, alert)
        } else {
            // Sin notificación visual, sonido y vibración se disparan directo.
            if (prefs.sound) {
                runCatching {
                    RingtoneManager
                        .getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        ?.play()
                }
            }
            if (prefs.vibrate) vibrate()
        }
    }

    private fun postNotification(prefs: NotificationPrefs, alert: MachineAlert) {
        val channelId = ensureChannel(prefs.sound, prefs.vibrate)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alert.title)
            .setContentText(alert.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching {
            NotificationManagerCompat.from(context)
                .notify("${alert.machineId}:${alert.event.storageKey}".hashCode(), notification)
        }
    }

    // El sonido y la vibración de un canal no se pueden cambiar después de crearlo,
    // así que hay un canal por combinación.
    private fun ensureChannel(sound: Boolean, vibrate: Boolean): String {
        val id = "alerts_${if (sound) "s" else "x"}${if (vibrate) "v" else "x"}"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(id, "Avisos de máquinas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cambios de estado de impresoras, láseres y CNC"
                enableVibration(vibrate)
                if (!sound) setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        return id
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    internal companion object {
        fun alertsFor(before: Machine, after: Machine): List<MachineAlert> {
            val alerts = mutableListOf<MachineAlert>()

            fun add(event: AlertEvent, text: String) {
                alerts.add(MachineAlert(event, after.id, "${after.name}: ${event.title}", text))
            }

            if (before.online && !after.online) {
                add(AlertEvent.MACHINE_OFFLINE, "${after.name} dejó de responder.")
            }
            if (!after.online) return alerts

            val was = before.status.state
            val now = after.status.state
            val file = (after.status.job?.filename ?: before.status.job?.filename)
                ?.let { " ($it)" }.orEmpty()
            if (now == "error" && was != "error") add(AlertEvent.JOB_ERROR, "${after.name} reporta un error.")
            if (now == "paused" && was != "paused") add(AlertEvent.JOB_PAUSED, "El trabajo$file está en pausa.")
            val finished = (now == "completed" && was != "completed") ||
                (was in PRINTING_STATES && now in setOf("idle", "ready", "standby") &&
                    (before.status.job?.percent ?: 0) >= 98)
            if (finished) add(AlertEvent.JOB_FINISHED, "El trabajo$file terminó.")

            val heaters = listOf(
                Triple("Extrusor", before.status.hotend, after.status.hotend),
                Triple("Cama", before.status.bed, after.status.bed),
            )
            heaters.forEach { (name, b, a) ->
                val phaseBefore = thermalPhaseOf(b)
                val phaseAfter = thermalPhaseOf(a)
                if (phaseBefore == ThermalPhase.HEATING && phaseAfter == ThermalPhase.READY) {
                    add(AlertEvent.HEATING_READY, "$name llegó a ${a?.current?.roundToInt()}°C.")
                }
                if (phaseBefore == ThermalPhase.COOLING && phaseAfter == ThermalPhase.IDLE) {
                    add(AlertEvent.COOLED_DOWN, "$name ya se enfrió (${a?.current?.roundToInt()}°C).")
                }
            }
            return alerts
        }
    }
}
