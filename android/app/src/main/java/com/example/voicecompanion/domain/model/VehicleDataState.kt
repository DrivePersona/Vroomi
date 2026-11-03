package com.example.voicecompanion.domain.model

enum class VehicleDomain { MOTION, CONVENIENCE, SAFETY }

enum class VehicleValueType { BOOLEAN, ENUM }

enum class VehicleAvailability { CHECKING, ACTIVE, UNSUPPORTED, ERROR }

data class VehicleDataDefinition(
    val dataKey: String,
    val displayName: String,
    val domain: VehicleDomain,
    val capability: String,
    val permission: String,
    val valueType: VehicleValueType,
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

    val definitions = listOf(
        VehicleDataDefinition(BRAKE, "브레이크 페달", VehicleDomain.MOTION, "checkBrakePedalStatusCapabilities", "pleos.car.permission.CAR_DRIVE", VehicleValueType.BOOLEAN),
        VehicleDataDefinition(TURN_SIGNAL, "방향지시등", VehicleDomain.CONVENIENCE, "checkTurnSignalStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM),
        VehicleDataDefinition(DRIVING_MODE, "주행 모드", VehicleDomain.MOTION, "checkDrivingModeStateCapabilities", "pleos.car.permission.CAR_DRIVE", VehicleValueType.ENUM),
        VehicleDataDefinition(SEAT_BELT_LEFT, "운전석 안전벨트 경고", VehicleDomain.SAFETY, "checkSeatNotBuckledWarningCapabilities", "pleos.car.permission.CAR_SAFETY", VehicleValueType.BOOLEAN),
        VehicleDataDefinition(SEAT_BELT_RIGHT, "동승석 안전벨트 경고", VehicleDomain.SAFETY, "checkSeatNotBuckledWarningCapabilities", "pleos.car.permission.CAR_SAFETY", VehicleValueType.BOOLEAN),
    )

    val initialState: List<VehicleDataState> = definitions.map { VehicleDataState(it, VehicleAvailability.CHECKING) }
}
