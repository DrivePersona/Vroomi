# AGENTS.md — Vroomi (DrivePersona)

차량 상태(브레이크·방향지시등·드라이브 모드 등)를 사용자가 고른 캐릭터의 목소리와 몸짓으로 알려 주는 Pleos 차량 앱. 이 파일은 사람과 AI(Claude·Codex 등)가 가장 먼저 읽는 진입점이다. 세부는 아래 링크를 따라 내려간다.

## 읽는 순서 (위에서 아래로)
1. 이 파일 → 2. [docs/README.md](docs/README.md) (전체 지도) → 3. 작업 영역의 `docs/<영역>/README.md` → 4. 개별 문서.
처음이면 [docs/overview/PROJECT_STATUS.md](docs/overview/PROJECT_STATUS.md)부터. 이어서 개발하면 [docs/overview/ROADMAP.md](docs/overview/ROADMAP.md).

## 어디에 무엇이 있나
| 알고 싶은 것 | 위치 |
| --- | --- |
| 지금 상태·계획·열린 결정 | `docs/overview/` |
| 제품 요구·사용자 흐름·화면 | `docs/product/` |
| 앱 구조 계약 (캐릭터↔음성 연결, 음성·몸짓 동기) | `docs/architecture/`, [android/ARCHITECTURE.md](android/ARCHITECTURE.md), [android/DESIGN_SYSTEM.md](android/DESIGN_SYSTEM.md) |
| Pleos SDK 실측·권한·차량 신호 | `docs/platform/` |
| 캐릭터·음성 콘텐츠 계획·릴리스 | `docs/content/` |
| 따라 하는 절차 (에뮬레이터 E2E 등) | `docs/guides/` |
| 검증 증적(스냅샷) | `docs/evidence/` |
| 결정 기록 (ADR) | `docs/decisions/` |
| 작업 로그·조사·PR 이력 | `docs/log/` |

## 지킬 규칙
- **확인 수준을 구분한다**: 단위 테스트 통과 / 에뮬레이터 확인 / 실차 확인 / 미검증은 다른 말이다. 에뮬레이터 확인을 실차 지원으로 쓰지 않는다. 구현자가 직접 한 확인은 독립 리뷰가 아니다.
- **공개 저장소 경계**: 승인된 음성·캐릭터 원본, 참조 음성, 모델 캐시는 이 저장소에 없다(`voice-pack-sdk`·`character-pack-sdk`는 비공개). 여기엔 계약·절차·증적만 둔다.
- **엔진 순수성**: `engine.js`·`ReactionEngine.kt`에 DOM·Android·SDK를 넣지 않는다. 규칙을 바꾸면 `engine.test.js`와 `ReactionEngineTest.kt`에 케이스를 먼저 더한다.
- **Pleos 권한**: 실제 쓰는 API의 것만 선언한다. 미승인 권한은 심사 탈락 사유다.
- 기존 캐릭터·성우 목소리·대사를 코드나 자산에 넣지 않는다. 안전 경고는 중립 음성, 캐릭터 몸짓 없음.

## 빌드·검증
- 웹 시뮬레이터 테스트: `node --test` (루트, 의존성 없음)
- 문서 링크 검사: `node tools/check-doc-links.js` (CI가 실행한다)
- Android: `android/`에서 Gradle. 비공개 패키지 해석 방법은 [README.md](README.md).

## 문서를 고치거나 더할 때 (유지 규칙)
1. 문서 맨 위에 머리말을 둔다: `type`(status·spec·reference·guide·evidence·log·research·decision)과 `update`(rewrite·append·snapshot). 규칙은 [docs/README.md](docs/README.md)의 "문서 규칙".
2. 새 문서는 알맞은 영역 폴더에 두고 **그 영역의 README에 한 줄을 추가**한다. 지도에 안 닿는 문서는 CI가 막는다.
3. 파일 이름에 `_V1`·`_V2`를 붙이지 않는다. 버전·기준일은 문서 안에 적는다. 낡은 문서는 지우지 않고 `docs/archive/`로 옮기고 머리말에 대체 문서를 적는다.
4. 결정은 `docs/decisions/`에 [템플릿](docs/decisions/TEMPLATE.md)으로 한 건당 한 파일(`NNNN-제목.md`)을 더하고 인덱스(`docs/decisions/README.md`)에 한 줄 추가한다. 바뀐 결정은 지우지 않고 "대체됨"으로 표시한다.
5. 상태·계획(`overview/`)은 현재 상태로 고쳐 쓴다. 로그·결정은 아래에 추가만 한다. 숫자·날짜에는 기준일을 적는다.
