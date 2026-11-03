# Voice Companion Release Roster v1

`android/app/src/main/assets/release-roster.json` is the versioned allowlist used by the app. Candidate, archived, and development packs stay outside this file and are not shown by the character picker.

## Eligibility checked by the app

Each roster entry must match a loaded character and voice pack by stable ID. The character must be READY, renderable, commercially approved, and declare the same `voicePackId`. The voice pack must be a READY clip pack with approved consent and commercial use, a greeting, and all 12 canonical vehicle clips. The greeting plus those 12 slots make the v1 13-clip matrix. A VR eligible entry must use the `vr` release tier and list the gestures that the character manifest must provide.

Entries that fail a runtime check are excluded from the picker and cannot be selected. Malformed or missing roster data fails closed to an empty list.

## Roster changes

Add an entry only after reviewing the character and voice IDs, licenses, consent, commercial use, 13-slot voice coverage, and intended release tier. Keep `requiredGestureEventIds` explicit for VR entries; voice-only entries use an empty array and set `vrEligible` to false. Include the evidence and review rationale in the pull request. Do not promote candidates based on display-name matches.

The current roster is intentionally empty. The local candidate characters have no approved voice pairing and/or commercial-use approval, and the candidate voice packs do not have the full v1 dialogue matrix. `dev.mori` also has no voice pairing. No local candidate is release-eligible today.
