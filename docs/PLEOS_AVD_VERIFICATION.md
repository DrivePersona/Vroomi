# Pleos AVD UI 검증 증적

검증일: 2026-11-04  
대상: `Pleos_Connect_v2_1_0` (사용자 10, 콜드 부팅)  
APK: 메인 통합 브랜치의 `reviewDebug`

## 확인한 흐름

1. 최종 통합 APK를 Pleos AVD에 설치했다.
2. `com.example.voicecompanion.review/.ui.MainActivity`를 사용자 10으로 실행했다.
3. 앱이 검은 화면 없이 Pleos 차량 화면 위에 표시되는지 확인했다.
4. 차량 서비스 구독 상태와 Vehicle 데이터 디버그 영역이 표시되는지 확인했다.
5. Virtual Console의 도어 상태 변경이 앱 콜백과 디버그 기록으로 들어오는지 확인했다.

## UI 캡처

최종 통합 APK 실행 화면. Pleos 차량 화면, 정차 상태, 차량 데이터 디버그 영역과 각 capability 수신 상태가 표시된다.

![최종 통합 앱 화면](evidence/pleos-final-merged.png)

차량 도어 이벤트 검증 화면. Virtual Console에서 Front Left Door를 열었을 때 Pleos 화면에 도어 상태가 표시되고 앱의 수신 카운터가 증가했다.

![도어 이벤트 콜백](evidence/pleos-door-open-callback.png)

## 결과

| 항목 | 결과 |
| --- | --- |
| 앱 설치 | 통과 |
| Activity 실행 | 통과 |
| 검은 화면/초기 렌더 지연 회귀 | 재현되지 않음 |
| 차량 capability 구독 | 통과·미지원 항목은 상태에 표시 |
| Virtual Console 도어 이벤트 | 콜백 수신 확인 |
| 실차 검증 | 미실시 |
| 사람 귀로 음성 청취 | 미실시 |

에뮬레이터가 제공하지 않는 드라이브 모드·안전벨트 등은 실패로 처리하지 않고 `ERROR_NOT_SUPPORTED_PROPERTY`로 기록한다. 이 문서는 UI 증적을 남기기 위한 검증 기록이며, 실차 적합성 승인을 의미하지 않는다.
