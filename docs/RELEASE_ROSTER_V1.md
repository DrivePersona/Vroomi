# Voice Companion Release Roster v1

`android/app/src/main/assets/release-roster.json` is the versioned allowlist used by the app. Candidate, archived, and development packs stay outside this file and are not shown by either the character or voice picker.

## Eligibility checked by the app

Each roster entry must match a loaded character and voice pack by stable ID. The character must be READY, renderable, commercially approved, and declare the same `voicePackId`. The voice pack must be a READY clip pack with approved consent and commercial use, an indexed greeting clip, and at least one indexed variant for each of the 12 canonical vehicle clips. The greeting plus those 12 slots make the v1 13-slot matrix. A VR eligible entry must use the `vr` release tier and list event IDs whose mapped gesture IDs are present in the character manifest's supported gestures.

Entries that fail a runtime check are excluded from the picker and cannot be selected. Malformed or missing roster data fails closed to an empty list.

The app's `:app:testInternalDebugUnitTest` suite exercises missing packs/IDs, incomplete dialogue slots, unsupported VR gesture mappings, and the ID-only character binding. The voice-pack SDK repository tests file hashes, declared sizes, WAV parsing/duration, and clip playback; the character-pack SDK tests validate GLB structure and declared animation contracts. Internal Android CI runs the app suite after protected branch pushes when private packages are enabled. Public pull-request CI cannot download the private SDK AARs, so its web-simulator job does not execute these Kotlin release-gate tests.

## Roster changes

Add an entry only after reviewing the character and voice IDs, licenses, consent, commercial use, 13-slot voice coverage, and intended release tier. Keep `requiredGestureEventIds` explicit for VR entries; voice-only entries use an empty array and set `vrEligible` to false. Include the evidence and review rationale in the pull request. Do not promote candidates based on display-name matches.

The current roster is intentionally empty. The local candidate characters have no approved voice pairing and/or commercial-use approval, and the candidate voice packs do not have the full v1 dialogue matrix. `dev.mori` also has no voice pairing. No local candidate is release-eligible today.

Internal debug builds may show the loaded candidate GLB packs when the roster is empty. Those rows are explicitly labelled "내부 미리보기" and use the platform voice only; they are not release entries and are never enabled in a release build with a populated roster.
