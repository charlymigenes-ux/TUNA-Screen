package com.nopal.tunascreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo 1:1 de las capabilities que devuelve NOPAL en
 * GET /api/tunascreen/machines (ver backend/services/tunascreen_service.py,
 * que emite strings en minúsculas/snake_case -- de ahí el @SerialName en
 * cada entrada, kotlinx.serialization no convierte mayúsculas/minúsculas
 * por sí solo).
 *
 * Solo se listan acá las que YA tienen una acción real detrás en NOPAL --
 * no hay "aspiracionales" (ej. Klipper no tiene MOVEMENT todavía, ver el
 * docstring de tunascreen_service.py para el detalle por marca).
 */
@Serializable
enum class Capability {
    @SerialName("temperature") TEMPERATURE,
    @SerialName("movement") MOVEMENT,
    @SerialName("extrusion") EXTRUSION,
    @SerialName("camera") CAMERA,
    @SerialName("lights") LIGHTS,
    @SerialName("spool") SPOOL,
    @SerialName("laser_power") LASER_POWER,
    @SerialName("air_assist") AIR_ASSIST,
    @SerialName("exhaust") EXHAUST,
    @SerialName("spindle") SPINDLE,
    @SerialName("spindle_rpm") SPINDLE_RPM,
    @SerialName("coolant") COOLANT,
    @SerialName("probe") PROBE,
    @SerialName("limits") LIMITS,
    @SerialName("fan") FAN,
    @SerialName("speed_override") SPEED_OVERRIDE,
    @SerialName("flow_override") FLOW_OVERRIDE,
    @SerialName("z_offset") Z_OFFSET,
    @SerialName("endstops") ENDSTOPS,
    @SerialName("macros") MACROS,
    @SerialName("console") CONSOLE,

    /** Capability nueva del lado NOPAL que esta versión de la app todavía no
     * conoce -- kotlinx.serialization con `JsonBuilder.ignoreUnknownKeys`
     * NO cubre valores de enum desconocidos por defecto, así que este caso
     * se maneja explícito en NopalApiService (ver su comentario) en vez de
     * crashear al deserializar. */
    UNKNOWN,
}
