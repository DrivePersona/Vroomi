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

    @Test
    fun keysAreUniqueAndDoNotCollideWithTheBaseCatalog() {
        val keys = definitions.map { it.dataKey }
        assertEquals(11, keys.size)
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.none { key -> VehicleDataCatalog.definitions.any { it.dataKey == key } })
    }

    @Test
    fun signalsAreStateOnlyAndNeverCarryAReaction() {
        assertTrue(definitions.all { it.reactionEvent == null && it.reactionPolicy == null })
    }

    @Test
    fun everyPermissionIsAlreadyDeclaredInTheManifest() {
        val manifest = manifestText()
        val permissions = definitions.map { it.permission }.toSet()
        assertEquals(setOf("pleos.car.permission.CAR_LIGHTS", "pleos.car.permission.CAR_DRIVE"), permissions)
        permissions.forEach { assertTrue("$it must be declared in AndroidManifest.xml", manifest.contains("android:name=\"$it\"")) }
    }

    @Test
    fun valuesAreRecordedAndNeverEmitReactions() {
        val events = mutableListOf<VehicleEvent>()
        val repository = VehicleStateRepository({}, { event, _ -> events += event })
        definitions.forEach(repository::registerDefinition)

        repository.observe(PleosDriveSubscriptions.GEAR, "GEAR_PARK")
        repository.observe(PleosDriveSubscriptions.GEAR, "GEAR_DRIVE")
        repository.observe(PleosLampSubscriptions.HIGH_BEAM, "OFF")
        repository.observe(PleosLampSubscriptions.HIGH_BEAM, "ON")
        repository.observe(PleosDriveSubscriptions.STEERING_ANGLE, "0.00")
        repository.observe(PleosDriveSubscriptions.STEERING_ANGLE, "-45.00")

        assertTrue(events.isEmpty())
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
