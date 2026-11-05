# Internal VoiceDesign candidate preview

The internal debug flavor can audition the new VoiceDesign character/voice pairs
from local candidate assets. Candidate audio is not commercial release content.
The public `review` flavor does not package these assets.

Generate reference greetings and event clips in the private `voice-pack-sdk`
worktree, then assemble them with the matching GLB candidates from
`character-pack-sdk`:

```powershell
node tools/prepare-internal-voice-preview.cjs `
  --voice-repo C:\Users\LENOVO\Documents\DrivePersonaRepos\voice-main-verify `
  --character-repo C:\Users\LENOVO\Documents\DrivePersonaRepos\character-main-verify
```

The importer requires the **23 new** VoiceDesign greetings and 10
character-voiced event clips per pack. These are incremental to the 10 baseline
voice packs in the SDK; the full internal target remains 33 unique character
pairs, not 23. `WARNING` and `ENERGY_LOW` remain neutral built-in speech under
the vehicle safety policy. The importer writes only to
`android/app/src/internal/assets/voicepacks` and `characterpacks`; these local
audition assets are git-ignored. Each copied character manifest receives a
`previewVoicePackId` link, so selecting a candidate character selects its paired
VoiceDesign WAV pack in the internal flavor.

Before copying, the importer checks that the source matrix still has 33 unique
character/voice pairs, that all 10 baseline voice packs plus 23 new VoiceDesign
packs cover that set, and that every target character asset is present. A missing
pair fails the import instead of silently shrinking the preview catalog.

Build with the local Maven SDK packages if GitHub Packages credentials are not
available:

```powershell
cd android
./gradlew.bat :app:assembleInternalDebug -Pdrivepersona.useMavenLocal=true
```

The candidate voice manifests declare internal audition only and
`commercialUse: false`. The release roster remains the only source for release
eligibility.
