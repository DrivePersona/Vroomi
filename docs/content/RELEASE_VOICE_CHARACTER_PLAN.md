---
type: spec
update: rewrite
---

# Character and voice release target v1

## Fixed scope

The release target is **33 unique characters**, each paired by stable IDs with one voice pack. The number **13 means dialogue slots per character**: one greeting plus 12 vehicle-state lines. The complete target is therefore **33 × 13 = 429 slots**; 13 is not a character limit.

`CHARACTER_TARGET_INVENTORY.csv` is the countable target inventory. It has 33 stable character/voice ID pairs: 10 baseline pairs and 23 VoiceDesign expansion pairs. It is a target list, not the app's release allowlist.

## What the app can show today

| Surface | Selectable characters | Packaged assets | Meaning |
| --- | ---: | --- | --- |
| Local internal debug preview, after importing the 23 VoiceDesign packs | 33 | 10 baseline pairs + 23 new audition pairs | Team audition only |
| Current internal CI APK (`internalRelease`) | 0 | Published content AARs contain 12 character manifests and 10 voice packs | CI does not run the local importer; `internalCandidatePreview` is debug-only |
| Release picker (`release-roster.json`) | 0 | No roster entries | Fails closed until release gates pass |
| Required release target | 33 | 33 stable character/voice pairs | Do not reduce this target when reporting dialogue or asset progress |

The internal importer handles **23 new** pairs; it does not mean the product target is 23. The existing SDK content supplies the other 10 voice packs and 12 character manifests, with two character IDs overlapping the 23-pair expansion. The merged local preview catalog is 33 unique pairs. The CI workflow does not call this importer, and its git-ignored output is not in the internal CI APK; shipping those 23 audition packs requires an approved private-package or artifact path.

## Audio coverage snapshot (2026-11-05)

| Group | Packs | Slots with WAVs available | Slots required | Current use |
| --- | ---: | ---: | ---: | --- |
| Baseline voice packs | 10 | 50 (5 per pack) | 130 | Pack content; greeting plus BRAKE, TURN, MODE, TIRE |
| VoiceDesign expansion | 23 | 253 (11 per pack) | 299 | Local audition output; greeting plus 10 event clips, not a release pack |
| **Total** | **33** | **303** | **429** | **126 event slots still need WAVs** |

**Update 2026-11-14 (voice pack 0.8.0):** three baseline packs now have all 13 slots: `ai.fox-spirit.voicedesign`, `ai.chuunibyo-demon-lord.voicedesign` and `ai.cooldere-vehicle-os.voicedesign`. Each has 33 clips: the existing 13, plus three variants for each of DOOR, WINDOW, AMBIENT_LIGHT, HVAC, WIPER and CHARGE, plus one clip each for WARNING and ENERGY_LOW. They shipped through `voice-pack-sdk` 0.7.0 (fox) and 0.8.0 (the other two), and the app uses 0.8.0. The baseline gap is now 7 packs × 8 slots = 56, so **102 event slots still need WAVs** (56 baseline + 46 expansion safety slots). The process and audition record for the fox pack are in [FOX_SPIRIT_VOICE_SLOT_REQUEST](FOX_SPIRIT_VOICE_SLOT_REQUEST.md); the other two followed the same process. In [DIALOGUE_MATRIX.csv](DIALOGUE_MATRIX.csv), the six new character slots for these three characters are marked `PACK_READY`. The private `voice-pack-sdk` matrix stays canonical.

The 23 local audition pairs have `WARNING` and `ENERGY_LOW` pending. The baseline packs are missing the other eight canonical event clips. Generated audition files are not the same as reviewed, hashed, packaged, and release-approved WAVs.

## Why release currently shows zero

The runtime release gate is intentionally separate from the 33-character target. `release-roster.json` is empty, so a release build exposes no candidate character. Populating it with 33 unqualified entries would not make them launch-ready: the runtime checks exact ID pairing, renderable character assets, commercial-use/consent metadata, all 13 indexed audio slots, and required gesture support for VR entries.

The current character manifests mark 32 of 33 assets `commercialUse: false`. `dev.mori` is marked true, but it has only a preview voice link (`voicePackId` is still null), and its internal audition voice is not a release pack. The new VoiceDesign WAVs are also marked internal audition only. Rights metadata must not be changed to true without evidence that covers the exact source assets and intended distribution.

## Release completion order

1. Keep all 33 ID pairs in `CHARACTER_TARGET_INVENTORY.csv`; add or remove a target only through an explicit product decision.
2. Finish the 126 missing canonical WAV slots, then audition each clip and publish its hash, size, duration, and text in the private voice-pack SDK.
3. Resolve and record commercial-use rights for every character asset and voice pack; do not infer approval from a successful preview.
4. Verify character-to-voice IDs and the intended VR gesture contract for all 33.
5. Add the 33 entries to the runtime release roster only after all gates pass, then validate the internal APK and release build.

The public [DIALOGUE_MATRIX.csv](DIALOGUE_MATRIX.csv) is an older 10-character draft used for app/event-policy discussion. It does not define the 33-character inventory or override the private voice-pack dialogue matrix.
