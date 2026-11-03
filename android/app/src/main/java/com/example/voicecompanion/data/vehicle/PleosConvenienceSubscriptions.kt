package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.AmbientColorAutoByDriveModeEnabledListener
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessColorListener
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessListener
import ai.pleos.playground.vehicle.listener.DoorLockStateListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.GloveBoxLockStateListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SmartGlassLevelListener
import ai.pleos.playground.vehicle.listener.SmartGlassStateListener
import ai.pleos.playground.vehicle.listener.WindowLockStateListener
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import com.example.voicecompanion.domain.model.VehicleValueType

/** Capability-gated, read-only subscriptions for convenience controls. */
internal class PleosConvenienceSubscriptions(
    private val vehicle: Vehicle,
    private val states: VehicleStateRepository,
    private val capabilities: VehicleCapabilityRequests,
    private val addUndo: (() -> Unit) -> Unit,
    private val onFailure: (String, Exception) -> Unit,
    private val stopped: () -> Boolean,
) {
    private val lights by lazy { vehicle.getLight() }
    private val windows by lazy { vehicle.getWindow() }
    private val doors by lazy { vehicle.getDoor() }
    private val subscriptions = VehicleAreaSubscriptionRegistry(states, stopped, addUndo, onFailure)

    fun start() {
        subscribeAmbientState()
        subscribeAmbientBrightness()
        subscribeAmbientColor()
        subscribeAmbientAutoMode()
        subscribeCabinLight()
        subscribeReadingLights()
        subscribeWindowLocks()
        subscribeWindowPositions()
        subscribeWindowBlinds()
        subscribeSmartGlass()
        subscribeSmartGlassLevels()
        subscribeDoorLocks()
        subscribeDoorPositions()
        subscribeGloveBoxLock()
    }

    private fun subscribeAmbientState() {
        val fallback = definition("feature:ambient-state", "앰비언트 조명", VehicleDomain.CONVENIENCE,
            "checkAmbientLightStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM, VehicleEvent.LIGHT)
        capabilities.request(fallback, { complete, failed ->
            lights.checkAmbientLightStateCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { (area, _) ->
                val key = "ambient-state:${area.key()}"
                VehicleAreaSubscription(definition(key, "앰비언트 조명 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkAmbientLightStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM, VehicleEvent.LIGHT)) {
                    val listener = lightListener(key, "앰비언트 조명")
                    val unregister = { lights.unregisterAmbientLightStateListener(area, listener) }
                    lights.registerAmbientLightStateListener(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeAmbientBrightness() {
        val fallback = definition("feature:ambient-brightness", "앰비언트 밝기", VehicleDomain.CONVENIENCE,
            "checkAmbientLightBrightnessCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.INTEGER,
            VehicleEvent.LIGHT, VehicleReactionPolicy.BRIGHTNESS_BUCKET)
        capabilities.request(fallback, { complete, failed ->
            lights.checkAmbientLightBrightnessCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { (area, bounds) ->
                val key = "ambient-brightness:${area.key()}"
                VehicleAreaSubscription(definition(key, "앰비언트 밝기 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkAmbientLightBrightnessCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.INTEGER,
                    VehicleEvent.LIGHT, VehicleReactionPolicy.BRIGHTNESS_BUCKET), bounds) {
                    val listener = object : AmbientLightBrightnessListener {
                        override fun onBrightnessUpdated(value: Int?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "앰비언트 밝기 ${area.key()}", e)
                    }
                    val unregister = { lights.unregisterAmbientLightBrightnessListener(area, listener) }
                    lights.registerAmbientLightBrightnessListener(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeAmbientColor() {
        val fallback = definition("feature:ambient-color", "앰비언트 색상", VehicleDomain.CONVENIENCE,
            "checkAmbientLightBrightnessColorCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.COLOR, VehicleEvent.LIGHT)
        capabilities.request(fallback, { complete, failed ->
            lights.checkAmbientLightBrightnessColorCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { area ->
                val key = "ambient-color:${area.key()}"
                VehicleAreaSubscription(definition(key, "앰비언트 색상 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkAmbientLightBrightnessColorCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.COLOR,
                    VehicleEvent.LIGHT)) {
                    val listener = object : AmbientLightBrightnessColorListener {
                        override fun onBrightnessColorUpdated(value: Int?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "앰비언트 색상 ${area.key()}", e)
                    }
                    val unregister = { lights.unregisterAmbientLightBrightnessColorListener(area, listener) }
                    lights.registerAmbientLightBrightnessColorListener(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeAmbientAutoMode() {
        val key = "ambient-auto-by-drive-mode"
        val definition = definition(key, "주행 모드 연동 조명", VehicleDomain.CONVENIENCE,
            "checkAmbientColorAutoByDriveModeEnabledCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.BOOLEAN, VehicleEvent.LIGHT)
        capabilities.request(definition, { complete, failed ->
            lights.checkAmbientColorAutoByDriveModeEnabledCapabilities({ complete(it.accessType.canRead) }, failed)
        }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "읽기 capability가 없어요")
            subscribe(definition) {
                val listener = object : AmbientColorAutoByDriveModeEnabledListener {
                    override fun onAmbientColorAutoByDriveModeEnabledUpdated(value: Boolean?) = states.observe(key, value?.toString())
                    override fun onFailed(e: Exception) = fail(key, "주행 모드 연동 조명", e)
                }
                val unregister = { lights.unregisterAmbientColorAutoByDriveModeEnabled(listener) }
                lights.registerAmbientColorAutoByDriveModeEnabled(listener)
                unregister
            }
        }
    }

    private fun subscribeCabinLight() {
        val key = "cabin-light-state"
        val definition = definition(key, "실내등", VehicleDomain.CONVENIENCE,
            "checkCabinLightStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM, VehicleEvent.LIGHT)
        capabilities.request(definition, { complete, failed ->
            lights.checkCabinLightStateCapabilities({ config, values -> complete(config.accessType.canRead to values) }, failed)
        }) { (readable, values) ->
            if (!readable || values.isEmpty()) return@request capabilities.unsupported(definition, "실내등 상태 읽기가 지원되지 않아요")
            subscribe(definition) {
                val listener = lightListener(key, "실내등")
                val unregister = { lights.unregisterCabinLightState(listener) }
                lights.registerCabinLightState(listener)
                unregister
            }
        }
    }

    private fun subscribeReadingLights() {
        val fallback = definition("feature:reading-light-state", "독서등", VehicleDomain.CONVENIENCE,
            "checkReadingLightStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM, VehicleEvent.LIGHT)
        capabilities.request(fallback, { complete, failed ->
            lights.checkReadingLightStateCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { (area, values) ->
                val key = "reading-light-state:${area.key()}"
                VehicleAreaSubscription(definition(key, "독서등 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkReadingLightStateCapabilities", "pleos.car.permission.CAR_LIGHTS", VehicleValueType.ENUM, VehicleEvent.LIGHT)) {
                    val listener = lightListener(key, "독서등 ${area.key()}")
                    val unregister = { lights.unregisterReadingLightState(area, listener) }
                    lights.registerReadingLightState(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeWindowLocks() {
        val fallback = definition("feature:window-lock", "창문 잠금", VehicleDomain.CONVENIENCE,
            "checkWindowLockCapabilities", "pleos.car.permission.CAR_WINDOWS", VehicleValueType.BOOLEAN, VehicleEvent.WINDOW)
        capabilities.request(fallback, { complete, failed ->
            windows.checkWindowLockCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { area ->
                val key = "window-lock:${area.key()}"
                VehicleAreaSubscription(definition(key, "창문 잠금 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkWindowLockCapabilities", "pleos.car.permission.CAR_WINDOWS", VehicleValueType.BOOLEAN, VehicleEvent.WINDOW)) {
                    val listener = object : WindowLockStateListener {
                        override fun onWindowLockStateUpdated(value: Boolean?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "창문 잠금 ${area.key()}", e)
                    }
                    val unregister = { windows.unregisterWindowLockState(area, listener) }
                    windows.registerWindowLockState(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeWindowPositions() = subscribeWindowRange(
        name = "창문 위치",
        capability = "checkWindowPositionCapabilities",
        policy = VehicleReactionPolicy.POSITION_BUCKET,
        register = { area, listener -> windows.registerWindowPosition(area, listener) },
        unregister = { area, listener -> windows.unregisterWindowPosition(area, listener) },
        check = { complete, failed -> windows.checkWindowPositionCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
        keyPrefix = "window-position",
    )

    private fun subscribeWindowBlinds() = subscribeWindowRange(
        name = "창문 블라인드 위치",
        capability = "checkWindowBlindPositionCapabilities",
        policy = VehicleReactionPolicy.POSITION_BUCKET,
        register = { area, listener -> windows.registerWindowBlindPosition(area, listener) },
        unregister = { area, listener -> windows.unregisterWindowBlindPosition(area, listener) },
        check = { complete, failed -> windows.checkWindowBlindPositionCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed) },
        keyPrefix = "window-blind-position",
    )

    private fun subscribeWindowRange(
        name: String,
        capability: String,
        policy: VehicleReactionPolicy,
        register: (WindowArea, WindowPositionListener) -> Unit,
        unregister: (WindowArea, WindowPositionListener) -> Unit,
        check: ((Pair<Boolean, Map<WindowArea, AreaBounds<Int>>>) -> Unit, (Exception) -> Unit) -> Unit,
        keyPrefix: String,
    ) {
        val fallback = definition("feature:$keyPrefix", name, VehicleDomain.CONVENIENCE,
            capability, "pleos.car.permission.CAR_WINDOWS", VehicleValueType.INTEGER, VehicleEvent.WINDOW, policy)
        capabilities.request(fallback, { complete, failed -> check(complete, failed) }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { (area, bounds) ->
                val key = "$keyPrefix:${area.key()}"
                VehicleAreaSubscription(definition(key, "$name ${area.key()}", VehicleDomain.CONVENIENCE,
                    capability, "pleos.car.permission.CAR_WINDOWS", VehicleValueType.INTEGER, VehicleEvent.WINDOW, policy), bounds) {
                    val listener = object : WindowPositionListener {
                        override fun onWindowPositionUpdated(value: Int?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "$name ${area.key()}", e)
                    }
                    val unregisterArea = { unregister(area, listener) }
                    register(area, listener)
                    unregisterArea
                }
            })
        }
    }

    private fun subscribeSmartGlass() {
        val key = "smart-glass-state"
        val definition = definition(key, "스마트 글라스", VehicleDomain.CONVENIENCE,
            "checkSmartGlassStateCapabilities", "pleos.car.permission.CAR_WINDOWS", VehicleValueType.BOOLEAN, VehicleEvent.WINDOW)
        capabilities.request(definition, { complete, failed ->
            windows.checkSmartGlassStateCapabilities({ complete(it.accessType.canRead) }, failed)
        }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "스마트 글라스 상태 읽기가 지원되지 않아요")
            subscribe(definition) {
                val listener = object : SmartGlassStateListener {
                    override fun onSmartGlassStateUpdated(value: Boolean?) = states.observe(key, value?.toString())
                    override fun onFailed(e: Exception) = fail(key, "스마트 글라스", e)
                }
                val unregister = { windows.unregisterSmartGlassState(listener) }
                windows.registerSmartGlassState(listener)
                unregister
            }
        }
    }

    private fun subscribeSmartGlassLevels() {
        val fallback = definition("feature:smart-glass-level", "스마트 글라스 농도", VehicleDomain.CONVENIENCE,
            "checkSmartGlassLevelCapabilities", "pleos.car.permission.CAR_WINDOWS", VehicleValueType.INTEGER,
            VehicleEvent.WINDOW, VehicleReactionPolicy.POSITION_BUCKET)
        capabilities.request(fallback, { complete, failed ->
            windows.checkSmartGlassLevelCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { (area, bounds) ->
                val key = "smart-glass-level:${area.key()}"
                VehicleAreaSubscription(definition(key, "스마트 글라스 농도 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkSmartGlassLevelCapabilities", "pleos.car.permission.CAR_WINDOWS", VehicleValueType.INTEGER,
                    VehicleEvent.WINDOW, VehicleReactionPolicy.POSITION_BUCKET), bounds) {
                    val listener = object : SmartGlassLevelListener {
                        override fun onSmartGlassLevelUpdated(value: Int?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "스마트 글라스 농도 ${area.key()}", e)
                    }
                    val unregister = { windows.unregisterSmartGlassLevel(area, listener) }
                    windows.registerSmartGlassLevel(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeDoorLocks() {
        val fallback = definition("feature:door-lock", "도어 잠금", VehicleDomain.CONVENIENCE,
            "checkDoorLockCapabilities", "pleos.car.permission.CAR_DOORS", VehicleValueType.BOOLEAN, VehicleEvent.DOOR)
        capabilities.request(fallback, { complete, failed ->
            doors.checkDoorLockCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { area ->
                val key = "door-lock:${area.key()}"
                VehicleAreaSubscription(definition(key, "도어 잠금 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkDoorLockCapabilities", "pleos.car.permission.CAR_DOORS", VehicleValueType.BOOLEAN, VehicleEvent.DOOR)) {
                    val listener = object : DoorLockStateListener {
                        override fun onDoorLockStateUpdated(value: Boolean?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "도어 잠금 ${area.key()}", e)
                    }
                    val unregister = { doors.unregisterDoorLockState(area, listener) }
                    doors.registerDoorLockState(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeDoorPositions() {
        val fallback = definition("feature:door-position", "도어 열림 상태", VehicleDomain.CONVENIENCE,
            "checkDoorPositionCapabilities", "pleos.car.permission.CAR_DOORS", VehicleValueType.INTEGER,
            VehicleEvent.DOOR, VehicleReactionPolicy.POSITION_BUCKET)
        capabilities.request(fallback, { complete, failed ->
            doors.checkDoorPositionCapabilities({ config, areas -> complete(config.accessType.canRead to areas) }, failed)
        }) { (readable, areas) ->
            applyAreas(fallback, readable, areas.map { (area, bounds) ->
                val key = "door-position:${area.key()}"
                VehicleAreaSubscription(definition(key, "도어 열림 상태 ${area.key()}", VehicleDomain.CONVENIENCE,
                    "checkDoorPositionCapabilities", "pleos.car.permission.CAR_DOORS", VehicleValueType.INTEGER,
                    VehicleEvent.DOOR, VehicleReactionPolicy.POSITION_BUCKET), bounds) {
                    val listener = object : DoorPositionListener {
                        override fun onPositionUpdated(value: Int?) = states.observe(key, value?.toString())
                        override fun onFailed(e: Exception) = fail(key, "도어 열림 상태 ${area.key()}", e)
                    }
                    val unregister = { doors.unregisterDoorPosition(area, listener) }
                    doors.registerDoorPosition(area, listener)
                    unregister
                }
            })
        }
    }

    private fun subscribeGloveBoxLock() {
        val key = "glove-box-lock"
        val definition = definition(key, "글로브 박스 잠금", VehicleDomain.CONVENIENCE,
            "checkGloveBoxLockCapabilities", "pleos.car.permission.CAR_DOORS", VehicleValueType.BOOLEAN, VehicleEvent.DOOR)
        capabilities.request(definition, { complete, failed ->
            doors.checkGloveBoxLockCapabilities({ complete(it.accessType.canRead) }, failed)
        }) { readable ->
            if (!readable) return@request capabilities.unsupported(definition, "글로브 박스 잠금 상태 읽기가 지원되지 않아요")
            subscribe(definition) {
                val listener = object : GloveBoxLockStateListener {
                    override fun onGloveBoxLockStateUpdated(value: Boolean?) = states.observe(key, value?.toString())
                    override fun onFailed(e: Exception) = fail(key, "글로브 박스 잠금", e)
                }
                val unregister = { doors.unregisterGloveBoxLockState(listener) }
                doors.registerGloveBoxLockState(listener)
                unregister
            }
        }
    }

    private fun applyAreas(fallback: VehicleDataDefinition, readable: Boolean, areas: List<VehicleAreaSubscription>) =
        subscriptions.applyCapability(fallback, readable, areas)

    private fun subscribe(definition: VehicleDataDefinition, register: () -> (() -> Unit)) =
        subscriptions.subscribe(definition, register = register)

    private fun lightListener(key: String, name: String) = object : LightStateListener {
        override fun onLightStateUpdated(value: ai.pleos.playground.vehicle.constant.control.LightState?) = states.observe(key, value?.name)
        override fun onFailed(e: Exception) = fail(key, name, e)
    }

    private fun definition(
        key: String,
        name: String,
        domain: VehicleDomain,
        capability: String,
        permission: String,
        valueType: VehicleValueType,
        event: VehicleEvent,
        policy: VehicleReactionPolicy = VehicleReactionPolicy.CHANGE,
    ) = VehicleDataCatalog.convenience(key, name, domain, capability, permission, valueType, event, policy)

    private fun fail(dataKey: String, name: String, e: Exception) {
        states.markError(dataKey, e.message ?: e.javaClass.simpleName)
        onFailure(name, e)
    }

    private fun SeatArea.key() = values.joinToString("+") { it.name }
    private fun WindowArea.key() = values.joinToString("+") { it.name }
    private fun DoorArea.key() = values.joinToString("+") { it.name }

    private val AccessType.canRead: Boolean get() = this == AccessType.READ || this == AccessType.READ_WRITE
}

