# Pleos 플랫폼 실측과 함정

**기준:** Pleos Vehicle SDK 2.3.2, TextToSpeech SDK 3.3.1, Pleos Connect v2.1.0 에뮬레이터(2026-10-26~11-12). 모두 에뮬레이터·SDK 바이너리(`javap`)·공식 문서에서 확인한 것이고 **실차는 확인하지 않았다.** 신호별 표는 [VEHICLE_SIGNALS](VEHICLE_SIGNALS.md), 신호를 보내 보는 절차는 [PLEOS_VIRTUAL_CONSOLE_E2E](PLEOS_VIRTUAL_CONSOLE_E2E.md).

## 1. SDK와 저장소

- Maven 저장소 `https://nexus-playground.pleos.ai/repository/maven-releases/` (인증 없음). 이 그룹(`ai.pleos.playground`)만 받도록 저장소 필터를 둔다.
- 공개된 Vehicle SDK 버전은 2.0.3과 2.3.2뿐이다(문서의 2.3.1은 없다). minSdk 28.
- 문서 사이트는 JS 기반이라 일반 HTTP 요청으로는 빈 페이지가 온다. 브라우저로 열어야 읽힌다. API Reference 본문은 로그인이 필요한 것으로 보인다.

## 2. 차량 신호 구독

| 신호 | 결과 (에뮬레이터) |
| --- | --- |
| 브레이크 페달, 방향지시등 | 서비스가 등록을 받고 구독 직후 첫 값을 보낸 뒤 콘솔 조작이 이벤트로 온다 |
| 드라이브 모드 | 서비스 로그는 `result = false`인데 앱에는 실패 콜백이 안 온다 (**조용한 거절**). 콘솔에도 카드가 없어 이 이미지가 지원하지 않는 것으로 본다(추정) |
| 안전벨트 경고 | `onFailed`: `property ID is not in carPropertyConfig list`. 이 VHAL에 속성이 없다 |
| 주행 규제(CarUxRestrictions) | 기어 P·속도 0은 정차, **기어 D만으로 주행**으로 바뀐다 |
| 외부등·조향·주행거리·크루즈·기어 | 값이 오고 원복도 따라온다. 앞/뒤 안개등은 속성 없음 |

함정:

- 상태줄의 "구독 요청"은 요청했다는 뜻일 뿐 서비스가 받았다는 증거가 아니다.
- 구독 직후 첫 값은 기준값으로만 쓴다(변화가 아니다).
- 리스너 콜백의 스레드는 문서에 없어, 받는 쪽이 메인으로 넘겨야 한다. 콜백이 잦은 신호는 모아서(예: 250ms) 화면에 넘긴다.
- 구독이 많아지면 등록 IPC가 첫 프레임을 막는다. 차량 입력 시작은 백그라운드 스레드로 옮겼다(PR #29).
- 콘솔 조향각 +45는 VHAL에서 −45가 된다(부호 반대). 콘솔 실내등을 켜면 독서등 4좌석도 같이 켜진다. 콘솔은 주행거리 감소를 막는다(HTTP 400) — 에뮬레이터 콜드 부팅으로만 0으로 돌아간다.
- "구독 가능"은 API가 있고 권한이 맞다는 조건일 뿐 값이 온다는 뜻이 아니다.

공개 SDK에 없는 것: 차량 속도, 가속도, 시동, 급제동 판별(브레이크 반응은 "페달을 밟음" 기준). 서드파티 불가(`PRIVATE_CAR_SAFETY`): 차량 경고등(telltale), 타이어 압력 구독. 타이어는 `getLowTirePressure`만 공개인데 값의 뜻을 문서에서 못 찾아 자동 반응에 쓰지 않는다. 기획서 초안의 "telltale 매핑"은 이 때문에 안전벨트 경고로 대체됐다.

## 3. 권한

현재 선언: `CAR_DRIVE`, `CAR_LIGHTS`, `CAR_SAFETY`, `pleos.car.permission.TTS_SERVICE`, 일반 권한 `SYSTEM_ALERT_WINDOW`. Pleos TextToSpeech SDK가 `INTERNET`도 병합해 온다.

- 미승인 권한은 심사 탈락 사유다. **실제 쓰는 API의 권한만 선언한다.**
- 새 권한이 필요한 신호(창문 `CAR_WINDOWS`, 도어 `CAR_DOORS`, 공조·미러·열선 `CAR_CLIMATE`, 좌석 `CAR_SEATS`, EV·연료 `CAR_ENERGY`)는 승인 전에 반응으로 연결하지 않는다.
- 이름이 SDK 어노테이션과 어긋나는 선언(`READ_WINDSHIELD_WIPERS` 등)은 Pleos 확인이 필요하다.
- `SYSTEM_ALERT_WINDOW`와 포그라운드 서비스 권한도 승인 범위 협의 대상이다. 에뮬레이터의 오버레이 검증은 개발용 `adb appops`로 허용한 상태에서 한 것이다.

## 4. 음성 (TTS)

- **Pleos 이미지에서는 안드로이드 표준 `TextToSpeech`를 못 쓴다.** 이미지의 TTS 엔진 서비스(`…caas/.service.speech.SpeechService`)는 인텐트 필터에 `DEFAULT` 카테고리가 없고 `tts_default_synth`가 비어 있어, 기본 엔진으로도 패키지 직접 지정으로도 초기화가 실패한다(AOSP 엔진 탐색 규칙 때문으로 추정).
- 대신 **Pleos TextToSpeech SDK**를 `ON_DEVICE` 모드로 쓴다(네트워크·앱 등록 불필요). `HYBRID`(기본)와 `SERVER`는 서버 앱 등록이 필요하다.
- 순서: `TextToSpeech(ctx, ON_DEVICE)` → `initialize` → `setAudioAttributes`·`setLocale(ko-KR)`·`addEventListener` → `speak` → `stop`. 지원 로케일 ko-KR·en-US·fr-CA·es-US/MX. 소리는 차량의 Speech Service가 낸다(기본 화자 `seoyoon`).
- **피치·속도·음량 API가 없다.** 목소리 프리셋과 음량 상한은 Pleos 기본 음성에 적용되지 않는다. `setSpeakerType(String)`은 있지만 값 목록을 찾지 못했다.
- 이벤트(`onStart`·`onDone`·`onStop`·`onError`)에는 요청 번호가 없다. **새 `speak`는 말하던 소리를 끊으면서 `onStop`을 주지 않는다** → 앱이 이전 요청에 "끊김"을 직접 붙여야 한다. `stop()`을 직접 부르면 `onStop`이 온다.
- 매니페스트에 `<queries>`로 TTS 서비스 액션을 선언해야 한다(안드로이드 11+).
- 비용: 딸려 오는 네이티브 라이브러리 때문에 APK가 약 19MB 늘었다(26.7MB → 45.6MB, 당시 측정).
- 녹음 클립은 앱이 `AudioTrack`으로 직접 재생한다. Pleos 에뮬레이터에서 TTS와 같은 `USAGE_MEDIA`·`CONTENT_TYPE_SPEECH`로 포커스 허가와 재생이 됐다(`dumpsys audio`로 확인). 클립 앞에는 150ms 묵음을 재생 시점에 붙인다(Pleos 선행 묵음 가이드).
- **오디오 포커스·존:** 서드파티가 쓸 오디오 컨텍스트가 문서에 없어 `USAGE_MEDIA`로 가정했다. 실차의 존·정책은 미확인이다.

## 5. 주행 규제와 화면

- 앱이 `distractionOptimized`를 선언하지 않으면 주행 중 Pleos가 앱 화면을 "주행 중에는 사용할 수 없습니다." 차단 화면으로 덮는다. 앱(엔진·차량 연결)은 뒤에서 계속 돌고 그 상태의 이벤트도 처리된다 → 기획(주행 중 아바타 숨김, 음성만)과 맞는다.
- 방향지시등을 켜면 Pleos가 측면 카메라 화면(EVS, `ai.umos.car.evs`)을 띄워 앱을 가린다. Off로 돌리면 사라진다. 오버레이도 가려진다.

## 6. 오버레이와 프로세스 수명

- 일반 배경 위에 떠 있는 `WindowManager` 오버레이는 에뮬레이터에서 동작한다. 정차 복귀(기어 P) 0.5초 안에 돌아오고, 기어 D에서 0.4초 안에 사라진다.
- 이 이미지에는 오버레이 권한 설정 화면이 없다. 허용은 `adb appops set --user 10 <패키지> SYSTEM_ALERT_WINDOW allow`뿐이다(끝나면 `appops reset`, 바꾼 뒤 앱 재시작).
- 액티비티를 `finish()`로 닫으면 기어 P에서 셸이 프로세스를 죽인다(`am_kill … remove task`). 화면 없이 주행하면 프로세스가 얼기도 했다(`am_freeze`). **포그라운드 서비스로 프로세스를 붙잡는 방식**으로 완화했다(프로토타입 계보에서 확인 — [ROADMAP](ROADMAP.md) 5절).
- 투명 합성은 Filament를 리플렉션으로 건드린다. 버전(1.75.1)을 고정한 의존이므로 자체 렌더러로 바꿀지는 열린 결정이다.

## 7. 에뮬레이터 환경

- 이미지: `system-images;android-34;pleos_car_emulator;x86_64` ("Pleos Connect v2.1.0 APIs", 약 4.09GB). 받는 곳은 Pleos nexus의 `connect-avd/2.x/crp_car_emulator.zip`.
- **라이선스:** 42dot Inc. "Pleos Connect SDK License Agreement". 설치·사용이 동의로 간주된다. 주요 조항: Pleos Connect 앱 개발에 한정 · 사용료를 사전 통지 후 요구할 수 있음 · 재배포·역공학·파생물 금지 · SDK 변경·중단·해지 가능 · 한국법. **플랫폼 APK는 역공학 금지 조항 때문에 열지 않는다.**
- 하드웨어: 문서는 전용 GPU를 요구하나(RTX 3060 Laptop 6GB 이상), 내장 그래픽에서도 자체 점검을 통과하고 부팅은 됐다.
- AVD: Automotive, API 34, **콜드 부팅 필수**(퀵 부팅이면 VHAL을 쓸 수 없음), RAM 4GB, 2560×1440, 가로. 부팅 성공은 `getprop sys.boot_completed`=1로 판정(31~67초).
- 현재 안드로이드 사용자가 **10**이다. `pm clear`, `run-as`, `am start`, `appops`에 `--user 10`을 줘야 한다(없으면 사용자 0을 건드려 상태가 안 지워진다).
- Virtual Console(에뮬레이터 ≥ 2.1.0): `adb forward tcp:8088 tcp:8088` 뒤 `http://127.0.0.1:8088/`. REST로 자동화할 땐 브라우저 하트비트(`POST /browser/session?client=…`을 1초마다)가 없으면 `503 emulator_inactive`.
- 부팅이 실패하면: Pleos 서비스가 시작 시간 제한 안에 못 올라와 패닉하는 증상(차량 HAL이 `network eth0 is not ready` 반복)을 겪었다. 원인은 **호스트 CPU 속도 저하**였고, 속도가 돌아오자 같은 AVD가 46초에 부팅했다. 재시도 전에 전원 모드·AC·불필요한 앱을 확인한다.
- **부하 불안정:** Gradle 같은 무거운 작업을 에뮬레이터와 같이 돌리면 `system_server`가 재시작을 반복한다. 오래 빌드할 땐 에뮬레이터를 끄고, 끝난 뒤 콜드 부팅한다. 폰 AVD를 같이 띄우면 첫 4~5분은 과부하다.

## 8. 자동 확인 도구의 함정

- `uiautomator dump`는 글자에 큰따옴표가 있으면 그 속성을 작은따옴표로 내보낸다. 큰따옴표로만 파싱하면 빈 값이 된다.
- Git Bash는 `adb shell cat /sdcard/…`의 경로를 변환해 버린다 → `MSYS_NO_PATHCONV=1`.
- 좌표 고정 탭은 쓰지 않는다. 호스트 부하에 따라 스크롤 양이 달라, 대사 편집 칸을 눌러 키보드가 뜨고 다음 탭이 글자를 입력한 적이 있다. 화면을 dump해 버튼 위치를 찾는다.
- 오버레이 창 확인: `dumpsys window windows | grep -c <오버레이 창 이름>`. 프로세스가 죽은 이유: `logcat -b events`의 `am_kill`·`am_freeze`·`wm_task_removed`.
- 앱 없이 신호를 층별로 확인하는 방법(콘솔 상태 → VHAL 이벤트 → Pleos 서비스 → 앱 화면)은 [VEHICLE_SIGNALS](VEHICLE_SIGNALS.md)의 "앱 없이 신호를 확인하는 방법". 이 `user` 빌드에서는 `cmd car_service`가 막혀 있지만 `dumpsys car_service --services VehicleHal`은 된다.

## 9. Pleos에 확인할 것

[ROADMAP](ROADMAP.md) 4절과 같은 목록의 근거다.

1. 서드파티 차량 앱의 오버레이(`SYSTEM_ALERT_WINDOW`) 허용 여부·범위와, 가려도 되는 영역
2. 포그라운드 서비스·부팅 자동 시작 허용 여부 (프로세스가 셸에 죽는 문제와 연결)
3. 서드파티 오디오 컨텍스트·존, 선행 묵음 가이드의 SDK 경로 적용 여부
4. `setSpeakerType` 유효 값, 서버 모드 음성 목록, 피치·속도 조절 계획
5. 새 권한 승인 범위, 권한 이름 불일치
6. 드라이브 모드·안전벨트 경고가 실차에서 오는지, 이 에뮬레이터 이미지가 안 주는 이유
