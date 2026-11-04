# Stability and lifecycle regression coverage

## Automated checks

- `ReactionEngineTest`: queued event cancellation, burst priority, cooldown, drive-state changes, focus loss, stop, and late event handling.
- `VehicleStateRepositoryTest` and `VehicleAreaSubscriptionRegistryTest`: rapid value changes, per-area cooldown/debounce, capability failures, and subscription cleanup.
- `VrOverlayPolicyTest` and `OverlayDragTest`: show/hide decisions during settings visibility, driving, permission changes, and drag bounds.
- Voice SDK `ClipVoiceTest`: repeated play, replacement, stop, late completion callbacks, and playback start failures.
- App `ReleaseRosterTest`: invalid/missing linked pack recovery and release eligibility.

Debug variants enable Android StrictMode logging for detected main-thread/VM violations, leaked closable objects, and leaked Activities. It logs findings without crashing the app so a Pleos session can collect them alongside logcat.

## Runtime soak still required

This workspace has no connected Android device and only a generic phone AVD; it does not contain a Pleos Connect AVD. The StrictMode policy and JVM tests were built locally, but process recreation, Pleos permission/capability failure, WindowManager/Filament teardown, AudioTrack resource cleanup, and repeated foreground/background soak were not exercised here. Keep issue #8 open until those runtime checks have evidence from the supported AVD or vehicle image.
