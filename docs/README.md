---
type: index
update: rewrite
---

# 문서 지도

Vroomi(DrivePersona Pleos Voice Companion) 문서의 시작점이다. 위에서 아래로 읽는다: [AGENTS.md](../AGENTS.md) → **이 지도** → 영역 README → 개별 문서.

## 영역

| 영역 | 무엇을 찾나 | 성격 |
| --- | --- | --- |
| [overview/](overview/README.md) | 현재 상태, 로드맵, 열린 결정, 발표 자료 | 갱신형 |
| [product/](product/README.md) | 제품 요구사항, 사용자 흐름, 와이어프레임, 목업 | 명세 |
| [architecture/](architecture/README.md) | 캐릭터↔음성 연결, 음성·몸짓 동기 등 앱 계약 | 명세 |
| [platform/](platform/README.md) | Pleos SDK·권한·TTS 실측, 차량 신호 표 | 레퍼런스 |
| [content/](content/README.md) | 33캐릭터·13슬롯 릴리스 계획, 대사, 연기, 팩 게시 기록 | 명세·기록 |
| [guides/](guides/README.md) | 에뮬레이터 E2E 등 따라 하는 절차 | 가이드 |
| [evidence/](evidence/README.md) | AVD 검증·안정성 점검 증적과 캡처 | 스냅샷 |
| [decisions/](decisions/README.md) | 결정 기록(ADR). 이유와 대안 | 추가형 |
| [log/](log/README.md) | 작업 로그, 조사, PR 이력 | 추가형 |

## 읽는 순서

| 누구 | 순서 |
| --- | --- |
| 프로젝트를 처음 보는 사람 | [PROJECT_STATUS](overview/PROJECT_STATUS.md) → [PRODUCT_REQUIREMENTS](product/PRODUCT_REQUIREMENTS.md) → [USER_FLOWS](product/USER_FLOWS.md) → [ROADMAP](overview/ROADMAP.md) |
| 이어서 개발할 사람 | [PROJECT_STATUS](overview/PROJECT_STATUS.md) → [ROADMAP](overview/ROADMAP.md) → [PLEOS_PLATFORM_FINDINGS](platform/PLEOS_PLATFORM_FINDINGS.md) → [VEHICLE_SIGNALS](platform/VEHICLE_SIGNALS.md) → [PLEOS_VIRTUAL_CONSOLE_E2E](guides/PLEOS_VIRTUAL_CONSOLE_E2E.md) |
| 캐릭터·음성 제작 담당 | [RELEASE_VOICE_CHARACTER_PLAN](content/RELEASE_VOICE_CHARACTER_PLAN.md) → [CHARACTER_VOICE_BINDING](architecture/CHARACTER_VOICE_BINDING.md) → [ANIMATION_AND_ACTING](content/ANIMATION_AND_ACTING.md) |
| Pleos 협의·심사 담당 | [PLEOS_PLATFORM_FINDINGS](platform/PLEOS_PLATFORM_FINDINGS.md)의 "Pleos에 확인할 것" → [ROADMAP](overview/ROADMAP.md)의 열린 결정 |
| 지난 결정의 이유가 궁금한 사람 | [decisions/](decisions/README.md) → [WORK_LOG](log/WORK_LOG.md) → [RESEARCH_NOTES](log/RESEARCH_NOTES.md) |

## 문서 규칙

모든 문서는 맨 위에 머리말을 둔다.

```yaml
---
type: status | spec | reference | guide | evidence | log | research | decision | index
update: rewrite | append | snapshot
---
```

| `update` | 뜻 | 고치는 법 |
| --- | --- | --- |
| `rewrite` | 갱신형 | 현재 상태로 고쳐 쓴다 |
| `append` | 추가형 | 지우지 않고 아래에 더한다. 바뀐 결정은 "대체됨"으로 표시 |
| `snapshot` | 그 시점의 기록 | 고치지 않는다. 새 상황은 새 문서로 쓰고 낡은 것은 `archive/`로 옮긴다 |

- **새 문서**: 알맞은 영역 폴더에 두고 그 영역 README에 한 줄을 더한다. 이 지도에서 링크로 닿지 않는 문서는 CI(`node tools/check-doc-links.js`)가 막는다.
- **이름**: 파일명에 `_V1`·`_V2`를 붙이지 않는다. 버전·기준일은 본문에 적는다.
- **낡은 문서**: 지우지 않고 `docs/archive/`로 옮기며(영역 README가 아니라 이 지도에 `archive/` 항목을 더한다) 머리말에 대체 문서를 적는다.
- **결정**: [템플릿](decisions/TEMPLATE.md)으로 한 건당 한 파일.

## 읽을 때 지킬 약속

- **확인 수준을 구분한다.** "단위 테스트 통과", "에뮬레이터에서 확인", "실차 확인", "미검증"은 서로 다른 말이다. 에뮬레이터 확인을 실차 지원으로 읽지 않는다.
- **구현자가 직접 확인한 것은 독립 리뷰가 아니다.** 문서가 이렇게 표시한 항목은 별도 검증이 남아 있다.
- **공개 저장소 경계.** 승인된 음성·캐릭터 원본, 참조 음성, 모델 캐시는 이 저장소에 없다. 그 제작물은 비공개 SDK 저장소(`voice-pack-sdk`, `character-pack-sdk`)에 있고, 여기엔 계약·절차·증적만 둔다.
