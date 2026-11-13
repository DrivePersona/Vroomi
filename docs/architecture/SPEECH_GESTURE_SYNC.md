---
type: spec
update: rewrite
---

# Speech and gesture synchronization

For a selected READY character/voice pair, clip playback's `재생됨` callback starts the one-shot gesture mapped from the same canonical `eventId`. The resolver accepts only a character manifest mapping whose gesture exists and is declared `oneShot`. Greetings, neutral safety clips, unmapped events, and playback fallbacks do not start a gesture.

The same callback path is used for dialogue previews and vehicle-event speech. When audio finishes, is replaced, loses focus, or the vehicle enters Drive, the runtime returns the character to its idle animation. The character SDK uses the declared gesture duration to stop the GLB one-shot and hold its last pose until the runtime changes it to idle.

The current voice-pack schema has no per-clip `gestureId` or `startOffsetMs`, and automatic round-robin playback does not expose its chosen variant index to the app. Therefore this implementation starts the character's event-mapped gesture at the audio-start callback (offset zero). Per-clip offsets and variant-specific gesture IDs need a voice-pack schema/API change before they can be enforced. A Pleos AVD is also required to record actual audio/Filament timing evidence.
