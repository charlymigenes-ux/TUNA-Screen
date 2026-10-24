package com.nopal.tunascreen.core.model

/** Progreso de un home: en curso o recién completado. */
enum class HomePhase { HOMING, DONE }

/** `true` cuando Klipper reporta X, Y y Z como referenciados. */
fun isFullyHomed(homedAxes: String?): Boolean =
    homedAxes != null && "x" in homedAxes.lowercase() && "y" in homedAxes.lowercase() && "z" in homedAxes.lowercase()

/**
 * Deduce el progreso de un home a partir de `homed_axes`: Klipper vacía los
 * ejes al empezar un G28 y los vuelve a reportar al terminar. Sirve también
 * para homes que no se pidieron desde esta app (macros, otra pantalla).
 *
 * Cuando el usuario lo pide desde la app se marca con [onRequest]; así se
 * muestra "haciendo home" aunque los ejes ya estuvieran en home o no se haya
 * visto aún el cambio, y se da por terminado en cuanto hay evidencia
 * (los ejes se perdieron y volvieron, o pasó [settleMs] sin ver cambios).
 */
class HomeTracker(
    private val doneForMs: Long = 5_000,
    private val requestWindowMs: Long = 120_000,
    private val settleMs: Long = 4_000,
) {
    private var lastHomed: String? = null
    private var requestedAt: Long? = null
    private var sawDrop = false
    private var doneUntil = 0L

    fun onRequest(now: Long) {
        requestedAt = now
        sawDrop = false
    }

    fun update(homedAxes: String?, now: Long): HomePhase? {
        if (homedAxes == null) {
            lastHomed = null
            return null
        }
        val before = lastHomed
        lastHomed = homedAxes
        val full = isFullyHomed(homedAxes)
        val pending = requestedAt?.takeIf { now - it < requestWindowMs }
        if (pending == null) requestedAt = null
        if (before != null && isFullyHomed(before) && !full) sawDrop = true

        if (full) {
            val cameFromUnhomed = before != null && !isFullyHomed(before)
            val settled = pending != null && now - pending >= settleMs
            if (sawDrop || cameFromUnhomed || settled) {
                doneUntil = now + doneForMs
                requestedAt = null
                sawDrop = false
            } else if (pending != null) {
                return HomePhase.HOMING
            }
        } else if (pending != null || sawDrop) {
            return HomePhase.HOMING
        }
        return if (now < doneUntil) HomePhase.DONE else null
    }
}
