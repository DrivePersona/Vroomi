# Pleos AVD and release SDK verification evidence

## Fresh app build and tests

- Built runtime AARs from the checked-out voice-pack and character-pack SDK sources and published them to local Maven only. The app was then tested and built against those exact local artifacts; no package was published to GitHub Packages.
- App: `:app:testReviewDebugUnitTest` passed, 58 tests; `:app:assembleReviewDebug` passed.
- App test APK: `com.example.voicecompanion.review`, version name `0.1.0-review`, installed on Android user 10.
- `pleos-voice-companion` scripts: `npm test`, 26 passed.
- `voice-pack-sdk`: `gradlew test`, 59 Debug + 59 Release passed.
- `character-pack-sdk`: `gradlew test`, 14 Debug + 14 Release passed.

## Real Pleos vehicle-event smoke test

- Device: Pleos Connect API 34 AVD `Pleos_Connect_API_34_v2_1`.
- Brake: Virtual Console 0% → 70%; Pleos broadcasts `BRAKE_PEDAL_STATUS=true` to the freshly rebuilt app and its TTS service logs `으앗!`. `pleos-vhal-brake-test.png` shows the app's Vehicle Data Debug state.
- Turn signal: Virtual Console Off → BlinkLeft; Pleos broadcasts `TURN_SIGNAL_STATE=2` to the app and its TTS service logs `깜빡이 켰어~`. `pleos-turn-signal-active.png` shows the active indicator and camera preview; the camera preview is a Pleos platform response to the turn signal.
- `pleos-review-debug-final.png` shows the app after reset. Gear P, brake 0%, and turn signal Off were verified after the test.
- Raw callback/TTS excerpts are in `brake-log.txt` and `turn-signal-log.txt`.

## Scope and limits

These AVD checks verify real VHAL callback delivery and the default Pleos TTS path. The public review APK has no approved character or voice content AAR, so this does not validate character-specific WAV playback, a populated release roster, all 13 approved clips, or GLB gesture rendering. Keep those acceptance criteria open until the release assets and rights approvals exist.

GitHub Packages direct access from this workstation returns HTTP 401. The local Maven builds let the app tests and APK build run without changing repository dependencies or publishing artifacts.
