package com.example.voicecompanion.data.vehicle

import com.example.voicecompanion.domain.model.EnergyLevelTracker
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleEvent
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 에너지 신호(SOC·연료·충전구)가 저장소에 등록돼 실제로 반응을 내는지 본다.
 * 회귀 배경: 정의를 등록하지 않아 값과 반응이 모두 버려졌고, 충전구는 반응 방식이 없어 말하지 않았다(2026-11-15 AVD 실측).
 */
class EnergySignalsTest {
    private val definitions = PleosEnergySubscriptions.definitions

    private fun repository(now: () -> Long = { 0L }, spoken: MutableList<Pair<VehicleEvent, Boolean>>) =
        VehicleStateRepository({}, { event, allowWhileDriving -> spoken += event to allowWhileDriving }, nowMonotonicMs = now)
            .also { repo -> definitions.forEach(repo::registerDefinition) }

    @Test
    fun keysAreUniqueAndDoNotCollideWithOtherSignals() {
        val keys = definitions.map { it.dataKey }
        assertEquals(keys.size, keys.toSet().size)
        val others = VehicleDataCatalog.definitions + PleosLampSubscriptions.definitions + PleosDriveSubscriptions.definitions
        assertTrue(keys.none { key -> others.any { it.dataKey == key } })
    }

    @Test
    fun energySignalsAreNeverSpokenWhileDrivingAndNeedOnlyTheEnergyPermission() {
        definitions.forEach { assertFalse("${it.dataKey} is a stationary signal", it.allowWhileDriving) }
        val permission = "pleos.car.permission.CAR_ENERGY"
        assertEquals(setOf(permission), definitions.map { it.permission }.toSet())
        val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml")).first(File::exists).readText()
        assertTrue("$permission must be declared", manifest.contains("android:name=\"$permission\""))
    }

    @Test
    fun chargePortSpeaksWhenItOpensOrClosesAfterTheBaseline() {
        var now = 0L
        val spoken = mutableListOf<Pair<VehicleEvent, Boolean>>()
        val repo = repository({ now }, spoken)

        repo.observe(PleosEnergySubscriptions.CHARGE_PORT_OPEN, "false")
        assertTrue("the first value is only a baseline", spoken.isEmpty())
        repo.observe(PleosEnergySubscriptions.CHARGE_PORT_OPEN, "true")
        assertEquals(listOf(VehicleEvent.CHARGE_PORT to false), spoken)
        repo.observe(PleosEnergySubscriptions.CHARGE_PORT_OPEN, "true")
        assertEquals("the same value again is not a change", 1, spoken.size)
        now += VehicleEvent.CHARGE_PORT.cooldownMs
        repo.observe(PleosEnergySubscriptions.CHARGE_PORT_OPEN, "false")
        assertEquals(2, spoken.size)
        assertEquals("charge port value is recorded", "false",
            repo.snapshot().first { it.definition.dataKey == PleosEnergySubscriptions.CHARGE_PORT_OPEN }.value)
        repo.close()
    }

    /** 말하지 않는 범주 전환이 바로 뒤의 '부족' 경고를 막으면 안 된다(같은 순간이라도 범주마다 이벤트가 나와야 한다). */
    @Test
    fun everyBandChangeReachesTheEngineEvenInQuickSuccession() {
        val spoken = mutableListOf<Pair<VehicleEvent, Boolean>>()
        val repo = repository(spoken = spoken)
        val tracker = EnergyLevelTracker()
        val soc = PleosEnergySubscriptions.STATE_OF_CHARGE
        val band = PleosEnergySubscriptions.EV_ENERGY_BAND

        PleosEnergySubscriptions.observeEnergyLevel(repo, soc, band, tracker, 80)
        assertTrue("the first value is only a baseline", spoken.isEmpty())
        listOf(50, 25, 10).forEach { PleosEnergySubscriptions.observeEnergyLevel(repo, soc, band, tracker, it) }

        assertEquals(
            listOf(VehicleEvent.ENERGY_COMFORTABLE, VehicleEvent.ENERGY_ATTENTION, VehicleEvent.ENERGY_URGENT).map { it to false },
            spoken,
        )
        val values = repo.snapshot().associate { it.definition.dataKey to it.value }
        assertEquals("10", values[soc])
        assertEquals("URGENT", values[band])
        repo.close()
    }

    @Test
    fun fuelPercentUsesTheSameBandReactions() {
        val spoken = mutableListOf<Pair<VehicleEvent, Boolean>>()
        val repo = repository(spoken = spoken)
        val tracker = EnergyLevelTracker()

        PleosEnergySubscriptions.observeEnergyLevel(repo, PleosEnergySubscriptions.FUEL_PERCENT, PleosEnergySubscriptions.FUEL_ENERGY_BAND, tracker, 40)
        PleosEnergySubscriptions.observeEnergyLevel(repo, PleosEnergySubscriptions.FUEL_PERCENT, PleosEnergySubscriptions.FUEL_ENERGY_BAND, tracker, 12)

        assertEquals(listOf(VehicleEvent.ENERGY_URGENT to false), spoken)
        repo.close()
    }
}
