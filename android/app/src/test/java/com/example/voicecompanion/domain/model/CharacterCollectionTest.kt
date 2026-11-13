package com.example.voicecompanion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCollectionTest {
    private fun roster(count: Int) = List(count) { CharacterRef("char.${it + 1}", "캐릭터 ${it + 1}") }

    @Test
    fun `로스터 앞 5개는 기본이고 다음 11개는 가챠 후보다`() {
        val collection = CharacterCollection.of(roster(16), owned = emptySet(), equippedId = null)
        assertEquals((1..5).map { "char.$it" }, collection.basic.map { it.characterId })
        assertEquals((6..16).map { "char.$it" }, collection.pool.map { it.characterId })
        assertTrue(collection.basic.all { it.tier == SlotTier.BASIC })
        assertTrue(collection.pool.all { it.tier == SlotTier.POOL })
    }

    @Test
    fun `기본 캐릭터는 항상 쓸 수 있고 후보는 보유해야 쓸 수 있다`() {
        val collection = CharacterCollection.of(roster(16), owned = setOf("char.7"), equippedId = null)
        assertTrue(collection.basic.all { it.owned })
        assertEquals(listOf("char.7"), collection.pool.filter { it.owned }.map { it.characterId })
        assertEquals(1, collection.ownedPoolCount)
    }

    @Test
    fun `보유하지 않은 후보는 장착할 수 없다`() {
        val collection = CharacterCollection.of(roster(16), owned = setOf("char.7"), equippedId = null)
        assertTrue(collection.isEquippable("char.1"))
        assertTrue(collection.isEquippable("char.7"))
        assertFalse(collection.isEquippable("char.6"))
        assertFalse(collection.isEquippable("char.16"))
    }

    @Test
    fun `로스터에 없는 id와 첫 웨이브 밖 캐릭터는 장착할 수 없다`() {
        val collection = CharacterCollection.of(roster(20), owned = setOf("char.20", "ghost"), equippedId = null)
        assertFalse(collection.isEquippable("ghost"))
        assertFalse(collection.isEquippable("char.17")) // 17번째부터는 첫 웨이브 밖
        assertFalse(collection.isEquippable("char.20")) // 보유 집합에 있어도 칸이 없으면 장착할 수 없다
        assertTrue((collection.basic + collection.pool).none { it.characterId == "char.17" })
    }

    @Test
    fun `로스터가 모자라도 5개와 11개 칸을 유지하고 빈 칸은 미정이며 장착할 수 없다`() {
        val collection = CharacterCollection.of(roster(3), owned = emptySet(), equippedId = null)
        assertEquals(CharacterCollection.BASIC_SLOTS, collection.basic.size)
        assertEquals(CharacterCollection.POOL_SLOTS, collection.pool.size)
        val empty = collection.basic[4]
        assertNull(empty.characterId)
        assertEquals("미정", empty.name)
        assertFalse(empty.owned)
        assertTrue(collection.pool.all { it.characterId == null })
    }

    @Test
    fun `비어 있는 로스터도 빈 칸으로만 채운다`() {
        val collection = CharacterCollection.of(emptyList(), owned = setOf("char.1"), equippedId = "char.1")
        assertEquals(16, collection.basic.size + collection.pool.size)
        assertTrue((collection.basic + collection.pool).none { it.owned || it.equipped })
        assertFalse(collection.isEquippable("char.1"))
    }

    @Test
    fun `앞쪽 기본 캐릭터를 쓸 수 없어도 뒤의 후보가 기본으로 당겨지지 않는다`() {
        // char.3(기본)을 읽을 수 없다. 목록에서 빼지 않고 null로 두므로 칸 위치와 등급이 그대로다.
        val full: List<CharacterRef?> = roster(16).mapIndexed { i, ref -> if (i == 2) null else ref }
        val collection = CharacterCollection.of(full, owned = emptySet(), equippedId = null)

        assertEquals(listOf("char.1", "char.2", null, "char.4", "char.5"), collection.basic.map { it.characterId })
        assertEquals((6..16).map { "char.$it" }, collection.pool.map { it.characterId })
        assertTrue(collection.pool.all { it.tier == SlotTier.POOL && !it.owned })
        assertFalse(collection.isEquippable("char.6")) // 6번째는 여전히 보유해야 쓸 수 있다
        assertFalse(collection.basic[2].owned)
    }

    @Test
    fun `후보 칸을 쓸 수 없어도 다른 후보의 칸 위치와 보유 판정은 그대로다`() {
        val full: List<CharacterRef?> = roster(16).mapIndexed { i, ref -> if (i == 6) null else ref } // char.7
        val collection = CharacterCollection.of(full, owned = setOf("char.8", "char.7"), equippedId = "char.8")

        assertNull(collection.pool[1].characterId) // char.7 칸은 비어 있다
        assertFalse(collection.pool[1].owned) // 보유 집합에 있어도 쓸 수 없는 칸은 보유로 세지 않는다
        assertEquals("char.8", collection.pool[2].characterId)
        assertTrue(collection.pool[2].owned)
        assertTrue(collection.pool[2].equipped)
        assertEquals(1, collection.ownedPoolCount)
        assertFalse(collection.isEquippable("char.7"))
    }

    @Test
    fun `장착 중인 캐릭터에만 장착 표시가 붙고 보유와는 별개다`() {
        val collection = CharacterCollection.of(roster(16), owned = emptySet(), equippedId = "char.3")
        assertEquals(listOf("char.3"), (collection.basic + collection.pool).filter { it.equipped }.map { it.characterId })

        val lockedButEquipped = CharacterCollection.of(roster(16), owned = emptySet(), equippedId = "char.9")
        assertTrue(lockedButEquipped.pool.first { it.characterId == "char.9" }.equipped)
        assertFalse(lockedButEquipped.pool.first { it.characterId == "char.9" }.owned)
    }
}
