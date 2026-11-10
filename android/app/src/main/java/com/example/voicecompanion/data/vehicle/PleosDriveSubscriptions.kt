package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.CruiseControlStateListener
import ai.pleos.playground.vehicle.listener.CruiseControlTargetSpeedListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleValueType
import java.util.Locale

/**
 * Capability-gated, read-only driving signals (CAR_DRIVE): steering wheel angle, odometer, remaining range,
 * cruise control state and target speed, and gear selection.
 *
 * Values are only recorded in [VehicleStateRepository]. No reaction event or policy is attached, so none of these
 * signals makes the companion speak or gesture. Every definition is registered up front (see [PleosLampSubscriptions]).
 */
internal class PleosDriveSubscriptions(
    private val vehicle: Vehicle,
    private val states: VehicleStateRepository,
    private val capabilities: VehicleCapabilityRequests,
    addUndo: (() -> Unit) -> Unit,
    private val onFailure: (String, Exception) -> Unit,
    stopped: () -> Boolean,
) {
    private val wheel by lazy { vehicle.getSteeringWheel() }
    private val odometer by lazy { vehicle.getOdometer() }
    private val drive by lazy { vehicle.getDrivingMode() }
    private val gear by lazy { vehicle.getGear() }
    private val subscriptions = VehicleAreaSubscriptionRegistry(states, stopped, addUndo, onFailure)

    fun start() {
        definitions.forEach(states::registerDefinition)
        watch(STEERING_ANGLE,
            check = { done, failed -> wheel.checkSteeringWheelAngleCapabilities({ config, _ -> done(config.accessType.canRead) }, failed) }) { definition ->
            register(
                object : SteeringWheelAngleListener {
                    override fun onSteeringWheelAngleUpdated(value: Float?) = observe(definition, value)
                    override fun onFailed(e: Exception) = failed(definition, e)
                },
                { wheel.registerSteeringWheelAngle(it) },
                { wheel.unregisterSteeringWheelAngle(it) },
            )
        }
        watch(DISTANCE_DRIVEN,
            check = { done, failed -> odometer.checkCurrentDistanceDrivenCapabilities({ config -> done(config.accessType.canRead) }, failed) }) { definition ->
            register(
                object : DistanceDrivenListener {
                    override fun onDistanceDrivenUpdated(value: Float?) = observe(definition, value)
                    override fun onFailed(e: Exception) = failed(definition, e)
                },
                { odometer.registerDistanceDriven(it) },
                { odometer.unregisterDistanceDriven(it) },
            )
        }
        watch(CRUISE_STATE,
            check = { done, failed -> drive.checkCruiseControlStateCapabilities({ config -> done(config.accessType.canRead) }, failed) }) { definition ->
            register(
                object : CruiseControlStateListener {
                    override fun onCruiseControlStateUpdated(value: CruiseControl?) = states.observe(definition.dataKey, value?.name)
                    override fun onFailed(e: Exception) = failed(definition, e)
                },
                { drive.registerCruiseControlState(it) },
                { drive.unregisterCruiseControlState(it) },
            )
        }
        watch(CRUISE_TARGET_SPEED,
            check = { done, failed -> drive.checkCruiseControlTargetSpeedCapabilities({ config -> done(config.accessType.canRead) }, failed) }) { definition ->
            register(
                object : CruiseControlTargetSpeedListener {
                    override fun onCruiseControlTargetSpeedUpdated(value: Float?) = observe(definition, value)
                    override fun onFailed(e: Exception) = failed(definition, e)
                },
                { drive.registerCruiseControlTargetSpeed(it) },
                { drive.unregisterCruiseControlTargetSpeed(it) },
            )
        }
        subscribeRangeRemaining()
        subscribeGear()
    }

    /**
     * Vehicle SDK 2.3.2 has no capability check for remaining range or gear, so they are registered directly and
     * judged by whether a value arrives. The SDK marks both listeners deprecated in favour of the Google
     * RANGE_REMAINING / GEAR_SELECTION properties; Pleos Connect v2.1.0 still delivers values through them.
     */
    @Suppress("DEPRECATION")
    private fun subscribeRangeRemaining() {
        val definition = definitionOf(RANGE_REMAINING)
        subscriptions.subscribe(definition) {
            register(
                object : RangeRemainingListener {
                    override fun onRangeRemainingChanged(value: Float?) = observe(definition, value)
                    override fun onFailed(e: Exception) = failed(definition, e)
                },
                { odometer.registerRangeRemainingListener(it) },
                { odometer.unregisterRangeRemainingListener(it) },
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun subscribeGear() {
        val definition = definitionOf(GEAR)
        subscriptions.subscribe(definition) {
            register(
                object : GearSelectionListener {
                    override fun onSelected(value: VehicleGear?) = states.observe(definition.dataKey, value?.name)
                    override fun onFailed(e: Exception) = failed(definition, e)
                },
                { gear.registerGearSelection(it) },
                { gear.unregisterGearSelection(it) },
            )
        }
    }

    private fun watch(
        dataKey: String,
        check: ((Boolean) -> Unit, (Exception) -> Unit) -> Unit,
        startListening: (VehicleDataDefinition) -> () -> Unit,
    ) {
        val definition = definitionOf(dataKey)
        capabilities.request(definition, { complete, failed -> check(complete, failed) }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "${definition.displayName} 읽기 capability가 없어요")
            subscriptions.subscribe(definition) { startListening(definition) }
        }
    }

    /** Registers the listener and returns the closure that unregisters exactly that listener. */
    private fun <L> register(listener: L, add: (L) -> Unit, remove: (L) -> Unit): () -> Unit {
        add(listener)
        return { remove(listener) }
    }

    private fun observe(definition: VehicleDataDefinition, value: Float?) = states.observe(definition.dataKey, decimalText(value))

    private fun failed(definition: VehicleDataDefinition, error: Exception) {
        states.markError(definition.dataKey, error.message ?: error.javaClass.simpleName)
        onFailure(definition.displayName, error)
    }

    private fun definitionOf(dataKey: String) = definitions.first { it.dataKey == dataKey }

    private val AccessType.canRead get() = this == AccessType.READ || this == AccessType.READ_WRITE

    internal companion object {
        const val STEERING_ANGLE = "steering-angle"
        const val DISTANCE_DRIVEN = "distance-driven"
        const val RANGE_REMAINING = "range-remaining"
        const val CRUISE_STATE = "cruise-control-state"
        const val CRUISE_TARGET_SPEED = "cruise-control-target-speed"
        const val GEAR = "gear-selection"

        val definitions: List<VehicleDataDefinition> = listOf(
            drive(STEERING_ANGLE, "조향각", "checkSteeringWheelAngleCapabilities", VehicleValueType.DECIMAL),
            drive(DISTANCE_DRIVEN, "주행거리", "checkCurrentDistanceDrivenCapabilities", VehicleValueType.DECIMAL),
            drive(RANGE_REMAINING, "남은 주행거리", "(none) registerRangeRemainingListener", VehicleValueType.DECIMAL),
            drive(CRUISE_STATE, "크루즈 상태", "checkCruiseControlStateCapabilities", VehicleValueType.ENUM),
            drive(CRUISE_TARGET_SPEED, "크루즈 목표 속도", "checkCruiseControlTargetSpeedCapabilities", VehicleValueType.DECIMAL),
            drive(GEAR, "기어", "(none) registerGearSelection", VehicleValueType.ENUM),
        )

        private fun drive(key: String, label: String, capability: String, type: VehicleValueType) = VehicleDataDefinition(
            dataKey = key,
            displayName = label,
            domain = VehicleDomain.MOTION,
            capability = capability,
            permission = "pleos.car.permission.CAR_DRIVE",
            valueType = type,
        )
    }
}

/** Two decimals like the energy signals, with negative zero shown as plain 0.00 so a resting wheel does not flicker. */
internal fun decimalText(value: Float?): String? =
    value?.let { String.format(Locale.ROOT, "%.2f", if (it == 0f) 0f else it) }
