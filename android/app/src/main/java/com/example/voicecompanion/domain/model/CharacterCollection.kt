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
 *
 * 등급(기본/후보)은 [of]에 넘기는 목록의 **위치**로만 정한다. 그 목록은 걸러내기 전의 출시 로스터 순서이고, 지금 쓸 수 없는
 * 캐릭터(읽을 수 없는 팩·부적격)는 목록에서 빼지 않고 null로 둔다. 빼 버리면 뒤의 후보가 한 칸씩 당겨져, 보유하지 않았는데
 * 기본 캐릭터가 되어 장착할 수 있게 된다.
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

        /** [roster]는 걸러내기 전의 출시 로스터 순서다. 쓸 수 없는 칸은 null이고, 그 칸은 비어 있는 채로 등급 위치를 지킨다. */
        fun of(
            roster: List<CharacterRef?>,
            owned: Set<String>,
            equippedId: String?,
            pendingNames: List<String?> = emptyList(),
        ): CharacterCollection {
            fun slot(index: Int, tier: SlotTier): CollectionSlot {
                val ref = roster.getOrNull(index)
                return CollectionSlot(
                characterId = ref?.id,
                name = ref?.name ?: pendingNames.getOrNull(index) ?: EMPTY_NAME,
                tier = tier,
                owned = ref != null && (tier == SlotTier.BASIC || ref.id in owned),
                equipped = ref != null && ref.id == equippedId,
                )
            }
            return CharacterCollection(
                basic = List(BASIC_SLOTS) { slot(it, SlotTier.BASIC) },
                pool = List(POOL_SLOTS) { slot(BASIC_SLOTS + it, SlotTier.POOL) },
            )
        }
    }
}

/**
 * 내부 미리보기 전용 기본 5칸 지정(2026-11-18 결정): 여우 무녀를 남기고 새로 만든 캐릭터로 바꾼다.
 * 팩이 아직 없는 캐릭터(서리·곰지·핀)는 칸을 비워 둔 채 이름만 보여 준다 — 팩이 생기면 같은 id로 자동 연결된다.
 * 출시 로스터(release-roster.json)와는 무관하고, 은퇴한 캐릭터는 기본·후보 어느 칸에도 나오지 않는다.
 */
object InternalPreviewRoster {
    /** 기본 5칸의 (캐릭터 id, 팩이 없을 때 보일 이름). ai.meshy.vtuber는 새 방송 진행자 디자인이 들어올 자리다. */
    val BASIC: List<Pair<String, String>> = listOf(
        "ai.meshy.fox" to "여우 무녀",
        "ai.meshy.vtuber" to "차 안 버튜버",
        "ai.seori" to "서리",
        "ai.gomji" to "곰지",
        "ai.pin" to "핀",
    )

    val RETIRED: Set<String> = setOf(
        "ai.meshy.chuuni", "ai.meshy.dojikko", "ai.meshy.knight", "ai.meshy.kuudere-os",
    )

    /** 지정한 기본 5칸을 앞에 두고, 은퇴한 캐릭터를 뺀 나머지 후보를 원래 순서로 잇는다. */
    fun order(candidateIds: List<String>): List<String> {
        val basic = BASIC.map { it.first }
        return basic + candidateIds.filter { it !in basic && it !in RETIRED }
    }

    fun pendingNames(): List<String?> = BASIC.map { it.second }
}
