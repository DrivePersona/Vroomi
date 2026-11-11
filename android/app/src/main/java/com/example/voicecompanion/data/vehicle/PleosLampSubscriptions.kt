package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.listener.LightStateListener
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import com.example.voicecompanion.domain.model.VehicleValueType

/**
 * Capability-gated, read-only exterior lamp states: high beam, low beam, tail lamp, front and rear fog.
 *
 * Values are recorded in [VehicleStateRepository]. A lamp that turns ON also speaks one short line in the built-in
 * voice (no gesture, see [SignalReactionRules.turnsOn]); turning off is never spoken. Every definition is registered
 * up front so the data panel shows CHECKING until the capability answer arrives (observe() ignores unregistered keys).
 */
internal class PleosLampSubscriptions(
    private val vehicle: Vehicle,
    private val states: VehicleStateRepository,
    private val capabilities: VehicleCapabilityRequests,
    addUndo: (() -> Unit) -> Unit,
    private val onFailure: (String, Exception) -> Unit,
    stopped: () -> Boolean,
) {
    private val lights by lazy { vehicle.getLight() }
    private val subscriptions = VehicleAreaSubscriptionRegistry(states, stopped, addUndo, onFailure)

    fun start() {
        definitions.forEach(states::registerDefinition)
        watch(HIGH_BEAM,
            check = { done, failed -> lights.checkHighBeamLightStateCapabilities(done, failed) },
            register = { lights.registerHighBeamLightState(it) },
            unregister = { lights.unregisterHighBeamLightState(it) })
        watch(LOW_BEAM,
            check = { done, failed -> lights.checkLowBeamLightStateCapabilities(done, failed) },
            register = { lights.registerLowBeamLightState(it) },
            unregister = { lights.unregisterLowBeamLightState(it) })
        watch(TAIL_LAMP,
            check = { done, failed -> lights.checkTailLampLightStateCapabilities(done, failed) },
            register = { lights.registerTailLampLightState(it) },
            unregister = { lights.unregisterTailLampLightState(it) })
        watch(FRONT_FOG,
            check = { done, failed -> lights.checkFrontFogLightStateCapabilities(done, failed) },
            register = { lights.registerFrontFogLightState(it) },
            unregister = { lights.unregisterFrontFogLightState(it) })
        watch(REAR_FOG,
            check = { done, failed -> lights.checkRearFogLightStateCapabilities(done, failed) },
            register = { lights.registerRearFogLightState(it) },
            unregister = { lights.unregisterRearFogLightState(it) })
    }

    private fun watch(
        dataKey: String,
        check: ((CapabilityConfig, List<LightState>) -> Unit, (Exception) -> Unit) -> Unit,
        register: (LightStateListener) -> Unit,
        unregister: (LightStateListener) -> Unit,
    ) {
        val definition = definitions.first { it.dataKey == dataKey }
        capabilities.request(definition, { complete, failed ->
            check({ config, supported -> complete(config.accessType.canRead && supported.isNotEmpty()) }, failed)
        }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "${definition.displayName} 상태 읽기가 지원되지 않아요")
            subscriptions.subscribe(definition) {
                val listener = object : LightStateListener {
                    override fun onLightStateUpdated(value: LightState?) = states.observe(dataKey, value?.name)
                    override fun onFailed(e: Exception) = failed(definition, e)
                }
                register(listener)
                val undo: () -> Unit = { unregister(listener) }
                undo
            }
        }
    }

    private fun failed(definition: VehicleDataDefinition, error: Exception) {
        states.markError(definition.dataKey, error.message ?: error.javaClass.simpleName)
        onFailure(definition.displayName, error)
    }

    private val AccessType.canRead get() = this == AccessType.READ || this == AccessType.READ_WRITE

    internal companion object {
        const val HIGH_BEAM = "high-beam-light-state"
        const val LOW_BEAM = "low-beam-light-state"
        const val TAIL_LAMP = "tail-lamp-light-state"
        const val FRONT_FOG = "front-fog-light-state"
        const val REAR_FOG = "rear-fog-light-state"

        val definitions: List<VehicleDataDefinition> = listOf(
            lamp(HIGH_BEAM, "상향등", "checkHighBeamLightStateCapabilities", VehicleEvent.HIGH_BEAM_ON),
            lamp(LOW_BEAM, "하향등", "checkLowBeamLightStateCapabilities", VehicleEvent.LOW_BEAM_ON),
            lamp(TAIL_LAMP, "후미등", "checkTailLampLightStateCapabilities", VehicleEvent.TAIL_LAMP_ON),
            lamp(FRONT_FOG, "앞 안개등", "checkFrontFogLightStateCapabilities", VehicleEvent.FRONT_FOG_ON),
            lamp(REAR_FOG, "뒤 안개등", "checkRearFogLightStateCapabilities", VehicleEvent.REAR_FOG_ON),
        )

        // 같은 램프를 다시 켰을 때 말하는 간격은 이벤트의 쿨다운이 정하므로 여기서는 두지 않는다(cooldownMs = 0).
        private fun lamp(key: String, label: String, capability: String, onEvent: VehicleEvent) = VehicleDataDefinition(
            dataKey = key,
            displayName = label,
            domain = VehicleDomain.CONVENIENCE,
            capability = capability,
            permission = "pleos.car.permission.CAR_LIGHTS",
            valueType = VehicleValueType.ENUM,
            reactionPolicy = VehicleReactionPolicy.RULE,
            cooldownMs = 0,
            reactionRule = SignalReactionRules.turnsOn(onEvent),
        )
    }
}
