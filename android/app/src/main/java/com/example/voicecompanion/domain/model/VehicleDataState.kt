package com.example.voicecompanion.domain.model

enum class VehicleDomain { MOTION, CONVENIENCE, SAFETY }

enum class VehicleValueType { BOOLEAN, ENUM, INTEGER, COLOR }

enum class VehicleReactionPolicy { CHANGE, BRIGHTNESS_BUCKET, POSITION_BUCKET }

enum class VehicleAvailability { CHECKING, ACTIVE, UNSUPPORTED, ERROR }

data class VehicleDataDefinition(
    val dataKey: String,
    val displayName: String,
    val domain: VehicleDomain,
    val capability: String,
    val permission: String,
    val valueType: VehicleValueType,
    val reactionEvent: VehicleEvent? = null,
    val reactionPolicy: VehicleReactionPolicy? = null,
    val cooldownMs: Long = 2_500,
    val allowWhileDriving: Boolean = true,
)

data class VehicleDataState(
    val definition: VehicleDataDefinition,
    val availability: VehicleAvailability,
    val value: String? = null,
    val updatedAtEpochMs: Long? = null,
    val detail: String? = null,
)

object VehicleDataCatalog {
    const val BRAKE = "brake-pedal-status"
    const val TURN_SIGNAL = "turn-signal-state"
    const val DRIVING_MODE = "driving-mode-state"
    const val SEAT_BELT_LEFT = "seat-belt-not-buckled-warning:ROW_1_LEFT"
    const val SEAT_BELT_RIGHT = "seat-belt-not-buckled-warning:ROW_1_RIGHT"

    fun convenience(
        dataKey: String,
        displayName: String,
        domain: VehicleDomain,
        capability: String,
        permission: String,
        valueType: VehicleValueType,
        event: VehicleEvent,
        policy: VehicleReactionPolicy = VehicleReactionPolicy.CHANGE,
    ) = VehicleDataDefinition(
        dataKey = dataKey,
        displayName = displayName,
        domain = domain,
        capability = capability,
        permission = permission,
        valueType = valueType,
        reactionEvent = event,
        reactionPolicy = policy,
        allowWhileDriving = false,
    )

    val definitions = listOf(
        VehicleDataDefinition(BRAKE, "브레이크 페달", VehicleDomain.MOTION, "checkBrakePedalStatusCapabilities", "pleos.car.permission.CAR_DRIVE", VehicleValueType.BOOLEAN),
        VehicleDataDefinition(TURN_SIGNAL, "방향지시등", VehicleDomain.CONVENIENCE, "checkTurnSignalStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM),
        VehicleDataDefinition(DRIVING_MODE, "주행 모드", VehicleDomain.MOTION, "checkDrivingModeStateCapabilities", "pleos.car.permission.CAR_DRIVE", VehicleValueType.ENUM),
        VehicleDataDefinition(SEAT_BELT_LEFT, "운전석 안전벨트 경고", VehicleDomain.SAFETY, "checkSeatNotBuckledWarningCapabilities", "pleos.car.permission.CAR_SAFETY", VehicleValueType.BOOLEAN),
        VehicleDataDefinition(SEAT_BELT_RIGHT, "동승석 안전벨트 경고", VehicleDomain.SAFETY, "checkSeatNotBuckledWarningCapabilities", "pleos.car.permission.CAR_SAFETY", VehicleValueType.BOOLEAN),
    )

    val initialState: List<VehicleDataState> = definitions.map { VehicleDataState(it, VehicleAvailability.CHECKING) }
}
