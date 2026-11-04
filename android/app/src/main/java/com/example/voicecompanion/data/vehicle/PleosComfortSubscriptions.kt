package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.MirrorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacAcStateListener
import ai.pleos.playground.vehicle.listener.HvacAirCleanerStateListener
import ai.pleos.playground.vehicle.listener.HvacAutoStateListener
import ai.pleos.playground.vehicle.listener.HvacDefrosterStateListener
import ai.pleos.playground.vehicle.listener.HvacDualStateListener
import ai.pleos.playground.vehicle.listener.HvacPowerStateListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacSeatTemperatureListener
import ai.pleos.playground.vehicle.listener.HvacSeatVentilationListener
import ai.pleos.playground.vehicle.listener.HvacSideMirrorHeatStateListener
import ai.pleos.playground.vehicle.listener.HvacSteeringWheelHeatStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import com.example.voicecompanion.domain.model.VehicleValueType

/** Read-only, capability-gated subscriptions for comfort state. */
internal class PleosComfortSubscriptions(
    private val vehicle: Vehicle,
    private val states: VehicleStateRepository,
    private val capabilities: VehicleCapabilityRequests,
    private val addUndo: (() -> Unit) -> Unit,
    private val onFailure: (String, Exception) -> Unit,
    private val stopped: () -> Boolean,
) {
    private val registry = VehicleAreaSubscriptionRegistry(states, stopped, addUndo, onFailure)
    private val hvac by lazy { vehicle.getHVAC() }
    private val seat by lazy { vehicle.getSeat() }
    private val wiper by lazy { vehicle.getWiper() }
    private val mirror by lazy { vehicle.getSideMirror() }
    private val wheel by lazy { vehicle.getSteeringWheel() }

    fun start() {
        subscribeHvacTemperature()
        subscribeHvacSwitches()
        subscribeFanSpeed()
        subscribeSeatOccupancy()
        subscribeSeatLevels(heating = true)
        subscribeSeatLevels(heating = false)
        subscribeWiper()
        subscribeMirrorHeat()
        subscribeWheelHeat()
    }

    private fun subscribeHvacSwitches() {
        subscribeBoolean("hvac-power", "공조 전원", "checkHvacPowerCapabilities",
            { complete, failed -> hvac.checkHvacPowerCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
            { area, update, failed ->
                val listener = object : HvacPowerStateListener {
                    override fun onHvacPowerStateUpdated(value: Boolean?) = update(value)
                    override fun onFailed(e: Exception) = failed(e)
                }
                { hvac.unregisterHvacPowerState(area, listener) }.also { hvac.registerHvacPowerState(area, listener) }
            })
        subscribeBoolean("hvac-ac", "에어컨", "checkHvacAcCapabilities",
            { complete, failed -> hvac.checkHvacAcCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
            { area, update, failed ->
                val listener = object : HvacAcStateListener {
                    override fun onHvacAcStateUpdated(value: Boolean?) = update(value)
                    override fun onFailed(e: Exception) = failed(e)
                }
                { hvac.unregisterHvacAcState(area, listener) }.also { hvac.registerHvacAcState(area, listener) }
            })
        subscribeBoolean("hvac-auto", "공조 자동 모드", "checkHvacAutoCapabilities",
            { complete, failed -> hvac.checkHvacAutoCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
            { area, update, failed ->
                val listener = object : HvacAutoStateListener {
                    override fun onHvacAutoStateUpdated(value: Boolean?) = update(value)
                    override fun onFailed(e: Exception) = failed(e)
                }
                { hvac.unregisterHvacAutoState(area, listener) }.also { hvac.registerHvacAutoState(area, listener) }
            })
        subscribeBoolean("hvac-dual", "좌우 온도 분리", "checkHvacDualCapabilities",
            { complete, failed -> hvac.checkHvacDualCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
            { area, update, failed ->
                val listener = object : HvacDualStateListener {
                    override fun onHvacDualStateUpdated(value: Boolean?) = update(value)
                    override fun onFailed(e: Exception) = failed(e)
                }
                { hvac.unregisterHvacDualState(area, listener) }.also { hvac.registerHvacDualState(area, listener) }
            })
        subscribeBoolean("hvac-recirculation", "내기 순환", "checkHvacRecirculationCapabilities",
            { complete, failed -> hvac.checkHvacRecirculationCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
            { area, update, failed ->
                val listener = object : HvacRecirculationStateListener {
                    override fun onHvacAutoRecirculationStateUpdated(value: Boolean?) = update(value)
                    override fun onFailed(e: Exception) = failed(e)
                }
                { hvac.unregisterHvacRecirculationState(area, listener) }.also { hvac.registerHvacRecirculationState(area, listener) }
            })
        subscribeBoolean("hvac-air-cleaner", "공기 청정", "checkHvacAirCleanerCapabilities",
            { complete, failed -> hvac.checkHvacAirCleanerCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
            { area, update, failed ->
                val listener = object : HvacAirCleanerStateListener {
                    override fun onHvacAirCleanerStateUpdated(value: Boolean?) = update(value)
                    override fun onFailed(e: Exception) = failed(e)
                }
                { hvac.unregisterHvacAirCleanerState(area, listener) }.also { hvac.registerHvacAirCleanerState(area, listener) }
            })
        subscribeDefroster()
    }

    private fun subscribeBoolean(
        id: String,
        label: String,
        capability: String,
        check: ((Pair<Boolean, List<SeatArea>>) -> Unit, (Exception) -> Unit) -> Unit,
        register: (SeatArea, (Boolean?) -> Unit, (Exception) -> Unit) -> (() -> Unit),
    ) {
        val fallback = base("feature:$id", label, capability, "pleos.car.permission.CAR_CLIMATE", VehicleValueType.BOOLEAN)
        capabilities.request(fallback, { complete, failed -> check(complete, failed) }) { (readable, areas) ->
            apply(fallback, readable, areas.map { area ->
                val key = "$id:${area.seatValue}"
                VehicleAreaSubscription(base(key, "$label ${area.seatValue}", capability, fallback.permission, fallback.valueType)) {
                    register(area, { observe(key, it?.toString()) }, { failed(key, label, it) })
                }
            })
        }
    }

    private fun subscribeDefroster() {
        val fallback = base("feature:hvac-defroster", "성에 제거", "checkHvacDefrosterCapabilities", "pleos.car.permission.CAR_CLIMATE", VehicleValueType.BOOLEAN, emitReaction = false)
        capabilities.request(fallback, { complete, failed ->
            hvac.checkHvacDefrosterCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { area ->
                val key = "hvac-defroster:${area.windowValue}"
                VehicleAreaSubscription(base(key, "성에 제거 ${area.windowValue}", fallback.capability, fallback.permission, fallback.valueType)) {
                    val listener = object : HvacDefrosterStateListener {
                        override fun onHvacDefrosterStateUpdated(value: Boolean?) = observe(key, value?.toString())
                        override fun onFailed(e: Exception) = failed(key, "성에 제거", e)
                    }
                    val unregister = { hvac.unregisterHvacDefrosterState(area, listener) }
                    hvac.registerHvacDefrosterState(area, listener)
                    unregister
                }
            })
        }
    }

    private fun base(key: String, name: String, capability: String, permission: String, type: VehicleValueType, emitReaction: Boolean = true) =
        VehicleDataDefinition(key, name, VehicleDomain.CONVENIENCE, capability, permission, type,
            if (emitReaction) VehicleEvent.COMFORT else null, VehicleReactionPolicy.DEBOUNCED_CHANGE, cooldownMs = 0,
            allowWhileDriving = false, debounceMs = 1_000)

    private fun subscribeHvacTemperature() {
        val fallback = base("feature:hvac-target-temperature", "공조 설정 온도", "checkHvacTemperatureCapabilities", "pleos.car.permission.CAR_CLIMATE", VehicleValueType.INTEGER)
        capabilities.request(fallback, { complete, failed ->
            hvac.checkHvacTemperatureCapabilities({ config, areaRanges, _ -> complete(config.accessType.canRead to areaRanges.keys.toList()) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { area ->
                val key = "hvac-target-temperature:${area.seatValue}"
                VehicleAreaSubscription(base(key, "공조 설정 온도 ${area.seatValue}", fallback.capability, fallback.permission, fallback.valueType)) {
                    val listener = object : HvacTemperatureListener {
                        override fun onHvacTemperatureUpdated(value: Float?) = observe(key, value?.let { "%.1f".format(java.util.Locale.ROOT, it) })
                        override fun onFailed(e: Exception) = failed(key, "공조 설정 온도", e)
                    }
                    val unregister = { hvac.unregisterHvacTemperature(area, listener) }
                    hvac.registerHvacTemperature(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeFanSpeed() {
        val fallback = base("feature:hvac-fan-speed", "송풍 세기", "checkHvacFanSpeedCapabilities", "pleos.car.permission.CAR_CLIMATE", VehicleValueType.INTEGER)
        capabilities.request(fallback, { complete, failed ->
            hvac.checkHvacFanSpeedCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { (area, bounds) ->
                val key = "hvac-fan-speed:${area.seatValue}"
                VehicleAreaSubscription(base(key, "송풍 세기 ${area.seatValue}", fallback.capability, fallback.permission, fallback.valueType), bounds) {
                    val listener = object : HvacFanSpeedListener {
                        override fun onHvacFanSpeedUpdated(value: Int?) = observe(key, value?.toString())
                        override fun onFailed(e: Exception) = failed(key, "송풍 세기", e)
                    }
                    val unregister = { hvac.unregisterHvacFanSpeed(area, listener) }
                    hvac.registerHvacFanSpeed(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeSeatOccupancy() {
        val fallback = base("feature:seat-occupancy", "좌석 착석 상태", "checkSeatOccupancyCapabilities", "pleos.car.permission.CAR_SEATS", VehicleValueType.BOOLEAN)
        capabilities.request(fallback, { complete, failed ->
            seat.checkSeatOccupancyCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { area ->
                val key = "seat-occupancy:${area.seatValue}"
                VehicleAreaSubscription(base(key, "좌석 착석 상태 ${area.seatValue}", fallback.capability, fallback.permission, fallback.valueType)) {
                    val listener = object : SeatOccupancyListener {
                        override fun onSeatOccupancyUpdated(value: Boolean?) = observe(key, value?.toString())
                        override fun onFailed(e: Exception) = failed(key, "좌석 착석 상태", e)
                    }
                    val unregister = { seat.unregisterSeatOccupancy(area, listener) }
                    seat.registerSeatOccupancy(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeSeatLevels(heating: Boolean) {
        val label = if (heating) "시트 열선" else "시트 통풍"
        val capability = if (heating) "checkSeatTemperatureCapabilities" else "checkSeatVentilationCapabilities"
        val fallback = base("feature:${if (heating) "seat-heat" else "seat-ventilation"}", label, capability, "pleos.car.permission.CAR_SEATS", VehicleValueType.INTEGER, emitReaction = false)
        capabilities.request(fallback, { complete, failed ->
            if (heating) seat.checkSeatTemperatureCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
            else seat.checkSeatVentilationCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { (area, bounds) ->
                val key = "${if (heating) "seat-heat" else "seat-ventilation"}:${area.seatValue}"
                VehicleAreaSubscription(base(key, "$label ${area.seatValue}", capability, fallback.permission, fallback.valueType), bounds) {
                    if (heating) {
                        val listener = object : HvacSeatTemperatureListener {
                            override fun onHvacSeatTemperatureUpdated(value: Int?) = observe(key, value?.toString())
                            override fun onFailed(e: Exception) = failed(key, label, e)
                        }
                        val unregister = { seat.unregisterHvacSeatTemperature(area, listener) }
                        seat.registerHvacSeatTemperature(area, listener)
                        unregister
                    } else {
                        val listener = object : HvacSeatVentilationListener {
                            override fun onHvacSeatVentilationUpdated(value: Int?) = observe(key, value?.toString())
                            override fun onFailed(e: Exception) = failed(key, label, e)
                        }
                        val unregister = { seat.unregisterHvacSeatVentilation(area, listener) }
                        seat.registerHvacSeatVentilation(area, listener)
                        unregister
                    }
                }
            })
        }
    }

    private fun subscribeWiper() {
        val fallback = base("feature:wiper-state", "와이퍼 상태", "checkWiperStateCapabilities", "pleos.car.permission.READ_WINDSHIELD_WIPERS", VehicleValueType.ENUM, emitReaction = false)
        capabilities.request(fallback, { complete, failed ->
            wiper.checkWiperStateCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { (area, _) ->
                val key = "wiper-state:${area.windowValue}"
                VehicleAreaSubscription(base(key, "와이퍼 ${area.windowValue}", fallback.capability, fallback.permission, fallback.valueType)) {
                    val listener = object : WiperStateListener {
                        override fun onWiperStateUpdated(value: ai.pleos.playground.vehicle.constant.control.WiperState?) = observe(key, value?.name)
                        override fun onFailed(e: Exception) = failed(key, "와이퍼 ${area.windowValue}", e)
                    }
                    val unregister = { wiper.unregisterWiperState(area, listener) }
                    wiper.registerWiperState(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeMirrorHeat() {
        val fallback = base("feature:mirror-heat", "사이드미러 열선", "checkHvacSideMirrorHeatCapabilities", "pleos.car.permission.CAR_MIRRORS", VehicleValueType.INTEGER, emitReaction = false)
        capabilities.request(fallback, { complete, failed ->
            mirror.checkHvacSideMirrorHeatCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            apply(fallback, readable, areas.map { (area, _) ->
                val key = "mirror-heat:${area.mirrorValue}"
                VehicleAreaSubscription(base(key, "사이드미러 열선 ${area.mirrorValue}", fallback.capability, fallback.permission, fallback.valueType)) {
                    val listener = object : HvacSideMirrorHeatStateListener {
                        override fun onHvacSideMirrorHeatStateUpdated(value: Int?) = observe(key, value?.toString())
                        override fun onFailed(e: Exception) = failed(key, "사이드미러 열선", e)
                    }
                    val unregister = { mirror.unregisterHvacSideMirrorHeatState(area, listener) }
                    mirror.registerHvacSideMirrorHeatState(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeWheelHeat() {
        val fallback = base("steering-wheel-heat", "핸들 열선", "checkHvacSteeringWheelHeatCapabilities", "pleos.car.permission.READ_CAR_STEERING", VehicleValueType.INTEGER, emitReaction = false)
        capabilities.request(fallback, { complete, failed ->
            wheel.checkHvacSteeringWheelHeatCapabilities({ config, _ -> complete(config.accessType.canRead) }, failed)
        }) { readable ->
            if (!readable) return@request capabilities.unsupported(fallback, "읽기 capability가 없어요")
            registry.subscribe(fallback) {
                val listener = object : HvacSteeringWheelHeatStateListener {
                    override fun onHvacSteeringWheelHeatStateUpdated(value: Int?) = observe(fallback.dataKey, value?.toString())
                    override fun onFailed(e: Exception) = failed(fallback.dataKey, fallback.displayName, e)
                }
                val unregister = { wheel.unregisterHvacSteeringWheelHeatState(listener) }
                wheel.registerHvacSteeringWheelHeatState(listener)
                unregister
            }
        }
    }

    private fun apply(fallback: VehicleDataDefinition, readable: Boolean, subscriptions: List<VehicleAreaSubscription>) =
        registry.applyCapability(fallback, readable, subscriptions)

    private fun observe(key: String, value: String?) = states.observe(key, value)
    private fun failed(key: String, label: String, error: Exception) {
        states.markError(key, error.message ?: error.javaClass.simpleName)
        onFailure(label, error)
    }

    private val AccessType.canRead get() = this == AccessType.READ || this == AccessType.READ_WRITE
    private val SeatArea.seatValue get() = values.singleOrNull()?.name ?: "UNKNOWN"
    private val WindowArea.windowValue get() = values.singleOrNull()?.name ?: "UNKNOWN"
    private val MirrorArea.mirrorValue get() = values.singleOrNull()?.name ?: "UNKNOWN"
}
