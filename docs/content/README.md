---
type: index
update: rewrite
---

# 콘텐츠: 캐릭터·음성·연기

캐릭터·음성 제작과 릴리스 기준. 승인된 원본 자산은 비공개 SDK 저장소에 있다.

| 문서 | 내용 |
| --- | --- |
| [RELEASE_VOICE_CHARACTER_PLAN](RELEASE_VOICE_CHARACTER_PLAN.md) | 33캐릭터 목표와 릴리스 차단 요소 |
| [RELEASE_ROSTER](RELEASE_ROSTER.md) | 릴리스 허용 목록과 게이트 |
| [CHARACTER_TARGET_INVENTORY.csv](CHARACTER_TARGET_INVENTORY.csv) | 33개 ID 쌍 |
| [DIALOGUE_MATRIX](DIALOGUE_MATRIX.md) · [csv](DIALOGUE_MATRIX.csv) | 대사 슬롯 초안 |
| [FOX_SPIRIT_VOICE_SLOT_REQUEST](FOX_SPIRIT_VOICE_SLOT_REQUEST.md) · [csv](FOX_SPIRIT_VOICE_SLOTS.csv) | 음성 팩 SDK 요청: 여우 신령 빠진 대사 슬롯 8개와 생성할 문구 목록 |
| [ANIMATION_AND_ACTING](ANIMATION_AND_ACTING.md) | 연기·애니메이션 제작 방법과 교훈 |
| [PACK_RELEASES](PACK_RELEASES.md) | 비공개 팩 게시 기록(워크플로우가 자동 추가) |

## 대사가 있는 곳 (기준일 2026-11-15)

| 무엇 | 위치 | 공개 | 상태 |
| --- | --- | --- | --- |
| 33캐릭터 × 13슬롯 문구 정본 | `voice-pack-sdk` `docs/RELEASE_DIALOGUE_MATRIX_V1.json` | 비공개 | 33팩 모두 13슬롯 문구가 있다(텍스트 버전 1.0.0). 청취·권리 승인 전 |
| 음성 제작 작업 목록 | `voice-pack-sdk` `docs/RELEASE_AUDIO_WORKLIST_V1.csv` | 비공개 | 아직 만들지 않은 클립의 문구·음색 지시 |
| 후보 10명 × 13슬롯 초안 | [DIALOGUE_MATRIX.csv](DIALOGUE_MATRIX.csv) | 공개 | 130행: DRAFT 107 · PACK_READY 18 · AUDIO_GENERATED 5. 승인된 문구가 아니다. 정본은 위 비공개 매트릭스 |
| 여우 신령 전체 대사 | [FOX_SPIRIT_VOICE_SLOTS.csv](FOX_SPIRIT_VOICE_SLOTS.csv) | 공개 | 33클립: EXISTING 13 · PACKAGED_0.7.0 20. 13슬롯 모두 팩에 있다 |
| 신호 11개의 한 줄 대사 15줄 | [VEHICLE_SIGNALS](../platform/VEHICLE_SIGNALS.md) | 공개 | **기본 음성(Pleos TTS)이 읽는다.** 캐릭터 목소리가 아니다 |
| 여우 신령 신호별 대사·몸짓 후보 | `character-pack-sdk` `docs/VEHICLE_SIGNAL_PERFORMANCE_CATALOG.md` | 비공개 | 신호 13개의 후보. 음성 팩에는 아직 없다 |
| 신호별 실제 발화 기록 | [FOX_UTTERANCE_CHECK](../evidence/FOX_UTTERANCE_CHECK.md) · [VIRTUAL_CONSOLE_SIGNAL_SWEEP](../evidence/VIRTUAL_CONSOLE_SIGNAL_SWEEP.md) | 공개 | 에뮬레이터 로그 기준. 소리를 귀로 확인한 것이 아니다 |

### 비어 있는 곳

- **신호 11개의 캐릭터 대사.** 캐릭터 목소리로 읽을 대사는 여우 신령의 후보 13개뿐이다. 안전 등급인 크루즈 해제·주행 가능 거리 부족의 캐릭터 대사는 없고, 나머지 32개 캐릭터는 이 신호들의 대사가 없다. 15줄 × 33캐릭터 = 최소 495줄(변형 제외, 단순 계산).
- **안전 경고 문구의 기준이 갈려 있다.** 저장소 정책과 공개 매트릭스는 중립 공통 문장, 비공개 매트릭스와 게시된 여우 팩 0.7.0은 캐릭터 말투, 앱 기본 문구는 또 다르다. 결정은 [ROADMAP D11](../overview/ROADMAP.md).
- **청취.** 문서에 기록된 사람 청취는 2026-11-14 여우 새 클립 18개뿐이다. 안전 클립 2개는 길이·에너지만 확인했고, 다른 캐릭터는 청취·권리 승인 기록이 없다.
