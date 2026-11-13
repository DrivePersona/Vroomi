package com.example.voicecompanion.domain.model

/** 캐릭터 선택기가 다루는 가장 작은 값. 팩 SDK 타입에 묶이지 않는다. */
data class CharacterRef(val id: String, val name: String)

enum class SlotTier { BASIC, POOL }

/** [characterId]가 null이면 아직 명단이 채워지지 않은 빈 칸이다. */
data class CollectionSlot(
    val characterId: String?,
    val name: String,
    val tier: SlotTier,
    val owned: Boolean,
    val equipped: Boolean,
)

/**
 * 출시 로스터 순서대로 앞 [BASIC_SLOTS]개는 항상 쓸 수 있는 기본 캐릭터, 다음 [POOL_SLOTS]개는 획득해야 하는 첫 후보 풀이다(이슈 #37).
 * 보유 여부는 [of]에 넘기는 보유 id 집합이 정한다. 보유와 장착은 별개이고, 보유하지 않은 후보는 장착할 수 없다.
 * 17번째부터는 첫 웨이브 밖이라 선택기에 나오지 않는다. 전체 목표 33개는 그대로다.
 */
class CharacterCollection private constructor(
    val basic: List<CollectionSlot>,
    val pool: List<CollectionSlot>,
) {
    val ownedPoolCount: Int get() = pool.count { it.characterId != null && it.owned }

    fun isEquippable(characterId: String): Boolean =
        (basic + pool).any { it.characterId == characterId && it.owned }

    companion object {
        const val BASIC_SLOTS = 5
        const val POOL_SLOTS = 11
        private const val EMPTY_NAME = "미정"

        fun of(roster: List<CharacterRef>, owned: Set<String>, equippedId: String?): CharacterCollection {
            fun slot(ref: CharacterRef?, tier: SlotTier) = CollectionSlot(
                characterId = ref?.id,
                name = ref?.name ?: EMPTY_NAME,
                tier = tier,
                owned = ref != null && (tier == SlotTier.BASIC || ref.id in owned),
                equipped = ref != null && ref.id == equippedId,
            )
            return CharacterCollection(
                basic = List(BASIC_SLOTS) { slot(roster.getOrNull(it), SlotTier.BASIC) },
                pool = List(POOL_SLOTS) { slot(roster.getOrNull(BASIC_SLOTS + it), SlotTier.POOL) },
            )
        }
    }
}
