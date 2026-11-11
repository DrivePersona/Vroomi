package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleEvent
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleSignalDefinitionsTest {
    private val definitions = PleosLampSubscriptions.definitions + PleosDriveSubscriptions.definitions

    /** 기준값(baseline) 다음에 trigger 값이 오면 event 하나를 말해야 하는 경우. */
    private data class Case(val key: String, val baseline: String, val trigger: String, val event: VehicleEvent)

    private val cases = listOf(
        Case(PleosLampSubscriptions.HIGH_BEAM, "OFF", "ON", VehicleEvent.HIGH_BEAM_ON),
        Case(PleosLampSubscriptions.LOW_BEAM, "OFF", "ON", VehicleEvent.LOW_BEAM_ON),
        Case(PleosLampSubscriptions.TAIL_LAMP, "OFF", "ON", VehicleEvent.TAIL_LAMP_ON),
        Case(PleosLampSubscriptions.FRONT_FOG, "OFF", "ON", VehicleEvent.FRONT_FOG_ON),
        Case(PleosLampSubscriptions.REAR_FOG, "OFF", "ON", VehicleEvent.REAR_FOG_ON),
        Case(PleosDriveSubscriptions.GEAR, "GEAR_PARK", "GEAR_DRIVE", VehicleEvent.SHIFT_TO_DRIVE),
        Case(PleosDriveSubscriptions.GEAR, "GEAR_DRIVE", "GEAR_PARK", VehicleEvent.SHIFT_TO_PARK),
        Case(PleosDriveSubscriptions.GEAR, "GEAR_DRIVE", "GEAR_REVERSE", VehicleEvent.SHIFT_TO_REVERSE),
        Case(PleosDriveSubscriptions.CRUISE_STATE, "ENABLED", "ACTIVATED", VehicleEvent.CRUISE_ON),
        Case(PleosDriveSubscriptions.CRUISE_STATE, "ACTIVATED", "ENABLED", VehicleEvent.CRUISE_OFF),
        Case(PleosDriveSubscriptions.STEERING_ANGLE, "0.00", "-400.00", VehicleEvent.STEER_BIG),
        Case(PleosDriveSubscriptions.DISTANCE_DRIVEN, "999.90", "1000.10", VehicleEvent.ODO_MILESTONE),
        Case(PleosDriveSubscriptions.RANGE_REMAINING, "250000.00", "49000.00", VehicleEvent.RANGE_LOW),
        Case(PleosDriveSubscriptions.RANGE_REMAINING, "49000.00", "19000.00", VehicleEvent.RANGE_CRITICAL),
    )

    @Test
    fun keysAreUniqueAndDoNotCollideWithTheBaseCatalog() {
        val keys = definitions.map { it.dataKey }
        assertEquals(11, keys.size)
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.none { key -> VehicleDataCatalog.definitions.any { it.dataKey == key } })
    }

    @Test
    fun everySignalHasAReactionAndMayBeSpokenWhileDriving() {
        definitions.forEach { definition ->
            assertTrue("${definition.dataKey} must have a reaction", definition.reactionRule != null || definition.reactionEvent != null)
            assertTrue("${definition.dataKey} is a driving signal", definition.allowWhileDriving)
        }
    }

    /** 11개 신호 모두 아래 경우표나 목표 속도 시험 중 하나에 걸려 있어야 한다(신호를 더하고 시험을 빼먹지 않게). */
    @Test
    fun everySignalIsCoveredByAWiringCase() {
        val covered = cases.map { it.key }.toSet() + PleosDriveSubscriptions.CRUISE_TARGET_SPEED
        assertEquals(definitions.map { it.dataKey }.toSet(), covered)
    }

    @Test
    fun everyPermissionIsAlreadyDeclaredInTheManifest() {
        val manifest = manifestText()
        val permissions = definitions.map { it.permission }.toSet()
        assertEquals(setOf("pleos.car.permission.CAR_LIGHTS", "pleos.car.permission.CAR_DRIVE"), permissions)
        permissions.forEach { assertTrue("$it must be declared in AndroidManifest.xml", manifest.contains("android:name=\"$it\"")) }
    }

    @Test
    fun eachSignalSpeaksItsOwnEventOnlyAfterTheBaselineValue() {
        cases.forEach { case ->
            val spoken = mutableListOf<Pair<VehicleEvent, Boolean>>()
            val repository = VehicleStateRepository({}, { event, allowWhileDriving -> spoken += event to allowWhileDriving })
            definitions.forEach(repository::registerDefinition)

            repository.observe(case.key, case.baseline)
            assertTrue("${case.key}: the first value is only a baseline", spoken.isEmpty())
            repository.observe(case.key, case.trigger)
            assertEquals("${case.key} ${case.baseline} -> ${case.trigger}", listOf(case.event to true), spoken)
            repository.observe(case.key, case.trigger)
            assertEquals("${case.key}: the same value again is not a change", 1, spoken.size)
            repository.close()
        }
    }

    @Test
    fun lampsSayNothingWhenTheyTurnOff() {
        val spoken = mutableListOf<VehicleEvent>()
        val repository = VehicleStateRepository({}, { event, _ -> spoken += event })
        definitions.forEach(repository::registerDefinition)

        val key = PleosLampSubscriptions.HIGH_BEAM
        repository.observe(key, "OFF")
        repository.observe(key, "ON")
        repository.observe(key, "OFF")
        repository.observe(key, "DAYTIME_RUNNING")
        repository.observe(key, "ON")

        assertEquals(listOf(VehicleEvent.HIGH_BEAM_ON, VehicleEvent.HIGH_BEAM_ON), spoken)
        repository.close()
    }

    @Test
    fun targetSpeedSpeaksOnceAfterTheLastButtonPress() {
        class Task(var cancelled: Boolean, val run: () -> Unit)
        val tasks = mutableListOf<Task>()
        val spoken = mutableListOf<VehicleEvent>()
        val repository = VehicleStateRepository(
            onSnapshot = {},
            onReaction = { event, allowWhileDriving ->
                assertTrue(allowWhileDriving)
                spoken += event
            },
            schedule = { delay, action ->
                assertEquals(2_000L, delay)
                val task = Task(false, action)
                tasks += task
                val cancel: () -> Unit = { task.cancelled = true }
                cancel
            },
        )
        definitions.forEach(repository::registerDefinition)

        val key = PleosDriveSubscriptions.CRUISE_TARGET_SPEED
        repository.observe(key, "13.89") // 기준값
        repository.observe(key, "15.00") // 버튼을 연타한다
        repository.observe(key, "16.00")
        assertTrue("nothing is spoken before the presses stop", spoken.isEmpty())

        tasks.filterNot { it.cancelled }.forEach { it.run() }
        assertEquals(listOf(VehicleEvent.CRUISE_TARGET_SET), spoken)
        repository.close()
    }

    @Test
    fun valuesAreRecordedInTheSnapshot() {
        val repository = VehicleStateRepository({}, { _, _ -> })
        definitions.forEach(repository::registerDefinition)

        repository.observe(PleosDriveSubscriptions.GEAR, "GEAR_PARK")
        repository.observe(PleosDriveSubscriptions.GEAR, "GEAR_DRIVE")
        repository.observe(PleosLampSubscriptions.HIGH_BEAM, "OFF")
        repository.observe(PleosLampSubscriptions.HIGH_BEAM, "ON")
        repository.observe(PleosDriveSubscriptions.STEERING_ANGLE, "0.00")
        repository.observe(PleosDriveSubscriptions.STEERING_ANGLE, "-45.00")

        val snapshot = repository.snapshot().associateBy { it.definition.dataKey }
        assertEquals("GEAR_DRIVE", snapshot.getValue(PleosDriveSubscriptions.GEAR).value)
        assertEquals("ON", snapshot.getValue(PleosLampSubscriptions.HIGH_BEAM).value)
        assertEquals("-45.00", snapshot.getValue(PleosDriveSubscriptions.STEERING_ANGLE).value)
        assertEquals(VehicleAvailability.ACTIVE, snapshot.getValue(PleosDriveSubscriptions.GEAR).availability)
        repository.close()
    }

    @Test
    fun registeredSignalsStayVisibleAsCheckingUntilTheCapabilityAnswers() {
        val repository = VehicleStateRepository({}, { _, _ -> })
        definitions.forEach(repository::registerDefinition)

        val snapshot = repository.snapshot().associateBy { it.definition.dataKey }
        assertTrue(definitions.all { snapshot.getValue(it.dataKey).availability == VehicleAvailability.CHECKING })

        repository.markUnsupported(PleosLampSubscriptions.FRONT_FOG, "앞 안개등 상태 읽기가 지원되지 않아요")
        val after = repository.snapshot().associateBy { it.definition.dataKey }
        assertEquals(VehicleAvailability.UNSUPPORTED, after.getValue(PleosLampSubscriptions.FRONT_FOG).availability)
        assertEquals(VehicleAvailability.CHECKING, after.getValue(PleosLampSubscriptions.REAR_FOG).availability)
        repository.close()
    }

    /** Why the groups register every definition up front: observe() silently drops keys nobody registered. */
    @Test
    fun observingAnUnregisteredKeyIsDropped() {
        val repository = VehicleStateRepository({}, { _, _ -> })
        repository.observe("not-registered", "1")
        assertTrue(repository.snapshot().none { it.definition.dataKey == "not-registered" })
        repository.close()
    }

    @Test
    fun decimalTextKeepsTwoDecimalsAndDropsNegativeZero() {
        assertNull(decimalText(null))
        assertEquals("0.00", decimalText(-0.0f))
        assertEquals("-45.00", decimalText(-45f))
        assertEquals("13.89", decimalText(13.8889f))
        assertEquals("250000.00", decimalText(250000f))
    }

    private fun manifestText(): String {
        val candidates = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml", "android/app/src/main/AndroidManifest.xml")
        val file = candidates.map(::File).firstOrNull { it.exists() }
            ?: error("AndroidManifest.xml not found from ${File(".").absolutePath}")
        return file.readText()
    }
}
