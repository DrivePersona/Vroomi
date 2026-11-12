# 문서 지도

DrivePersona Pleos Voice Companion의 모든 문서가 여기서 시작한다. 처음 오는 사람은 아래 "읽는 순서"를 따른다.

## 읽는 순서

| 누구 | 순서 |
| --- | --- |
| 프로젝트를 처음 보는 사람 | [PROJECT_STATUS](PROJECT_STATUS.md) → [PRODUCT_REQUIREMENTS_V2](PRODUCT_REQUIREMENTS_V2.md) → [USER_FLOWS_V2](USER_FLOWS_V2.md) → [ROADMAP](ROADMAP.md) |
| 이어서 개발할 사람 | [PROJECT_STATUS](PROJECT_STATUS.md) → [ROADMAP](ROADMAP.md) → [PLEOS_PLATFORM_FINDINGS](PLEOS_PLATFORM_FINDINGS.md) → [VEHICLE_SIGNALS](VEHICLE_SIGNALS.md) → [PLEOS_VIRTUAL_CONSOLE_E2E](PLEOS_VIRTUAL_CONSOLE_E2E.md) |
| 캐릭터·음성 제작 담당 | [RELEASE_VOICE_CHARACTER_PLAN_V1](RELEASE_VOICE_CHARACTER_PLAN_V1.md) → [CHARACTER_VOICE_BINDING](CHARACTER_VOICE_BINDING.md) → [ANIMATION_AND_ACTING](ANIMATION_AND_ACTING.md) |
| Pleos 협의·심사 담당 | [PLEOS_PLATFORM_FINDINGS](PLEOS_PLATFORM_FINDINGS.md)의 "Pleos에 확인할 것" → [ROADMAP](ROADMAP.md)의 열린 결정 |
| 지난 결정의 이유가 궁금한 사람 | [DECISIONS](DECISIONS.md) → [WORK_LOG](WORK_LOG.md) → [RESEARCH_NOTES](RESEARCH_NOTES.md) |

## 전체 목록

### 현재 상태와 계획

| 문서 | 내용 | 성격 |
| --- | --- | --- |
| [PROJECT_STATUS](PROJECT_STATUS.md) | 지금 무엇이 되고, 무엇이 검증됐고, 무엇이 안 됐는지 | 상태 (갱신형) |
| [ROADMAP](ROADMAP.md) | 앞으로 할 일, 출시 차단 요소, 열린 결정, 알려진 문제 | 계획 (갱신형) |
| [DECISIONS](DECISIONS.md) | 결정 기록: 무엇을, 왜, 언제, 누가 | 기록 (추가형) |
| [WORK_LOG](WORK_LOG.md) | 날짜순 작업 흐름 | 기록 (추가형) |

### 제품·설계

| 문서 | 내용 |
| --- | --- |
| [PRODUCT_REQUIREMENTS_V2](PRODUCT_REQUIREMENTS_V2.md) | 제품 요구사항, 출시 게이트 |
| [USER_FLOWS_V2](USER_FLOWS_V2.md) · [WIREFRAMES_V2](WIREFRAMES_V2.md) · [mockups/](mockups/voice-companion-flow-v2.html) | overlay-first 사용자 흐름과 화면 |
| [CHARACTER_VOICE_BINDING](CHARACTER_VOICE_BINDING.md) | 캐릭터 선택 → 음성 팩 자동 연결 계약 |
| [SPEECH_GESTURE_SYNC](SPEECH_GESTURE_SYNC.md) | 음성과 몸짓의 동기화 |
| [DIALOGUE_MATRIX_V1](DIALOGUE_MATRIX_V1.md) · [csv](DIALOGUE_MATRIX_V1.csv) | 대사 슬롯 초안 (10캐릭터 논의용) |
| [ANIMATION_AND_ACTING](ANIMATION_AND_ACTING.md) | 캐릭터 연기·애니메이션 제작 방법과 교훈 |

### 플랫폼·차량 신호

| 문서 | 내용 |
| --- | --- |
| [PLEOS_PLATFORM_FINDINGS](PLEOS_PLATFORM_FINDINGS.md) | Pleos SDK·TTS·권한·주행 규제·에뮬레이터 실측과 함정 |
| [VEHICLE_SIGNALS](VEHICLE_SIGNALS.md) · [xlsx](vehicle-signals.xlsx) | 수신 가능한 차량 신호 전체 표 |
| [PLEOS_VIRTUAL_CONSOLE_E2E](PLEOS_VIRTUAL_CONSOLE_E2E.md) | Virtual Console 신호가 앱 반응으로 이어지는지 확인하는 절차 |

### 릴리스·검증 증적

| 문서 | 내용 |
| --- | --- |
| [RELEASE_VOICE_CHARACTER_PLAN_V1](RELEASE_VOICE_CHARACTER_PLAN_V1.md) · [RELEASE_ROSTER_V1](RELEASE_ROSTER_V1.md) | 33개 캐릭터 목표와 릴리스 게이트 |
| [CHARACTER_TARGET_INVENTORY_V1.csv](CHARACTER_TARGET_INVENTORY_V1.csv) | 33개 ID 쌍 목록 |
| [PACK_RELEASES](PACK_RELEASES.md) | 비공개 팩 게시 기록 |
| [PROJECT_AND_PR_HISTORY_V1](PROJECT_AND_PR_HISTORY_V1.md) | 3개 저장소의 PR·이슈 인벤토리 (2026-11-05 기준) |
| [PLEOS_AVD_VERIFICATION](PLEOS_AVD_VERIFICATION.md) · [INTERNAL_VOICEDESIGN_AVD_VALIDATION](INTERNAL_VOICEDESIGN_AVD_VALIDATION.md) · [INTERNAL_VOICEDESIGN_PREVIEW](INTERNAL_VOICEDESIGN_PREVIEW.md) · [STABILITY_REGRESSION_COVERAGE](STABILITY_REGRESSION_COVERAGE.md) | 에뮬레이터 검증 증적과 안정성 점검 범위 |
| [evidence/](evidence/) · [images/](images/) | 캡처 |

### 조사

| 문서 | 내용 |
| --- | --- |
| [RESEARCH_NOTES](RESEARCH_NOTES.md) | 음성 팩 방식, 음성 기반 동작 생성, 라이선스 조사 결과 |

## 읽을 때 지킬 약속

- **확인 수준을 구분한다.** "단위 테스트 통과", "에뮬레이터에서 확인", "실차 확인", "미검증"은 서로 다른 말이다. 에뮬레이터 확인을 실차 지원으로 읽지 않는다.
- **구현자가 직접 확인한 것은 독립 리뷰가 아니다.** 문서가 이렇게 표시한 항목은 별도 검증이 남아 있다.
- **공개 저장소 경계.** 승인된 음성·캐릭터 원본, 참조 음성, 모델 캐시는 이 저장소에 없다. 그 제작물은 비공개 SDK 저장소(`voice-pack-sdk`, `character-pack-sdk`)에 있고, 여기엔 계약·절차·증적만 둔다.
- **갱신 규칙.** 상태·계획 문서(PROJECT_STATUS, ROADMAP)는 현재 상태로 고쳐 쓴다. 결정·작업 기록(DECISIONS, WORK_LOG)은 지우지 않고 아래에 추가한다. 숫자와 날짜에는 기준일을 적는다.
