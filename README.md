# DrivePersona — Pleos Voice Companion

DrivePersona is an in-car voice companion for Pleos. It converts vehicle events such as braking, turn signals, drive-mode changes, and tire checks into concise character-led voice feedback.

This repository is the public review surface for the application: vehicle reaction rules, UI/UX, simulator, and integration contracts are visible here. Approved voice and character assets are intentionally isolated in private SDK repositories.

## Repository map

| Repository | Visibility | Purpose |
| --- | --- | --- |
| `DrivePersona/pleos-voice-companion` | Public | This app, web simulator, design documents, and pack integration contracts |
| `DrivePersona/voice-pack-sdk` | Private | Voice-pack runtime AAR and approved audio packs |
| `DrivePersona/character-pack-sdk` | Private | Character-pack runtime AAR and approved character packs |

## Build variants

- `reviewDebug` contains no commercial voice or character assets. It remains the safe presentation build.
- `internalRelease` resolves approved private AARs from GitHub Packages and is for the DrivePersona team only.

The web simulator has no external dependency:

```powershell
node --test
python -m http.server 5183
```

## Pleos Connect end-to-end verification

To verify that a Virtual Console vehicle signal reaches the installed app and starts its character/voice reaction, follow [the Pleos Virtual Console E2E guide](docs/PLEOS_VIRTUAL_CONSOLE_E2E.md). It covers the shared AVD/Console setup, brake and turn-signal test cases, evidence criteria, cleanup, and troubleshooting.

For an internal Android build, place GitHub Packages credentials outside source control in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_CLASSIC_PAT_WITH_READ_PACKAGES
```

The current internal character/voice pairing and 13-slot release contract are documented in [docs/RELEASE_VOICE_CHARACTER_PLAN_V1.md](docs/RELEASE_VOICE_CHARACTER_PLAN_V1.md). The release roster remains an explicit allowlist and stays empty while Meshy character rights or any required audio slot is pending.

Package releases dispatch `voicepack-published` or `characterpack-published` with a version, or `packs-published` with both package versions. The protected `internal-build` workflow resolves private AARs, runs roster/unit validation, assembles the internal release APK, records supplied content metadata in `docs/PACK_RELEASES.md`, and only then opens or refreshes a reviewable PR. Applied versions are a no-op. It never auto-merges. Keep `DRIVEPERSONA_PACKAGES_USER`, `DRIVEPERSONA_PACKAGES_TOKEN`, and the fine-grained `DRIVEPERSONA_SYNC_TOKEN` in the protected environment; the sync token needs `Contents: write` and `Pull requests: write` for this repository. The private SDK publish workflows use the same sync token only to send the dispatch.

The protected `internal-android` CI job runs only after main receives a reviewed change and `INTERNAL_PACKAGES_ENABLED` is true. Public pull-request CI never receives package credentials.

## Asset policy

No Qwen model cache, reference voice, raw audition, unverified reference-derived character, or approved production pack binary is committed to this public repository.
