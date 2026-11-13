---
type: spec
update: rewrite
---

# Character and voice binding

Selecting a release-eligible character resolves `CharacterPackInfo.voicePackId` against the loaded READY voice packs. The match is by ID only. The character selector and `PackSelector` are updated in one main-thread operation; the previous voice selection is restored if character selection fails. The app persists only `characterpack.selected`; the legacy `voicepack.selected` preference is removed at startup.

Startup waits until both SDK repositories have loaded before restoring the character preference and deriving its voice pack. This handles either repository finishing first. If the character is absent, its manifest has no `voicePackId`, or its linked voice pack is not READY, the app uses built-in speech and does not show a character or silently attach another pack.

For an eligible character, the companion screen renders the greeting and 12 canonical vehicle dialogue slots from the selected voice pack. Each recorded variant has a `듣기` action. It sends the stable `eventId` and zero-based `variantIndex` to the voice SDK and is disabled while driving. Runtime event playback continues through the same selected pack and its normal per-event variant rotation. Audio focus loss stops playback; a character switch stops active pack audio before changing the pair.

This binding uses `CharacterPackInfo.voicePackId` from character-pack-sdk 0.3.0 and exact preview uses the additive `SpeakRequest.variantIndex`, `VoicePack.variantCount`, and indexed caption API from voice-pack-sdk 0.3.0. Both dependencies must be published at 0.3.0 before this app change can be built from a clean checkout.

Local verification used Maven Local builds of both SDK checkouts because GitHub Packages credentials are not available to the app's Android CI job. No Pleos Virtual Console or connected Android/Pleos emulator was available for runtime interaction testing.
