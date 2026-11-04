package com.example.voicecompanion.app.release

import com.example.voicecompanion.characterpack.model.CharacterAnimations
import com.example.voicecompanion.characterpack.model.CharacterModel
import com.example.voicecompanion.characterpack.model.CharacterOverlaySpec
import com.example.voicecompanion.characterpack.model.CharacterPack
import com.example.voicecompanion.characterpack.model.CharacterPackInfo
import com.example.voicecompanion.characterpack.model.CharacterPackState
import com.example.voicecompanion.characterpack.model.CharacterProvenance
import com.example.voicecompanion.characterpack.model.SupportedGesture
import com.example.voicecompanion.voicepack.domain.ClipGesture
import com.example.voicecompanion.voicepack.domain.PackGreeting
import com.example.voicecompanion.voicepack.domain.PackInfo
import com.example.voicecompanion.voicepack.domain.PackKind
import com.example.voicecompanion.voicepack.domain.PackState
import com.example.voicecompanion.voicepack.domain.SpeakRequest
import com.example.voicecompanion.voicepack.domain.Voice
import com.example.voicecompanion.voicepack.domain.VoicePack
import com.example.voicecompanion.voicepack.model.Provenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseRosterTest {
    private val entry = ReleaseRosterEntry("char.one", "voice.one", "launch", false, emptySet())

    @Test
    fun `empty versioned roster parses and leaves candidate packs out`() {
        val roster = ReleaseRoster.parse("""{"schemaVersion":1,"entries":[]}""")
        assertTrue(roster.eligibleCharacters(listOf(character()), listOf(voicePack())).isEmpty())
        assertTrue(roster.eligibleVoicePacks(listOf(character()), listOf(voicePack())).isEmpty())
    }

    @Test
    fun `duplicate character or voice ids invalidate the roster`() {
        val first = """{"characterId":"char.one","voicePackId":"voice.one","releaseTier":"launch","vrEligible":false}"""
        val duplicateCharacter = first.replace("voice.one", "voice.two")
        val duplicateVoice = first.replace("char.one", "char.two")
        assertTrue(runCatching { ReleaseRoster.parse("""{"schemaVersion":1,"entries":[$first,$duplicateCharacter]}""") }.isFailure)
        assertTrue(runCatching { ReleaseRoster.parse("""{"schemaVersion":1,"entries":[$first,$duplicateVoice]}""") }.isFailure)
    }

    @Test
    fun `missing character and voice pack are rejected`() {
        val reasons = ReleaseRoster.eligibilityFailures(entry, character = null, voicePack = null)
        assertTrue(reasons.contains("character pack is missing"))
        assertTrue(reasons.contains("voice pack is missing"))
    }

    @Test
    fun `all thirteen dialogue slots and approved rights are required`() {
        val completeVoice = voicePack()
        assertTrue(ReleaseRoster.eligibilityFailures(entry, character(), completeVoice).isEmpty())

        val incompleteVoice = voicePack(availableEvents = ReleaseRoster.REQUIRED_VOICE_EVENT_IDS - "ENERGY_LOW")
        assertTrue(ReleaseRoster.eligibilityFailures(entry, character(), incompleteVoice).any { it.contains("ENERGY_LOW") })

        val unlicensedCharacter = character(commercialUse = false)
        assertTrue(ReleaseRoster.eligibilityFailures(entry, unlicensedCharacter, completeVoice).contains("character commercial use is not approved"))

        val unlicensedVoice = voicePack(commercialUse = false)
        assertTrue(ReleaseRoster.eligibilityFailures(entry, character(), unlicensedVoice).contains("voice commercial use is not approved"))
    }

    @Test
    fun `voice id link and declared VR gestures must match`() {
        val mismatchedLink = character(voicePackId = "voice.other")
        assertTrue(ReleaseRoster.eligibilityFailures(entry, mismatchedLink, voicePack()).contains("character voicePackId does not match roster"))

        val vrEntry = entry.copy(releaseTier = "vr", vrEligible = true, requiredGestureEventIds = setOf("BRAKE"))
        assertTrue(ReleaseRoster.eligibilityFailures(vrEntry, character(), voicePack()).contains("required VR gesture is missing: BRAKE"))

        val mappedToUnsupportedGesture = character(gestures = mapOf("BRAKE" to "jump"), supportedGestureIds = emptySet())
        assertTrue(ReleaseRoster.eligibilityFailures(vrEntry, mappedToUnsupportedGesture, voicePack())
            .any { it.contains("not supported by the character manifest") })
    }

    @Test
    fun `release roster validates per-clip gesture IDs and requires one-shot character actions`() {
        val packWithGesture = voicePack(gestures = mapOf("BRAKE" to ClipGesture("surprise", 120)))
        assertTrue(ReleaseRoster.eligibilityFailures(entry, character(gestures = mapOf("BRAKE" to "surprise")), packWithGesture).isEmpty())
        assertTrue(ReleaseRoster.eligibilityFailures(entry, character(), packWithGesture)
            .any { it.contains("voice clip gesture is not supported") })
        val looping = character(gestures = mapOf("BRAKE" to "surprise"), oneShot = false)
        assertTrue(ReleaseRoster.eligibilityFailures(entry, looping, packWithGesture)
            .any { it.contains("voice clip gesture must be one-shot") })
    }

    @Test
    fun `picker allowlist contains only fully eligible roster matches`() {
        val roster = ReleaseRoster(listOf(entry))
        val candidate = character()
        assertEquals(listOf(candidate), roster.eligibleCharacters(listOf(candidate), listOf(voicePack())))
        assertEquals(listOf(voicePack().id), roster.eligibleVoicePacks(listOf(candidate), listOf(voicePack())).map { it.id })
        assertTrue(roster.eligibleCharacters(listOf(character(commercialUse = false)), listOf(voicePack())).isEmpty())
        assertTrue(roster.eligibleVoicePacks(emptyList(), listOf(voicePack())).isEmpty())
        assertTrue(roster.eligibleCharacters(listOf(character()), emptyList()).isEmpty())
    }

    @Test
    fun `character binding resolves only the exact READY manifest voice id`() {
        val selectedCharacter = character(voicePackId = "voice.two")
        val sameNameWrongId = voicePack(packId = "voice.one")
        val exactReady = voicePack(packId = "voice.two")
        val exactInvalid = voicePack(packId = "voice.two", packState = PackState.Invalid("bad manifest"))

        assertEquals(exactReady, CharacterVoiceBinding.resolve(selectedCharacter, listOf(sameNameWrongId, exactReady)))
        assertEquals(null, CharacterVoiceBinding.resolve(selectedCharacter, listOf(sameNameWrongId)))
        assertEquals(null, CharacterVoiceBinding.resolve(selectedCharacter, listOf(exactInvalid)))
        assertEquals(null, CharacterVoiceBinding.resolve(character(voicePackId = null), listOf(exactReady)))
    }

    @Test
    fun `dialogue matrix exposes greeting and twelve canonical ids only for its linked pack`() {
        val pack = voicePack()
        val rows = CharacterDialogueMatrix.rows(pack.id, pack)
        assertEquals(13, rows.size)
        assertEquals("__GREETING", rows.first().eventId)
        assertEquals(ReleaseRoster.REQUIRED_VOICE_EVENT_IDS, rows.drop(1).map { it.eventId }.toSet())
        assertTrue(CharacterDialogueMatrix.rows("other.voice", pack).isEmpty())
    }

    @Test
    fun `release roster rejects a required event with no indexed clip`() {
        val invalidCount = voicePack(variantCounts = mapOf("BRAKE" to 0))
        assertTrue(ReleaseRoster.eligibilityFailures(entry, character(), invalidCount)
            .contains("required voice clip has no indexed variant: BRAKE"))
    }

    @Test
    fun `gesture resolver returns at most one declared one-shot reaction`() {
        val valid = CharacterAnimations(
            "idle",
            mapOf("BRAKE" to "surprise"),
            mapOf("surprise" to SupportedGesture("jump", 700, oneShot = true)),
        )
        assertEquals(SupportedGesture("jump", 700, oneShot = true), CharacterGestureResolver.resolve(valid, "BRAKE"))
        assertEquals(SupportedGesture("jump", 700, oneShot = true), CharacterGestureResolver.resolve(valid.copy(reactions = emptyMap()), "BRAKE", "surprise"))
        assertEquals(null, CharacterGestureResolver.resolve(valid, "BRAKE", "undeclared"))
        assertEquals(null, CharacterGestureResolver.resolve(valid, "WARNING"))
        assertEquals(null, CharacterGestureResolver.resolve(valid.copy(supportedGestures = emptyMap()), "BRAKE"))
        assertEquals(null, CharacterGestureResolver.resolve(
            valid.copy(supportedGestures = mapOf("surprise" to SupportedGesture("jump", 700, oneShot = false))),
            "BRAKE",
        ))
    }

    private fun character(
        voicePackId: String? = entry.voicePackId,
        commercialUse: Boolean = true,
        gestures: Map<String, String> = emptyMap(),
        supportedGestureIds: Set<String> = gestures.values.toSet(),
        oneShot: Boolean = true,
    ) = object : CharacterPack {
        private val characterAnimations = CharacterAnimations(
            "idle",
            gestures,
            gestures.values.filter { it in supportedGestureIds }.associateWith { SupportedGesture(it, 500, oneShot) },
        )
        override val id = entry.characterId
        override val name = "Character"
        override val state: CharacterPackState = CharacterPackState.Ready
        override val info = CharacterPackInfo(
            "1.0.0",
            CharacterProvenance("original", "Proprietary", commercialUse, null),
            voicePackId,
        )
        override val model = CharacterModel(byteArrayOf(1), characterAnimations)
        override val sprite = null
        override val previewPng = null
        override val animations: CharacterAnimations = characterAnimations
        override val overlay = CharacterOverlaySpec.DEFAULT
    }

    private fun voicePack(
        availableEvents: Set<String> = ReleaseRoster.REQUIRED_VOICE_EVENT_IDS,
        commercialUse: Boolean = true,
        consent: Boolean = true,
        packId: String = entry.voicePackId,
        packState: PackState = PackState.Ready,
        variantCounts: Map<String, Int> = emptyMap(),
        gestures: Map<String, ClipGesture> = emptyMap(),
    ) = object : VoicePack {
        override val id = packId
        override val name = "Voice"
        override val kind = PackKind.CLIP
        override val freeText = false
        override val state: PackState = packState
        override val voice = object : Voice {
            override fun speak(request: SpeakRequest, note: (String) -> Unit, done: () -> Unit) = true
            override fun stop() = Unit
        }
        override val info = PackInfo(
            "1.0.0",
            Provenance("original", "self-recorded", consent, "Proprietary", commercialUse, null),
        )
        override fun canSpeak(eventId: String) = eventId in availableEvents
        override fun greeting() = PackGreeting("__GREETING", "안녕하세요.")
        override fun variantCount(eventId: String) = when {
            eventId == "__GREETING" -> 1
            eventId in availableEvents -> variantCounts[eventId] ?: 1
            else -> 0
        }
        override fun lineFor(eventId: String, variantIndex: Int): String? =
            if (variantIndex != 0 || variantCount(eventId) == 0) null
            else if (eventId == "__GREETING") "안녕하세요." else "대사 $eventId"
        override fun gestureFor(eventId: String, variantIndex: Int) = gestures[eventId]?.takeIf { variantIndex == 0 }
    }
}
