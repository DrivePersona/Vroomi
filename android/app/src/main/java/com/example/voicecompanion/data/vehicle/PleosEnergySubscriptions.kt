package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.EvBatteryEfficiencyListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.FuelEconomyListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import com.example.voicecompanion.domain.model.EnergyBand
import com.example.voicecompanion.domain.model.EnergyLevelTracker
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleEnergyProfile
import com.example.voicecompanion.domain.model.VehicleEnergyProfileResolver
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import com.example.voicecompanion.domain.model.toVehicleEvent
import com.example.voicecompanion.domain.model.VehicleValueType
import java.util.Locale

/** Capability-gated, read-only energy signals. Classification uses capabilities, never model metadata. */
internal class PleosEnergySubscriptions(
    private val vehicle: Vehicle,
    private val states: VehicleStateRepository,
    private val capabilities: VehicleCapabilityRequests,
    private val addUndo: (() -> Unit) -> Unit,
    private val onFailure: (String, Exception) -> Unit,
    private val stopped: () -> Boolean,
) {
    private val ev by lazy { vehicle.getEvBattery() }
    private val fuel by lazy { vehicle.getFuel() }
    private val profileLock = Any()
    private var evReadable: Boolean? = null
    private var fuelReadable: Boolean? = null
    private val evTracker = EnergyLevelTracker()
    private val fuelTracker = EnergyLevelTracker()
    private val profileDefinition = VehicleDataDefinition(
        dataKey = PROFILE_KEY,
        displayName = "에너지 프로필",
        domain = VehicleDomain.MOTION,
        capability = "checkEvBatteryStateOfChargeCapabilities + checkFuelPercentCapabilities",
        permission = "pleos.car.permission.CAR_ENERGY",
        valueType = VehicleValueType.ENUM,
    )

    fun start() {
        states.registerDefinition(profileDefinition)
        definitions.forEach(states::registerDefinition)
        checkEvProfile()
        checkFuelProfile()
        subscribeEvStateOfCharge()
        subscribeEvBatteryLevel()
        subscribeEvChargePort()
        readEvChargeSwitch()
        readEvChargeLimits()
        readEvBatteryCapacity()
        subscribeFuelPercent()
        subscribeEvEfficiency("ev-efficiency-current", "EV 현재 전비", "checkCurrentEvEfficiencyCapabilities",
            { complete, failed -> ev.checkCurrentEvEfficiencyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> ev.registerCurrentEvEfficiency(listener) },
            { listener -> ev.unregisterCurrentEvEfficiency(listener) })
        subscribeEvEfficiency("ev-efficiency-instantaneous", "EV 순간 전비", "checkInstantaneousEvEfficiencyCapabilities",
            { complete, failed -> ev.checkInstantaneousEvEfficiencyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> ev.registerInstantaneousEvEfficiency(listener) },
            { listener -> ev.unregisterInstantaneousEvEfficiency(listener) })
        subscribeEvEfficiency("ev-efficiency-since-charge", "충전 이후 EV 전비", "checkSinceChargeEvEfficiencyCapabilities",
            { complete, failed -> ev.checkSinceChargeEvEfficiencyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> ev.registerSinceChargeEvEfficiency(listener) },
            { listener -> ev.unregisterSinceChargeEvEfficiency(listener) })
        subscribeEvEfficiency("ev-efficiency-cumulative", "누적 EV 전비", "checkCumulativeEvEfficiencyCapabilities",
            { complete, failed -> ev.checkCumulativeEvEfficiencyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> ev.registerCumulativeEvEfficiency(listener) },
            { listener -> ev.unregisterCumulativeEvEfficiency(listener) })
        subscribeFuelEconomy("fuel-economy-current", "현재 연비", "checkCurrentFuelEconomyCapabilities",
            { complete, failed -> fuel.checkCurrentFuelEconomyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> fuel.registerCurrentFuelEconomy(listener) },
            { listener -> fuel.unregisterCurrentFuelEconomy(listener) })
        subscribeFuelEconomy("fuel-economy-instantaneous", "순간 연비", "checkInstantaneousFuelEconomyCapabilities",
            { complete, failed -> fuel.checkInstantaneousFuelEconomyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> fuel.registerInstantaneousFuelEconomy(listener) },
            { listener -> fuel.unregisterInstantaneousFuelEconomy(listener) })
        subscribeFuelEconomy("fuel-economy-since-charge", "충전 이후 연비", "checkSinceChargeFuelEconomyCapabilities",
            { complete, failed -> fuel.checkSinceChargeFuelEconomyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> fuel.registerSinceChargeFuelEconomy(listener) },
            { listener -> fuel.unregisterSinceChargeFuelEconomy(listener) })
        subscribeFuelEconomy("fuel-economy-cumulative", "누적 연비", "checkCumulativeFuelEconomyCapabilities",
            { complete, failed -> fuel.checkCumulativeFuelEconomyCapabilities({ complete(it.accessType.canRead) }, failed) },
            { listener -> fuel.registerCumulativeFuelEconomy(listener) },
            { listener -> fuel.unregisterCumulativeFuelEconomy(listener) })
    }

    private fun checkEvProfile() {
        val definition = data("feature:ev-energy-profile", "EV 에너지 capability", "checkEvBatteryStateOfChargeCapabilities", VehicleValueType.INTEGER)
        capabilities.request(definition, { complete, failed ->
            ev.checkEvBatteryStateOfChargeCapabilities({ complete(it.accessType.canRead) }, failed)
        }, { readable ->
            synchronized(profileLock) { evReadable = readable }
            updateProfile()
            if (!readable) capabilities.unsupported(definition, "EV 배터리 잔량 읽기가 지원되지 않아요")
        }, onFailure = { error ->
            // 확인이 실패한 쪽은 읽을 수 없는 것으로 친다. 전기차는 연료 확인이 ERROR_NOT_SUPPORTED_PROPERTY로 끝나는데,
            // null로 두면 프로필이 끝내 정해지지 않아 에너지 반응이 모두 무시됐다(2026-11-15 AVD 실측).
            synchronized(profileLock) { evReadable = false }
            updateProfile()
            capabilities.unsupported(definition, "EV 배터리 잔량 capability 확인 실패: ${error.message ?: error.javaClass.simpleName}")
        })
    }

    private fun checkFuelProfile() {
        val definition = data("feature:fuel-energy-profile", "연료 capability", "checkFuelPercentCapabilities", VehicleValueType.INTEGER)
        capabilities.request(definition, { complete, failed ->
            fuel.checkFuelPercentCapabilities({ complete(it.accessType.canRead) }, failed)
        }, { readable ->
            synchronized(profileLock) { fuelReadable = readable }
            updateProfile()
            if (!readable) capabilities.unsupported(definition, "연료 잔량 읽기가 지원되지 않아요")
        }, onFailure = { error ->
            // 확인이 실패한 쪽은 읽을 수 없는 것으로 친다. 전기차는 연료 확인이 ERROR_NOT_SUPPORTED_PROPERTY로 끝나는데,
            // null로 두면 프로필이 끝내 정해지지 않아 에너지 반응이 모두 무시됐다(2026-11-15 AVD 실측).
            synchronized(profileLock) { fuelReadable = false }
            updateProfile()
            capabilities.unsupported(definition, "연료 잔량 capability 확인 실패: ${error.message ?: error.javaClass.simpleName}")
        })
    }

    private fun updateProfile() {
        val (evSupported, fuelSupported) = synchronized(profileLock) { evReadable to fuelReadable }
        val profile = VehicleEnergyProfileResolver.resolve(evSupported, fuelSupported) ?: return
        if (profile == VehicleEnergyProfile.UNAVAILABLE) {
            states.markUnsupported(PROFILE_KEY, "에너지 관련 기능을 지원하지 않아요")
        } else {
            states.observe(PROFILE_KEY, profile.name)
        }
    }

    private fun subscribeEvStateOfCharge() {
        val definition = STATE_OF_CHARGE_DEFINITION
        watch(definition,
            check = { complete, failed -> ev.checkEvBatteryStateOfChargeCapabilities({ complete(it.accessType.canRead) }, failed) },
            register = {
                val listener = object : EvBatteryStateOfChargeListener {
                    override fun onEvBatteryStateOfChargeUpdated(value: Int?) { value?.let { observeEnergyLevel(states, STATE_OF_CHARGE, EV_ENERGY_BAND, evTracker, it) } }
                    override fun onFailed(e: Exception) = failed(definition, e)
                }
                { ev.unregisterEvBatteryStateOfCharge(listener) }.also { ev.registerEvBatteryStateOfCharge(listener) }
            })
    }

    private fun subscribeEvBatteryLevel() {
        val definition = data("ev-battery-level", "배터리 에너지 레벨", "checkEvBatteryStateOfChargeCapabilities", VehicleValueType.INTEGER, event = null)
        watch(definition,
            check = { complete, failed -> ev.checkEvBatteryStateOfChargeCapabilities({ complete(it.accessType.canRead) }, failed) },
            register = {
                val listener = object : ai.pleos.playground.vehicle.listener.EvBatteryLevelListener {
                    override fun onEvBatteryLevelUpdated(value: Float?) = observeFloat(definition, value)
                    override fun onFailed(e: Exception) = failed(definition, e)
                }
                { ev.unregisterEvBatteryLevel(listener) }.also { ev.registerEvBatteryLevel(listener) }
            })
    }

    private fun subscribeEvChargePort() {
        val definition = CHARGE_PORT_DEFINITION
        watch(definition,
            check = { complete, failed -> ev.checkEvChargePortOpenCapabilities({ complete(it.accessType.canRead) }, failed) },
            register = {
                val listener = object : EvChargePortOpenListener {
                    override fun onEvChargePortOpenUpdated(value: Boolean?) = states.observe(definition.dataKey, value?.toString())
                    override fun onFailed(e: Exception) = failed(definition, e)
                }
                { ev.unregisterEvChargePortOpen(listener) }.also { ev.registerEvChargePortOpen(listener) }
            })
    }

    /** Vehicle SDK 2.3.2 exposes a capability and one-shot getter, but no charge-switch update listener. */
    private fun readEvChargeSwitch() {
        val definition = data("ev-charge-switch", "충전 스위치", "checkEvChargeSwitchCapabilities", VehicleValueType.BOOLEAN, event = null)
        states.registerDefinition(definition)
        capabilities.request(definition, { complete, failed ->
            ev.checkEvChargeSwitchCapabilities({ complete(it.accessType.canRead) }, failed)
        }, { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "충전 스위치 읽기가 지원되지 않아요")
            capabilities.request(definition, { complete, failed -> ev.getEvChargeSwitchState(complete, failed) }) {
                states.observe(definition.dataKey, it.toString())
            }
        })
    }

    private fun readEvChargeLimits() {
        val current = data("ev-charge-current-draw-limit", "충전 전류 한도", "checkEvChargeCurrentDrawLimitCapabilities", VehicleValueType.INTEGER, event = null)
        readOnce(current,
            check = { complete, failed -> ev.checkEvChargeCurrentDrawLimitCapabilities({ config, values -> complete(config.accessType.canRead && values.isNotEmpty()) }, failed) },
            read = { complete, failed -> ev.getEvChargeCurrentDrawLimit(complete, failed) })
        val percent = data("ev-charge-percent-limit-options", "충전 목표 한도 지원값", "checkEvChargePercentLimitCapabilities", VehicleValueType.ENUM, event = null)
        states.registerDefinition(percent)
        capabilities.request(percent, { complete, failed ->
            ev.checkEvChargePercentLimitCapabilities({ config, values -> complete(config.accessType.canRead to values) }, failed)
        }) { (readable, values) ->
            if (!readable || values.isEmpty()) capabilities.unsupported(percent, "충전 목표 한도 capability가 없어요")
            else states.observe(percent.dataKey, values.joinToString(","))
        }
    }

    private fun readEvBatteryCapacity() {
        val definition = data("ev-battery-capacity", "배터리 용량", "checkEvBatteryStateOfChargeCapabilities", VehicleValueType.INTEGER, event = null)
        readOnce(definition,
            check = { complete, failed -> ev.checkEvBatteryStateOfChargeCapabilities({ complete(it.accessType.canRead) }, failed) },
            read = { complete, failed -> ev.getEvBatteryCapacity(complete, failed) })
    }

    private fun readOnce(
        definition: VehicleDataDefinition,
        check: ((Boolean) -> Unit, (Exception) -> Unit) -> Unit,
        read: ((Float?) -> Unit, (Exception) -> Unit) -> Unit,
    ) {
        states.registerDefinition(definition)
        capabilities.request(definition, { complete, failed -> check(complete, failed) }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "읽기 capability가 없어요")
            capabilities.request(definition, { complete, failed -> read(complete, failed) }) {
                observeFloat(definition, it)
            }
        }
    }

    private fun subscribeEvEfficiency(
        key: String,
        label: String,
        capability: String,
        check: ((Boolean) -> Unit, (Exception) -> Unit) -> Unit,
        register: (EvBatteryEfficiencyListener) -> Unit,
        unregister: (EvBatteryEfficiencyListener) -> Unit,
    ) {
        val definition = data(key, label, capability, VehicleValueType.INTEGER, event = null)
        watch(definition, { complete, failed -> check(complete, failed) }, {
            val listener = object : EvBatteryEfficiencyListener {
                override fun onEvBatteryEfficiencyUpdated(value: Float?) = observeFloat(definition, value)
                override fun onFailed(e: Exception) = failed(definition, e)
            }
            { unregister(listener) }.also { register(listener) }
        })
    }

    private fun subscribeFuelPercent() {
        val definition = FUEL_PERCENT_DEFINITION
        watch(definition, { complete, failed -> fuel.checkFuelPercentCapabilities({ complete(it.accessType.canRead) }, failed) }, {
            val listener = object : FuelPercentListener {
                override fun onFuelPercentUpdated(value: Int?) { value?.let { observeEnergyLevel(states, FUEL_PERCENT, FUEL_ENERGY_BAND, fuelTracker, it) } }
                override fun onFailed(e: Exception) = failed(definition, e)
            }
            { fuel.unregisterFuelPercent(listener) }.also { fuel.registerFuelPercent(listener) }
        })
    }

    private fun subscribeFuelEconomy(
        key: String,
        label: String,
        capability: String,
        check: ((Boolean) -> Unit, (Exception) -> Unit) -> Unit,
        register: (FuelEconomyListener) -> Unit,
        unregister: (FuelEconomyListener) -> Unit,
    ) {
        val definition = data(key, label, capability, VehicleValueType.INTEGER, event = null)
        watch(definition, { complete, failed -> check(complete, failed) }, {
            val listener = object : FuelEconomyListener {
                override fun onFuelEconomyUpdated(value: Float?) = observeFloat(definition, value)
                override fun onFailed(e: Exception) = failed(definition, e)
            }
            { unregister(listener) }.also { register(listener) }
        })
    }

    private fun observeFloat(definition: VehicleDataDefinition, value: Float?) {
        states.observe(definition.dataKey, value?.let { String.format(Locale.ROOT, "%.2f", it) })
    }

    private fun watch(
        definition: VehicleDataDefinition,
        check: ((Boolean) -> Unit, (Exception) -> Unit) -> Unit,
        register: () -> (() -> Unit),
    ) {
        // VehicleStateRepository는 등록되지 않은 키의 값과 반응을 버린다. 구독 전에 등록해 둔다(이미 등록된 키는 그대로 둔다).
        states.registerDefinition(definition)
        capabilities.request(definition, { complete, failed -> check(complete, failed) }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "읽기 capability가 없어요")
            if (stopped()) return@request
            try {
                addUndo(register())
                states.markActive(definition.dataKey)
            } catch (e: Exception) {
                failed(definition, e)
            }
        }
    }

    private fun data(
        key: String,
        label: String,
        capability: String,
        type: VehicleValueType,
        event: VehicleEvent? = null,
    ) = energyDefinition(key, label, capability, type, event)

    private fun failed(definition: VehicleDataDefinition, error: Exception) {
        states.markError(definition.dataKey, error.message ?: error.javaClass.simpleName)
        onFailure(definition.displayName, error)
    }

    private val AccessType.canRead get() = this == AccessType.READ || this == AccessType.READ_WRITE

    internal companion object {
        const val PROFILE_KEY = "vehicle-energy-profile"
        const val STATE_OF_CHARGE = "ev-state-of-charge"
        const val CHARGE_PORT_OPEN = "ev-charge-port-open"
        const val FUEL_PERCENT = "fuel-percent"
        const val EV_ENERGY_BAND = "ev-energy-band"
        const val FUEL_ENERGY_BAND = "fuel-energy-band"

        private fun energyDefinition(
            key: String,
            label: String,
            capability: String,
            type: VehicleValueType,
            event: VehicleEvent? = null,
            policy: VehicleReactionPolicy? = null,
            cooldownMs: Long = event?.cooldownMs ?: 60_000,
        ) = VehicleDataDefinition(
            dataKey = key,
            displayName = label,
            domain = VehicleDomain.MOTION,
            capability = capability,
            permission = "pleos.car.permission.CAR_ENERGY",
            valueType = type,
            reactionEvent = event,
            reactionPolicy = policy,
            cooldownMs = cooldownMs,
            allowWhileDriving = false,
        )

        /**
         * 잔량은 에너지 범주가 바뀔 때만 반응한다([observeEnergyLevel]). 범주에는 히스테리시스가 있고 범주마다 엔진 쿨다운이 따로 있으니
         * 이 키의 쿨다운은 0이다. 키 쿨다운을 두면 말하지 않는 범주(여유·보통·확인) 전환이 그 시간을 차지해 바로 뒤의 '부족' 경고를 삼킨다.
         */
        val STATE_OF_CHARGE_DEFINITION = energyDefinition(STATE_OF_CHARGE, "EV 배터리 잔량", "checkEvBatteryStateOfChargeCapabilities", VehicleValueType.INTEGER, cooldownMs = 0)
        val FUEL_PERCENT_DEFINITION = energyDefinition(FUEL_PERCENT, "연료 잔량", "checkFuelPercentCapabilities", VehicleValueType.INTEGER, cooldownMs = 0)

        /** 충전구는 열림·닫힘이 바뀔 때마다(첫 값은 기준값) 말한다. 정차 중에만 말한다. */
        val CHARGE_PORT_DEFINITION = energyDefinition(CHARGE_PORT_OPEN, "충전구 열림 상태", "checkEvChargePortOpenCapabilities", VehicleValueType.BOOLEAN,
            event = VehicleEvent.CHARGE_PORT, policy = VehicleReactionPolicy.CHANGE)

        private val EV_BAND_DEFINITION = energyDefinition(EV_ENERGY_BAND, "EV 에너지 범주", "SOC threshold", VehicleValueType.ENUM)
        private val FUEL_BAND_DEFINITION = energyDefinition(FUEL_ENERGY_BAND, "연료 에너지 범주", "fuel threshold", VehicleValueType.ENUM)

        /** 반응을 낼 수 있는 에너지 신호. [start]가 구독 전에 등록한다. */
        val definitions = listOf(STATE_OF_CHARGE_DEFINITION, FUEL_PERCENT_DEFINITION, CHARGE_PORT_DEFINITION, EV_BAND_DEFINITION, FUEL_BAND_DEFINITION)

        /** 잔량(%)을 기록하고, 범주가 바뀌었을 때만 그 범주의 이벤트를 낸다. 첫 값은 기준값이라 말하지 않는다. SDK와 무관해 시험할 수 있다. */
        fun observeEnergyLevel(states: VehicleStateRepository, dataKey: String, bandKey: String, tracker: EnergyLevelTracker, value: Int) {
            states.observe(dataKey, value.coerceIn(0, 100).toString())
            val changedBand = tracker.observe(value)
            val band = tracker.current() ?: return
            states.observe(bandKey, band.name)
            if (changedBand == null) return
            states.emitReaction(dataKey, band.toVehicleEvent())
        }
    }
}
