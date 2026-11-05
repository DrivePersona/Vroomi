# Pleos AVD and release SDK verification evidence

## Real Pleos runtime smoke test

- Device: Pleos Connect API 34 AVD `Pleos_Connect_API_34_v2_1`, Android user 10.
- App: existing `com.example.voicecompanion.review` reviewDebug APK; MainActivity was active during the vehicle callback checks.
- Brake: Virtual Console 0% → 70%; Pleos broadcasts `BRAKE_PEDAL_STATUS=true` to the app. `pleos-vhal-brake-test.png` shows the real app's Vehicle Data Debug state. Reset to 0%.
- Turn signal: Virtual Console `Off` → `BlinkLeft`; Pleos broadcasts `TURN_SIGNAL_STATE=2` to the app. The Pleos TTS service logs the app speech text `깜빡이 켰어~`. `pleos-turn-signal-active.png` captures the active signal and Pleos camera preview; `pleos-turn-signal-test.png` captures the app after signal was reset to `Off`.
- Both controls were returned to their idle state after capture. These checks confirm vehicle callbacks and the default TTS path only; they do not validate character-pack clip selection, approved content, or gesture playback.

## Automated test results

| Project | Command | Result |
|---|---|---|
| pleos-voice-companion | `npm test` | 26 passed, 0 failed |
| voice-pack-sdk | `gradlew test` | 59 Debug + 59 Release passed, 0 failed |
| character-pack-sdk | `gradlew test` | 14 Debug + 14 Release passed, 0 failed |

## Remaining limits

- A fresh `:app:testReviewDebugUnitTest` could not resolve private `voicepack-runtime:0.4.0` and `characterpack-runtime:0.3.0` from GitHub Packages (HTTP 401). The run used the already available reviewDebug APK.
- Release roster, approved voice text/audio, the full 13-state playback, character-to-voice binding, and animated GLB gestures were not proven by this runtime smoke test. Keep the related issues open until the relevant approved assets and runtime checks are available.
