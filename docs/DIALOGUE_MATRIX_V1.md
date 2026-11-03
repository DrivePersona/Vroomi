# Dialogue Matrix v1 — DRAFT

This document is a content draft for issue #16. It contains one row per candidate character and canonical event ID in DIALOGUE_MATRIX_V1.csv (10 candidates × 13 slots = 130 rows). Every row is DRAFT; no text or voice is approved for recording or release.

## Roster and ID status

The ten candidates and voicePackId values come from the current inventory quoted in issue #16 and voice-pack-sdk#5. Candidate characterId values are inferred by removing .voicedesign from each voicePackId. This is a working convention only. Issue #3 is still open, so none of these entries is asserted to be releaseEligible; confirm inclusion and IDs against the approved roster before treating this matrix as canonical.

| Candidate characterId | Display name | Current voicePackId |
| --- | --- | --- |
| ai.cooldere-vehicle-os | 쿨데레 차량 OS | ai.cooldere-vehicle-os.voicedesign |
| ai.clumsy-junior | 덜렁이 신입 후배 | ai.clumsy-junior.voicedesign |
| ai.stoic-guardian-knight | 과묵한 수호 기사 | ai.stoic-guardian-knight.voicedesign |
| ai.tsundere-lady | 츤데레 아가씨 | ai.tsundere-lady.voicedesign |
| ai.chuunibyo-demon-lord | 중2병 마왕 | ai.chuunibyo-demon-lord.voicedesign |
| ai.gentle-older-sister | 다정한 연상 누나 | ai.gentle-older-sister.voicedesign |
| ai.apprentice-magical-girl | 견습 마법소녀 | ai.apprentice-magical-girl.voicedesign |
| ai.car-geek-mechanic | 차덕 정비 소녀 | ai.car-geek-mechanic.voicedesign |
| ai.fox-spirit | 여우 신령 | ai.fox-spirit.voicedesign |
| ai.in-car-streamer | 차 안 버튜버 | ai.in-car-streamer.voicedesign |

## Event and runtime alignment

The matrix uses the exact 13 canonical IDs in voice-pack-sdk#5: GREETING, BRAKE, TURN, MODE, TIRE, WARNING, DOOR, WINDOW, AMBIENT_LIGHT, HVAC, WIPER, CHARGE, ENERGY_LOW.

Current app event names differ for several slots: AMBIENT_LIGHT maps to app LIGHT, HVAC to COMFORT, and CHARGE to CHARGE_PORT. GREETING is a pack preview rather than a vehicle event. WIPER has no enabled speech event. The app exposes four energy bands and ENERGY_URGENT; it does not yet emit canonical ENERGY_LOW. Resolve these mappings before approved text is packaged.

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
| AMBIENT_LIGHT | LIGHT | Canonical ID adapter is needed |
| HVAC | COMFORT | Canonical ID adapter is needed |
| WIPER | None | Keep silent until policy approval |
| CHARGE | CHARGE_PORT | Charge start/completion signal remains unavailable in Vehicle SDK 2.3.2 |
| ENERGY_LOW | ENERGY_URGENT | Canonical event and neutral clip policy need an explicit adapter |

## Draft voice personas

Persona descriptions are inferred from the candidate display names because approved character bibles were not found in the public repository. They are short, adult-facing, and avoid imitation of real people or existing commercial characters. Their wording needs content-owner review.

## Safety and activation notes

- WARNING uses the same neutral sentence for every candidate; it must have no gesture. ENERGY_LOW is also shared and neutral. The #14 app policy currently routes safety events through system TTS, while voice-pack-sdk#5 expects canonical clips; resolve this policy conflict before audio production.
- BRAKE and TURN are edge-triggered. MODE, TIRE, and WARNING remain inactive until their public Vehicle Service signal/capability is verified.
- DOOR, WINDOW, AMBIENT_LIGHT, HVAC, and CHARGE are stationary-only. CHARGE start/completion remains inactive until a supported state-change signal exists.
- WIPER remains silent by default until #12 and #14 explicitly authorize speech.
- No WAV, model, reference voice, or audition asset is included. Voice generation and packaging must wait for TEXT_APPROVED.
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
