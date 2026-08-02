package com.nopal.tunascreen.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaterialSpool(
    val id: Int,
    val name: String,
    val material: String,
    val vendor: String? = null,
    @SerialName("color_hex") val colorHex: String? = null,
    @SerialName("remaining_g") val remainingG: Double = 0.0,
    @SerialName("reserved_g") val reservedG: Double = 0.0,
    @SerialName("available_g") val availableG: Double = 0.0,
    @SerialName("initial_g") val initialG: Double? = null,
    val archived: Boolean = false,
)

@Serializable
data class MaterialsResponse(
    val available: Boolean = false,
    val reason: String? = null,
    val spools: List<MaterialSpool> = emptyList(),
    val links: Map<String, Int> = emptyMap(),
)

@Serializable
data class ActiveMaterialRequest(
    @SerialName("machine_id") val machineId: String,
    @SerialName("spool_id") val spoolId: Int? = null,
)

@Serializable
data class WorkshopAccessory(
    val id: String,
    val name: String,
    val kind: String = "other",
    val driver: String = "unknown",
    val online: Boolean = false,
    val on: Boolean? = null,
)

@Serializable
data class AccessoryScene(
    val id: String,
    val name: String,
    @SerialName("action_count") val actionCount: Int = 0,
)

@Serializable
data class AccessoriesResponse(
    val available: Boolean = false,
    val reason: String? = null,
    val accessories: List<WorkshopAccessory> = emptyList(),
    val scenes: List<AccessoryScene> = emptyList(),
)

@Serializable
data class AccessoryPowerRequest(val on: Boolean)

@Serializable
data class ResourceActionResponse(val success: Boolean = false)
