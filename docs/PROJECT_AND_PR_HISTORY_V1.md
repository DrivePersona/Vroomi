# 프로젝트와 PR 작업 정리 v1

**조회 기준:** GitHub 저장소 목록 및 PR/issue 상태, 2026-11-05
**저장소:** `DrivePersona/Vroomi`(public), `DrivePersona/voice-pack-sdk`(private), `DrivePersona/character-pack-sdk`(private)

조회된 PR은 **46건**이다: public 앱 22건 병합 + 1건 진행 중, private voice SDK 7건 병합 + 1건 종료/미병합, private character SDK 15건 병합. 총 **44건 병합, 1건 미병합 종료, 1건 진행 중**이다. 별도로 관련 저장소에 열린 이슈는 앱 9건, voice SDK 1건, character SDK 2건이다(2026-11-05 기준).

## 1. 저장소 역할

| 저장소 | 공개 범위 | 책임 |
| --- | --- | --- |
| [Vroomi](https://github.com/DrivePersona/Vroomi) | Public | 앱, 웹 시뮬레이터, 차량/안전 정책, 팩 연결 계약, 통합 문서와 공개 검증 증거. 상업용 원본 pack asset은 저장하지 않음 |
| [voice-pack-sdk](https://github.com/DrivePersona/voice-pack-sdk) | Private | 클립 재생 SDK, manifest 검사, 캐릭터별 음성/13-slot 제작물, 음성 동의 및 이용 권리 메타데이터 |
| [character-pack-sdk](https://github.com/DrivePersona/character-pack-sdk) | Private | 캐릭터 manifest/GLB 검증·표시, 애니메이션/gesture 계약, 내부 후보와 승인된 캐릭터 에셋 |

### 합쳐진 사용자 흐름

`Vehicle Service read callback → capability/event 정규화 → cooldown·주행·오디오 정책 → 선택 characterId → 그 manifest의 voicePackId → eventId WAV → 재생 callback에 연결된 GLB gesture → 완료/중단/idle 및 기록`

캐릭터 선택은 voice pack을 자동으로 결정한다. 별도 음성 팩 선택/대사 편집을 제공하지 않는다. 앱은 차량 제어 명령을 보내지 않는다.

## 2. 현재 인벤토리와 릴리스 상태

- 대상 인벤토리: 33개 characterId/voicePackId 쌍, 10 baseline + 23 VoiceDesign expansion.
- 목표 음성: 33 × 13 = 429 WAV 슬롯. 지금 보이는 13은 각 캐릭터당 슬롯 수이며 대상 캐릭터 수가 아니다.
- 2026-11-05 기준 303/429 파일이 존재하지만, baseline 50은 이전 5-slot 구성이고 253은 23개 내부 오디션의 11-slot 결과다. 126 슬롯은 WAV가 없고, 존재하는 파일도 모두 AUDIO_APPROVED/PACK_READY를 뜻하지 않는다.
- release roster는 빈 상태(0). 32/33 character manifest는 상업 이용 미승인이다. `dev.mori`는 commercialUse=true지만 release voicePackId가 비어 있고 내부 preview voice만 링크되어 있다.
- 23개 후보 importer는 로컬 내부 미리보기 용도다. 현재 internal CI `internalRelease`는 로컬 importer output을 받지 않아 출시 콘텐츠가 아니라 공개/CI 빌드에서 선택 가능한 캐릭터는 0개다.
- Pleos Connect AVD에서 브레이크·방향지시등·gear P/D 상태 전환이 확인됐다. Drive Mode/Seat Belt signal은 이 이미지가 제공하지 않았다. 실차 검증 및 실제 승인 13-WAV를 캐릭터와 재생하며 촬영한 최종 E2E는 아직 없다.
- PR #28의 event-mapped gesture start/idle 동기화는 구현됐지만 per-clip offset·variant-specific gesture가 없다. 실제 GLB와 WAV를 묶은 정차 시연 증거도 보강이 필요하다.

## 3. Public 앱 PR 기록

GitHub PR 제목은 원문으로 유지했다. 조회 시 #1–#35는 `MERGED`, #40은 `OPEN`이다.

| PR | 병합 작업 | 사용자 흐름상 결과 |
| --- | --- | --- |
| [#1](https://github.com/DrivePersona/Vroomi/pull/1) | `feat: adapt VR overlay to character viewport` | 캐릭터 bounds 기반 overlay 크기 대응 |
| [#2](https://github.com/DrivePersona/Vroomi/pull/2) | `chore: update voice pack content to 0.2.0` | 음성 pack content 버전 갱신 |
| [#15](https://github.com/DrivePersona/Vroomi/pull/15) | `docs: add Pleos Virtual Console E2E guide` | Virtual Console → Vehicle callback E2E 재현 절차 |
| [#17](https://github.com/DrivePersona/Vroomi/pull/17) | `feat: capability-driven vehicle state intake` | capability 중심 읽기 입력 |
| [#18](https://github.com/DrivePersona/Vroomi/pull/18) | `feat: react to convenience vehicle state changes` | 조명/창문/도어 편의 상태 |
| [#19](https://github.com/DrivePersona/Vroomi/pull/19) | `feat: add HVAC and comfort vehicle subscriptions` | HVAC/좌석/comfort 구독 |
| [#20](https://github.com/DrivePersona/Vroomi/pull/20) | `feat: add EV, fuel, and hybrid energy profiles` | capability 기반 에너지 프로필 |
| [#21](https://github.com/DrivePersona/Vroomi/pull/21) | `feat: implement vehicle event speech policy` | 주행/안전 이벤트 발화 정책 |
| [#22](https://github.com/DrivePersona/Vroomi/pull/22) | `docs: draft canonical 13-slot dialogue matrix` | 상태 ID 및 문구 초안 계약 |
| [#23](https://github.com/DrivePersona/Vroomi/pull/23) | `feat: gate character picker with release roster` | 명시적 roster 기반 선택 |
| [#24](https://github.com/DrivePersona/Vroomi/pull/24) | `feat: bind character selection to paired voice pack` | 캐릭터 선택 시 voice ID 자동 연결 |
| [#25](https://github.com/DrivePersona/Vroomi/pull/25) | `test: strengthen release roster validation` | 권리/자산/slot/gesture 출시 검증 |
| [#26](https://github.com/DrivePersona/Vroomi/pull/26) | `ci: validate and open private pack compatibility PRs` | private pack publish 후 public compatibility PR 흐름 |
| [#27](https://github.com/DrivePersona/Vroomi/pull/27) | `test: enable debug lifecycle diagnostics` | 디버그 lifecycle 안정성 진단 |
| [#28](https://github.com/DrivePersona/Vroomi/pull/28) | `feat: synchronize speech and one-shot gestures` | 재생 시작/종료와 event gesture 연결 |
| [#29](https://github.com/DrivePersona/Vroomi/pull/29) | `fix: prevent vehicle subscription startup from blocking the UI` | 차량 구독 시작 지연이 UI를 막지 않게 개선 |
| [#30](https://github.com/DrivePersona/Vroomi/pull/30) | `chore: consolidate vehicle, pack, and speech updates into main` | 여러 통합 작업 main 반영 |
| [#31](https://github.com/DrivePersona/Vroomi/pull/31) | `docs: add Pleos AVD UI verification evidence` | AVD 화면/차량 입력 증거 기록 |
| [#32](https://github.com/DrivePersona/Vroomi/pull/32) | `docs: define Virtual Console as vehicle event source` | 테스트 입력과 앱 callback 경계 명시 |
| [#33](https://github.com/DrivePersona/Vroomi/pull/33) | `Expose character picker and internal candidate preview` | 내부 후보 선택 UI |
| [#34](https://github.com/DrivePersona/Vroomi/pull/34) | `Pair internal characters with voices and expose 13-slot dialogue status` | 내부 캐릭터/음성 매칭 및 13-slot 표시 |
| [#35](https://github.com/DrivePersona/Vroomi/pull/35) | `docs: clarify 33-character release target` | 33-character target/CSV와 importer 검사 명확화 |
| [#40](https://github.com/DrivePersona/Vroomi/pull/40) | `docs: define overlay-first companion flow` | WindowManager 중심 PRD/flow/wireframe과 인터랙티브 목업. 현재 리뷰 중 |

조회 결과 main repo에는 PR #3–14와 #16이 등록된 PR로 보이지 않았다. 이 문서는 해당 작업의 존재를 추정하거나 번호를 채워 쓰지 않는다.

## 4. Private voice-pack-sdk PR 기록

| PR | 상태 | 병합/종료 작업 |
| --- | --- | --- |
| [#1](https://github.com/DrivePersona/voice-pack-sdk/pull/1) | CLOSED, not merged | `feat: VoiceDesign 신규 캐릭터 음성 후보 10종`. 병합 콘텐츠로 계산하지 않음 |
| [#2](https://github.com/DrivePersona/voice-pack-sdk/pull/2) | MERGED | VoiceDesign 주행 대사 변형 |
| [#3](https://github.com/DrivePersona/voice-pack-sdk/pull/3) | MERGED | 캐릭터별 음성 팩 게시 |
| [#4](https://github.com/DrivePersona/voice-pack-sdk/pull/4) | MERGED | package publish workflow token 전달 수정 |
| [#6](https://github.com/DrivePersona/voice-pack-sdk/pull/6) | MERGED | 명시적 음성 clip variant 지원 |
| [#7](https://github.com/DrivePersona/voice-pack-sdk/pull/7) | MERGED | clip 단위 gesture timing metadata |
| [#8](https://github.com/DrivePersona/voice-pack-sdk/pull/8) | MERGED | variant/gesture API 통합 |
| [#9](https://github.com/DrivePersona/voice-pack-sdk/pull/9) | MERGED | 캐릭터 대사 matrix와 release metadata |

조회 시 SDK 이슈 [#5](https://github.com/DrivePersona/voice-pack-sdk/issues/5)는 모든 release companion에 canonical 13-state WAV를 만드는 작업으로 열려 있었다.

## 5. Private character-pack-sdk PR 기록

| PR | 상태 | 병합 작업 |
| --- | --- | --- |
| [#1](https://github.com/DrivePersona/character-pack-sdk/pull/1) | MERGED | VoiceDesign 캐릭터 10종 설계 문서 |
| [#2](https://github.com/DrivePersona/character-pack-sdk/pull/2) | MERGED | Meshy VR 캐릭터 후보 11종 |
| [#3](https://github.com/DrivePersona/character-pack-sdk/pull/3) | MERGED | 투명 VR 캐릭터 surface 안정화 |
| [#4](https://github.com/DrivePersona/character-pack-sdk/pull/4) | MERGED | Night Route Archive season 1 컨셉 |
| [#5](https://github.com/DrivePersona/character-pack-sdk/pull/5) | MERGED | 적응형 캐릭터 overlay viewport |
| [#6](https://github.com/DrivePersona/character-pack-sdk/pull/6) | MERGED | package publish workflow token 전달 수정 |
| [#7](https://github.com/DrivePersona/character-pack-sdk/pull/7) | MERGED | Night Route Archive Meshy 후보 |
| [#8](https://github.com/DrivePersona/character-pack-sdk/pull/8) | MERGED | 컨셉과 맞지 않는 Doha GLB 교체 |
| [#9](https://github.com/DrivePersona/character-pack-sdk/pull/9) | MERGED | VoiceDesign expansion Meshy 후보 7/10 |
| [#12](https://github.com/DrivePersona/character-pack-sdk/pull/12) | MERGED | manifest `voicePackId` |
| [#13](https://github.com/DrivePersona/character-pack-sdk/pull/13) | MERGED | one-shot gesture 검증/재생 |
| [#14](https://github.com/DrivePersona/character-pack-sdk/pull/14) | MERGED | character metadata를 app으로 dispatch |
| [#15](https://github.com/DrivePersona/character-pack-sdk/pull/15) | MERGED | release gesture contract 통합 |
| [#16](https://github.com/DrivePersona/character-pack-sdk/pull/16) | MERGED | 내부 미리보기 GLB 후보 |
| [#17](https://github.com/DrivePersona/character-pack-sdk/pull/17) | MERGED | VoiceDesign 후보 캐릭터와 voice pack 연결 |

조회 시 열린 이슈는 [#10](https://github.com/DrivePersona/character-pack-sdk/issues/10) (release manifest의 voicePackId) 및 [#11](https://github.com/DrivePersona/character-pack-sdk/issues/11) (대사-gesture 계약과 애니메이션 출시)이다.

## 6. 이슈 기록 점검 및 후속 조치

| 이슈 | 상태 | PRD/프로젝트 판정 |
| --- | --- | --- |
| app [#3](https://github.com/DrivePersona/Vroomi/issues/3) — release roster | OPEN | roster 0개인 현재 출시 blocker와 일치. 전 캐릭터 자격 승인 후 닫아야 함 |
| app [#4](https://github.com/DrivePersona/Vroomi/issues/4) — character/voice binding | OPEN | PR #24 구현과 겹친다. 구현 완료 사실을 이슈에 링크하고 남은 문제/완료 여부를 maintainer가 확인해 상태 정리 필요 |
| app [#5](https://github.com/DrivePersona/Vroomi/issues/5) — 13-clip gate | OPEN | PR #25 구현의 자동 검증과 전체 33×13 승인 음원/권리 준비를 구분. 검증 코드 완료만으로 콘텐츠 완료 처리 금지 |
| app [#7](https://github.com/DrivePersona/Vroomi/issues/7) — Pleos 13-clip E2E | CLOSED | 현재 기록상 13개 승인 WAV로 캐릭터 선택→재생한 완료 증거는 없음. 댓글에는 “아직 전제 미충족/미완료”가 남아 있어 닫힘 상태와 완료 기준이 충돌한다. 최종 E2E를 다시 열거나 별도 follow-up 이슈로 추적해야 함 |
| app [#8](https://github.com/DrivePersona/Vroomi/issues/8) — 안정성/lifecycle | CLOSED | PR #27은 진단/회귀 범위를 추가했으나 문서에는 soak 및 Pleos lifecycle 검증을 미완료로 기록. 실제 증거를 보강하거나 후속 issue로 관리 필요 |
| app [#9](https://github.com/DrivePersona/Vroomi/issues/9) — speech/gesture sync | OPEN | PR #28은 시작 callback 기반 sync를 구현. issue 요구의 per-clip offset/실물 GLB 시연까지 끝났는지 명확히 나눠 완료 조정 필요 |
| app [#10–14](https://github.com/DrivePersona/Vroomi/issues) — vehicle intake/state | CLOSED | 코드 작업은 통합됐지만 특정 속성은 이미지/차종에 따라 unavailable. 지원 범위표와 미지원 실측을 유지해야 함 |
| app [#16](https://github.com/DrivePersona/Vroomi/issues/16) — dialogue content | OPEN | public draft는 10캐릭터 discussion용. 33개×13 출시 원본과 private 제작물 완료로 오해하지 않도록 단일 authoritative matrix 위치를 지정해야 함 |
| app [#36](https://github.com/DrivePersona/Vroomi/issues/36) — overlay runtime without Activity | OPEN | Pleos 지원 시작 진입점과 WindowManager runtime 생명주기; Activity는 명시적 설정/복구에서만 사용 |
| app [#37](https://github.com/DrivePersona/Vroomi/issues/37) — floating controls and overlay sheets | OPEN | 캐릭터 anchor 메뉴, 캐릭터 목록/보관함/대사/설정 overlay 구현. #3/#4/#9/#16의 roster, binding, audio-gesture 계약을 재정의하지 않음 |
| app [#38](https://github.com/DrivePersona/Vroomi/issues/38) — Pleos AVD overlay-first E2E | OPEN | 실제 HMI에서 Activity 비노출, safe bounds, 제스처, D/P·unknown 숨김/복귀와 PR 증적 확인. #9의 음성-gesture 동기화 증거와 별도 |
| app [#39](https://github.com/DrivePersona/Vroomi/issues/39) — acquisition policy | OPEN | 5+11 표시와 실제 draw/grant 활성화 사이의 정책 결정. 결제·확률·재화·중복 규칙은 승인 전까지 미정 |
| voice [#5](https://github.com/DrivePersona/voice-pack-sdk/issues/5) | OPEN | 429개 canonical clip, 청취 승인, rights, publish 상태를 추적하는 주 작업 |
| character [#10](https://github.com/DrivePersona/character-pack-sdk/issues/10) | OPEN | required release `voicePackId`의 schema/manifest 범위 조정 및 compatibility 확인 |
| character [#11](https://github.com/DrivePersona/character-pack-sdk/issues/11) | OPEN | 실제 GLB gesture 계약/애니메이션 제작 및 시연 증거 |

### 중복/종료 권고

- App #4의 자동 binding 코드는 PR #24로 병합됐으므로 미완료 잔여분을 구체화하거나 구현 범위가 끝났다면 PR 링크를 붙여 이슈를 정리한다.
- App #5의 릴리스 검사 코드와 실제 음원·권리 준비를 별도 체크리스트로 분리한다. 같은 acceptance criteria로 voice SDK #5와 중복 추적하지 않도록 App #5는 앱 gate 동작, Voice #5는 콘텐츠 생성/승인에 한정한다.
- App #7은 이전 댓글과 close 상태가 모순된다. GitHub 상태를 수정하지 않았으며, 실제 승인 WAV E2E가 끝났다는 증거를 확인한 뒤 close/reopen을 정리한다.
- App #8/#9도 병합된 코드와 현장/GLB 증거를 구분한다. PR 존재만으로 runtime test completion으로 세지 않는다.
- PR #31/#34의 캡처는 차량 callback/내부 후보 화면 확인에 사용되며, 승인 음성 WAV 13개와 선택 캐릭터 GLB gesture의 E2E 증거를 대신하지 않는다.

## 7. 무엇이 완료됐고 무엇이 남았나

### 구현 계약상 완료

- 3-repository 분리와 pack-to-app publish/compatibility PR 경로
- capability 기반 차량 입력, 주요 편의·공조·에너지 구독, 이벤트 speech policy
- release roster 검증과 character → exact voicePackId binding
- 내부 후보 picker, 음성 대사 13-slot 상태 표시
- clip playback start/done/stop, one-shot gesture 및 overlay lifecycle 연계 기반
- Virtual Console과 Pleos AVD에서 브레이크/방향지시등/P↔D를 확인하는 절차와 기존 증거

### 제품 출시 전 필수

1. 33개 target 모두에 대한 캐릭터 디자인/ID/권리 승인과 release-grade GLB 검수
2. 정확히 연결된 33개 voice pack과 각 팩의 13 canonical WAV (429개), 대사/발음/클립 끝 청취 검수
3. 캐릭터/음성 이용 동의·상업 권리 증빙 및 manifest metadata 정리
4. release roster 33개를 채우고 33개 모두가 실제 runtime gate를 통과하는 빌드
5. Pleos AVD에서 선택 → 대사 탭 실제 WAV → 캐릭터 gesture → 기존 Virtual Console 이벤트 반응 → D/P 전환을 캡처/logcat과 함께 검증
6. Android lifecycle/overlay/audio focus 장시간 안정성 검증
7. app #3–9의 구현/증거/콘텐츠 책임을 중복 없이 정리하고, 모순된 issue close 상태를 검증 증거와 맞춤
8. app #36–39에서 overlay runtime, floating interaction, Pleos AVD 증적, acquisition policy를 순서대로 완료

이 문서는 현재 열린 PR/issue 상태를 자동으로 바꾸지 않는다. 이 표를 기준으로 후속 GitHub issue 업데이트와 close/reopen을 할 때는 각 항목에 실제 결과물과 검증 증거를 연결한다.
