# Voice Companion 사용자 흐름 v2 — WindowManager overlay-first

**기준:** 2026-11-05
**목표:** 사용자는 Pleos의 현재 화면을 그대로 보면서 캐릭터와 필요한 기능을 오버레이에서 사용한다. 정상 흐름에서 컴패니언 Activity를 열거나 뒤에 상시 노출하지 않는다.

## 제품 결정과 전제

- 전체 출시 목표는 33개다. 첫 웨이브는 기본 캐릭터 5개와 가챠 후보 11개로 시작하고, 나머지 17개 목표도 유지한다.
- 기본 5개/가챠 11개의 이름과 캐릭터 자산은 선정 전이다. 문서·목업은 번호 슬롯만 보여 주며 임의 캐릭터를 확정하지 않는다.
- 결제 UI, 재화, 확률, 중복/보장 규칙은 정책 결정 전까지 넣지 않는다.
- 기본 진행 전제는 `SYSTEM_ALERT_WINDOW`와 `TYPE_APPLICATION_OVERLAY`를 사용하는 것이다. 공개 문서에서 명시적 반려 근거는 확인되지 않았지만, 공식 제품 승인을 받았다고 단정하지 않는다.
- 본 문서의 TTS/대사 재생은 캐릭터별로 검수한 WAV 클립을 뜻한다. 주행 중 임의의 수동 상호작용과 패널은 잠근다.

## 1. Pleos 시작 → 캐릭터 표시

| 순서 | 사용자에게 보이는 흐름 | runtime 동작 |
| --- | --- | --- |
| 1 | Pleos가 평소처럼 시작되고 차량/HMI 화면이 유지됨 | 제품에서 실제 지원하는 시작 진입점이 runtime을 시작. 구체 hook은 확인 후 연결 |
| 2 | 별도 컴패니언 화면 없이 현재 화면이 계속 보임 | 캐릭터 ID, 연결 voicePack, 보유 목록과 설정 복원 |
| 3 | 차량이 정차 상태로 확인되면 캐릭터만 HMI 위에 나타남 | 차량 상태·overlay 권한·safe bounds 확인 후 WindowManager surface 표시 |
| 4 | 정차 및 오디오 조건이 허용되면 캐릭터가 인사 | 세션당 GREETING WAV 1회 재생 |
| 5 | 주행 또는 상태 미확인 | 캐릭터/메뉴/패널을 숨기고 수동 조작을 잠금 |

자동 실행이 실패해도 Activity를 강제로 앞으로 띄우지 않는다. 사용자가 설정/복구를 요청한 경우에만 Activity로 이동한다.

## 2. 캐릭터를 누르고 floating controls 열기

| 입력 | 판정 | 반응 |
| --- | --- | --- |
| 짧게 탭 | 홀드/드래그가 아닌 탭 | TOUCH WAV와 해당 캐릭터에 승인된 짧은 동작 |
| 약 500ms 제자리 홀드 | 이동이 없고 홀드 시간이 지남 | 캐릭터 주변 floating buttons 표시. TOUCH는 재생하지 않음 |
| 드래그 | 이동 거리가 tap slop 초과 | 캐릭터 위치 이동/저장. 메뉴/TOUCH는 실행하지 않음 |
| X | 메뉴 또는 sheet 열린 상태 | 현재 메뉴/sheet만 닫고 캐릭터 유지 |
| 주행 또는 상태 미확인 | 언제든지 | 캐릭터와 메뉴/sheet 숨김, 입력 잠금 |

세 제스처는 포인터 입력 하나당 하나만 실행한다. 주행 제한은 제스처보다 우선한다.

### 캐릭터 주변 메뉴

1. 캐릭터 바꾸기
2. 가챠 / 보관함
3. 고정 대사 듣기
4. 캐릭터 설정
5. X 닫기

메뉴는 캐릭터의 GLB head anchor, 없으면 모델 bounds 상단에 맞춘다. 메뉴와 패널은 화면 safe bounds 안에 두며 Pleos 상태 바·차량 제어·시스템 바를 가리지 않는다.

## 3. 메뉴별 overlay 흐름

### 캐릭터 바꾸기

1. 기본 5개와 보유한 가챠 캐릭터를 compact overlay sheet로 보여 준다.
2. 기본 캐릭터 5개는 선택 가능하고, 미보유 가챠 캐릭터는 잠금 상태를 보여 준다.
3. 새 캐릭터 선택 시 현재 캐릭터의 SWITCH_OUT을 재생하고 안전하게 끝난 뒤 캐릭터와 voicePack을 함께 바꾼다.
4. 새 캐릭터가 SWITCH_IN으로 합류한다. 두 캐릭터 음성은 겹치지 않는다.

### 가챠 / 보관함

1. 첫 후보 11개와 보유 여부를 compact overlay sheet에서 확인한다.
2. 뽑기 결과는 보유 목록에 등록하는 제안 흐름이다. 보유와 장착은 별개다.
3. 뽑기 비용·확률·중복 규칙이 결정되기 전에는 결제, 재화, 확률 숫자를 보여 주지 않는다.
4. PRD 목업의 추첨은 시각 시연이며 실제 소유권/결제 결과를 만들지 않는다.

### 대사 듣기 / 설정

- 대사 목록은 선택 캐릭터의 고정 자막과 WAV를 1:1로 연결한다. 문장 편집이나 별도 음성팩 선택은 제공하지 않는다.
- 사용자가 대사 행을 누르면 정차 중 해당 캐릭터 WAV를 재생한다.
- 설정은 자동 인사, 정차 중 캐릭터 표시처럼 짧은 항목만 compact overlay로 제공한다.

## 4. 주행 및 예외 흐름

| 조건 | 처리 |
| --- | --- |
| 차량 상태 미확인 | 주행으로 간주하고 overlay 조작을 표시하지 않음 |
| 주행 규제 시작 | 캐릭터·floating menu·sheet를 즉시 숨기고 수동 WAV/가챠/교체를 중단 또는 보류 |
| 오디오 포커스 거절 | WAV가 재생된 것으로 기록하지 않고 재시도/보류 상태 표시 |
| 캐릭터/voicePack 손상 | 다른 캐릭터 파일로 조용히 대체하지 않고 준비 상태와 이유 표시 |
| overlay 사용 불가 | 현재 HMI를 유지하고 overlay 상태를 기록. Activity는 사용자가 복구를 요청할 때 열기 |
| 시작 hook 미지원 | 점화/부팅 성공을 추정하지 않고 수동 복구 진입점 제공 |

기존 차량 신호는 앱에서 새로 만들지 않는다. Virtual Console이나 차량이 보내는 값을 Pleos Vehicle Service callback으로 받고 기존 이벤트 정책을 적용한다.

## 5. 구현·검증 경계

- 현재 앱 소스에는 캐릭터 overlay/드래그/탭 구현이 있지만 floating 메뉴·collection sheet·가챠 UI는 아직 없다.
- 기존 개발 검증은 `adb appops`로 overlay를 켠 AVD 범위다. 이는 앱 동작 확인이지 Pleos 제품 권한이 공식 승인됐다는 증거가 아니다.
- Manifest의 기존 Launcher Activity는 설치·수동 설정·복구 진입용으로 남길 수 있다. 정상 런타임에서 Activity를 뒤 화면으로 띄우지 않는 것이 이번 UX 기준이다.
- Pleos가 제공하는 자동 시작 진입점, 사용자/프로세스 생명주기, overlay 권한의 배포 후 지속 여부는 구현 착수 시 확인해야 한다.
- Android 구현 시 WindowManager overlay가 시스템/긴급 안내보다 우선하지 않도록 하고, 주행 상태 변경에서 숨김 처리를 검증한다.

## 관련 문서

- [제품 요구사항 v2](PRODUCT_REQUIREMENTS_V2.md)
- [WindowManager overlay-first 와이어프레임](WIREFRAMES_V2.md)
- [overlay-first 인터랙티브 목업](mockups/voice-companion-flow-v2.html)
- [33개 캐릭터 음성 대상](RELEASE_VOICE_CHARACTER_PLAN_V1.md)

## 구현 추적 이슈

- [#36 Pleos 시작 진입점과 Activity 없는 overlay runtime](https://github.com/DrivePersona/Vroomi/issues/36)
- [#37 floating controls 및 캐릭터 관리 overlay sheet](https://github.com/DrivePersona/Vroomi/issues/37)
- [#38 Pleos AVD UI/lifecycle 검증과 PR 증적](https://github.com/DrivePersona/Vroomi/issues/38)
- [#39 첫 가챠 풀 획득 정책](https://github.com/DrivePersona/Vroomi/issues/39)
