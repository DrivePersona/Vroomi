package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleEvent
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
        val repository = VehicleStateRepository({}, events::add)

        repository.observe(VehicleDataCatalog.BRAKE, "true")
        repository.observe(VehicleDataCatalog.BRAKE, "true")

        assertTrue(events.isEmpty())
        assertEquals("true", repository.snapshot().first { it.definition.dataKey == VehicleDataCatalog.BRAKE }.value)
    }

    @Test
    fun onlyRelevantValueTransitionsEmitEvents() {
        val events = mutableListOf<VehicleEvent>()
        val repository = VehicleStateRepository({}, events::add)

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
        val repository = VehicleStateRepository({}, {})
        repository.markUnsupported(VehicleDataCatalog.SEAT_BELT_RIGHT, "좌석 미지원")
        repository.markError(VehicleDataCatalog.BRAKE, "권한 오류")

        val snapshot = repository.snapshot().associateBy { it.definition.dataKey }
        assertEquals(VehicleAvailability.UNSUPPORTED, snapshot.getValue(VehicleDataCatalog.SEAT_BELT_RIGHT).availability)
        assertEquals("좌석 미지원", snapshot.getValue(VehicleDataCatalog.SEAT_BELT_RIGHT).detail)
        assertEquals(VehicleAvailability.ERROR, snapshot.getValue(VehicleDataCatalog.BRAKE).availability)
        assertEquals("권한 오류", snapshot.getValue(VehicleDataCatalog.BRAKE).detail)
        assertNull(snapshot.getValue(VehicleDataCatalog.BRAKE).value)
    }
}
