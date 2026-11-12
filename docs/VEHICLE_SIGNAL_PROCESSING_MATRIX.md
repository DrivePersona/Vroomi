# 차량 신호 처리 매트릭스

이 문서는 Pleos Connect Virtual Control에 보이는 차량 신호, 앱에서 실제로 수신하는 신호, 신호를 이용한 TTS 발화와 캐릭터 애니메이션 처리를 한 표에서 구분한다.

## 1. 숫자를 세는 기준

| 기준 | 개수 | 설명 |
| --- | ---: | --- |
| Virtual Control 카탈로그 항목 | **63** | 화면 카드 48개 + 숨김 카드 15개 |
| Virtual Control 화면 카드 | **48** | 사용자가 Virtual Control에서 조작을 시도할 수 있도록 노출된 고유 카드 ID |
| Virtual Control 숨김 카드 | **15** | 카탈로그에는 있으나 화면에 노출되지 않는 항목 |
| Telltales 하위 경고 | **37종** | `telltales` 한 카드 안에 들어 있는 개별 계기판 경고 |
| main SDK 신호 표 | **106행** | 63개 카탈로그 항목 외에 SDK에만 존재하는 신호까지 포함 |

`Virtual Control 카드가 있다`는 것은 조작 요청을 보낼 수 있다는 뜻이지, VHAL·Pleos 서비스·앱까지 값이 반드시 도착한다는 뜻은 아니다. 수신 확인은 콘솔 → VHAL → Pleos 서비스 → 앱 콜백까지 따로 확인해야 한다.

상태 표기:

- `✓`: 앱에서 값 수신까지 확인
- `△`: 일부 값·상태만 확인
- `코드`: main에 구독 코드가 있으나 이번 에뮬레이터에서 값까지 확인하지 않음
- `✗`: 속성 없음·서비스 거절·서드파티 제한 등으로 수신하지 못함
- `—`: SDK에 대응하는 수신 경로가 없거나 아직 구독하지 않음

## 1-1. Virtual Control 카드 기준 교집합

| 구분 | 고유 카드 수 | 의미 |
| --- | ---: | --- |
| Virtual Control 화면 카드 | **48** | 화면에 노출된 카드 |
| 화면 카드 + main이 수신 중 | **36** | `docs/vehicle-signals.xlsx`에서 `수신 중 (main)`인 고유 카드 |
| 화면 카드 + main 반응 매핑 | **31** | TTS 또는 캐릭터 클립 이벤트가 연결된 카드 |
| 화면 카드 + 상태만 기록 | **5** | 값은 받지만 TTS·gesture 이벤트가 없는 카드 |
| 화면 카드 + SDK 구독 없음 | **11** | Virtual Control은 있으나 Vehicle SDK register 경로가 없는 카드 |
| 화면 카드 + 서드파티 불가 | **1** | `telltales` |

이 표의 `36`은 “앱 코드가 구독 중”이라는 뜻이고, `✓`는 그중 실제 에뮬레이터 값까지 확인했다는 뜻이다. 따라서 `36`과 `✓` 개수를 같은 것으로 보면 안 된다.

## 2. Virtual Control 전체 63개

| 영역 | Virtual Control 화면 카드 | 숨김 카드 | 합계 |
| --- | --- | --- | ---: |
| Vehicle | `notification-1` 차량 알림 | — | 1 |
| Motion | `gear-selection`, `brake-pedal-status`, `perf-odometer`, `steering-angle`, `steering-wheel-heat` | `trip-driving-time` | 6 |
| ADAS | `cruise-control-state`, `cruise-control-target-speed` | — | 2 |
| Convenience | `etcs-activated`, `etcs-scenario`, `door-pos`, `door-lock`, `trunk-position`, `cabin-lights-switch`, `reading-lights-switch`, `ambient-light-brightness`, `ambient-light-color`, `exterior-lights-mode`, `high-beam-lights`, `hazard-lights`, `stop-lights`, `door-lights`, `turn-signal-state`, `window-position`, `window-lock`, `seat-occupancy`, `windshield-wipers-switch`, `glove-box-door-pos` | `ambient-light-switch`, `escort-lights`, `seat-fore-aft-position`, `seat-fore-aft-move`, `seat-backrest-position`, `seat-backrest-move`, `seat-height-position`, `seat-height-move`, `seat-tilt-position`, `seat-tilt-move`, `seat-lumbar-position`, `seat-lumbar-move` | 32 |
| Safety | `telltales` (37종) | — | 1 |
| Energy | `range-remaining`, `ev-battery-level`, `ev-battery-soc`, `ev-battery-capacity`, `ev-charge-current-limit`, `ev-charge-percent-limit`, `ev-charge-switch`, `ev-charge-port` | `power-system-actual` | 9 |
| Climate | `hvac-power`, `hvac-ac`, `hvac-auto`, `hvac-dual`, `hvac-recirc`, `hvac-fan-speed`, `hvac-fan-direction`, `hvac-defroster`, `hvac-temperature-set`, `hvac-seat-temperature`, `hvac-air-cleaner` | — | 11 |
| Misc | — | `tire-pressure` | 1 |
| **합계** | **48개** | **15개** | **63개** |

## 3. 앱 수신·TTS·애니메이션 처리 매트릭스

| 처리 단계 | 신호/ID | Virtual Control | main 앱 수신 | TTS 처리 | 애니메이션 처리 |
| --- | --- | --- | --- | --- | --- |
| 실시간 반응 | `brake-pedal-status` | 화면 카드 | ✓ | `BRAKE` 캐릭터 클립 | 캐릭터 gesture 슬롯 |
| 실시간 반응 | `turn-signal-state` | 화면 카드 | ✓ | `TURN` 캐릭터 클립 | 캐릭터 gesture 슬롯 |
| 실시간 반응 시도 | `driving-mode-state` | 카드 없음 | ✗ (이 AVD에서 서비스 거절) | `MODE` 캐릭터 클립 정책 | gesture 슬롯 정책은 있으나 E2E 미확인 |
| 수동 반응 | `tire-pressure` / `TIRE` | 숨김·PRIVATE | ✗ 자동 구독 불가 | `TIRE` 캐릭터 클립 정책 | gesture 슬롯 정책만 있음 |
| 편의 반응 | 조명 계열: `ambient-light-*`, `cabin-lights-switch`, `reading-lights-switch` | 화면·숨김 혼합 | main 구독 | `LIGHT` 캐릭터 클립 정책 | `LIGHT` gesture 슬롯 정책 |
| 편의 반응 | 도어 계열: `door-pos`, `door-lock`, `trunk-position`, `glove-box-door-pos` | 화면 카드 | main 구독 | `DOOR` 캐릭터 클립 정책 | `DOOR` gesture 슬롯 정책 |
| 편의 반응 | 창문 계열: `window-position`, `window-lock`, 스마트 글라스·블라인드 | 화면·SDK 혼합 | main 구독 또는 capability 대기 | `WINDOW` 캐릭터 클립 정책 | `WINDOW` gesture 슬롯 정책 |
| 편의 반응 | 공조 계열: 전원·A/C·자동·듀얼·순환·팬·설정 온도·공기청정·착석 | 화면 카드 | main 구독 | `COMFORT` 캐릭터 클립 정책 | `COMFORT` gesture 슬롯 정책 |
| 편의 반응 조건부 | `ev-charge-port` | 화면 카드 | main 구독 코드 있음. 에너지 등록 결함으로 E2E 미완료 | `CHARGE_PORT` 캐릭터 클립 정책 | `CHARGE` gesture 슬롯 정책 |
| 기본 음성만 | `high-beam-lights`, `exterior-lights-mode`, 앞·뒤 안개등 | 화면·SDK 혼합 | 상·하향·후미등은 ✓, 안개등은 ✗ | `HIGH_BEAM_ON`, `LOW_BEAM_ON`, `TAIL_LAMP_ON`, `FRONT_FOG_ON`, `REAR_FOG_ON` 시스템 TTS | 없음 |
| 기본 음성만 | `gear-selection` | 화면 카드 | ✓ | `SHIFT_TO_DRIVE`, `SHIFT_TO_PARK`, `SHIFT_TO_REVERSE` 시스템 TTS | 없음. P/D는 별도로 주행 게이트에 사용 |
| 기본 음성만 | `cruise-control-state`, `cruise-control-target-speed` | 화면 카드 | 상태 △, 목표 속도 ✓ | `CRUISE_ON`, `CRUISE_OFF`, `CRUISE_TARGET_SET` 시스템 TTS | 없음 |
| 기본 음성만 | `steering-angle`, `perf-odometer`, `range-remaining` | 화면 카드 | ✓ | `STEER_BIG`, `ODO_MILESTONE`, `RANGE_LOW`, `RANGE_CRITICAL` 시스템 TTS | 없음 |
| 안전 중립 음성 | 안전벨트 경고, `telltales`, 에너지 긴급 상태 | 카드·SDK 혼합 | 안전벨트는 이 AVD에서 ✗, telltales·타이어는 PRIVATE | `WARNING`·`ENERGY_LOW` 중립 음성 | **없음** |
| 상태만 기록 | `windshield-wipers-switch`, `hvac-defroster`, `hvac-seat-temperature`, `steering-wheel-heat`, `ev-battery-level`, EV·연료 전비 계열 | 화면·SDK 혼합 | main 수신 또는 미검증 | 없음 | 없음 |
| 미수신·미처리 | 비상등, 제동등, ETCS, 차량 알림, 좌석 이동 명령, 일부 충전 한도·배터리 용량 | 화면·숨김 | SDK에 대응 register 없음 | 없음 | 없음 |

## 4. TTS와 애니메이션의 현재 정책

main의 캐릭터 클립·gesture 정책 대상은 다음 9개 상위 이벤트다.

`BRAKE`, `TURN`, `MODE`, `TIRE`, `LIGHT`, `WINDOW`, `DOOR`, `COMFORT`, `CHARGE_PORT`

| 처리 결과 | 개수 | 현재 대상 |
| --- | ---: | --- |
| 캐릭터 음성 클립·gesture 정책 | **9개 상위 이벤트** | `BRAKE`, `TURN`, `MODE`, `TIRE`, `LIGHT`, `WINDOW`, `DOOR`, `COMFORT`, `CHARGE_PORT` |
| 기본 시스템 TTS | **15개 이벤트 / 11개 신호** | 등화 5, 기어 3, 크루즈 3, 조향 1, 주행거리 1, 남은 거리 2 |
| 중립 안전 TTS | **2개 정책** | `WARNING`, `ENERGY_LOW` |
| 상태만 기록하는 화면 카드 | **5개** | 와이퍼, 성에 제거, 좌석 열선·통풍, 배터리 에너지, 핸들 열선 |

반면 PR #42·#43에서 추가한 11개 신호의 15개 이벤트(등화·기어·크루즈·조향·주행거리·남은 거리)는 현재 **기본 시스템 TTS만** 사용하며 캐릭터 gesture는 연결하지 않았다. 이 문서에서 “gesture 슬롯 정책”이라고 적은 항목은 앱 정책과 manifest 계약상 가능한 상태이며, 특정 캐릭터 팩의 실제 렌더링 완료를 의미하지 않는다.

안전 계열(`WARNING`, `ENERGY_LOW`, 텔테일)은 중립 음성만 사용하고 캐릭터 동작을 금지한다. 연속값은 매 표본마다 말하지 않고 임계값·상태 구간이 바뀔 때만 반응 후보로 삼는다.

## 5. 근거와 한계

- 전체 상태·SDK·권한·에뮬레이터 확인: [`VEHICLE_SIGNALS.md`](VEHICLE_SIGNALS.md)
- Virtual Control E2E 절차: [`PLEOS_VIRTUAL_CONSOLE_E2E.md`](PLEOS_VIRTUAL_CONSOLE_E2E.md)
- 앱 이벤트·음성 정책: `android/app/src/main/java/com/example/voicecompanion/domain/model/ReactionModels.kt`
- 차량 구독: `android/app/src/main/java/com/example/voicecompanion/data/vehicle/`
- 기준 환경: Pleos Connect v2.1.0 AVD, Vehicle SDK 2.3.2

이 표의 “앱 수신”은 해당 AVD에서 확인한 결과이며 실차 수신을 뜻하지 않는다. 사람 귀로 TTS 음질을 확인한 결과도 아니다. main에 구독 코드가 있어도 차량 이미지가 해당 VHAL 속성을 제공하지 않으면 `✗` 또는 `코드`로 남긴다.
