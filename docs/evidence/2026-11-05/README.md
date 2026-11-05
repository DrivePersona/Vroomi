# Pleos AVD smoke evidence — 2026-11-05

- Device: Pleos_Connect_API_34_v2_1 (Android 14, emulator-5554)
- App: com.example.voicecompanion.review / reviewDebug APK
- Test: Set Virtual Console brake pedal to 70%; Pleos vehicle service published `BRAKE_PEDAL_STATUS=true` and broadcast the property callback to `com.example.voicecompanion.review`; app Vehicle Data Debug displayed the received brake event.
- Screenshot: `pleos-vhal-brake-test.png` (captured while the event was visible in the app)
- Cleanup: Brake pedal reset to 0% after capture.
- Scope: Runtime integration smoke only. Does not verify approved voice content, audio/character assets, all issue/PR acceptance criteria, or release builds.
- Build limitation: A fresh `assembleReviewDebug` could not resolve private `voicepack-runtime:0.4.0` and `characterpack-runtime:0.3.0` packages (GitHub Packages HTTP 401). Existing reviewDebug APK ran successfully.
