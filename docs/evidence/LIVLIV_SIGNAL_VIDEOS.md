---
type: evidence
update: snapshot
---

# LivLiv 캐릭터 신호별 동작 영상 (2026-11-18)

Pleos AVD(internalDebug, 로컬 SDK 후보: livlivpack `0.1.0-candidate.8`, characterpack `0.3.6`)에서 `adb screenrecord`(1280×720, 약 42초)로 찍었다. 신호는 가상 콘솔로 넣었다: 브레이크 70→0(약 2.5초), 방향지시등 BlinkLeft(약 12초), 그 뒤 오버레이 탭 3회(약 22·26·30초).

| 캐릭터 | 영상 | 로그로 확인한 몸짓 |
| --- | --- | --- |
| 여우 무녀 | [fox](videos/livliv-signals-fox-2026-11-18.mp4) | BRAKE → `brake_l1`, TURN → `turn_ack`, TOUCH → `touch_shy_1·2·3` |
| 차 안 버튜버 | [vtuber](videos/livliv-signals-vtuber-2026-11-18.mp4) | BRAKE → `brake_react`, TURN → `turn_react`, TOUCH → `touch_react`(음성 클립이 없어 소리 없이 몸짓만) |
| 서리 | [seori](videos/livliv-signals-seori-2026-11-18.mp4) | BRAKE → `brake_react`, TURN → `turn_react`, TOUCH → `touch_shy_1·2·3`(음성 팩 gestureId `touch_1·3·4`) |

## 확인한 것

- 세 캐릭터 모두 신호마다 앱 로그에 `몸짓 시작`이 찍히고, 영상 프레임에서 브레이크 시점에 자세가 움직이는 것을 확인했다(OpenCV로 캐릭터 영역 프레임 차이를 재고 대표 프레임을 눈으로 봤다).
- 버튜버·서리는 캐릭터 팩 SDK가 GLB 클립만 몸짓으로 인정해서 이전에는 `몸짓 없음`이었다. `livlivPackId`(character-pack-sdk 변경)로 고쳤다.
- 오버레이의 캐릭터 바꾸기(여우 무녀 ↔ 버튜버 ↔ 서리)가 LivLiv끼리 바뀔 때 이전 캐릭터가 남던 문제를 고쳤고, 메뉴로 세 방향 교체를 확인했다.

## 확인하지 못한 것

- 방향지시등을 켜면 Pleos가 측면 카메라 화면을 띄워 오버레이를 가려서, TURN 몸짓은 영상에서 보이지 않는다(로그로만 확인).
- MODE·TIRE는 가상 콘솔에 값을 넣는 길이 없어 영상이 없다.
- 몸짓의 자연스러움·보기 좋음은 사람 눈 판정 전이다. 서리의 터치 몸짓은 `touch_shy` 계열(부끄러움)이라 서리의 설계 연기와 다르다. 버튜버는 음성 팩에 터치 클립이 없다.
- 소리는 확인하지 못했다(로그의 '재생됨'만 봤다). 실차 미확인.
