package com.nopal.tunascreen.feature.kiosk

import android.app.Activity

/**
 * TODO (fuera del alcance del MVP, ver plan): soporte real de "pantalla
 * dedicada" -- una tablet de 5" atornillada a una impresora que arranca
 * Android, abre TUNA-Screen solo, conecta con NOPAL, entra directo a esa
 * máquina asignada y opcionalmente no deja salir a Modo Taller.
 *
 * Piezas que faltan para que esto sea real (no solo el nombre de la
 * clase):
 * - Guardar en TokenStore (o una prefs separada) cuál `machineId` es la
 *   "asignada" de este dispositivo, para que MainActivity la abra directo
 *   en vez de Modo Taller.
 * - `startLockTask()`/`stopLockTask()` (Activity.setLockTaskEnabled o el
 *   Device Owner API completo si el dispositivo se gestiona por MDM) para
 *   el modo "no dejar salir".
 * - Un ajuste de Settings visible en la app para activar/desactivar este
 *   modo y elegir la máquina asignada.
 */
object KioskModeManager {
    fun enableLockTaskIfConfigured(activity: Activity) {
        // No-op por ahora -- ver TODO arriba.
    }
}
