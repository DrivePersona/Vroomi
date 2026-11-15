# Vroomi — Pleos Voice Companion

## 설치 방법

Vroomi는 차량 신호(브레이크·방향지시등 등)에 반응해 캐릭터 목소리로 알려 주는 Pleos 음성 컴패니언입니다. **release APK를 Pleos 에뮬레이터에 설치해서 확인합니다.** 소스를 빌드할 필요는 없습니다(공개 저장소만으로는 빌드할 수 없습니다. [아래 설명](#building-from-source-team-only)).

**한눈에 보기**

1. [GitHub Releases](https://github.com/DrivePersona/Vroomi/releases/latest)에서 `DrivePersona_Vroomi_release.apk`를 받습니다.
2. Pleos Connect 에뮬레이터를 콜드 부팅하고 APK를 설치합니다.
3. 오버레이 권한을 허용하고 '정지됨' 상태를 푼 뒤, 에뮬레이터를 **한 번 껐다가 다시 콜드 부팅**합니다.
4. 정차 화면(기어 P)에 **여우 무녀**가 나타납니다. Virtual Console에서 브레이크를 밟으면 여우가 말합니다.

**먼저 알아 둘 점**

- 이 앱은 화면(Activity)과 런처 아이콘이 없습니다. 설치해도 바로 열리지 않고, 에뮬레이터가 **부팅될 때** 백그라운드 서비스로 시작되어 정차 중인 화면 위에 캐릭터를 띄웁니다. 그래서 설치한 뒤 한 번 더 부팅해야 합니다.
- 이 APK에는 상업 이용 승인 전인 후보 캐릭터·음성이 들어 있어, 심사·시연 확인 용도로만 제공합니다.
- 확인 수준은 Pleos Connect 에뮬레이터입니다. 실차는 확인하지 않았고, 소리를 사람 귀로 듣고 확인한 것도 아닙니다(재생 시작·완료 이벤트까지 확인). 에뮬레이터에서는 캐릭터가 2D 미리보기로 보입니다.

### 테스트 환경

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | Pleos Connect AVD **2.1.0** (Android 14 / API 34, Automotive). 설치는 [PLEOS_PLATFORM_FINDINGS](docs/platform/PLEOS_PLATFORM_FINDINGS.md) 7절 |
| 화면 규격 | 2560×1440, 가로 |
| Android 사용자 | `10` (`adb shell am get-current-user`로 확인) |
| 앱 | 패키지 `com.example.voicecompanion`, 버전 0.1.0, minSdk 28 / targetSdk 35 |
| APK | `DrivePersona_Vroomi_release.apk` — [GitHub Releases](https://github.com/DrivePersona/Vroomi/releases/latest) (SHA-256은 릴리스 노트에 있습니다) |
| 필요한 도구 | `adb`(Android platform-tools), 웹 브라우저 |

### 설치 순서

**1. APK를 받습니다.** [GitHub Releases](https://github.com/DrivePersona/Vroomi/releases/latest)에서 `DrivePersona_Vroomi_release.apk`를 내려받고, 릴리스 노트의 SHA-256과 같은지 확인합니다.

```bash
shasum -a 256 DrivePersona_Vroomi_release.apk      # Windows: certutil -hashfile DrivePersona_Vroomi_release.apk SHA256
```

**2. 에뮬레이터를 콜드 부팅합니다**(스냅샷을 쓰지 않습니다). AVD 이름은 설치한 이름으로 바꾸세요.

```bash
emulator -avd Pleos_Connect_v2_1_0 -no-snapshot -no-boot-anim
adb shell am get-current-user    # 보통 10
```

**3. APK를 설치하고 오버레이 권한을 허용합니다.**

```bash
adb install -r --user 10 DrivePersona_Vroomi_release.apk
adb shell appops set --user 10 com.example.voicecompanion SYSTEM_ALERT_WINDOW allow
adb shell cmd appops write-settings
```

**4. 설치 직후의 '정지됨(stopped)' 상태를 풉니다.** 정지 상태에서는 앱이 부팅 신호를 받지 못합니다. 아래 명령 뒤 `stopped=false`가 나와야 합니다.

```bash
adb shell am broadcast --user 10 -a androidx.profileinstaller.action.BENCHMARK_OPERATION -n com.example.voicecompanion/androidx.profileinstaller.ProfileInstallReceiver --es EXTRA_BENCHMARK_OPERATION DROP_SHADER_CACHE
adb shell "pm dump com.example.voicecompanion | grep -E 'User 10: ceData' | grep -o -E 'stopped=[a-z]+'"
```

**5. 15초 기다린 뒤** 저장하고 에뮬레이터를 껐다가 다시 콜드 부팅합니다. Android가 정지 해제를 디스크에 늦게 기록하므로, 해제 직후에 끄면 부팅 후 다시 정지 상태로 돌아가 서비스가 시작되지 않습니다. 이 에뮬레이터에서는 `adb reboot`을 쓰지 않습니다([E2E 가이드](docs/guides/PLEOS_VIRTUAL_CONSOLE_E2E.md) 참고).

```bash
adb shell sync
adb emu kill
emulator -avd Pleos_Connect_v2_1_0 -no-snapshot -no-boot-anim
```

**6. 서비스가 시작되고 여우 무녀가 떴는지 확인합니다**(부팅이 끝나고 몇 초 뒤).

```bash
adb logcat -d -s VoiceCompanionService VoiceCompanion
adb shell "dumpsys window | grep -c VoiceCompanionCharacterOverlay"
```

로그에 아래 두 줄이 있고, 두 번째 명령이 0보다 큰 수를 출력하면 정상입니다. Pleos 화면의 내비 지도 위에 여우 무녀가 서 있습니다("길게 누르면 메뉴가 열려요" 말풍선이 함께 보일 수 있습니다).

```text
VoiceCompanionService: runtime started; action=com.example.voicecompanion.action.START_BACKGROUND
VoiceCompanion: overlay sync: VrOverlayInput(enabled=true, stationary=true, settingsVisible=false, readyCharacter=true, overlayPermissionGranted=true) character=ai.meshy.fox
```

**7. Virtual Console을 열고 브레이크 신호를 보냅니다.** 콘솔은 브라우저가 열려 있어야 조작이 반영됩니다.

```bash
adb forward tcp:8088 tcp:8088
```

브라우저에서 `http://127.0.0.1:8088/`을 열고 Brake Pedal을 0%에서 **70% 안팎**으로 올립니다([E2E 가이드](docs/guides/PLEOS_VIRTUAL_CONSOLE_E2E.md) 3절). 짧게 눌러 값이 몇 %에 그치면 앱은 입력만 받고 말하지 않습니다. 같은 신호를 명령으로 보낼 수도 있습니다(콘솔을 브라우저로 열어 둔 상태에서).

```bash
curl -X PUT "http://127.0.0.1:8088/vehicle/controls?brake=70"
```

**8. 앱의 반응을 확인합니다.**

```bash
adb logcat -d -s VoiceCompanion
```

```text
차량 입력: BRAKE (주행=false, 주행 중 허용=true)
반응 BRAKE: 말하기 요청 "오냐, 천천히 가거라."
반응 음성 BRAKE: 재생됨
몸짓 없음: ai.meshy.fox BRAKE
```

여우의 브레이크 몸짓은 아직 없어서 마지막 줄(`몸짓 없음`)이 남고, 캐릭터는 가만히 서 있습니다. 브레이크 반응에는 10초 쿨다운이 있으니, 다시 확인하려면 0%로 내리고 10초 뒤에 다시 70%로 올리세요.

**9. 정리합니다.**

```bash
curl -X PUT "http://127.0.0.1:8088/vehicle/controls?brake=0"
adb forward --remove tcp:8088
adb uninstall --user 10 com.example.voicecompanion
```

| 증상 | 점검 |
| --- | --- |
| 3단계 설치가 서명 충돌로 실패한다 | 서명이 다른 같은 패키지가 이미 있으면 설치되지 않습니다. `adb uninstall --user 10 com.example.voicecompanion` 뒤 다시 설치합니다. |
| 6단계에서 로그가 없다 | 4단계의 `stopped=false` 확인 → 15초 대기 → 5단계의 콜드 부팅을 다시 합니다. |
| 서비스는 시작됐는데 여우가 안 보인다 | 기어가 P(정차)인지, 3단계의 오버레이 권한을 허용했는지 확인합니다. 로그의 `overlay sync` 줄에서 `stationary`·`readyCharacter`·`overlayPermissionGranted`가 모두 `true`여야 합니다. |
| 입력은 있는데 말하지 않는다 | 브레이크가 0% → 70% 안팎으로 올랐는지, 10초 쿨다운이 지났는지 확인합니다. |
| 그 밖의 증상 | [E2E 가이드](docs/guides/PLEOS_VIRTUAL_CONSOLE_E2E.md)의 실패 시 점검 순서를 봅니다. |

### 웹 시뮬레이터 (선택: Pleos 없이 반응 규칙만)

실제 앱이 아니라 반응 규칙(우선순위·쿨다운·정차 게이트)을 확인하는 도구입니다. 클론만으로 실행되며 Node.js 20 이상과 Python 3이 필요합니다.

```bash
git clone https://github.com/DrivePersona/Vroomi.git
cd Vroomi
node --test                      # 모두 통과해야 합니다
python3 -m http.server 5183      # Windows는 python. 브라우저에서 http://localhost:5183
```

화면에서 **브레이크 밟기**를 누르면 반응 기록에 `브레이크 밟기 → 말하기 요청 "으앗!" · 재생됨`이 남습니다.

## About

Vroomi (by DrivePersona) is an in-car voice companion for Pleos. It converts vehicle events such as braking, turn signals, drive-mode changes, and tire checks into concise character-led voice feedback.

This repository is the public review surface for the application: vehicle reaction rules, UI/UX, simulator, and integration contracts are visible here. Approved voice and character assets are intentionally isolated in private SDK repositories.

## Repository map

| Repository | Visibility | Purpose |
| --- | --- | --- |
| `DrivePersona/Vroomi` | Public | This app, web simulator, design documents, and pack integration contracts |
| `DrivePersona/voice-pack-sdk` | Private | Voice-pack runtime AAR and approved audio packs |
| `DrivePersona/character-pack-sdk` | Private | Character-pack runtime AAR and approved character packs |

## Building from source (team only)

You do not need this to try the app: use the APK as described in [설치 방법](#설치-방법). A public clone **cannot build the Android app**. Both variants depend on the private `voicepack-runtime` and `characterpack-runtime` packages on GitHub Packages, which answer `401 Unauthorized` without team credentials. The build variants below matter only to the team:

- `reviewDebug` contains no commercial voice or character assets. It remains the safe presentation build.
- `internalRelease` resolves the private AARs from GitHub Packages. The APK on the Releases page is this variant. It includes candidate characters and voices that are not yet approved for commercial use and is provided for review and demonstration only. Only internal builds show candidate characters (the fox shrine maiden, `ai.meshy.fox`, is the first-run character); the `review` build follows the release roster.

## Documentation

Start at the [documentation map](docs/README.md) (AI agents: [AGENTS.md](AGENTS.md)). It is organized top-down by area: overview, product, architecture, platform, content, guides, evidence, decisions, and log.

## Product and user-flow design

- [Product requirements (PRD v2)](docs/product/PRODUCT_REQUIREMENTS.md) — product scope, vehicle input contract, safety policy, release criteria, and current constraints.
- [User flows v2](docs/product/USER_FLOWS.md) — WindowManager overlay-first startup, avatar gestures, 5+11 acquisition, dialogue, and driving lock.
- [Wireframes v2](docs/product/WIREFRAMES.md) — Pleos HMI stays visible while avatar-anchored floating controls and compact overlay sheets handle companion actions.
- [Interactive overlay-first mockup v2](docs/product/mockups/voice-companion-flow.html) and [PNG preview](docs/product/mockups/voice-companion-flow.png) — real Pleos HMI frame/vehicle area with a schematic map underlay replacing the old debug Activity pane; approved roster assets remain unassigned.
- [Project and PR history](docs/log/PROJECT_AND_PR_HISTORY.md) — repository roles, merged PR inventory, open work, and evidence gaps across all three repositories.

Package releases dispatch `voicepack-published` or `characterpack-published` with a version, or `packs-published` with both package versions. The protected `internal-build` workflow resolves private AARs, runs roster/unit validation, assembles the internal release APK, records supplied content metadata in `docs/content/PACK_RELEASES.md`, and only then opens or refreshes a reviewable PR. Applied versions are a no-op. It never auto-merges. Keep `DRIVEPERSONA_PACKAGES_USER`, `DRIVEPERSONA_PACKAGES_TOKEN`, and the fine-grained `DRIVEPERSONA_SYNC_TOKEN` in the protected environment; the sync token needs `Contents: write` and `Pull requests: write` for this repository. The private SDK publish workflows use the same sync token only to send the dispatch.

The protected `internal-android` CI job runs only after main receives a reviewed change and `INTERNAL_PACKAGES_ENABLED` is true. Public pull-request CI never receives package credentials.

## Asset policy

No Qwen model cache, reference voice, raw audition, unverified reference-derived character, or approved production pack binary is committed to this public repository.
