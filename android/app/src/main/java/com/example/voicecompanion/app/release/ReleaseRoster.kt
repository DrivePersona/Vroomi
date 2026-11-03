package com.example.voicecompanion.app.release

import com.example.voicecompanion.characterpack.model.CharacterPack
import com.example.voicecompanion.characterpack.model.CharacterPackState
import com.example.voicecompanion.characterpack.model.isRenderable
import com.example.voicecompanion.voicepack.domain.PackKind
import com.example.voicecompanion.voicepack.domain.PackState
import com.example.voicecompanion.voicepack.domain.VoicePack
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseRosterEntry(
    val characterId: String,
    val voicePackId: String,
    val releaseTier: String,
    val vrEligible: Boolean,
    val requiredGestureEventIds: Set<String>,
)

/** Versioned allowlist. Candidate assets stay hidden unless a reviewed roster entry passes every runtime gate. */
data class ReleaseRoster(val entries: List<ReleaseRosterEntry>) {
    fun eligibleCharacters(
        candidates: List<CharacterPack>,
        voicePacks: List<VoicePack>,
    ): List<CharacterPack> = entries.mapNotNull { entry ->
        val character = candidates.firstOrNull { it.id == entry.characterId } ?: return@mapNotNull null
        val voicePack = voicePacks.firstOrNull { it.id == entry.voicePackId } ?: return@mapNotNull null
        if (eligibilityFailures(entry, character, voicePack).isEmpty()) character else null
    }

    companion object {
        const val ASSET_PATH = "release-roster.json"

        /** The 13 dialogue slots are one greeting plus these 12 canonical vehicle clips. */
        val REQUIRED_VOICE_EVENT_IDS = setOf(
            "BRAKE", "TURN", "MODE", "TIRE", "WARNING", "DOOR", "WINDOW",
            "AMBIENT_LIGHT", "HVAC", "WIPER", "CHARGE", "ENERGY_LOW",
        )

        fun parse(json: String): ReleaseRoster {
            val root = JSONObject(json)
            require(root.optInt("schemaVersion", -1) == 1) { "release roster schemaVersion must be 1" }
            val array = root.optJSONArray("entries") ?: error("release roster entries must be an array")
            val entries = (0 until array.length()).map { index -> parseEntry(array.getJSONObject(index)) }
            require(entries.map { it.characterId }.distinct().size == entries.size) { "duplicate characterId" }
            require(entries.map { it.voicePackId }.distinct().size == entries.size) { "duplicate voicePackId" }
            return ReleaseRoster(entries)
        }

        fun eligibilityFailures(
            entry: ReleaseRosterEntry,
            character: CharacterPack?,
            voicePack: VoicePack?,
        ): List<String> = buildList {
            if (character == null) {
                add("character pack is missing")
            } else {
                if (character.id != entry.characterId) add("characterId does not match roster")
                if (character.state !is CharacterPackState.Ready || !character.isRenderable) add("character pack is not renderable and READY")
                val info = character.info
                if (info == null) {
                    add("character manifest info is missing")
                } else {
                    if (info.voicePackId != entry.voicePackId) add("character voicePackId does not match roster")
                    if (!info.provenance.commercialUse) add("character commercial use is not approved")
                }
                if (entry.vrEligible) {
                    val reactions = character.animations?.reactions.orEmpty()
                    if (entry.requiredGestureEventIds.isEmpty()) add("VR entry must declare required gestures")
                    entry.requiredGestureEventIds.filterNot { it in reactions }.forEach {
                        add("required VR gesture is missing: $it")
                    }
                }
            }

            if (voicePack == null) {
                add("voice pack is missing")
            } else {
                if (voicePack.id != entry.voicePackId) add("voicePackId does not match roster")
                if (voicePack.state !is PackState.Ready || voicePack.kind != PackKind.CLIP) add("voice pack is not a READY clip pack")
                val info = voicePack.info
                if (info == null) {
                    add("voice manifest info is missing")
                } else {
                    if (!info.provenance.consent) add("voice consent is not approved")
                    if (!info.provenance.commercialUse) add("voice commercial use is not approved")
                }
                if (voicePack.greeting() == null) add("greeting clip is missing")
                REQUIRED_VOICE_EVENT_IDS.filterNot(voicePack::canSpeak).forEach {
                    add("required voice clip is missing: $it")
                }
            }
        }

        private fun parseEntry(json: JSONObject): ReleaseRosterEntry {
            val characterId = json.getString("characterId").trim().also { require(it.isNotEmpty()) }
            val voicePackId = json.getString("voicePackId").trim().also { require(it.isNotEmpty()) }
            val releaseTier = json.getString("releaseTier").trim().also { require(it.isNotEmpty()) }
            val vrEligible = json.getBoolean("vrEligible")
            val gestures = json.optJSONArray("requiredGestureEventIds") ?: JSONArray()
            val requiredGestures = (0 until gestures.length()).map { gestures.getString(it).trim() }.toSet()
            require(requiredGestures.size == gestures.length()) { "duplicate requiredGestureEventIds for $characterId" }
            require(!vrEligible || releaseTier.equals("vr", ignoreCase = true)) { "VR eligibility requires releaseTier=vr for $characterId" }
            return ReleaseRosterEntry(characterId, voicePackId, releaseTier, vrEligible, requiredGestures)
        }
    }
}
