package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleDataDefinition
import com.example.voicecompanion.domain.model.VehicleDomain
import com.example.voicecompanion.domain.model.VehicleReactionPolicy
import com.example.voicecompanion.domain.model.VehicleValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleStateRepositoryTest {
    @Test
    fun catalogContainsOnlyTheCurrentVehicleSubscriptions() {
        assertEquals(5, VehicleDataCatalog.definitions.size)
        assertTrue(VehicleDataCatalog.definitions.all { it.capability.isNotBlank() && it.permission.isNotBlank() })
        assertEquals(VehicleAvailability.CHECKING, VehicleDataCatalog.initialState.first().availability)
    }

    @Test
    fun firstCallbackIsBaselineAndDuplicateValueDoesNotEmit() {
        val events = mutableListOf<VehicleEvent>()
        val repository = VehicleStateRepository({}, { event, _ -> events += event })

        repository.observe(VehicleDataCatalog.BRAKE, "true")
        repository.observe(VehicleDataCatalog.BRAKE, "true")

        assertTrue(events.isEmpty())
        assertEquals("true", repository.snapshot().first { it.definition.dataKey == VehicleDataCatalog.BRAKE }.value)
    }

    @Test
    fun onlyRelevantValueTransitionsEmitEvents() {
        val events = mutableListOf<VehicleEvent>()
        val repository = VehicleStateRepository({}, { event, _ -> events += event })

        repository.observe(VehicleDataCatalog.BRAKE, "false")
        repository.observe(VehicleDataCatalog.BRAKE, "true")
        repository.observe(VehicleDataCatalog.TURN_SIGNAL, "OFF")
        repository.observe(VehicleDataCatalog.TURN_SIGNAL, "LEFT")
        repository.observe(VehicleDataCatalog.TURN_SIGNAL, "LEFT")
        repository.observe(VehicleDataCatalog.DRIVING_MODE, "NORMAL")
        repository.observe(VehicleDataCatalog.DRIVING_MODE, "SPORT")
        repository.observe(VehicleDataCatalog.SEAT_BELT_LEFT, "false")
        repository.observe(VehicleDataCatalog.SEAT_BELT_LEFT, "true")

        assertEquals(listOf(VehicleEvent.BRAKE, VehicleEvent.TURN, VehicleEvent.MODE, VehicleEvent.WARNING), events)
    }

    @Test
    fun unsupportedAndErrorsRemainVisibleInSnapshots() {
        val repository = VehicleStateRepository({}, { _, _ -> })
        repository.markUnsupported(VehicleDataCatalog.SEAT_BELT_RIGHT, "좌석 미지원")
        repository.markError(VehicleDataCatalog.BRAKE, "권한 오류")

        val snapshot = repository.snapshot().associateBy { it.definition.dataKey }
        assertEquals(VehicleAvailability.UNSUPPORTED, snapshot.getValue(VehicleDataCatalog.SEAT_BELT_RIGHT).availability)
        assertEquals("좌석 미지원", snapshot.getValue(VehicleDataCatalog.SEAT_BELT_RIGHT).detail)
        assertEquals(VehicleAvailability.ERROR, snapshot.getValue(VehicleDataCatalog.BRAKE).availability)
        assertEquals("권한 오류", snapshot.getValue(VehicleDataCatalog.BRAKE).detail)
        assertNull(snapshot.getValue(VehicleDataCatalog.BRAKE).value)
    }

    @Test
    fun continuousPositionWaitsUntilStableAndEmitsOnlyWhenBucketChanges() {
        data class Scheduled(var cancelled: Boolean, val action: () -> Unit)
        val tasks = mutableListOf<Scheduled>()
        val events = mutableListOf<VehicleEvent>()
        var now = 0L
        val repository = VehicleStateRepository(
            onSnapshot = {},
            onReaction = { event, allowWhileDriving ->
                assertEquals(false, allowWhileDriving)
                events += event
            },
            nowEpochMs = { now },
            nowMonotonicMs = { now },
            schedule = { delay, action ->
                assertEquals(800L, delay)
                val task = Scheduled(false, action)
                tasks += task
                val cancel: () -> Unit = { task.cancelled = true }
                cancel
            },
        )
        val key = "window-position:ROW_1_LEFT"
        repository.registerDefinition(convenienceDefinition(key, VehicleReactionPolicy.POSITION_BUCKET))
        repository.setRange(key, 0, 100)
        repository.observe(key, "0") // baseline: closed
        repository.observe(key, "20")
        repository.observe(key, "42") // slider keeps moving; restart debounce
        assertTrue(events.isEmpty())
        tasks.filterNot { it.cancelled }.forEach { it.cancelled = true; it.action() }
        assertEquals(listOf(VehicleEvent.WINDOW), events)

        now = 1_000
        repository.observe(key, "45") // still partly open; same category
        tasks.filterNot { it.cancelled }.forEach { it.cancelled = true; it.action() }
        assertEquals(listOf(VehicleEvent.WINDOW), events)

        now = 3_000
        repository.observe(key, "100") // stable open category
        tasks.filterNot { it.cancelled }.forEach { it.cancelled = true; it.action() }
        assertEquals(listOf(VehicleEvent.WINDOW, VehicleEvent.WINDOW), events)
        repository.close()
    }

    @Test
    fun cooldownIsTrackedPerDataKeyAndConvenienceEventsAreMutedDuringDriving() {
        var now = 0L
        val events = mutableListOf<Pair<VehicleEvent, Boolean>>()
        val repository = VehicleStateRepository(
            onSnapshot = {},
            onReaction = { event, allowed -> events += event to allowed },
            nowEpochMs = { now },
            nowMonotonicMs = { now },
            schedule = { _, _ -> { } },
        )
        val left = "door-lock:ROW_1_LEFT"
        val right = "door-lock:ROW_1_RIGHT"
        repository.registerDefinition(convenienceDefinition(left, VehicleReactionPolicy.CHANGE, VehicleEvent.DOOR))
        repository.registerDefinition(convenienceDefinition(right, VehicleReactionPolicy.CHANGE, VehicleEvent.DOOR))
        repository.observe(left, "false")
        repository.observe(right, "false")
        repository.observe(left, "true")
        repository.observe(left, "false") // same key cooldown suppresses this
        repository.observe(right, "true") // different key has an independent cooldown

        assertEquals(listOf(VehicleEvent.DOOR to false, VehicleEvent.DOOR to false), events)
        now = 3_000
        repository.observe(left, "true")
        assertEquals(3, events.size)
        repository.close()
    }

    @Test
    fun comfortChangesWaitOneSecondAndOnlyAnnounceTheFinalChangedValue() {
        data class Scheduled(var cancelled: Boolean, val delay: Long, val action: () -> Unit)
        val tasks = mutableListOf<Scheduled>()
        val events = mutableListOf<Pair<VehicleEvent, Boolean>>()
        val repository = VehicleStateRepository(
            onSnapshot = {},
            onReaction = { event, allowed -> events += event to allowed },
            schedule = { delay, action ->
                val task = Scheduled(false, delay, action)
                tasks += task
                { task.cancelled = true }
            },
        )
        val definition = VehicleDataDefinition(
            dataKey = "hvac-target:driver",
            displayName = "공조 설정 온도",
            domain = VehicleDomain.CONVENIENCE,
            capability = "checkHvacTemperatureCapabilities",
            permission = "pleos.car.permission.CAR_CLIMATE",
            valueType = VehicleValueType.INTEGER,
            reactionEvent = VehicleEvent.COMFORT,
            reactionPolicy = VehicleReactionPolicy.DEBOUNCED_CHANGE,
            cooldownMs = 0,
            allowWhileDriving = false,
            debounceMs = 1_000,
        )
        repository.registerDefinition(definition)
        repository.observe(definition.dataKey, "21.0")
        repository.observe(definition.dataKey, "22.0")
        repository.observe(definition.dataKey, "21.0") // back to baseline, so stay quiet
        assertEquals(listOf(1_000L, 1_000L, 1_000L), tasks.map { it.delay })
        tasks.filterNot { it.cancelled }.forEach { it.action() }
        assertTrue(events.isEmpty())

        repository.observe(definition.dataKey, "23.0")
        tasks.filterNot { it.cancelled }.forEach { it.action() }
        assertEquals(listOf(VehicleEvent.COMFORT to false), events)
        repository.close()
    }

    private fun convenienceDefinition(
        key: String,
        policy: VehicleReactionPolicy,
        event: VehicleEvent = VehicleEvent.WINDOW,
    ) = VehicleDataDefinition(
        dataKey = key,
        displayName = key,
        domain = VehicleDomain.CONVENIENCE,
        capability = "testCapability",
        permission = "test.permission",
        valueType = VehicleValueType.INTEGER,
        reactionEvent = event,
        reactionPolicy = policy,
        allowWhileDriving = false,
    )
}
