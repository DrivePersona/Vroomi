---
type: decision
update: append
---

# 0001. 문서는 계층형 지도 + 영역 폴더 + 문서 유형 머리말로 관리한다

- **상태**: 채택
- **날짜**: 2026-11-13
- **결정자**: 사용자

## 배경
`docs/` 한 폴더에 30여 개 문서가 섞여 있었고, 파일명에 `_V1`·`_V2`가 붙었으며, AI가 시작할 진입점(`AGENTS.md`)과 문서의 갱신 성격 표기가 없었다. 요구: ① Top down ② Claude·다른 AI가 쉽게 접근 ③ 이후 개선·유지보수 때 문서를 계속 더하고 정리할 수 있을 것.

## 결정
- 진입점 `AGENTS.md`(루트, 짧게) → `docs/README.md`(지도) → 영역 README → 개별 문서 순으로 내려간다. `CLAUDE.md`는 `AGENTS.md`를 가리키는 한 줄 포인터다.
- 영역 폴더: overview·product·architecture·platform·content·guides·evidence·decisions·log(+필요 시 archive).
- 문서마다 머리말 `type`·`update`를 둔다. 파일명에는 버전을 넣지 않는다.
- 결정은 한 건당 파일 하나(ADR). 기존 결정 표는 `decisions/README.md`에 이력으로 남긴다.
- CI가 상대 링크와 "지도에서 닿는가"를 검사한다.

## 검토한 대안과 이유
| 대안 | 채택하지 않은 이유 |
| --- | --- |
| Diátaxis 4분류만 | 상태·계획·결정 기록 같은 "움직이는 문서"의 자리가 없다. 다만 `guides/`(방법)·`platform/`(레퍼런스)·`architecture/`(설명)에 그 구분을 반영했다 |
| arc42 전체 템플릿 | 12장 구조가 제품·콘텐츠·검증 증적까지 담기엔 무겁다 |
| 지금처럼 평면 + 지도 | 문서가 늘면 지도가 따라가지 못한다 |

## 결과
- 파일 경로가 바뀌어 비공개 SDK 저장소 등 외부에서 옛 경로를 링크한 곳은 깨질 수 있다(저장소 안은 모두 고쳤다).
- 참고: [Diátaxis](https://canonical.com/blog/diataxis-a-new-foundation-for-canonical-documentation), [MADR](https://llm-coding.github.io/Semantic-Anchors/anchor/madr), [점진적 공개](https://sahaavi.github.io/agentic-playbook/reference/patterns/progressive-disclosure.html).
