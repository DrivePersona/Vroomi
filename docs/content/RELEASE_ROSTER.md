---
type: spec
update: rewrite
---

# Voice Companion Release Roster v1

`android/app/src/main/assets/release-roster.json` is the versioned allowlist used by the release picker. Candidate, archived, and development packs stay outside this file and remain hidden from release builds; the separate internal preview path is for team audition only.

## Eligibility checked by the app

Each roster entry must match a loaded character and voice pack by stable ID. The character must be READY, renderable, commercially approved, and declare the same `voicePackId`. The voice pack must be a READY clip pack with approved consent and commercial use, an indexed greeting clip, and at least one indexed variant for each of the 12 canonical vehicle clips. The greeting plus those 12 slots make the v1 13-slot matrix. A VR eligible entry must use the `vr` release tier and list event IDs whose mapped gesture IDs are present in the character manifest's supported gestures.

Entries that fail a runtime check are excluded from the picker and cannot be selected. Malformed or missing roster data fails closed to an empty list.

The app's `:app:testInternalDebugUnitTest` suite exercises missing packs/IDs, incomplete dialogue slots, unsupported VR gesture mappings, and the ID-only character binding. The voice-pack SDK repository tests file hashes, declared sizes, WAV parsing/duration, and clip playback; the character-pack SDK tests validate GLB structure and declared animation contracts. Internal Android CI runs the app suite after protected branch pushes when private packages are enabled. Public pull-request CI cannot download the private SDK AARs, so its web-simulator job does not execute these Kotlin release-gate tests.

## Roster changes

Add an entry only after reviewing the character and voice IDs, licenses, consent, commercial use, 13-slot voice coverage, and intended release tier. Keep `requiredGestureEventIds` explicit for VR entries; voice-only entries use an empty array and set `vrEligible` to false. Include the evidence and review rationale in the pull request. Do not promote candidates based on display-name matches.

The current roster is empty, so the release picker exposes zero candidates. This is not a 13-character limit: the fixed target inventory contains 33 character/voice pairs, with 13 slots required for each. The current blockers are incomplete release audio coverage and unapproved rights/pairing metadata. `dev.mori` has an internal preview voice link, but its release `voicePackId` is still null. See [the 33-character coverage report](RELEASE_VOICE_CHARACTER_PLAN.md). Do not add placeholders to this release allowlist to make its count look like 33; each row must pass the runtime checks.

Internal debug builds may show the loaded candidate GLB packs when the roster is empty. With the 23 local VoiceDesign pairs imported, the 10 baseline pairs plus those 23 candidates form the 33-character internal preview. Those rows are explicitly labelled "내부 미리보기"; they are not release entries and are never enabled in a release build with a populated roster.
