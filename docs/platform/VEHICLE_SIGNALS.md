---
type: reference
update: rewrite
---

# 차량 신호 현황 (수신 가능한 신호)

> 기준: `origin/main` e786dd0(PR #42 머지 뒤)의 구독 코드 + 이 문서를 갱신한 PR의 코드, Pleos Connect v2.1.0 에뮬레이터(Virtual Console 63개 카탈로그), SDK `ai.pleos.playground:Vehicle:2.3.2`. 같은 내용의 필터 가능한 표는 [`vehicle-signals.xlsx`](vehicle-signals.xlsx)이다.

## 요약

| 수신 상태 | 개수 |
| --- | --- |
| 수신 중 (main) | 56 |
| 수신 가능·미구독 | 29 |
| 권한 미선언 | 1 |
| SDK 구독 없음 | 18 |
| 서드파티 불가 | 2 |
| 합계 | 106 |

- **수신 중 (main)**: `main`이 구독하는 신호. 외부등·주행 신호 11개(PR #42)는 값을 기록하고, 아래 "대사를 붙인 신호"의 규칙에 맞으면 한 줄 대사를 기본 음성으로 말한다.
- 새 권한은 추가하지 않았다. 대사를 붙인 신호는 이미 선언된 `CAR_LIGHTS`·`CAR_DRIVE`만 쓴다.

## 대사를 붙인 신호 (11개, 대사 15줄)

| 신호 | 이벤트 | 말하는 때 | 대사 | 쿨다운 | 말하기 확인 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 상향등 | HIGH_BEAM_ON | 꺼짐·주간주행등 → 켜짐 | 상향등이 켜졌어요. | 30초 | ✓ | Pleos 에뮬레이터(2026-11-11, 콘솔 조작): 반응 기록에 말하기 요청 · 재생됨 |
| 하향등 | LOW_BEAM_ON | 꺼짐·주간주행등 → 켜짐 | 하향등이 켜졌어요. | 30초 | ✓ | 후미등과 같은 순간에 켜지면 하향등만 말함 |
| 후미등 | TAIL_LAMP_ON | 꺼짐·주간주행등 → 켜짐 | 후미등이 켜졌어요. | 30초 | ✓ | 우선순위가 한 단계 낮아 하향등과 같은 순간이면 밀림 |
| 앞 안개등 | FRONT_FOG_ON | 꺼짐·주간주행등 → 켜짐 | 앞 안개등이 켜졌어요. | 30초 | ✗ | 에뮬레이터 VHAL에 속성이 없어 신호·말하기 모두 확인할 수 없음 |
| 뒤 안개등 | REAR_FOG_ON | 꺼짐·주간주행등 → 켜짐 | 뒤 안개등이 켜졌어요. | 30초 | ✗ | 에뮬레이터 VHAL에 속성이 없어 신호·말하기 모두 확인할 수 없음 |
| 기어 | SHIFT_TO_DRIVE | D로 바뀔 때 | 출발해요. | 30초 | ✓ | Pleos 에뮬레이터(2026-11-11, 콘솔 조작): 반응 기록에 말하기 요청 · 재생됨 |
| 기어 | SHIFT_TO_PARK | P로 바뀔 때 | 도착했어요. | 30초 | ✓ | Pleos 에뮬레이터(2026-11-11, 콘솔 조작): 반응 기록에 말하기 요청 · 재생됨 |
| 기어 | SHIFT_TO_REVERSE | R로 바뀔 때 | 후진 중이에요. 뒤를 살펴 주세요. | 30초 | ✓ | 중립(N)·모름·수동 단수는 말하지 않음 |
| 크루즈 상태 | CRUISE_ON | ENABLED·OTHER → ACTIVATED | 크루즈가 켜졌어요. | 10초 | ✓ | Pleos 에뮬레이터(2026-11-11, 콘솔 조작): 반응 기록에 말하기 요청 · 재생됨 |
| 크루즈 상태 | CRUISE_OFF | ACTIVATED·USER_OVERRIDE·SUSPENDED·FORCED_DEACTIVATION_WARNING → ENABLED·OTHER | 크루즈가 해제됐어요. 직접 운전해 주세요. | 10초 | ✓ | 안전 등급: 같은 순간의 다른 대사를 이김. 페달로 덮어쓰기·일시 정지는 켜짐도 해제도 아님 |
| 크루즈 목표 속도 | CRUISE_TARGET_SET | 마지막 변경 뒤 2초 동안 조용하면 1회 | 크루즈 속도를 바꿨어요. | 10초 | ✓ | Pleos 에뮬레이터(2026-11-11, 콘솔 조작): 반응 기록에 말하기 요청 · 재생됨 |
| 조향각 | STEER_BIG | \|각도\|가 360°(핸들 한 바퀴) 미만에서 이상으로 넘을 때 [임시 기준] | 핸들을 크게 꺾었어요. 주변을 살펴 주세요. | 60초 | ✓ | 방향은 가리지 않음 |
| 주행거리 | ODO_MILESTONE | 1,000km 단위를 지날 때 [임시 기준] | 누적 주행거리 이정표를 지났어요. | 60초 | ✓ | 주행 중에는 버리지 않고 보관했다가 정차하고 5초 뒤에 말함(도착 안내와 겹치지 않게) |
| 남은 주행거리 | RANGE_LOW | 50km 이하로 내려갈 때 [임시 기준] | 주행 가능 거리가 줄었어요. | 5분 | ✓ | Pleos 에뮬레이터(2026-11-11, 콘솔 조작): 반응 기록에 말하기 요청 · 재생됨 |
| 남은 주행거리 | RANGE_CRITICAL | 20km 이하로 내려갈 때 [임시 기준] | 주행 가능 거리가 많이 부족해요. 충전이나 주유를 준비해 주세요. | 2분 | ✓ | 안전 등급. 값 0은 "모름"으로 보고 말하지 않음 |

- 대사는 비공개 음성 팩의 슬롯이 아니라서 **기본 음성(시스템 TTS)** 이 읽고 캐릭터 몸짓은 없다. 캐릭터 목소리로 말하게 하려면 음성 팩에 슬롯과 클립이 더 필요하다.
- 값(숫자)은 읽지 않는 고정 문장이다. 꺼짐은 말하지 않는다(크루즈 해제만 예외). 캐릭터를 고르지 않은 화면의 대사 편집 목록에서 문장을 바꿀 수 있고, 비우면 기본 대사로 돌아온다.
- 주행 중에도 말한다. 다만 "주행 중 음성 허용"을 끄면 정차 중에만 말한다. 주행거리 이정표만 주행 중에 보관했다가 정차한 뒤에 말한다.
- `[임시 기준]`과 쿨다운은 임시값이다(`SignalReactionRules`, `VehicleEvent`). 실차·에뮬레이터 실측 뒤에 조정한다.
- 한 순간(300ms)에 여러 대사가 오면 안전 > 상태 > 연출 순으로 하나만 말하고 나머지는 버린다. 하향등과 같이 켜지는 후미등은 한 단계 낮아 하향등이 먼저다.

말하기 확인 기호: ✓ 에뮬레이터에서 앱 반응 기록에 `재생됨`까지 이어짐 · ✗ 이 에뮬레이터에서 확인 못 함 · - 이번 작업에서 확인하지 않음.

## PR #42로 추가한 신호 (11개) — SDK·권한·콘솔 대응

| 신호 | SDK 구독 메서드 | 권한 | 콘솔 신호 | VHAL/서비스 속성 | 확인 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 상향등 | `Light.registerHighBeamLightState` | CAR_LIGHTS | high-beam-lights | HIGH_BEAM_LIGHTS_STATE 0x11400e01 | ✓ | 콘솔 켬 → VHAL 0→1 → Pleos 서비스 → 앱 목록 ON. 앱을 끈 채로도 VHAL 변화 확인 |
| 하향등 | `Light.registerLowBeamLightState` | CAR_LIGHTS | exterior-lights-mode (LowBeam) | LOW_BEAM_LIGHTS_STATE (콘솔 대상 EXTERIOR_LIGHTS_MODE 0x21400754) | ✓ | 콘솔 외부등 모드가 하향등+후미등 상태를 함께 바꿈 |
| 후미등 | `Light.registerTailLampLightState` | CAR_LIGHTS | exterior-lights-mode (LowBeam/Position) | TAIL_LAMP_LIGHTS_STATE | ✓ | LowBeam·Position 모두 ON (Position은 후미등만) |
| 앞 안개등 | `Light.registerFrontFogLightState` | CAR_LIGHTS | (콘솔 카드 없음) | (이 에뮬레이터 VHAL에 속성 없음) | ✗ | 안개등 속성이 있는 차량·이미지에서는 수신될 것으로 예상(미검증) |
| 뒤 안개등 | `Light.registerRearFogLightState` | CAR_LIGHTS | (콘솔 카드 없음) | (이 에뮬레이터 VHAL에 속성 없음) | ✗ | 안개등 속성이 있는 차량·이미지에서는 수신될 것으로 예상(미검증) |
| 조향각 | `SteeringWheel.registerSteeringWheelAngle` | CAR_DRIVE | steering-angle | PERF_STEERING_ANGLE 0x11600209 | ✓ | 부호가 콘솔과 반대(VHAL: 반시계 방향 +) |
| 주행거리 | `Odometer.registerDistanceDriven` | CAR_DRIVE | perf-odometer | PERF_ODOMETER 0x11600204 | ✓ | 콘솔이 값 감소를 막음(HTTP 400). 에뮬레이터 콜드 부팅 시 0으로 초기화 |
| 남은 주행거리 | `Odometer.registerRangeRemainingListener (deprecated)` | CAR_DRIVE | range-remaining | RANGE_REMAINING 0x11600308 | ✓ | SDK가 "Google RANGE_REMAINING 속성을 쓰라"며 deprecated 표시, 값은 정상 수신 |
| 크루즈 상태 | `DrivingMode.registerCruiseControlState` | CAR_DRIVE | cruise-control-state (ad_mode_state) | CRUISE_CONTROL_STATE 0x11401011 | △ | 콘솔의 ready·longitudinal·lateral은 모두 ENABLED로 보임 |
| 크루즈 목표 속도 | `DrivingMode.registerCruiseControlTargetSpeed` | CAR_DRIVE | cruise-control-target-speed | CRUISE_CONTROL_TARGET_SPEED 0x11601013 | ✓ | 콘솔 20 → 앱 20.00 → 원복 13.89 |
| 기어 | `Gear.registerGearSelection (deprecated)` | CAR_DRIVE | gear-selection | GEAR_SELECTION 0x11400400 / VENDOR_GEAR_SELECTION 0x21400400 | ✓ | SDK가 "Google GEAR_SELECTION 속성을 쓰라"며 deprecated 표시, 값은 정상 수신. 정차/주행 판단은 기존 CarUx 경로 그대로 |

확인 기호: ✓ 콘솔 → VHAL → Pleos 서비스 → 앱 목록까지 값이 바뀌어 옴 · △ 일부 값에서만 바뀜 · ✗ 이 에뮬레이터에서 안 됨(속성 없음·이벤트 없음).

## 수신 가능한 신호 전체 (`main`)

| 상태 | 도메인 | 신호 | 콘솔 id | SDK 메서드 | 권한 | 반응 연결 | 확인 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 수신 중 (main) | ADAS | 크루즈 목표 속도 | `cruise-control-target-speed` | `DrivingMode.registerCruiseControlTargetSpeed` | CAR_DRIVE | CRUISE_TARGET_SET (2초 디바운스) | ✓ |
| 수신 중 (main) | ADAS | 크루즈 상태 | `cruise-control-state` | `DrivingMode.registerCruiseControlState` | CAR_DRIVE | CRUISE_ON / CRUISE_OFF | △ |
| 수신 중 (main) | Climate | 공기 청정 | `hvac-air-cleaner` | `HVAC.registerHvacAirCleanerState(SeatArea)` | CAR_CLIMATE | COMFORT | - |
| 수신 중 (main) | Climate | 공조 자동 | `hvac-auto` | `HVAC.registerHvacAutoState(SeatArea)` | CAR_CLIMATE | COMFORT | - |
| 수신 중 (main) | Climate | 공조 전원 | `hvac-power` | `HVAC.registerHvacPowerState(SeatArea)` | CAR_CLIMATE | COMFORT | - |
| 수신 중 (main) | Climate | 내기 순환 | `hvac-recirc` | `HVAC.registerHvacRecirculationState / registerHvacAutoRecirculationState` | CAR_CLIMATE | COMFORT | - |
| 수신 중 (main) | Climate | 설정 온도 | `hvac-temperature-set` | `HVAC.registerHvacTemperature(SeatArea)` | CAR_CLIMATE | COMFORT (1초 디바운스) | - |
| 수신 중 (main) | Climate | 성에 제거 | `hvac-defroster` | `HVAC.registerHvacDefrosterState(WindowArea)` | CAR_CLIMATE | 없음 (상태만 기록) | - |
| 수신 중 (main) | Climate | 송풍 세기 | `hvac-fan-speed` | `HVAC.registerHvacFanSpeed(SeatArea)` | CAR_CLIMATE | COMFORT (1초 디바운스) | - |
| 수신 중 (main) | Climate | 에어컨(A/C) | `hvac-ac` | `HVAC.registerHvacAcState(SeatArea)` | CAR_CLIMATE | COMFORT | - |
| 수신 중 (main) | Climate | 좌석 열선·통풍 | `hvac-seat-temperature` | `Seat.registerHvacSeatTemperature(SeatArea) / registerHvacSeatVentilation` | CAR_SEATS | 없음 (상태만 기록) | - |
| 수신 중 (main) | Climate | 좌우 온도 분리 | `hvac-dual` | `HVAC.registerHvacDualState(SeatArea)` | CAR_CLIMATE | COMFORT | - |
| 수신 중 (main) | Convenience | 글로브박스 | `glove-box-door-pos` | `Door.registerGloveBoxLockState` | CAR_DOORS | DOOR | - |
| 수신 중 (main) | Convenience | 도어 열림 상태 | `door-pos` | `Door.registerDoorPosition(DoorArea)` | CAR_DOORS | DOOR (열림 구간) | - |
| 수신 중 (main) | Convenience | 도어 잠금 | `door-lock` | `Door.registerDoorLockState(DoorArea)` | CAR_DOORS | DOOR | - |
| 수신 중 (main) | Convenience | 독서등 | `reading-lights-switch` | `Light.registerReadingLightState(SeatArea)` | CAR_LIGHTS | LIGHT | ✓ |
| 수신 중 (main) | Convenience | 방향지시등 | `turn-signal-state` | `TurnSignal.registerTurnSignalState` | CAR_LIGHTS | TURN | ✓ |
| 수신 중 (main) | Convenience | 상향등 | `high-beam-lights` | `Light.registerHighBeamLightState` | CAR_LIGHTS | HIGH_BEAM_ON (켜질 때) | ✓ |
| 수신 중 (main) | Convenience | 실내등 | `cabin-lights-switch` | `Light.registerCabinLightState` | CAR_LIGHTS | LIGHT | ✓ |
| 수신 중 (main) | Convenience | 앰비언트 밝기 | `ambient-light-brightness` | `Light.registerAmbientLightBrightnessListener(SeatArea)` | CAR_LIGHTS | LIGHT (밝기 구간) | ✓ |
| 수신 중 (main) | Convenience | 앰비언트 색상 | `ambient-light-color` | `Light.registerAmbientLightBrightnessColorListener(SeatArea)` | CAR_LIGHTS | LIGHT | ✓ |
| 수신 중 (main) | Convenience | 와이퍼 | `windshield-wipers-switch` | `Wiper.registerWiperState(WindowArea)` | CAR_WINDOWS | 없음 (상태만 기록) | - |
| 수신 중 (main) | Convenience | 좌석 착석 | `seat-occupancy` | `Seat.registerSeatOccupancy(SeatArea)` | CAR_SEATS | COMFORT | - |
| 수신 중 (main) | Convenience | 창문 위치 | `window-position` | `Window.registerWindowPosition(WindowArea)` | CAR_WINDOWS | WINDOW (위치 구간) | - |
| 수신 중 (main) | Convenience | 창문 잠금 | `window-lock` | `Window.registerWindowLockState(WindowArea)` | CAR_WINDOWS | WINDOW | - |
| 수신 중 (main) | Convenience | 트렁크·프렁크 열림 | `trunk-position` | `Door.registerDoorPosition(DoorArea)` | CAR_DOORS | DOOR (열림 구간) | - |
| 수신 중 (main) | Convenience | 하향등 | `exterior-lights-mode` | `Light.registerLowBeamLightState` | CAR_LIGHTS | LOW_BEAM_ON (켜질 때) | ✓ |
| 수신 중 (main) | Convenience | 후미등 | `exterior-lights-mode` | `Light.registerTailLampLightState` | CAR_LIGHTS | TAIL_LAMP_ON (켜질 때) | ✓ |
| 수신 중 (main) | Energy | 남은 주행거리 | `range-remaining` | `Odometer.registerRangeRemainingListener` | CAR_DRIVE | RANGE_LOW / RANGE_CRITICAL | ✓ |
| 수신 중 (main) | Energy | 배터리 에너지 | `ev-battery-level` | `EvBattery.registerEvBatteryLevel` | CAR_ENERGY | 없음 (상태만 기록) | - |
| 수신 중 (main) | Energy | 배터리 잔량(SOC) | `ev-battery-soc` | `EvBattery.registerEvBatteryStateOfCharge` | CAR_ENERGY | ENERGY_* (범주 전환) | - |
| 수신 중 (main) | Energy | 충전구 | `ev-charge-port` | `EvBattery.registerEvChargePortOpen` | CAR_ENERGY | CHARGE_PORT | - |
| 수신 중 (main) | Motion | 기어 | `gear-selection` | `Gear.registerGearSelection` | CAR_DRIVE | SHIFT_TO_DRIVE / SHIFT_TO_PARK / SHIFT_TO_REVERSE | ✓ |
| 수신 중 (main) | Motion | 브레이크 페달 | `brake-pedal-status` | `Brake.registerBrakePedalStatus` | CAR_DRIVE | BRAKE | ✓ |
| 수신 중 (main) | Motion | 조향각 | `steering-angle` | `SteeringWheel.registerSteeringWheelAngle` | CAR_DRIVE | STEER_BIG (큰 각도) | ✓ |
| 수신 중 (main) | Motion | 주행거리 | `perf-odometer` | `Odometer.registerDistanceDriven` | CAR_DRIVE | ODO_MILESTONE (이정표, 정차 후) | ✓ |
| 수신 중 (main) | Motion | 핸들 열선 | `steering-wheel-heat` | `SteeringWheel.registerHvacSteeringWheelHeatState` | CAR_CLIMATE | 없음 (상태만 기록) | - |
| 수신 중 (main) | Convenience | 앰비언트 모드 | `ambient-light-switch` | `Light.registerAmbientLightStateListener(SeatArea)` | CAR_LIGHTS | LIGHT | ✗ |
| 수신 중 (main) | DrivingMode | 주행 모드 |  | `DrivingMode.registerDrivingModeState` | CAR_DRIVE | MODE | ✗ |
| 수신 중 (main) | EvBattery | EV 순간 전비 |  | `EvBattery.registerInstantaneousEvEfficiency` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | EvBattery | EV 현재 전비 |  | `EvBattery.registerCurrentEvEfficiency` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | EvBattery | 누적 EV 전비 |  | `EvBattery.registerCumulativeEvEfficiency` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | EvBattery | 충전 이후 EV 전비 |  | `EvBattery.registerSinceChargeEvEfficiency` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | Fuel | 누적 연비 |  | `Fuel.registerCumulativeFuelEconomy` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | Fuel | 순간 연비 |  | `Fuel.registerInstantaneousFuelEconomy` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | Fuel | 연료 잔량 |  | `Fuel.registerFuelPercent` | CAR_ENERGY | ENERGY_* (범주 전환) | ✗ |
| 수신 중 (main) | Fuel | 충전 이후 연비 |  | `Fuel.registerSinceChargeFuelEconomy` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | Fuel | 현재 연비 |  | `Fuel.registerCurrentFuelEconomy` | CAR_ENERGY | 없음 (상태만 기록) | ✗ |
| 수신 중 (main) | Light | 뒤 안개등 |  | `Light.registerRearFogLightState` | CAR_LIGHTS | REAR_FOG_ON (켜질 때) | ✗ |
| 수신 중 (main) | Light | 앞 안개등 |  | `Light.registerFrontFogLightState` | CAR_LIGHTS | FRONT_FOG_ON (켜질 때) | ✗ |
| 수신 중 (main) | Light | 주행 모드 연동 조명 |  | `Light.registerAmbientColorAutoByDriveModeEnabled` | CAR_LIGHTS | LIGHT | ✗ |
| 수신 중 (main) | Safety | 안전벨트 미착용 경고 |  | `Safety.registerSeatBeltNotBuckledWarning` | CAR_SAFETY | WARNING (중립 음성) | ✗ |
| 수신 중 (main) | SideMirror | 사이드미러 열선 |  | `SideMirror.registerHvacSideMirrorHeatState` | CAR_CLIMATE | 없음 (상태만 기록) | - |
| 수신 중 (main) | Window | 스마트 글라스 농도 |  | `Window.registerSmartGlassLevel` | CAR_WINDOWS | WINDOW (위치 구간) | ✗ |
| 수신 중 (main) | Window | 스마트 글라스 상태 |  | `Window.registerSmartGlassState` | CAR_WINDOWS | WINDOW | ✗ |
| 수신 중 (main) | Window | 창문 블라인드 위치 |  | `Window.registerWindowBlindPosition` | CAR_WINDOWS | WINDOW (위치 구간) | - |

"확인 -"은 이번 작업에서 에뮬레이터로 확인하지 않았다는 뜻이다(코드에는 구독이 있다).

## 받을 수 없거나 조건이 붙는 신호

| 상태 | 신호 | 콘솔 id | 비고 |
| --- | --- | --- | --- |
| 수신 가능·미구독 | 등받이 각도 | `seat-backrest-position` |  |
| 수신 가능·미구독 | 요추 위치 | `seat-lumbar-position` |  |
| 수신 가능·미구독 | 좌석 높이 | `seat-height-position` |  |
| 수신 가능·미구독 | 좌석 앞뒤 위치 | `seat-fore-aft-position` |  |
| 수신 가능·미구독 | 좌석 틸트 | `seat-tilt-position` |  |
| 수신 가능·미구독 | MAX A/C |  |  |
| 수신 가능·미구독 | MAX 성에 제거 |  |  |
| 수신 가능·미구독 | 공조 현재 온도 |  |  |
| 수신 가능·미구독 | 트립 주행 시간 | `trip-driving-time` | SDK 대응 부분 일치 · 현재/누적/리필 후 중 어느 쪽인지 미확인 |
| 수신 가능·미구독 | 누적 주행 시간 |  |  |
| 수신 가능·미구독 | 누적 트립 주행거리 |  |  |
| 수신 가능·미구독 | 순간 트립 주행거리 |  |  |
| 수신 가능·미구독 | 주유·충전 이후 주행 시간 |  |  |
| 수신 가능·미구독 | 주유·충전 이후 주행거리 |  |  |
| 수신 가능·미구독 | 현재 트립 주행거리 |  |  |
| 수신 가능·미구독 | 변속기 오일 수준 |  |  |
| 수신 가능·미구독 | 브레이크액 수준 |  |  |
| 수신 가능·미구독 | 엔진 오일 수준 |  |  |
| 수신 가능·미구독 | 워셔액 부족 경고 |  |  |
| 수신 가능·미구독 | 좌석 에어백 전개 |  |  |
| 수신 가능·미구독 | 좌석 에어백 활성 |  |  |
| 수신 가능·미구독 | 파워다운 경고 |  |  |
| 수신 가능·미구독 | 안전벨트 높이 |  |  |
| 수신 가능·미구독 | 안전벨트 체결 상태 |  |  |
| 수신 가능·미구독 | 요추 측면 지지 |  |  |
| 수신 가능·미구독 | 좌석 깊이 |  |  |
| 수신 가능·미구독 | 헤드레스트 각도 |  |  |
| 수신 가능·미구독 | 헤드레스트 높이 |  |  |
| 수신 가능·미구독 | 헤드레스트 앞뒤 |  |  |
| 권한 미선언 | 온도 표시 단위 |  |  |
| SDK 구독 없음 | 송풍 방향 | `hvac-fan-direction` | 풍향 전용 register 메서드를 못 찾음 |
| SDK 구독 없음 | ETCS 시나리오 | `etcs-scenario` | Toll에는 registerEtcsVoiceGuidanceVolume(음성 안내 볼륨, CAR_TOLL)만 있다 |
| SDK 구독 없음 | ETCS 전원 | `etcs-activated` | Toll에는 registerEtcsVoiceGuidanceVolume(음성 안내 볼륨, CAR_TOLL)만 있다 |
| SDK 구독 없음 | 도어 조명 | `door-lights` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 비상등 | `hazard-lights` | 비상등 전용 register 메서드를 못 찾음 |
| SDK 구독 없음 | 제동등 | `stop-lights` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 배터리 용량 | `ev-battery-capacity` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 충전 목표 한도 | `ev-charge-percent-limit` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 충전 스위치 | `ev-charge-switch` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 충전 전류 한도 | `ev-charge-current-limit` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 차량 알림 1 | `notification-1` | Vehicle SDK 구독 메서드에서 대응을 못 찾음 (콘솔 쓰기 경로는 /vehicle/notifications/simulator) |
| SDK 구독 없음 | 등받이 이동 명령 | `seat-backrest-move` | 이동 명령 값이라 대응하는 register 메서드가 없음 |
| SDK 구독 없음 | 에스코트 조명 | `escort-lights` | 대응하는 register 메서드를 못 찾음 |
| SDK 구독 없음 | 요추 이동 명령 | `seat-lumbar-move` | 이동 명령 값이라 대응하는 register 메서드가 없음 |
| SDK 구독 없음 | 좌석 높이 이동 명령 | `seat-height-move` | 이동 명령 값이라 대응하는 register 메서드가 없음 |
| SDK 구독 없음 | 좌석 앞뒤 이동 명령 | `seat-fore-aft-move` | 이동 명령 값이라 대응하는 register 메서드가 없음 |
| SDK 구독 없음 | 좌석 틸트 이동 명령 | `seat-tilt-move` | 이동 명령 값이라 대응하는 register 메서드가 없음 |
| SDK 구독 없음 | 전원 시스템 상태 | `power-system-actual` | 대응하는 register 메서드를 못 찾음 |
| 서드파티 불가 | 계기판 경고등(텔테일) | `telltales` |  |
| 서드파티 불가 | 타이어 압력 | `tire-pressure` |  |

- SDK에 구독 API가 없는 신호: 차량 속도, 가속 페달, 차량 알림.
- 서드파티 불가(`PRIVATE_CAR_SAFETY`): 텔테일 37종, 타이어 압력(공개된 건 조회용 `getLowTirePressure`뿐).
- 이 에뮬레이터에서만 안 되는 것: 앰비언트 켜짐/꺼짐(콘솔이 VHAL 이벤트를 만들지 않음), 앞·뒤 안개등·연료·연비·EV 전비·스마트 글라스(`ERROR_NOT_SUPPORTED_PROPERTY`), 주행 모드·안전벨트 경고.

## 앱 없이 신호를 확인하는 방법

콘솔 조작이 실제 차량 신호가 되는지는 앱과 무관하게 층별로 볼 수 있다. 자세한 명령은 [Virtual Console E2E 가이드](../guides/PLEOS_VIRTUAL_CONSOLE_E2E.md)의 "앱 없이 신호 확인하기"를 따른다.

1. **A 콘솔**: `PUT <write_endpoint>?<field>=<값>` 으로 조작하고 `GET /vehicle/state`로 읽는다(하트비트 필요).
2. **B VHAL**: `adb shell dumpsys car_service --services VehicleHal`의 `All Events`에서 속성별 `event count`와 `lastEvent`를 조작 전후로 비교한다.
3. **C Pleos 서비스**: logcat의 `collect event: PropertyEventItem(packageNames=[…])`.
4. **D 앱**: review 디버그 빌드의 "Vehicle 데이터 디버그".

## 알려진 문제

- `PleosEnergySubscriptions`의 `watch()`는 성공 경로에서 `states.registerDefinition()`을 부르지 않는다. `VehicleStateRepository.observe()`는 등록되지 않은 키를 버리므로 EV 배터리 잔량·배터리 에너지 레벨·충전구 상태 항목이 기록되지 않고 해당 반응도 나오지 않는다(에뮬레이터 데이터 디버그 목록에서 항목 부재 확인, 에너지 범주만 기록됨). 이 PR에서는 고치지 않았다.
