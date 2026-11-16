---
type: spec
update: rewrite
---

# LivLiv(.livliv) 오버레이 스파이크

2.5D 캐릭터 재생기 [LivLiv Player](https://github.com/livliv-dev/player)(Apache-2.0, 0.5.1)로 `.livliv` 캐릭터를 Pleos 오버레이에서 움직일 수 있는지 본 **디버그 전용 스파이크**다. 제품 경로(캐릭터 팩·음성 팩)에는 연결하지 않았다. 확인 수준은 **Pleos AVD(에뮬레이터)** 이며 구현자가 직접 한 것이라 독립 리뷰가 아니다. 실차·사람 눈과 귀 판정은 없다.

## 구조
```
오버레이 창(TYPE_APPLICATION_OVERLAY, 320×430dp, 투명)
 ├ WebView — livliv-spike/index.html: 공식 Player + mapping.js
 └ 투명 터치 View — OverlayDrag로 탭·끌기
```
- 코드: `android/app/src/debug/` (`LivLivOverlayProbe`, `LivLivProbeReceiver`, 에셋 `livliv-spike/`). 릴리스 빌드에는 들어가지 않는다.
- 제어는 Activity가 아니라 브로드캐스트로 한다(Pleos는 task가 지워지면 프로세스를 죽인다).
  `adb shell am broadcast --user 10 -n <패키지>/com.example.voicecompanion.ui.LivLivProbeReceiver --es overlay show --es persona seori|gomji|pin` 이후 `--es overlay event --es name BRAKE|TURN|MODE|TIRE|TOUCH|WARNING`, `report`, `park`, `unpark`.
- 립싱크는 시험용 WebAudio(클립 소리 크기 → `mouthOpen`)다. 제품에서는 네이티브 `AudioTrack` 재생에서 만든 포락선을 넘기는 구조를 상정한다.

## 확정한 이벤트 → 동작 매핑
정본은 `android/app/src/debug/assets/livliv-spike/mapping.js`다. 반응은 **음성이 시작된 뒤** 지연만큼 기다렸다가 시작하고, 클립이 끝나면(`done`) 표정·시선·idle을 기본으로 되돌린다. `act()`는 LivLiv가 제공하는 `nod·shake·tilt·hop·shy` 5종, 표정은 `smile·shy·angry·sad` 4종뿐이다. 크기(amp)·지연은 **임시값**이고 아래 선택이 확정된 부분이다(사용자 결정 2026-11-16).

| 이벤트 | 서리 (amp 0.6, 300ms 늦게) | 곰지 (amp 1.4) | 핀 (amp 1.0) |
| --- | --- | --- | --- |
| BRAKE | nod | hop | nod 후 작은 hop |
| TURN | tilt | tilt | tilt 후 작은 hop |
| MODE | nod | nod 두 번 | nod |
| TIRE | sad + nod | sad + shake | sad + nod |
| TOUCH 1줄(약) | shy 작게 | shy + shy | shy 작게 |
| TOUCH 3줄(중) | shy 표정 + idle 정지 | shy + hop | shy + 작은 hop |
| TOUCH 4줄(강) | 정면 응시 + idle 정지 | angry + shake | angry + idle 정지 |
| WARNING | 몸짓 없음(중립 음성) | 몸짓 없음 | 몸짓 없음 |

이벤트마다 줄을 순환한다(BRAKE·TURN·MODE·TIRE 1→2→3, TOUCH 1→3→4). 줄별로 다른 동작은 없다(LivLiv는 줄마다 저작 클립을 두는 구조가 아니라 강도·지연·표정 조합으로만 달라진다).

## Pleos AVD 확인 결과
- 지도 앱 위에 배경 사각형 없이 캐릭터만 합성된다. 탭과 끌기가 된다.
- 서리·곰지·핀 각각 7개 시나리오(이벤트 4 + TOUCH 3줄)에서 클립 재생 시작 → 지연 뒤 동작 시작 → 클립 종료 → 기본 자세 복귀(`idle=true`)가 모두 돌았고, WARNING은 동작·재생이 없었다. 머리 각도 범위로 크기 차이가 확인됐다(예: 서리 BRAKE y −4.8~1.8°, 곰지 BRAKE x −13~−1°, 곰지 TIRE의 shake x −11.8~14.2°). 매 클립에서 `mouthOpen`이 최대 1.0까지 올랐다.
- 표시(WebView 로드까지) 약 5~12초(새 프로세스, 부하에 따라 다름). 창을 숨겼다 되살리는 `park`/`unpark`는 0.7초에 첫 프레임이 나왔다. 반면 `removeViewImmediate` + `WebView.destroy()`로 버리는 경로는 메인 스레드를 막아 ANR이 났다(원인 미분리) → WebView는 버리지 않고 일시정지하는 방식을 상정한다.
- 숨김 10초 뒤 Pleos가 프로세스를 얼린다(`am_freeze`). 화면 없는 상태에서의 애니메이션은 서비스 호스팅 없이는 이어지지 않는다.
- 메모리(이전 측정): 앱 PSS 약 129MB + WebView 샌드박스 프로세스 RSS 약 229MB + `webview_service` 약 98MB.

## 라이선스와 공개 저장소 경계
- Player 코드는 Apache-2.0이다(`player/LICENSE`·`NOTICE` 동봉).
- **샘플 캐릭터(Featured Characters License 1.1)는 이 저장소로 재배포할 수 없고, 회사·조직의 상업 이용에는 사전 동의가 필요하다.** 그래서 캐릭터 파일은 커밋하지 않는다. `node tools/fetch-livliv-samples.cjs`가 `d01·d02·q03`과 라이선스를 받아 `android/app/src/debug/assets/livliv-spike/characters/`(`.gitignore`)에 둔다. 캐릭터 이미지에는 LivLiv 로고 워터마크가 캔버스에 그려지며 끌 수 없다.
- 시험용 음성 클립(`clips/<persona>/*.wav`)은 비공개 파이프라인 산출물이라 저장소에 없다(`.gitignore`). 클립이 없으면 페이지가 `clip-error`를 로그로 남기고 1.6초 뒤 동작만 되돌린다.
- 이 스파이크의 샘플 외형은 시안이며 제품 캐릭터가 아니다. 서리·곰지·핀의 `.livliv` 확보 경로(제작 방식·약관·워터마크 제거 조건)는 확인하지 못했다.

## 열린 것
- 앱 manifest 형식(동작·표정·강도·지연·유지 조건을 담는 객체), 캐릭터 팩 형식 `livliv`(이미지가 많아 `.livliv` ZIP 한 파일만 받는 전제), 서비스 호스팅.
- 길게 누름 제스처, 투명 합성의 다른 GPU·실차 동작, 사람 눈·귀로 본 자연스러움과 립싱크 어긋남.
- 표 안 크기·지연 숫자는 임시값이다.
