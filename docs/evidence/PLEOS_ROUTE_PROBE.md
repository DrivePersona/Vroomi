# Pleos 경로 탐색 콜백 실측

Pleos Connect 2.1.0 사용자 10에 내부 디버그 APK를 설치했다. 공식 NaviHelper 3.0.5를 사용했다.

| 항목 | 결과 |
| --- | --- |
| 내부 디버그 빌드와 설치 | 성공 |
| SDK 초기화 | connected |
| 고정 테스트 목적지 요청 | SDK 호출 정상 반환 |
| onRouteStarted | 요청 약 7.5초 후 앱 콜백 수신 |
| onRouteEnded | 같은 시점에 SDK 수신 로그 확인 |
| onRoutingCompleted / onError | 관찰한 요청에서 앱 콜백 미확인 |
| onRouteCancelled | 미확인 |

경로 시작 및 종료 신호만으로 성공·실패·취소를 구분할 수 없었다. 결과 발화를 연결하려면 요청에 대응하는 종료 원인이나 목적지·경로 상태를 추가로 대조해야 한다. 기본 내비의 별도 음성 안내와 중복 여부도 검증이 필요하다.

[공식 SDK 버전](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk) · [목적지와 결과 콜백](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)
