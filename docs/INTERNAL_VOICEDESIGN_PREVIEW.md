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

The importer requires 23 VoiceDesign greetings and the 10 character-voiced
event clips per pack. `WARNING` and `ENERGY_LOW` remain neutral built-in speech
under the vehicle safety policy. The importer writes only to
`android/app/src/internal/assets/voicepacks` and `characterpacks`; these local
audition assets are git-ignored. Each copied character manifest receives a
`previewVoicePackId` link, so selecting a candidate character selects its paired
VoiceDesign WAV pack in the internal flavor.

Build with the local Maven SDK packages if GitHub Packages credentials are not
available:

```powershell
cd android
./gradlew.bat :app:assembleInternalDebug -Pdrivepersona.useMavenLocal=true
```

The candidate voice manifests declare internal audition only and
`commercialUse: false`. The release roster remains the only source for release
eligibility.
