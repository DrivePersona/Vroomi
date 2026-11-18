---
type: decision
update: append
---

# 0002. 캐릭터 표시 방식을 GLB에서 LivLiv(2.5D)로 대체한다

- **상태**: 채택
- **날짜**: 2026-11-18
- **결정자**: 사용자

## 배경
- 지금까지 캐릭터는 `character-pack-sdk`의 **GLB(3D)** 를 Filament로 그렸다. 에뮬레이터는 Meshy GLB를 그리지 못하고 2D 미리보기만 보였고, 몸짓은 신호당 클립 1개로 제한됐으며, 출시 등급 GLB 검수가 막혀 있었다(32/33이 상업 이용 미승인).
- 2026-11-16~18에 **LivLiv**(`.livliv` 형식 4, 공식 Player를 Android WebView로 구동)를 오버레이에 연결해 Pleos 에뮬레이터에서 확인했다: 지도 위에 배경 없이 캐릭터만 합성, 탭·길게 누름·끌기, 신호 → 몸짓 이름 호출, 오버레이 크기 조절(PR #70). 비공개 `livliv-pack-sdk`가 팩 검증과 표시를 맡는다.
- 확인 수준: 에뮬레이터, 구현자 본인 확인. 몸짓이 화면에서 실제로 움직이는지, 실차 WebView, 사람 눈 판정은 미확인.

## 결정
- **GLB 캐릭터 팩 방식은 폐기하고 LivLiv로 대체한다.** 앞으로의 캐릭터 제작·표시·몸짓 작업은 LivLiv 기준으로만 한다.
- 이 결정은 방향이다. 코드와 `character-pack-sdk`는 이 결정으로 지우지 않았고, 대체가 끝나는 순서는 [LIVLIV_OVERLAY](../architecture/LIVLIV_OVERLAY.md)의 남은 일에 둔다.
- 음성 팩·반응 엔진·신호 수신·오버레이 제스처·안전 규칙(주행 중 숨김, 안전 경고 몸짓 없음)은 바뀌지 않는다.

## 검토한 대안과 이유
| 대안 | 장점 | 단점 |
| --- | --- | --- |
| GLB 유지 | 이미 만든 코드·클립(여우 12클립)이 있다 | 에뮬레이터에서 3D가 안 그려져 확인이 막힘, 클립 제작·검수 비용이 큼, 출시 등급 검수가 길다 |
| 병행 | 이전 자산을 버리지 않는다 | 렌더러·검증·팩 두 벌을 유지해야 하고 로스터 게이트가 둘이 된다 |
| **LivLiv로 대체 (채택)** | 일러스트에서 레이어를 직접 만들어 제작이 가볍고, WebView라 에뮬레이터에서 그려진다 | 몸짓이 기본 동작 5종과 표정 수준, 실차 WebView 성능·메모리 미확인, 모델 공개는 원화 권리에 묶임 |

## 결과
- 로드맵 1번(출시 등급 GLB 검수)과 [RELEASE_ROSTER](../content/RELEASE_ROSTER.md)의 GLB 기반 게이트는 LivLiv 팩 기준으로 **다시 정의해야 한다**(미정).
- 여우 12클립 연기 규격([ANIMATION_AND_ACTING](../content/ANIMATION_AND_ACTING.md))은 LivLiv로 직접 옮겨지지 않는다. LivLiv는 동작 5종(`nod`·`shake`·`tilt`·`hop`·`shy`)과 표정을 조합한다.
- 원화(여우 미코)의 권리는 Meshy 7.1 약관 확인 전이라 LivLiv 팩도 내부 검수용이다. 33캐릭터 전부의 LivLiv 원화는 아직 없다.
- 열린 일과 현재 상태는 [LIVLIV_OVERLAY](../architecture/LIVLIV_OVERLAY.md).
