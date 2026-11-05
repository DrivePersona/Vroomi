# Voice Companion 제품 요구사항 v2

**기준일:** 2026-11-05
**범위:** Pleos HMI 자동 시작, 33개 캐릭터 목표, 기본 5개 + 첫 가챠 풀 11개, 캐릭터 직접 상호작용

## 1. 제품 목적

차량이 켜지면 사용자가 Android 앱을 직접 찾아 누르지 않아도 선택된 캐릭터가 Pleos HMI의 배경 위에 나타난다. 캐릭터는 부팅 인사와 차량 상태 안내를 음성으로 제공하고, 사용자는 캐릭터를 누르거나 길게 눌러 짧은 상호작용과 플로팅 메뉴를 사용한다.

캐릭터는 공개 제품 목표 33개 안에서 기본 캐릭터와 가챠 획득 캐릭터로 제공한다. 첫 구성은 기본 5개와 가챠 풀 11개로 계획한다. 총 목표 33개는 유지하고 나머지는 다음 캐릭터 웨이브로 둔다.

여기서 캐릭터의 “TTS”는 런타임에 임의 문장을 합성하는 기능이 아니다. Qwen VoiceDesign 등으로 제작·검수한 캐릭터별 음성 클립을 VoicePack에서 재생하는 방식이다.

## 2. 확정된 제품 방향

| 항목 | 결정 |
| --- | --- |
| 캐릭터 전체 목표 | 33개 유지 |
| 첫 캐릭터 구성 | 항상 선택할 수 있는 기본 캐릭터 5개 + 랜덤 획득 대상 11개 |
| 첫 구성 이후 | 남은 17개는 다음 캐릭터 웨이브로 편성; 현재 기능의 범위를 16개로 줄이지 않음 |
| 가챠 결제 | 미정. 이 목업과 현재 요구사항에는 결제 UI를 넣지 않음 |
| 기본 캐릭터 선정 | GLB 외형, 표정/동작, 화면 크기에서의 가독성을 사람이 검토한 뒤 확정 |
| 가챠 획득 | 뽑은 캐릭터를 보유 목록에 등록하는 흐름을 제안. 자동 장착 여부는 최종 결정 전 |
| 시작 인사 | 기존 GREETING 클립을 Pleos 세션당 한 번 재사용 |
| 캐릭터 터치 | 정차 중 전용 TOUCH 클립과 지원 gesture를 재생 |
| 캐릭터 교체 | 기존 캐릭터 작별 클립 후 선택 캐릭터의 합류 클립을 순서대로 재생 |
| 설정 진입 | 캐릭터 길게 누름으로 여는 캐릭터 주변 플로팅 액션 메뉴. 닫기 X 제공 |

캐릭터 5개·11개 명단, 가챠 확률, 뽑기 재화, 중복 보상, 천장/보장 규칙은 정해지지 않았다. 이를 확정된 게임 경제로 묘사하지 않는다.

## 3. 사용자와 주요 과업

| 사용자 과업 | 완료 상태 |
| --- | --- |
| Pleos 시작 후 컴패니언 만나기 | 지원되는 자동 시작 경로로 앱/오버레이가 올라오고 선택된 캐릭터가 정차 화면에 표시됨 |
| 캐릭터와 짧게 상호작용 | 캐릭터를 한 번 누르면 해당 캐릭터의 TOUCH 음성과 동작이 실행됨 |
| 메뉴 열고 닫기 | 캐릭터를 길게 누르면 머리/모델 위치를 기준으로 메뉴가 뜨고 X를 누르면 사라짐 |
| 기본 캐릭터 선택 | 기본 5개 중 하나를 보유/선택 |
| 새 캐릭터 획득 | 가챠 풀 11개에서 추첨 결과를 보유 목록에 기록 |
| 다른 캐릭터로 전환 | 현재 캐릭터 작별 음성 → 캐릭터/음성 pair 전환 → 새 캐릭터 합류 음성 |
| 차량 상태에 반응 | Vehicle Service의 read-only 상태 변화를 기존 안전 정책과 캐릭터 음성에 전달 |

## 4. 캐릭터와 음성 계약

### 4.1 ID와 그룹

- 제품 목표 인벤토리는 docs/CHARACTER_TARGET_INVENTORY_V1.csv의 33개 characterId/voicePackId 쌍이다. 표시 이름이 아니라 안정 ID로 연결한다.
- 첫 구성은 기본 5개와 가챠 후보 11개다. 그룹 지정과 기본 캐릭터 5개 선정은 별도 콘텐츠 승인 항목이다.
- 나머지 17개는 이후 웨이브로 남긴다. 현재 캐릭터 수를 16개로 축소했다는 뜻이 아니다.
- 캐릭터 선택은 대응 voicePackId를 자동 선택한다. 음성팩 선택기와 자막 편집기를 별도 기능으로 두지 않는다.

### 4.2 음성 슬롯

기존 13개 canonical 슬롯은 차량·인사 콘텐츠의 기준으로 유지한다.

| 종류 | 슬롯 | 목적 |
| --- | --- | --- |
| 기존 | GREETING | Pleos 시작 인사와 정차 중 수동 인사 미리듣기에 재사용 |
| 기존 | BRAKE, TURN, MODE, TIRE, WARNING, DOOR, WINDOW, AMBIENT_LIGHT, HVAC, WIPER, CHARGE, ENERGY_LOW | 차량 상태 반응 12종 |
| 신규 추가 | TOUCH | 캐릭터를 한 번 눌렀을 때의 상호작용 |
| 신규 추가 | SWITCH_OUT | 현재 캐릭터가 떠날 때 작별 |
| 신규 추가 | SWITCH_IN | 새 캐릭터가 선택됐을 때 합류 |

전체 33개 캐릭터에 신규 상호작용 음성까지 제작하면 캐릭터당 16개, 총 528개 슬롯이 필요하다. 첫 16개만 먼저 완성하면 256개다. 기존 429개는 13개 core 슬롯 기준이며 신규 99개(33×3)의 제작/승인 상태는 별도로 추적한다. GREETING은 시작 인사에 재사용하므로 별도 BOOT 클립을 중복 생성하지 않는다.

가챠 당첨 연출은 우선 화면으로 보여 주고, 당첨 직후 자동 장착하지 않는 설계를 권장한다. 따라서 별도 ACQUIRE 음성은 첫 범위에서 요구하지 않는다. 자동 장착을 선택하면 SWITCH_IN의 재사용 가능성을 콘텐츠 검토 후 확정한다.

### 4.3 말투와 재생 순서

- GREETING: 선택된 캐릭터의 고유 인사. 시스템 재시작/화면 재진입마다 반복하지 않고 Pleos 세션에서 한 번만 재생한다.
- TOUCH: 짧은 단문과 캐릭터별 호칭/말버릇을 쓴다. 잦은 탭 반복은 쿨다운으로 제한한다.
- SWITCH_OUT: 아쉬움을 표현하되 사용자를 죄책감 들게 하거나 선택을 강요하지 않는다.
- SWITCH_IN: 당당하고 장난스러운 만족감을 표현한다. 비하·모욕·공격성은 금지한다.
- 전환은 이전 음성이 끝나거나 중단된 뒤 pair를 바꾸고 새 음성을 재생한다. 두 캐릭터 음성이 겹치지 않는다.
- 특별 클립도 canonical vehicle slot과 같은 형식으로 생성·청취·manifest hash/size/duration 검증을 거친다. 공개 앱 레포에는 private 음성 바이너리를 넣지 않는다.

## 5. Pleos 시작과 WindowManager overlay-first 경험

### 기본 흐름

1. Pleos가 시작될 때 제품에서 지원하는 실행 진입점이 companion runtime을 시작한다. 구체적인 진입점은 별도 플랫폼 확인 항목이다.
2. runtime은 선택 캐릭터, 해당 voicePack, 가챠 보유 목록과 표시 설정을 복원한다.
3. 차량 상태를 확인한다. 상태가 미확인이거나 주행 중이면 캐릭터·floating control·패널을 표시하지 않는다.
4. 정차가 확인되면 `SYSTEM_ALERT_WINDOW` 권한과 WindowManager 호스트를 확인하고 캐릭터 overlay만 현재 Pleos 화면 위에 표시한다.
5. 정차 및 오디오 포커스 조건이 충족되면 세션 GREETING을 한 번 재생한다.
6. 평상시에는 companion Activity를 열지 않는다. 캐릭터가 표시된 동안 뒤에 별도 컴패니언 화면을 유지하지 않는다.

### WindowManager 사용 전제

- 이 프로젝트의 개발·설계 기준은 `SYSTEM_ALERT_WINDOW`와 `TYPE_APPLICATION_OVERLAY` 사용을 허용하는 것으로 둔다. 공개 문서에서 이를 명시적으로 반려한다는 근거는 확인하지 못했다. 이는 이 제품의 진행 전제이며, Pleos가 공식 허용을 확인했다는 뜻은 아니다.
- 캐릭터·floating menu·보관함·가챠·대사·설정은 WindowManager overlay surface 안에서 단계적으로 표시한다. 차량/HMI 본래 화면, 시스템 상태 바, 주행 정보는 그대로 둔다.
- 현재 개발 구현은 개발자용 `adb appops` 설정으로 overlay를 확인했다. 제품 배포 경로에서 권한이 유지되는지와 앱 시작 진입점은 통합 검증에 남아 있다. 이 미확인은 overlay-first 설계를 중단하는 이유가 아니라 구현/출시 체크 항목이다.
- Activity는 최초 설정, 권한 복구 또는 overlay에서 제공하기 어려운 복구 경로에 사용자가 명시적으로 진입할 때만 연다. 정상 시작 시 Activity를 자동으로 앞에 띄우지 않는다.
- 공개 Vehicle SDK는 점화 이벤트를 제공한다고 전제하지 않는다. `BOOT_COMPLETED`, HMI 세션 callback, 제품 전용 hook 중 실제 제공되는 진입점을 확인해 연결한다.
- 주행 상태가 미확인 또는 주행 중이면 GREETING, TOUCH, 캐릭터 교체, 가챠, floating menu와 패널을 실행하지 않는다. 기존 차량 이벤트 음성은 별도의 주행 정책을 따른다.

## 6. 캐릭터 터치와 floating controls

### 입력과 제스처 우선순위

- 짧은 탭은 캐릭터 전용 TOUCH 클립과 지원 동작을 실행한다.
- 길게 누름은 약 500ms 기준으로 floating controls를 연다. 길게 누름으로 판정된 입력은 TOUCH를 실행하지 않는다.
- 드래그 이동은 캐릭터 위치만 바꾸고 TOUCH나 메뉴 열기를 실행하지 않는다. 탭·홀드·드래그는 한 번의 포인터 입력에서 하나만 확정한다.
- 주행 전환 또는 차량 상태 미확인 시 캐릭터, floating controls와 모든 수동 패널을 즉시 숨기고 입력을 잠근다.

### overlay 구성과 배치

- 하나의 WindowManager overlay 경험 안에서 캐릭터, 캐릭터 주변 radial/floating buttons, 필요한 컴팩트 패널을 표시한다. 오른쪽에 상시 노출되는 companion 앱 화면은 두지 않는다.
- floating controls는 GLB head anchor를 우선 사용하고, 없으면 모델 bounds 상단 또는 상체 anchor를 기준으로 배치한다. 얼굴과 주요 동작을 가리지 않도록 safe bounds 안에서 위치를 제한한다.
- 버튼은 캐릭터 변경, 가챠/보관함, 고정 대사 듣기, 캐릭터 설정, X 닫기로 구성한다. 캐릭터 탭의 TOUCH와 메뉴 열기는 입력 규칙으로 구분한다.
- 목록과 설정은 캐릭터 인접 위치에서 열리는 compact overlay sheet로 제공한다. 화면 전체를 차지하는 별도 앱 화면으로 전환하지 않는다.
- X는 현재 메뉴/패널만 닫고 캐릭터 overlay는 유지한다. 바깥 탭은 메뉴/패널을 닫되 캐릭터 입력과 혼동되지 않게 처리한다.
- 최초 설정·권한 복구·비정상 상태 안내는 명시적으로 요청한 경우에만 Activity로 연결한다. 주행 중에는 Activity를 자동으로 열지 않는다.

## 7. 가챠와 보유 캐릭터

- 기본 5개는 첫 실행부터 보유 상태다.
- 초기 뽑기 풀 11개는 별도 후보 풀로 노출한다. 아직 보유하지 않은 캐릭터를 결과에 추가한다는 UX를 권장한다.
- 보유 목록과 현재 장착 캐릭터는 분리한다. 뽑았다는 이유만으로 현재 캐릭터를 자동 교체하지 않는 것이 기본 제안이다.
- 추첨 결과가 나오면 결과 카드에 이름, 캐릭터 미리보기, 보유 등록 상태를 보여 준다. 새 캐릭터를 장착할 때만 SWITCH_OUT/SWITCH_IN 전환 대사를 재생한다.
- 뽑기 횟수/재화, 유료 구매 여부, 캐릭터별 확률, 중복 당첨 보상, 보장 규칙, 취소/오류 복구 정책은 미결정이다. 결정과 표시 문구가 정해지기 전에는 실제 뽑기 비용이나 확률 수치를 UI/구현에 넣지 않는다.
- 목업은 무작위 결과의 시각 시연만 제공한다. 결제나 실제 보유권한을 만들지 않는다.
- 가챠 보유 상태는 앱/시스템 재시작 후에도 복원되어야 한다. 저장 위치, Pleos profile/account 연계, 백업 정책은 플랫폼 확인 후 확정한다.

## 8. 화면 요구사항

1. 제품에서 지원하는 시작 진입점으로 runtime을 시작하고, 정차가 확인되면 companion Activity를 띄우지 않은 채 캐릭터 overlay와 세션 GREETING을 시작한다.
2. 캐릭터의 짧은 탭과 길게 누르기는 서로 배타적으로 동작한다.
3. 길게 누르면 캐릭터 anchor 주변에 floating action과 X가 표시된다.
4. 기본 캐릭터 5개와 가챠 소유 캐릭터가 구분되고, 가챠 풀 11개의 소유/미소유 상태가 보인다.
5. 획득과 장착은 별도 상태로 표현한다.
6. 캐릭터 전환 음성은 이전 → 새 캐릭터 순으로 재생하고 body gesture를 해당 음성에 맞춘다.
7. Pleos 차량/지도/상태 화면과 시스템 바를 유지하며 floating menu와 compact sheet가 safe bounds 밖으로 나가지 않는다.
8. 제품 컬러/컴포넌트는 실제 Pleos 캡처와 현재 CompanionTheme 토큰을 기준으로 한다. TDS 정식 인증을 주장하지 않는다.

## 9. 현재 상태와 출시에 필요한 작업

| 항목 | 확인 상태 |
| --- | --- |
| 캐릭터 ID target | 33개 유지 |
| 첫 구성 | 기본 5 + 가챠 11; 구체 ID와 표시 이름은 승인 전 |
| canonical 차량/인사 슬롯 | 33×13=429 목표 |
| 2026-11-05 WAV 현황 | 303개 후보 파일; 126개 canonical 슬롯 누락. 오디션 파일은 출시 승인 WAV가 아님 |
| 추가 interaction voice | TOUCH/SWITCH_OUT/SWITCH_IN 제작 상태 미반영 |
| release roster | 0개 |
| 캐릭터 상업 이용 | 32/33 manifest가 commercialUse=false; dev.mori도 release voice 연결 없음 |
| Pleos auto-start | 제품 시작 진입점과 앱 생명주기 확인 전; 정상 흐름은 Activity 비노출 |
| overlay 구현 전제 | 이 프로젝트는 `SYSTEM_ALERT_WINDOW`/`TYPE_APPLICATION_OVERLAY` 허용을 가정해 진행. 공개 명시적 반려 근거 미확인; AVD 검증은 개발용 `adb appops` 범위 |
| 가챠 정책 | 유료성/재화/확률/중복 규칙 미정 |

### 출시 게이트

- [ ] 기본 5개 선정 및 미술/동작/권리 승인
- [ ] 첫 가챠 11개 ID/명칭/권리 및 확률·중복·재화 정책 승인
- [ ] 첫 16개 캐릭터 각각 13 canonical + TOUCH/SWITCH_OUT/SWITCH_IN 클립 검수
- [ ] 저장된 보유 목록/선택 캐릭터의 복원 및 식별자 안정성 검증
- [ ] Pleos 지원 시작 진입점과 사용자 잠금/프로세스 생명주기 연결
- [ ] Pleos AVD에서 Activity 없이 캐릭터 overlay 시작, 인사 1회, 터치, long press/X, 가챠 sheet, 교체 순서 확인
- [ ] Pleos 제품 배포 이미지에서 overlay permission이 유지되는지 및 HMI/시스템 우선순위 동작 확인
- [ ] head anchor를 이용한 menu 위치/추적/화면 safe bounds 검증
- [ ] 주행 전환과 runtime 재생성 시 캐릭터/menu/sheet/touch/audio가 안전하게 정리되는지 확인

## 10. 관련 문서

- [33개 대상/현재 오디오 범위](RELEASE_VOICE_CHARACTER_PLAN_V1.md)
- [Pleos 사용자 흐름 v2](USER_FLOWS_V2.md)
- [Pleos 와이어프레임 v2](WIREFRAMES_V2.md)
- [실제 HMI 캡처 기반 인터랙티브 목업](mockups/voice-companion-flow-v2.html) · [PNG 미리보기](mockups/voice-companion-flow-v2.png)
- [Virtual Console E2E](PLEOS_VIRTUAL_CONSOLE_E2E.md)
- [speech/gesture 동기화 계약](SPEECH_GESTURE_SYNC.md)

### 구현 추적 이슈

- [#36 overlay runtime 시작과 Activity 비노출](https://github.com/DrivePersona/pleos-voice-companion/issues/36)
- [#37 캐릭터 주변 floating controls와 overlay sheet](https://github.com/DrivePersona/pleos-voice-companion/issues/37)
- [#38 Pleos AVD overlay-first E2E 및 화면 증적](https://github.com/DrivePersona/pleos-voice-companion/issues/38)
- [#39 실제 가챠 draw 활성화 전 획득 정책 결정](https://github.com/DrivePersona/pleos-voice-companion/issues/39)
