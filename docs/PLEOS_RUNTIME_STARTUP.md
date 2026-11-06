# Pleos runtime startup

## Implemented entry points

- **Launcher Activity:** starts `CompanionRuntimeService` with `ACTION_SETTINGS`, then shows the existing explicit setup/recovery screen.
- **VR start:** `CompanionRuntime.beginVr()` hides setup state and shows the character when policy permits; the Activity finishes. The started service keeps the runtime alive independently of that Activity.
- **Android system boot:** `CompanionBootReceiver` handles `BOOT_COMPLETED` and starts the service with `ACTION_BACKGROUND`. It never launches an Activity. A sticky service restart with a null intent also restores the background session.

`startBackgroundSession()` marks settings as hidden before starting the runtime. On Automotive, the runtime defaults to driving until `CarUxRestrictions` supplies its current value. The overlay remains hidden while state is unknown or driving, and can appear only after parked is confirmed, the selected character pack is ready, VR is enabled, and overlay access is available. The selected character is restored from app preferences; its paired voice pack is derived from the character manifest.

## Platform boundary

The public [Pleos app structure and screen model](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model) describes foreground/background screen transitions. The public [Pleos app launch/debug guide](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide) describes launching an app through the app launcher or an explicit Activity. The reviewed public Pleos documentation does not define an ignition callback or guarantee that third-party apps receive `BOOT_COMPLETED` on every vehicle start.

Therefore, `BOOT_COMPLETED` is implemented as the standard Android cold-boot entry point and must be verified on the target image. Android lists this action as an implicit-broadcast exception ([broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)); that does not make it a Pleos ignition signal. If the vehicle suspends and resumes Android without rebooting, this receiver will not run again. Automatic startup on every ignition still needs a Pleos product-level app launch contract or an explicit approved lifecycle hook.

## Background-service behavior and limits

The service is a regular started service with `START_STICKY`; no foreground-service notification or guessed service type was added. Android's [service guidance](https://developer.android.com/develop/background-work/services) documents background-service limits, so this implementation is sufficient only if target-image testing confirms the runtime remains active. Android may reclaim a non-foreground service under resource pressure, and sticky restart is best-effort. If the Pleos image stops the service while the device remains booted, the target platform's background-execution policy must be resolved before calling this production-ready.

The receiver runs only after Android boot and user unlock. Android also suppresses boot receivers for a force-stopped package until the user launches the app again. The launcher Activity remains available for explicit recovery and status checks. Missing `SYSTEM_ALERT_WINDOW` access is handled by the existing runtime gate; it does not open settings automatically or launch the Activity.

## Verification attempt — 2026-11-06

- `:app:testReviewDebugUnitTest`: **58 passed, 0 failed**.
- `:app:assembleReviewDebug`: **passed**.
- `:app:assembleInternalDebug`: **passed** using the already-published local Maven artifacts for the private pack SDKs. The isolated worktree had no GitHub Packages credentials, so a remote dependency-resolution attempt returned HTTP 401; no credentials were added to the repository.
- Pleos Connect v2.1.0 AVD: two cold-boot attempts were made. The first eventually reached `sys.boot_completed=1`, but the internal app cold start then hit an ActivityManager startup ANR; the captured screen was black and is not valid UI evidence. The second boot did not reach `sys.boot_completed` within 180 seconds. Logs showed `network eth0 is not ready yet`, Pleos vehicle service panics, `system_server`/ActivityManager unavailable, and both `eth0` and `wlan0` down. Enabling Wi-Fi was not possible because the Wi-Fi system service was unavailable. The temporary AVD CPU-core setting was restored to 6; app data was not wiped.
- Because the AVD did not produce a stable user session, the boot receiver's `BOOT_COMPLETED` delivery, overlay-only rendering, parked/driving transitions, duplicate-window behavior, and the required HMI-plus-character screenshot remain **unverified**. Keep issue #36 open until these target-image checks pass. The failed black-screen capture must not be presented as a successful UI screenshot.

## Pleos AVD verification

Use the Pleos Connect AVD only (not a phone AVD). Record the image version and capture a screenshot with the HMI visible, the selected character overlay present, and no companion Activity.

1. Install and launch the internal/debug build once. Select a character, enable VR, and verify that the selected character and overlay preference persist.
2. Grant overlay access in the emulator setup used for this test. The Pleos image used in earlier checks had no usable permission settings screen, so prior tests used `adb appops`; this is AVD configuration, not a product UI flow.
3. Cold-boot the AVD. Capture the boot/service log and confirm the receiver starts one runtime service without starting `MainActivity`.
4. Confirm the character stays hidden until `CarUxRestrictions` reports parked, appears once while parked, and disappears when drive restrictions turn on. Confirm the HMI remains visible.
5. Start the app explicitly and confirm the recovery screen hides the overlay; tap the overlay and confirm the same screen can be opened again.

Useful diagnostics:

```powershell
adb logcat -s VoiceCompanionBoot:I VoiceCompanionService:I VoiceCompanion:I
adb shell dumpsys activity services com.example.voicecompanion
adb shell dumpsys window windows
```

Record the actual AVD evidence below before closing the related implementation issue:

- Pleos image / AVD:
- Boot receiver and service log:
- HMI plus overlay screenshot:
- Unknown/driving → parked behavior:
- Duplicate service/window check:
- Explicit Activity recovery check:
- Remaining limitation:
