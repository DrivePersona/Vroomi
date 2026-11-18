---
type: reference
update: rewrite
---

# LivLiv 오버레이: 캐릭터 표시 구조와 현재 상태

**기준일:** 2026-11-18 · 결정 [0002](../decisions/0002-livliv-replaces-glb.md)(GLB 폐기, LivLiv 대체) · 에뮬레이터에서 구현자가 확인한 것이며 독립 리뷰는 아니다.

## 구조

| 부품 | 위치 | 하는 일 |
| --- | --- | --- |
| `livlivpack-runtime` | 비공개 `DrivePersona/livliv-pack-sdk` | 팩 manifest(schema 1) 검증, WebView + 공식 Player 0.5.1로 그리기, `perform(제스처)`·`setScale`·`pause/resume` |
| `livlivpack-content` | 같은 저장소 | 팩 데이터. 지금은 `fox-miko` 하나이고 권리 미확정이라 `internalOnly` |
| `CharacterOverlayHost` | 이 저장소 `ui/overlay/` | 오버레이 창에 LivLiv 뷰를 올리고 탭·500ms 길게 누름·끌기를 직접 판정한다. 반응 엔진이 고른 몸짓 이름을 `perform`으로 넘긴다 |
| 메뉴·시트 | 같은 폴더 | 길게 누르면 플로팅 메뉴, 설정 시트에 **캐릭터 크기**(1~5배, 기본 3배) |

- 켜는 방법(내부 opt-in): 앱 설정 `livliv.map.<characterId>`에 LivLiv 팩 id를 넣는다. 지금은 이 설정이 있는 캐릭터만 LivLiv로 그리고, 없으면 기존 GLB 경로가 남아 있다(결정 0002의 대체 대상).
- 숨길 때는 `pause`, 다시 보일 때는 `resume`(약 0.7초)을 쓴다. 뷰를 새로 만들면 첫 프레임까지 5~8초가 걸리고 `destroy`가 ANR을 낸 적이 있어 오버레이를 완전히 내릴 때만 `release`한다.
- 오버레이 창 크기는 캐릭터 몸 240×250dp × 배율이고 안전 영역(시스템 바 안쪽, 계기판 제외)에 맞게 줄어든다. 저장된 위치가 없으면 몸 중심이 가로 47.8%·세로 44.3%인 곳에 뜬다.

## 신호·터치 → 몸짓

팩의 `gestures`가 몸짓 이름을 LivLiv 동작·표정으로 바꾼다. fox-miko의 이름은 기존 여우 GLB의 이름과 같게 맞춰 두었다.

| 입력 | 몸짓 이름 | LivLiv 동작 |
| --- | --- | --- |
| 브레이크 | `brake_l1~3`, `brake_react` | `hop` |
| 방향지시등 | `turn_ack`, `turn_react` | `tilt` |
| 드라이브 모드 | `mode_shift`, `mode_react` | `nod` |
| 타이어 | `comfort_react`, `tire_react` | `nod` + 표정 `sad` |
| 터치 | `touch_shy_1~3`, `touch_react` | `shy` + 표정 `shy` |

## 확인한 것과 못 한 것

- **확인**: Pleos 에뮬레이터에서 지도 위 투명 합성, 탭 → TOUCH 라우팅, 길게 누름 메뉴, 크기 3배 → 4배 → 3배와 저장, 가상 콘솔 브레이크 → 로그 `몸짓 시작 BRAKE → brake_l1`, 기어 D에서 제거·P에서 복귀. 녹화: [데모 영상](../evidence/videos/pleos-overlay-demo-2026-11-18.mp4).
- **미확인**: 몸짓이 화면에서 실제로 움직이는 모습(로그와 정지 화면만 봄), 크기 1배·5배 극단, fps·메모리, 실차 WebView, 사람 눈 판정, 재시작 뒤 크기 유지(저장값만 확인).
- **알려진 문제**: 크기를 바꾸면 설정 시트가 움직여 같은 자리를 연달아 누르면 두 번째 탭이 빗나갈 수 있다.

## 남은 일 (GLB 대체를 끝내려면)

1. main이 고정한 캐릭터 팩 0.3.4에는 여우의 브레이크·터치 몸짓 연결이 없다. LivLiv 쪽 이름으로 신호 → 몸짓 연결을 새 기준으로 다시 확정한다.
2. 몸짓이 동작 5종과 표정 수준이다. 대사 줄별·캐릭터별 연기는 LivLiv 방식으로 새로 설계한다(GLB 12클립 규격은 옮겨지지 않음).
3. 캐릭터 선택·로스터가 아직 GLB 팩 기준이다. LivLiv 팩 기준 게이트(권리·13슬롯·몸짓)로 재정의하고 기존 GLB 경로를 제거한다.
4. 33캐릭터의 LivLiv 원화·레이어 제작과 권리 확인. 원화 권리 확인 전에는 내부 검수용이다.
5. 립싱크(`setMouthOpen`) 연결, 팩 릴리스(`repository_dispatch` 봇 PR) 연결.
