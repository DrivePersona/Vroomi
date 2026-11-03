# Pleos Virtual Console 차량 이벤트 E2E 확인 방법

이 문서는 **Pleos Connect 에뮬레이터의 Virtual Console 조작이 Voice Companion 앱까지 실제로 전달되고, 앱의 반응(기록·음성·캐릭터)이 발생하는지** 확인하는 재현 절차다.

Virtual Console은 별도 차량 에뮬레이터가 아니다. 이미 실행 중인 Pleos Connect AVD의 VHAL(차량 HAL)에 값을 보내는 브라우저 조작 화면이다. 따라서 하나의 Pleos AVD에 앱을 설치한 뒤, 같은 AVD의 Virtual Console을 열어 함께 확인한다.

## 확인 범위와 판정 기준

현재 앱이 읽기 전용으로 구독하는 항목 중 Virtual Console에서 재현 가능한 핵심 흐름은 다음과 같다.

| 흐름 | Virtual Console 조작 | 앱에서 성공으로 보는 증거 |
| --- | --- | --- |
| 브레이크 | Motion > Brake Pedal: `0 → 70%` | 기록에 `브레이크 밟기`와 `말하기 요청`이 추가됨. 정차 중이면 캐릭터 반응과 음성 재생 시작이 이어짐 |
| 방향지시등 | Convenience > Turn Signal State: `Off → Left Blink` 또는 `Right Blink` | 기록에 `방향지시등 켜기`와 `말하기 요청`이 추가됨. 정차 중이면 캐릭터 반응과 음성 재생 시작이 이어짐 |
| 주행 제한 | Motion > Gear: `P → D` | 앱이 주행 상태가 되고 캐릭터/편집 UI가 숨거나 차단됨. 허용된 짧은 상태 음성만 정책에 따라 재생됨 |
| 창문 잠금 | Convenience > Window Lock: 잠김/해제 전환 | capability가 지원하는 영역만 활성으로 나타나고, dataKey cooldown으로 반복 반응이 제한됨 |
| 창문 위치 | Convenience > Window Position: 닫힘 → 중간 → 열림 | 마지막 조작 후 800ms 안정화된 최종 범주에 한 번 반응함 |
| 앰비언트 밝기 | Convenience > Ambient Brightness: 어두움 → 보통 → 밝음 | 슬라이더 조작 중 반복 발화 없이, 안정화된 밝기 범주에 한 번 반응함 |
| 앰비언트 색상 | Convenience > Ambient Color: 서로 다른 색상 선택 | 변경값을 기록하고 dataKey cooldown 안의 반복 반응을 제한함 |
| 도어 잠금/위치 | Convenience > Door Position: 지원 영역에서 변경 | capability가 반환한 영역에서 열림 범주 변경에 한 번 반응함 |

`음성 재생 시작`은 앱 기록의 `재생됨` 또는 Pleos Speech Service의 시작 이벤트를 뜻한다. 단지 Virtual Console 카드의 값만 바뀐 것은 **차량 신호 송신 성공**일 뿐, 앱 반응까지 확인한 것은 아니다.

> 현재 Pleos Connect v2.1.0 이미지에서는 브레이크와 방향지시등이 실측 가능하다. 드라이브 모드와 안전벨트 경고는 해당 이미지가 콜백을 제공하지 않아 이 절차의 합격 항목이 아니다.

> 빠른 설정 구독(이슈 #11)은 `CAR_LIGHTS`, `CAR_WINDOWS`, `CAR_DOORS` 읽기 권한과 각 API의 capability 결과가 필요하다. 아래 시나리오는 구현 후 Pleos 전용 AVD에서 실행할 항목이며, 해당 차량/이미지에서 지원되지 않는 영역은 미지원으로 기록하고 건너뛴다.

## 사전 조건

- Pleos Connect AVD 2.1.0 이상을 **콜드 부팅**한다.
- 앱 APK를 그 AVD의 현재 Android 사용자에 설치한다. 이 이미지에서는 보통 사용자 `10`이다.
- 앱의 실제 패키지 이름을 확인한다. `reviewDebug`는 `com.example.voicecompanion.review`, 기본 debug는 `com.example.voicecompanion`이다.
- 앱 설정의 `주행 중 음성`은 테스트 목적에 맞게 설정한다. 브레이크·방향지시등은 우선 기어 `P`에서 검증한다.
- 앱 Manifest와 Pleos App Market 사전 승인 범위에 읽기 권한 `CAR_LIGHTS`, `CAR_WINDOWS`, `CAR_DOORS`가 포함되어 있는지 확인한다. 제어 권한은 필요하지 않다.
- 앱을 연 직후 각 차량 API가 보내는 **첫 상태 값은 기준값**으로만 사용한다. 이벤트 확인 전에는 브레이크를 `0%`, 방향지시등을 `Off`로 맞춘다.

## 1. AVD와 Virtual Console 연결

PowerShell에서 연결된 디바이스와 Android 사용자를 확인한다.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb -s emulator-5554 shell am get-current-user
```

예시는 시리얼이 `emulator-5554`, 사용자가 `10`인 경우다. 다른 값이면 이후 명령의 시리얼과 `--user` 값을 바꾼다.

Virtual Console 포트를 AVD에 전달한 뒤 브라우저에서 연다.

```powershell
& $adb -s emulator-5554 forward tcp:8088 tcp:8088
Start-Process 'http://127.0.0.1:8088/'
```

브라우저의 Console 화면과 AVD 화면을 나란히 놓는다. Virtual Console의 값 변화와 앱의 반응 기록을 같은 시점에 비교할 수 있어야 한다.

## 2. 앱을 전면에 열고 기준 상태를 만든다

설치된 패키지를 확인한다.

```powershell
& $adb -s emulator-5554 shell pm list packages voicecompanion
```

예를 들어 review APK를 설치했다면 다음과 같이 앱을 전면으로 연다.

```powershell
& $adb -s emulator-5554 shell am start --user 10 `
  -n com.example.voicecompanion.review/.ui.MainActivity
```

기본 debug APK라면 패키지만 `com.example.voicecompanion`으로 바꾼다. 앱의 상태 표시에서 차량 구독 요청이 완료된 뒤, Virtual Console에서 다음 기준 상태를 만든다.

- Gear: `P`
- Brake Pedal: `0%`
- Turn Signal State: `Off`

방향지시등을 켜면 Pleos가 EVS 카메라를 전면에 띄울 수 있다. 앱 기록을 읽을 때는 다시 `Off`로 돌려 EVS를 닫는다.

## 3. 브레이크 이벤트 확인

1. Virtual Console에서 **Motion > Brake Pedal**을 `0%`에서 `70%` 정도로 올린다.
2. 앱의 기록 카드에서 가장 최근 행을 확인한다.
3. 정차 상태(P)라면 캐릭터의 브레이크 반응과 `말하기 요청` 후 `재생됨`을 확인한다.
4. 같은 값을 유지한 채 재시도하지 말고 `0%`로 내린 뒤, 이벤트 쿨다운(기본 10초)이 지난 후 다시 `70%`로 올린다.

성공 예시는 다음 순서다.

```text
Virtual Console: Brake Pedal 0% → 70%
앱 기록: 브레이크 밟기 → 말하기 요청 → 재생됨
화면: 정차 중에는 캐릭터 브레이크 반응 표시
오디오: Pleos 차량 Speech Service가 해당 대사를 재생
```

브레이크가 `70%`로 표시되어도 앱의 기록이 없다면, 해당 값은 VHAL까지는 전달되었지만 앱 콜백 검증은 실패한 것이다. 아래의 문제 해결 순서로 진행한다.

## 4. 방향지시등 이벤트 확인

1. **Convenience > Turn Signal State**를 먼저 `Off`로 맞춘다.
2. `Left Blink` 또는 `Right Blink`로 바꾼다.
3. 앱 기록에서 `방향지시등 켜기 → 말하기 요청 → 재생됨`을 확인한다.
4. 확인 후 반드시 `Off`로 되돌린다. 그래야 EVS 전면 화면이 닫히고 다음 테스트의 앱 UI를 확인할 수 있다.

방향지시등은 `Off`에서 좌/우 상태로 바뀌는 순간만 이벤트다. 이미 `Left Blink`인 상태에서 같은 버튼을 다시 누르는 것은 새 이벤트가 아니다.

## 5. 주행 제한 동작 확인

1. Motion 카드에서 Gear를 `P`에서 `D`로 변경한다.
2. 앱이 주행 상태로 전환되는지 확인한다. 이 앱은 주행 중 캐릭터를 숨기고, 편집·미리듣기 같은 주의 분산 동작을 차단한다.
3. 필요 시 브레이크를 한 번 조작해, 설정된 주행 중 음성 정책에 따라 짧은 음성만 처리되는지 확인한다.
4. 확인을 마치면 Gear를 반드시 `P`로 복구한다.

Pleos 셸이 주행 중 앱 화면을 `주행 중에는 사용할 수 없습니다` 화면으로 덮는 것은 별도 결함이 아니라, 앱이 distraction-optimized 앱으로 선언되지 않았을 때의 플랫폼 주행 제한이다. 앱의 차량 연결과 음성 처리 자체는 백그라운드에서 계속될 수 있으므로, 화면이 아니라 기록과 재생 상태로 판단한다.

## 6. 빠른 설정 편의 신호 확인

아래 신호는 기어 `P`에서 검증한다. 조작 전에 각 데이터가 개발 빌드의 `Vehicle 데이터 디버그`에서 `수신 중`인지 확인한다. 첫 값은 기준 상태이므로 상태를 바꾸기 전에 원하는 시작값을 만든다.

1. **Window Lock**를 지원 영역에서 `Unlocked → Locked`로 바꾼다. 동일 key를 다시 바꾸려면 dataKey cooldown이 지난 뒤 조작하고, 한 번의 변경이 반복 반응을 만들지 않는지 확인한다.
2. **Window Position**을 닫힘에서 열림 방향으로 여러 번 움직인 뒤 손을 뗀다. 마지막 조작 후 800ms 동안 반복 반응이 없어야 하고, 최종 범주가 안정된 뒤 한 번 반응해야 한다.
3. **Ambient Brightness**를 빠르게 움직여 밝기 범주를 바꾸고 멈춘다. 슬라이더 조작 중에는 발화하지 않고 마지막 범주가 안정된 뒤 한 번 반응해야 한다.
4. **Ambient Color**를 서로 다른 색상으로 바꾼다. 새 값은 데이터 스냅샷에 기록되고, dataKey cooldown 동안 반복 반응이 제한되는지 확인한다.
5. **Door Lock / Door Position**에서 capability가 반환한 영역 하나를 바꿔 앱의 상태·음성·gesture가 한 번 실행되는지 확인한다. `HOOD` 또는 `REAR` 영역이 반환되면 frunk/trunk 데이터로 구분해 기록한다.
6. 기어를 `D`로 바꾸고 같은 편의 신호를 변경한다. 상태는 갱신되어도 편의 음성·gesture는 실행되지 않아야 한다. 끝나면 기어를 `P`로 복구한다.

창문 위치, 블라인드, 스마트 글라스 농도와 도어 위치의 범주는 capability가 반환한 `AreaBounds`를 기준으로 계산한다. 지원 여부와 값 범위를 특정 차종이나 영역에 고정하지 않는다.

## 자동화 보조: 신호 송신 상태를 HTTP로 확인하기

브라우저 UI가 작거나 조작 결과를 수집하고 싶을 때만 사용한다. 이는 Virtual Console 브라우저가 호출하는 동일한 로컬 API를 확인하는 보조 수단이며, 앱 반응 확인을 대체하지 않는다.

```powershell
# 현재 Virtual Console이 인식하는 브레이크 상태 확인
$state = Invoke-RestMethod 'http://127.0.0.1:8088/vehicle/state'
$state.controls.brake_pct

# 브레이크 70% 입력 후 다시 상태 확인
Invoke-WebRequest -Method Put 'http://127.0.0.1:8088/vehicle/controls?brake=70' | Out-Null
(Invoke-RestMethod 'http://127.0.0.1:8088/vehicle/state').controls.brake_pct

# 테스트 종료 시 브레이크 해제
Invoke-WebRequest -Method Put 'http://127.0.0.1:8088/vehicle/controls?brake=0' | Out-Null
```

마지막 출력이 `70`이면 Virtual Console에서 VHAL 상태까지 입력이 반영된 것이다. 그 뒤 앱 기록과 재생 상태까지 확인해야 E2E 통과다.

## 실패 시 점검 순서

| 증상 | 점검 및 조치 |
| --- | --- |
| `http://127.0.0.1:8088`에 접속하지 못함 | `adb forward tcp:8088 tcp:8088`을 다시 실행하고 AVD가 완전히 부팅됐는지 확인한다. |
| 카드 값은 바뀌지만 앱 기록이 없음 | 앱을 완전히 전면에 열고 2~3초 기다려 구독을 완료한다. 브레이크는 `0 → 70`, 방향지시등은 `Off → Left/Right`의 **에지 변화**로 다시 만든다. 쿨다운도 지난 뒤 재시도한다. |
| `SecurityException` 또는 구독 실패 | 앱 매니페스트의 읽기 권한(`CAR_DRIVE`, `CAR_LIGHTS`, `CAR_SAFETY`)과 설치된 APK의 패키지/사용자가 맞는지 확인한다. 폰 AVD에서는 차량 권한 실패가 정상이며 Pleos Connect AVD에서만 이 E2E를 한다. |
| 앱이 보이지 않음 | Gear가 `D`인지 먼저 확인하고 `P`로 돌린다. 방향지시등은 `Off`로 바꿔 EVS 전면 화면을 닫는다. 그 다음 `am start --user <사용자> -n <패키지>/.ui.MainActivity`로 다시 연다. |
| 소리가 안 들림 | 앱 기록의 `재생됨` 여부를 먼저 분리해 본다. `재생됨`이 있으면 앱 요청/서비스 시작은 성공한 것이며, 에뮬레이터 호스트 오디오 출력·볼륨을 점검한다. `재생됨`도 없으면 앱의 음성 초기화/오디오 포커스를 로그로 확인한다. |
| 안전벨트·드라이브 모드가 반응하지 않음 | 현재 v2.1.0 VHAL에서는 앱이 해당 공개 콜백을 받지 못하는 것으로 확인됐다. 브레이크·방향지시등과 별개로 기록하고, 지원 차량 또는 다음 에뮬레이터 이미지에서 재검증한다. |

## 테스트 종료 체크리스트

- [ ] Gear를 `P`로 복구했다.
- [ ] Brake Pedal을 `0%`로 복구했다.
- [ ] Turn Signal State를 `Off`로 복구했다.
- [ ] 브레이크와 방향지시등 각각에 대해 Virtual Console 값 변화, 앱 기록, 음성 재생 상태를 함께 캡처했다.
- [ ] 실패했다면 AVD 버전, APK 버전/패키지, Android 사용자, Virtual Console 상태와 앱 기록을 이슈에 남겼다.

## 구현상 주의할 점

- Virtual Console 버튼 클릭을 앱이 직접 받는 구조가 아니다. Vehicle Service의 상태 콜백을 `PleosVehicleSource`가 받고, 이를 도메인 `VehicleEvent`로 변환한다.
- 차량 모델·에뮬레이터 이미지별 지원 속성이 다르므로, 앱은 가능한 API를 capability/구독 결과에 따라 다뤄야 한다. 특정 차종 이름이나 Console 카드의 존재만으로 지원을 가정하지 않는다.
- 반복 음성은 이벤트별 쿨다운과 동시 이벤트 우선순위를 적용한다. 그래서 두 번째 조작이 의도적으로 `버림 · 쿨다운`으로 기록될 수 있으며, 이는 신호 미수신이 아니다.
- 주행 중에는 상태를 읽을 수 있어도 사용자 편집·미리듣기·화면 조작은 제한한다. 음성은 짧고 안전 관련된 사전 승인 대사만 정책에 따라 허용한다.
