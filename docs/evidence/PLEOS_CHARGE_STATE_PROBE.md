# 충전 상태 TTS 신호 확인

Pleos Connect 2.1.0 AVD에서 Virtual Console의 충전 스위치와 Android Automotive 충전 상태 속성을 대조했다.

| 확인 항목 | 관찰 결과 |
| --- | --- |
| 충전 스위치 | 콘솔에서 Off → On → Off를 입력했다. 콘솔 상태의 charging_activated 값이 False → True → False로 바뀌었다. |
| 충전구 | 같은 실험의 charging_door_opened는 계속 False였다. |
| 실제 충전 상태 | car_service 진단에 읽기 전용 EV_CHARGE_STATE (0x11400f41)가 노출됐다. 속성 목록만으로는 값 변경이나 Vroomi 수신을 확인할 수 없다. |
| 직접 조회 | AVD의 사용자 빌드는 cmd car_service get-property-value를 거부한다. |
| Vroomi 상태 | Pleos SDK의 충전 스위치 조회는 초기 1회용이며 발화 이벤트가 없다. 현재 충전구 이벤트는 포트 상태만 알린다. |

## 발화 연결 조건

Android Automotive의 EV_CHARGE_STATE를 후보로 두고, Vroomi 앱 UID에서 필요한 읽기 권한과 구독 성공 여부를 확인한다. 콘솔의 스위치 값은 충전 요청 상태로만 취급한다. 충전 시작·완료·중단은 각 상태가 앱 콜백으로 확인될 때에만 중립 TTS에 매핑한다. 초기 값과 같은 값의 반복은 발화에서 제외한다. 충전 완료는 충전 목표량 도달을 포함한다. 충전 종료 원인이 불명확하면 완료나 중단으로 분류하지 않는다.

검증에는 초기 구독 무음, 시작·완료·중단 각각 한 번, 중복 콜백 무음, 충전구만 열 때 충전 시작 무음, Pleos TTS 재생 시작과 종료 로그를 포함한다.

[Android Automotive 충전 속성과 읽기 권한](https://developer.android.com/reference/android/car/VehiclePropertyIds#EV_CHARGE_STATE)

## 앱 권한으로 구독한 결과

내부 디버그 APK에만 Android Automotive 에너지 읽기 권한과 진단 버튼을 추가했다. 사용자 10에서 권한을 부여한 뒤 EV_CHARGE_STATE 구독에 성공했고, 최초 콜백은 status=0, value=2 (STATE_FULLY_CHARGED)였다.

Virtual Console에서 충전 스위치를 On/Off로 바꿨을 때 추가 콜백이 없었다. 콘솔 SOC를 100%에서 40%로 낮춘 뒤 스위치를 On으로 바꿔도 추가 콜백이 없었다. 실험 후 SOC 100%, 스위치 Off로 복원했다. 최초 상태만 확인됐으므로 실제 충전 전환의 TTS 검증은 보류한다.

[Android Automotive 충전 상태 값](https://developer.android.com/reference/android/car/hardware/property/EvChargeState)
