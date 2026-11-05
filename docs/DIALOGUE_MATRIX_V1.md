# Dialogue Matrix v1 — DRAFT

This file is an older public content draft for issue #16. It contains 10 characters × 13 dialogue slots = 130 draft rows; it is **not** the product character count. The release target is 33 characters × 13 slots each = 429 slots. See [the stable 33-pair target inventory](CHARACTER_TARGET_INVENTORY_V1.csv) and [the current coverage report](RELEASE_VOICE_CHARACTER_PLAN_V1.md). Every row here is DRAFT; no text or voice is approved for recording or release.

## Roster and ID status

The ten names below are the first dialogue-discussion subset. This legacy table was assembled by display name and is not a release pairing source; production pairing uses the stable ID pairs in `CHARACTER_TARGET_INVENTORY_V1.csv` and the pack manifests. Do not infer that the target is ten characters from this file.

| Candidate characterId | Display name | Current voicePackId |
| --- | --- | --- |
| ai.meshy.kuudere-os | 쿨데레 차량 OS | ai.cooldere-vehicle-os.voicedesign |
| ai.meshy.dojikko | 덜렁이 신입 후배 | ai.clumsy-junior.voicedesign |
| ai.meshy.knight | 과묵한 수호 기사 | ai.stoic-guardian-knight.voicedesign |
| ai.meshy.tsundere | 츤데레 아가씨 | ai.tsundere-lady.voicedesign |
| ai.meshy.chuuni | 중2병 마왕 | ai.chuunibyo-demon-lord.voicedesign |
| ai.meshy.onee | 다정한 연상 누나 | ai.gentle-older-sister.voicedesign |
| ai.meshy.magical | 견습 마법소녀 | ai.apprentice-magical-girl.voicedesign |
| ai.meshy.mechanic | 차덕 정비 소녀 | ai.car-geek-mechanic.voicedesign |
| ai.meshy.fox | 여우 신령 | ai.fox-spirit.voicedesign |
| ai.meshy.vtuber | 차 안 버튜버 | ai.in-car-streamer.voicedesign |

## Event and runtime alignment

The matrix uses the exact 13 canonical IDs in voice-pack-sdk#5: GREETING, BRAKE, TURN, MODE, TIRE, WARNING, DOOR, WINDOW, AMBIENT_LIGHT, HVAC, WIPER, CHARGE, ENERGY_LOW.

The app adapts its runtime names to these canonical IDs before voice-pack lookup, character-pack lookup, and speech requests: AMBIENT_LIGHT maps from LIGHT, HVAC from COMFORT, CHARGE from CHARGE_PORT, and ENERGY_LOW from ENERGY_URGENT. GREETING is a pack preview rather than a vehicle event. WIPER has no enabled speech event. Only ENERGY_LOW has a v1 clip slot; the app keeps the other three energy bands silent.

| Canonical eventId | Current app mapping | Status |
| --- | --- | --- |
| GREETING | Voice-pack greeting preview | Available outside vehicle events |
| BRAKE | BRAKE | Capability and transition implemented; Pleos E2E remains required |
| TURN | TURN | Capability and transition implemented; Pleos E2E remains required |
| MODE | MODE | Vehicle callback not verified on current emulator |
| TIRE | TIRE | Threshold and supported capability still require verification |
| WARNING | WARNING | Public signal mapping still requires verification |
| DOOR | DOOR | Stationary-only input; release capability needs verification |
| WINDOW | WINDOW | Stationary-only input; release capability needs verification |
| AMBIENT_LIGHT | LIGHT → AMBIENT_LIGHT | Canonical ID adapter implemented in #14; Pleos signal E2E remains required |
| HVAC | COMFORT → HVAC | Canonical ID adapter implemented in #14; Pleos signal E2E remains required |
| WIPER | None | Keep silent until policy approval |
| CHARGE | CHARGE_PORT → CHARGE | Canonical ID adapter implemented in #14; charge start/completion signal remains unavailable in Vehicle SDK 2.3.2 |
| ENERGY_LOW | ENERGY_URGENT → ENERGY_LOW | Canonical ID adapter implemented in #14; intermediate energy bands stay silent |

## Draft voice personas

The persona descriptions in this 10-character draft were inferred from candidate display names because approved character bibles were not found in the public repository. They are short, adult-facing, and avoid imitation of real people or existing commercial characters. Their wording needs content-owner review; they do not replace the 33 per-character profiles in the private voice-pack workstream.

## Safety and activation notes

- WARNING uses the same neutral sentence for every candidate; it must have no gesture. ENERGY_LOW is also shared and neutral. The #14 app policy now prefers an approved neutral voice-pack clip and uses built-in system TTS only when that clip is unavailable; safety events remain excluded from character gestures.
- The app emits ENERGY_URGENT internally and maps it to the canonical ENERGY_LOW pack ID. Candidate voice packs do not yet contain approved canonical safety clips; until one is available, the app falls back to the built-in system voice without a character gesture.
- BRAKE and TURN are edge-triggered. MODE, TIRE, and WARNING remain inactive until their public Vehicle Service signal/capability is verified.
- DOOR, WINDOW, AMBIENT_LIGHT, HVAC, and CHARGE are stationary-only. CHARGE start/completion remains inactive until a supported state-change signal exists.
- WIPER remains silent by default until #12 and #14 explicitly authorize speech.
- No approved WAV, model, reference voice, or audition asset is included in this public draft. Local audition outputs do not count as approved production audio; release packaging still requires TEXT_APPROVED.
- sourceCapability and drivingPolicy values describe issue #16 intended contract; they do not claim that the current app can observe or speak every slot.

## Approval and production handoff

| Status | Meaning | Next step |
| --- | --- | --- |
| DRAFT | Current matrix text and inferred persona; no approval | Content owner reviews roster, IDs, persona, wording, and policies |
| TEXT_APPROVED | Text is approved for one confirmed release character | Approve one reference voice design, then generate 13 separate WAVs in the private SDK |
| AUDIO_GENERATED | WAVs use the approved voice design | Listen for pronunciation, clipped endings, acting intensity, overlap, and tail |
| AUDIO_APPROVED | Human listening review passed | Update private manifest, hash, byte size, duration, and event mapping |
| PACK_READY | Private pack checks pass with exactly one clip per canonical event | Run public #5 release gate and character-to-voice mapping checks |
| RELEASE_READY | Roster, rights, pack, and app E2E checks pass | Prepare a reviewed release candidate |

The private pack target is 24 kHz, mono, PCM16. Keep reference audio, Qwen model files, audition outputs, and approved WAV binaries out of this public repository. Do not start voice generation from DRAFT rows.
