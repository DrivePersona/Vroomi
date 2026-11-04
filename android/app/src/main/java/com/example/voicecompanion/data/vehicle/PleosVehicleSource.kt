package com.example.voicecompanion.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.DrivingModeType
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DrivingModeStateListener
import ai.pleos.playground.vehicle.listener.SeatBeltNotBuckledWarningListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.content.Context
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleEvent

/** Pleos Vehicle SDK의 capability를 먼저 확인하고, 읽기 가능한 신호만 구독한다. */
class PleosVehicleSource(
    context: Context,
    private val onEvent: (VehicleEvent, Boolean) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onCatalog: (List<com.example.voicecompanion.domain.model.VehicleDataState>) -> Unit,
) {
    private val vehicle = Vehicle(context.applicationContext)
    private val undo = mutableListOf<() -> Unit>()
    private val undoLock = Any()
    private val pendingLock = Any()
    private var pendingCapabilities = 0
    private var completedCapabilities = 0
    private var expectedCapabilities = 0
    private var capabilityLaunchFinished = false
    private var stopRequested = false
    private var released = false
    private var started = false
    private val states = VehicleStateRepository(onCatalog, onEvent)
    private val capabilityRequests = VehicleCapabilityRequests(
        begin = ::beginCapability,
        finish = ::capabilityFinished,
        isStopped = ::isStopped,
        markUnavailable = { definition, availability, detail ->
            states.registerDefinition(definition)
            if (availability == VehicleAvailability.UNSUPPORTED) states.markUnsupported(definition.dataKey, detail)
            else states.markError(definition.dataKey, detail)
        },
        onError = { definition, error ->
            states.registerDefinition(definition)
            capabilityError(definition.dataKey, definition.displayName, error)
        },
    )
    @Volatile private var summary = "시작 전"
    private val failures = LinkedHashSet<String>()

    fun start() {
        if (started) return
        started = true
        states.resetChecking()
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            VehicleDataCatalog.definitions.forEach { states.markError(it.dataKey, describe(e)) }
            summary = "차량 서비스 초기화 실패"
            publish()
            runCatching { vehicle.release() }
            started = false
            return
        }
        summary = "capability 확인 중"
        publish()
        checkCapability(
            dataKey = VehicleDataCatalog.BRAKE,
            name = "브레이크",
            request = { granted, denied ->
                vehicle.getBrake().checkBrakePedalStatusCapabilities(
                    { config -> granted(config.accessType.canRead) }, denied,
                )
            },
            register = ::watchBrake,
        )
        checkCapability(
            dataKey = VehicleDataCatalog.TURN_SIGNAL,
            name = "방향지시등",
            request = { granted, denied ->
                vehicle.getTurnSignal().checkTurnSignalStateCapabilities(
                    { config -> granted(config.accessType.canRead) }, denied,
                )
            },
            register = ::watchTurnSignal,
        )
        checkCapability(
            dataKey = VehicleDataCatalog.DRIVING_MODE,
            name = "주행 모드",
            request = { granted, denied ->
                vehicle.getDrivingMode().checkDrivingModeStateCapabilities(
                    { config -> granted(config.accessType.canRead) }, denied,
                )
            },
            register = ::watchDrivingMode,
        )
        checkSeatBeltCapabilities()
        val convenience = PleosConvenienceSubscriptions(
            vehicle = vehicle,
            states = states,
            capabilities = capabilityRequests,
            addUndo = ::addUndo,
            onFailure = { name, error -> recordFailure(name, error) },
            stopped = ::isStopped,
        )
        convenience.start()
        finishCapabilityLaunch()
    }

    private fun checkCapability(
        dataKey: String,
        name: String,
        request: (onGranted: (Boolean) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
        register: () -> Unit,
    ) {
        beginCapability()
        try {
            request(
                { readable ->
                    try {
                        if (!isStopped()) {
                            if (readable) registerSafely(dataKey, name, register)
                            else states.markUnsupported(dataKey, "읽기 capability가 없어요")
                        }
                    } finally { capabilityFinished() }
                },
                { error ->
                    try { if (!isStopped()) capabilityError(dataKey, name, error) }
                    finally { capabilityFinished() }
                },
            )
        } catch (e: Exception) {
            capabilityError(dataKey, name, e)
            capabilityFinished()
        }
    }

    private fun checkSeatBeltCapabilities() {
        val safety = vehicle.getSafety()
        beginCapability()
        try {
            safety.checkSeatNotBuckledWarningCapabilities(
                { config, supportedAreas ->
                    try {
                        listOf(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT).forEach { seat ->
                            val key = seatKey(seat)
                            val area = SeatArea(seat)
                            if (isStopped()) return@forEach
                            if (!config.accessType.canRead) {
                                states.markUnsupported(key, "안전벨트 경고 읽기 capability가 없어요")
                            } else if (area !in supportedAreas) {
                                states.markUnsupported(key, "이 좌석의 안전벨트 경고는 지원되지 않아요")
                            } else {
                                registerSafely(key, seatLabel(seat)) { watchSeatBelt(seat, key) }
                            }
                        }
                    } finally { capabilityFinished() }
                },
                { error ->
                    try {
                        if (!isStopped()) listOf(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT).forEach {
                            capabilityError(seatKey(it), seatLabel(it), error)
                        }
                    } finally { capabilityFinished() }
                },
            )
        } catch (e: Exception) {
            listOf(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT).forEach { capabilityError(seatKey(it), seatLabel(it), e) }
            capabilityFinished()
        }
    }

    private fun registerSafely(dataKey: String, name: String, register: () -> Unit) {
        try {
            if (isStopped()) return
            register()
            states.markActive(dataKey)
        } catch (e: Exception) {
            capabilityError(dataKey, name, e)
        }
    }

    private fun capabilityError(dataKey: String, name: String, e: Exception) {
        val detail = if (e is SecurityException) "권한 오류 · ${describe(e)}" else "서비스 오류 · ${describe(e)}"
        states.registerDefinition(VehicleDataCatalog.definitions.firstOrNull { it.dataKey == dataKey }
            ?: com.example.voicecompanion.domain.model.VehicleDataCatalog.convenience(
                dataKey, name, com.example.voicecompanion.domain.model.VehicleDomain.CONVENIENCE,
                "listener", "", com.example.voicecompanion.domain.model.VehicleValueType.ENUM, VehicleEvent.LIGHT,
            ))
        states.markError(dataKey, detail)
        recordFailure(name, e)
        publish()
    }

    private fun recordFailure(name: String, e: Exception) {
        synchronized(failures) { failures += "$name(${describe(e)})" }
        publish()
    }

    private fun beginCapability() = synchronized(pendingLock) {
        pendingCapabilities++
        expectedCapabilities++
    }

    private fun finishCapabilityLaunch() {
        val allChecked = synchronized(pendingLock) {
            capabilityLaunchFinished = true
            completedCapabilities == expectedCapabilities
        }
        if (allChecked) publishCapabilitySummary()
    }

    private fun capabilityFinished() {
        val (shouldRelease, allChecked) = synchronized(pendingLock) {
            pendingCapabilities = (pendingCapabilities - 1).coerceAtLeast(0)
            completedCapabilities++
            (stopRequested && pendingCapabilities == 0 && !released) to
                (capabilityLaunchFinished && completedCapabilities == expectedCapabilities)
        }
        if (allChecked) publishCapabilitySummary()
        if (shouldRelease) releaseVehicle()
    }

    private fun publishCapabilitySummary() {
        val snapshot = states.snapshot()
        val active = snapshot.count { it.availability == VehicleAvailability.ACTIVE }
        val unsupported = snapshot.count { it.availability == VehicleAvailability.UNSUPPORTED }
        val errors = snapshot.count { it.availability == VehicleAvailability.ERROR }
        summary = "상태 확인 완료 · 수신 $active · 미지원 $unsupported · 오류 $errors"
        publish()
    }

    private fun isStopped() = synchronized(pendingLock) { stopRequested }

    private fun describe(e: Exception) = e.javaClass.simpleName + (e.message?.let { ": ${it.take(80)}" } ?: "")

    private fun publish() {
        val text = synchronized(failures) {
            if (failures.isEmpty()) summary else "$summary\n오류: ${failures.joinToString(" / ")}"
        }
        onStatus(text)
    }

    private fun watchBrake() {
        val brake = vehicle.getBrake()
        val listener = object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(status: Boolean?) = states.observe(VehicleDataCatalog.BRAKE, status?.toString())
            override fun onFailed(e: Exception) = capabilityError(VehicleDataCatalog.BRAKE, "브레이크", e)
        }
        brake.registerBrakePedalStatus(listener)
        addUndo { brake.unregisterBrakePedalStatus(listener) }
    }

    private fun watchTurnSignal() {
        val turn = vehicle.getTurnSignal()
        val listener = object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = states.observe(VehicleDataCatalog.TURN_SIGNAL, state?.name)
            override fun onFailed(e: Exception) = capabilityError(VehicleDataCatalog.TURN_SIGNAL, "방향지시등", e)
        }
        turn.registerTurnSignalState(listener)
        addUndo { turn.unregisterTurnSignalState(listener) }
    }

    private fun watchDrivingMode() {
        val mode = vehicle.getDrivingMode()
        val listener = object : DrivingModeStateListener {
            override fun onDrivingModeStateUpdated(state: DrivingModeType?) = states.observe(VehicleDataCatalog.DRIVING_MODE, state?.name)
            override fun onFailed(e: Exception) = capabilityError(VehicleDataCatalog.DRIVING_MODE, "주행 모드", e)
        }
        mode.registerDrivingModeState(listener)
        addUndo { mode.unregisterDrivingModeState(listener) }
    }

    private fun watchSeatBelt(seat: SeatValue, dataKey: String) {
        val safety = vehicle.getSafety()
        val area = SeatArea(seat)
        val listener = object : SeatBeltNotBuckledWarningListener {
            override fun onSeatBeltNotBuckledWarningUpdated(status: Boolean?) = states.observe(dataKey, status?.toString())
            override fun onFailed(e: Exception) = capabilityError(dataKey, seatLabel(seat), e)
        }
        safety.registerSeatBeltNotBuckledWarning(area, listener)
        addUndo { safety.unregisterSeatBeltNotBuckledWarning(area, listener) }
    }

    private fun seatKey(seat: SeatValue) = when (seat) {
        SeatValue.ROW_1_LEFT -> VehicleDataCatalog.SEAT_BELT_LEFT
        else -> VehicleDataCatalog.SEAT_BELT_RIGHT
    }

    private fun seatLabel(seat: SeatValue) = if (seat == SeatValue.ROW_1_LEFT) "운전석 안전벨트" else "동승석 안전벨트"

    fun stop() {
        if (!started) return
        val cancellations = synchronized(undoLock) { undo.toList().also { undo.clear() } }
        cancellations.forEach { runCatching(it) }
        states.close()
        val shouldRelease = synchronized(pendingLock) {
            stopRequested = true
            pendingCapabilities == 0 && !released
        }
        if (shouldRelease) releaseVehicle()
        started = false
    }

    private fun releaseVehicle() {
        synchronized(pendingLock) { if (released) return else released = true }
        runCatching { vehicle.release() }
    }

    private fun addUndo(unregister: () -> Unit) {
        val stopNow = synchronized(undoLock) {
            if (isStopped()) true else { undo += unregister; false }
        }
        if (stopNow) runCatching(unregister)
    }

    private val AccessType.canRead: Boolean get() = this == AccessType.READ || this == AccessType.READ_WRITE
}
