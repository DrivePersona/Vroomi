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

## 2. Virtual Control 전체 63개 — 개별 상태표

아래 표는 63개 고유 카탈로그 항목을 한 행씩 표시한다. `앱 수신`은 main 코드의 구독 상태이고, `에뮬레이터 확인`은 실제 값 전달 확인이다. `현재 사용`은 TTS 또는 애니메이션 반응에 연결되었는지를 뜻한다.

| # | 영역 | 신호 ID | VC | 앱 수신 | 에뮬레이터 확인 | TTS | 애니메이션 | 현재 사용 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | ADAS | `cruise-control-target-speed` (크루즈 목표 속도) | 화면 | 구독 | ✓ | 시스템 TTS: CRUISE_TARGET_SET (2초 디바운스) | 없음 | TTS만 |
| 2 | ADAS | `cruise-control-state` (크루즈 상태) | 화면 | 구독 | △ | 시스템 TTS: CRUISE_ON / CRUISE_OFF | 없음 | TTS만 |
| 3 | Climate | `hvac-air-cleaner` (공기 청정) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 4 | Climate | `hvac-auto` (공조 자동) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 5 | Climate | `hvac-power` (공조 전원) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 6 | Climate | `hvac-recirc` (내기 순환) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 7 | Climate | `hvac-temperature-set` (설정 온도) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 8 | Climate | `hvac-defroster` (성에 제거) | 화면 | 구독 | 미확인 | 아니오 | 없음 | 상태만 |
| 9 | Climate | `hvac-fan-speed` (송풍 세기) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 10 | Climate | `hvac-ac` (에어컨(A/C)) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 11 | Climate | `hvac-seat-temperature` (좌석 열선·통풍) | 화면 | 구독 | 미확인 | 아니오 | 없음 | 상태만 |
| 12 | Climate | `hvac-dual` (좌우 온도 분리) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 13 | Convenience | `glove-box-door-pos` (글로브박스) | 화면 | 구독 | 미확인 | 캐릭터 클립: DOOR | gesture 정책 | 반응 |
| 14 | Convenience | `door-pos` (도어 열림 상태) | 화면 | 구독 | 미확인 | 캐릭터 클립: DOOR | gesture 정책 | 반응 |
| 15 | Convenience | `door-lock` (도어 잠금) | 화면 | 구독 | 미확인 | 캐릭터 클립: DOOR | gesture 정책 | 반응 |
| 16 | Convenience | `reading-lights-switch` (독서등) | 화면 | 구독 | ✓ | 캐릭터 클립: LIGHT | gesture 정책 | 반응 |
| 17 | Convenience | `turn-signal-state` (방향지시등) | 화면 | 구독 | ✓ | 캐릭터 클립: TURN | gesture 정책 | 반응 |
| 18 | Convenience | `high-beam-lights` (상향등) | 화면 | 구독 | ✓ | 시스템 TTS: HIGH_BEAM_ON (켜질 때) | 없음 | TTS만 |
| 19 | Convenience | `cabin-lights-switch` (실내등) | 화면 | 구독 | ✓ | 캐릭터 클립: LIGHT | gesture 정책 | 반응 |
| 20 | Convenience | `ambient-light-brightness` (앰비언트 밝기) | 화면 | 구독 | ✓ | 캐릭터 클립: LIGHT | gesture 정책 | 반응 |
| 21 | Convenience | `ambient-light-color` (앰비언트 색상) | 화면 | 구독 | ✓ | 캐릭터 클립: LIGHT | gesture 정책 | 반응 |
| 22 | Convenience | `windshield-wipers-switch` (와이퍼) | 화면 | 구독 | 미확인 | 아니오 | 없음 | 상태만 |
| 23 | Convenience | `seat-occupancy` (좌석 착석) | 화면 | 구독 | 미확인 | 캐릭터 클립: COMFORT | gesture 정책 | 반응 |
| 24 | Convenience | `window-position` (창문 위치) | 화면 | 구독 | 미확인 | 캐릭터 클립: WINDOW | gesture 정책 | 반응 |
| 25 | Convenience | `window-lock` (창문 잠금) | 화면 | 구독 | 미확인 | 캐릭터 클립: WINDOW | gesture 정책 | 반응 |
| 26 | Convenience | `trunk-position` (트렁크·프렁크 열림) | 화면 | 구독 | 미확인 | 캐릭터 클립: DOOR | gesture 정책 | 반응 |
| 27 | Convenience | `exterior-lights-mode` (하향등) | 화면 | 구독 | ✓ | 시스템 TTS: LOW_BEAM_ON (켜질 때)/TAIL_LAMP_ON (켜질 때) | 없음 | TTS만 |
| 28 | Energy | `range-remaining` (남은 주행거리) | 화면 | 구독 | ✓ | 시스템 TTS: RANGE_LOW / RANGE_CRITICAL | 없음 | TTS만 |
| 29 | Energy | `ev-battery-level` (배터리 에너지) | 화면 | 구독 | 미확인 | 아니오 | 없음 | 상태만 |
| 30 | Energy | `ev-battery-soc` (배터리 잔량(SOC)) | 화면 | 구독 | 미확인 | 조건부: ENERGY_LOW | 없음 | 조건부 |
| 31 | Energy | `ev-charge-port` (충전구) | 화면 | 구독 | 미확인 | 캐릭터 클립: CHARGE_PORT | gesture 정책 | 반응 |
| 32 | Motion | `gear-selection` (기어) | 화면 | 구독 | ✓ | 시스템 TTS: SHIFT_TO_DRIVE / SHIFT_TO_PARK / SHIFT_TO_REVERSE | 없음 | TTS만 |
| 33 | Motion | `brake-pedal-status` (브레이크 페달) | 화면 | 구독 | ✓ | 캐릭터 클립: BRAKE | gesture 정책 | 반응 |
| 34 | Motion | `steering-angle` (조향각) | 화면 | 구독 | ✓ | 시스템 TTS: STEER_BIG (큰 각도) | 없음 | TTS만 |
| 35 | Motion | `perf-odometer` (주행거리) | 화면 | 구독 | ✓ | 시스템 TTS: ODO_MILESTONE (이정표, 정차 후) | 없음 | TTS만 |
| 36 | Motion | `steering-wheel-heat` (핸들 열선) | 화면 | 구독 | 미확인 | 아니오 | 없음 | 상태만 |
| 37 | Convenience | `ambient-light-switch` (앰비언트 모드) | 숨김 | 구독 | ✗ | 캐릭터 클립: LIGHT | gesture 정책 | 반응 |
| 38 | Convenience | `seat-backrest-position` (등받이 각도) | 숨김 | 가능·미구독 | 미확인 | 아니오 | 없음 | 미사용 |
| 39 | Convenience | `seat-lumbar-position` (요추 위치) | 숨김 | 가능·미구독 | 미확인 | 아니오 | 없음 | 미사용 |
| 40 | Convenience | `seat-height-position` (좌석 높이) | 숨김 | 가능·미구독 | 미확인 | 아니오 | 없음 | 미사용 |
| 41 | Convenience | `seat-fore-aft-position` (좌석 앞뒤 위치) | 숨김 | 가능·미구독 | 미확인 | 아니오 | 없음 | 미사용 |
| 42 | Convenience | `seat-tilt-position` (좌석 틸트) | 숨김 | 가능·미구독 | 미확인 | 아니오 | 없음 | 미사용 |
| 43 | Motion | `trip-driving-time` (트립 주행 시간) | 숨김 | 가능·미구독 | 미확인 | 아니오 | 없음 | 미사용 |
| 44 | Climate | `hvac-fan-direction` (송풍 방향) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 45 | Convenience | `etcs-scenario` (ETCS 시나리오) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 46 | Convenience | `etcs-activated` (ETCS 전원) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 47 | Convenience | `door-lights` (도어 조명) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 48 | Convenience | `hazard-lights` (비상등) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 49 | Convenience | `stop-lights` (제동등) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 50 | Energy | `ev-battery-capacity` (배터리 용량) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 51 | Energy | `ev-charge-percent-limit` (충전 목표 한도) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 52 | Energy | `ev-charge-switch` (충전 스위치) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 53 | Energy | `ev-charge-current-limit` (충전 전류 한도) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 54 | Vehicle | `notification-1` (차량 알림 1) | 화면 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 55 | Convenience | `seat-backrest-move` (등받이 이동 명령) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 56 | Convenience | `escort-lights` (에스코트 조명) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 57 | Convenience | `seat-lumbar-move` (요추 이동 명령) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 58 | Convenience | `seat-height-move` (좌석 높이 이동 명령) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 59 | Convenience | `seat-fore-aft-move` (좌석 앞뒤 이동 명령) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 60 | Convenience | `seat-tilt-move` (좌석 틸트 이동 명령) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 61 | Energy | `power-system-actual` (전원 시스템 상태) | 숨김 | SDK 없음 | 미확인 | 아니오 | 없음 | 미사용 |
| 62 | Safety | `telltales` (계기판 경고등(텔테일)) | 화면 | PRIVATE | 미확인 | 아니오 | 없음 | 미사용 |
| 63 | Misc | `tire-pressure` (타이어 압력) | 숨김 | PRIVATE | 미확인 | 아니오 | 없음 | 미사용 |

별도 SDK 전용 신호(카탈로그에 없는 주행 모드·안전벨트 콜백 등)는 아래 처리표와 `VEHICLE_SIGNALS.md`의 106행 표에서 확인한다.

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
