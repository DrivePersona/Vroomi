# Android 패키지 규칙

각 Gradle 모듈은 독립된 기능 경계이며, 모듈 안에서는 역할로 패키지를 나눈다. 새 코드는 파일 크기나 화면 이름이 아니라 아래 책임에 따라 배치한다.

## Pleos Clean Architecture

의존성은 바깥에서 안쪽으로만 흐른다.

```text
Presentation (Compose View + MVVM/MVI)
  ui/                         화면·Activity·오버레이
  presentation/model/         불변 UiState와 화면 모델
  presentation/event/         사용자 Intent와 일회성 Effect
  presentation/viewmodel/     StateFlow, Intent 라우팅
                │
                ▼
Domain (순수 Kotlin)
  domain/model/                Entity·정책·값 객체
  domain/repository/           외부 데이터 포트(인터페이스)
  domain/usecase/              차량 특화 판단 규칙
                ▲
                │ 구현
Data (Pleos/AAOS 격리)
  data/datasource/             VehicleDataSource 계약
  data/vehicle/                Pleos SDK·CarUxRestrictions 어댑터
  data/mapper/                 SDK 원시 콜백 → Domain 입력 변환
  data/repository/             Domain Repository 구현체
  data/audio/                  Android·Pleos 오디오 어댑터
```

- `domain`에는 `android.*`, Pleos SDK, Compose import를 넣지 않는다.
- `data`만 Pleos Vehicle SDK, AAOS 시스템 서비스, Android 오디오와 직접 통신한다.
- `presentation`은 `UiState`와 `Intent`를 통해 UseCase/Runtime 결과를 표시하며 SDK를 직접 호출하지 않는다.
- `app/`은 조립 루트다. `CompanionRuntime`이 Data Repository와 Domain UseCase를 연결하고, Activity는 ViewModel만 만든다.
- `voicepack`·`characterpack`은 각각 독립 모듈의 계약을 유지한다. 앱은 이 모듈의 Domain/Data를 조립 루트에서만 선택한다.

```text
app/src/main/java/com/example/voicecompanion/
  app/           Application·런타임 조립과 앱 수명
  domain/        차량 반응 규칙과 외부 포트
  domain/model/  순수 도메인 값과 기록 모델
  domain/repository/ 차량·외부 데이터 포트 인터페이스
  domain/usecase/ 차량 입력·포커스 등 단일 정책 실행 단위
  data/datasource/ 외부 입력 경계 계약
  data/mapper/ SDK DTO·Property를 Domain 모델로 바꾸는 변환
  data/repository/ Domain 포트의 Pleos/AAOS 구현체
  data/audio/    Android·Pleos 음성 및 오디오 포커스 어댑터
  data/vehicle/  차량 SDK·주행 규제 어댑터(기존 VehicleInputRepository 호환 포함)
  presentation/model/ 화면에 전달하는 읽기 전용 UI 상태 모델
  presentation/event/ 사용자 Intent와 일회성 UI 효과
  presentation/viewmodel/ StateFlow UI 상태와 Intent 처리
  presentation/CompanionUiRuntime.kt 화면이 소비하는 앱 동작 포트
  ui/            Activity와 Compose 화면
  ui/theme/      앱 고유 색상·타이포그래피·형상 토큰과 CompanionTheme
  ui/component/  토큰을 적용한 재사용 Compose 컴포넌트
  ui/overlay/    WindowManager 기반 캐릭터 오버레이 UI

voicepack/       data/, domain/, model/, util/
characterpack/   data/, domain/, model/, ui/, util/ — GLB 3D와 검증된 PNG 스프라이트 2D 표현을 같은 선택 계약으로 제공
```

- `model`과 `util`은 화면·Activity에 의존하지 않는다.
- `domain`은 정책·계약을 갖고 Android·Pleos 구현을 직접 참조하지 않는다.
- `data`는 파일, SDK, Android 시스템처럼 외부 입력을 구현한다.
- `ui`는 상태를 표시하고 `CompanionTouch`를 전달한다. 차량 규칙이나 팩 검증을 직접 구현하지 않는다.
- `ui/theme`과 `ui/component`는 화면별 하드코딩 대신 공통 시각·상호작용 규칙을 제공한다. 기준은 `DESIGN_SYSTEM.md`에 둔다.
- `presentation/viewmodel`은 `StateFlow<CompanionUiState>`를 제공하고, Intent를 `CompanionUiRuntime` 계약으로 전달한다.
- `data/vehicle`의 Pleos 어댑터는 `VehicleDataSource`로 입력을 내보내고, `data/mapper`가 같은 `VehicleInput` 모델로 바꾼다. `data/repository/PleosVehicleRepository`가 Domain `VehicleRepository`를 구현한다.
- `app`만 위 요소를 조립한다. 이 앱에는 Activity가 없다. `CompanionBootReceiver`가 부팅 완료 시 `CompanionRuntimeService`를 시작하고, 서비스가 `CompanionRuntime`(차량 구독·반응 엔진·오버레이)을 소유한다. 이전 설정 화면(`CompanionScreen` 계열)은 더 이상 진입점이 없다.

테스트도 대상 패키지를 따라 배치한다. 실제 디버그 팩을 읽는 교차 검사는 `integration/`에 둔다.

캐릭터 팩은 `format: "glb"` 또는 `format: "sprite-sheet"`을 선언한다. 두 형식 모두 `files`의 크기·SHA-256과 이벤트별 `animations`를 검증한다. 스프라이트 팩은 PNG 실측 크기와 격자/프레임 시간도 추가로 확인한다. 권리 확인이 끝나지 않은 참조 기반 캐릭터는 `provenance.character: "reference-derived"`, `commercialUse: false`로 선언하고 `app/src/debug/assets/characterpacks/`에만 둔다. release 에셋과 제품 배포에는 넣지 않는다.
