# Release voice/character plan v1

The internal candidate preview now pairs ten GLB candidates with ten VoiceDesign packs by stable ID. The app never infers a pairing from a display name.

| Character ID | Voice pack ID | Internal preview | Release |
| --- | --- | --- | --- |
| `ai.meshy.kuudere-os` | `ai.cooldere-vehicle-os.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.dojikko` | `ai.clumsy-junior.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.tsundere` | `ai.tsundere-lady.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.chuuni` | `ai.chuunibyo-demon-lord.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.onee` | `ai.gentle-older-sister.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.magical` | `ai.apprentice-magical-girl.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.mechanic` | `ai.car-geek-mechanic.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.fox` | `ai.fox-spirit.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.knight` | `ai.stoic-guardian-knight.voicedesign` | ready | blocked: character rights/audio coverage |
| `ai.meshy.vtuber` | `ai.in-car-streamer.voicedesign` | ready | blocked: character rights/audio coverage |

`ai.meshy.onii` is intentionally not paired: a dedicated adult male voice pack has not been approved, so the app must not reuse the sister voice.

## 13-slot dialogue contract

Every release pack must provide one greeting and these twelve vehicle slots:

`BRAKE`, `TURN`, `MODE`, `TIRE`, `WARNING`, `DOOR`, `WINDOW`, `AMBIENT_LIGHT`, `HVAC`, `WIPER`, `CHARGE`, `ENERGY_LOW`.

The ten packs already contain 13 generated WAV files each: `__GREETING` plus three variants for `BRAKE`, `TURN`, `MODE`, and `TIRE`. This is five covered slot categories, not all thirteen canonical vehicle slots. The other eight lines are text-ready in the private voice repository's matrix; the first completion pass adds one Qwen clip per missing slot (80 clips total), then auditions, hashes, and adds them to each pack manifest.

## Commercial-use gate

Voice manifests already record synthetic-source consent and commercial-use metadata. The Meshy character manifests remain `commercialUse: false` with the terms pending. Therefore `release-roster.json` remains empty and the app's internal preview must not be promoted to a release build. Promotion requires the private character approval matrix, all 13 audio slots, supported gesture checks, and the existing roster validator to pass.
