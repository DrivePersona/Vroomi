---
type: spec
update: rewrite
---

# Speech and gesture synchronization

For a selected READY character/voice pair, clip playback's `재생됨` callback starts the one-shot gesture mapped from the same canonical `eventId`. The resolver accepts only a character manifest mapping whose gesture exists and is declared `oneShot`. Greetings, neutral safety clips, unmapped events, and playback fallbacks do not start a gesture.

The same callback path is used for dialogue previews and vehicle-event speech. When audio finishes, is replaced, loses focus, or the vehicle enters Drive, the runtime returns the character to its idle animation. The character SDK uses the declared gesture duration to stop the GLB one-shot and hold its last pose until the runtime changes it to idle.

The current voice-pack schema has no per-clip `gestureId` or `startOffsetMs`, and automatic round-robin playback does not expose its chosen variant index to the app. Therefore this implementation starts the character's event-mapped gesture at the audio-start callback (offset zero). Per-clip offsets and variant-specific gesture IDs need a voice-pack schema/API change before they can be enforced. A Pleos AVD is also required to record actual audio/Filament timing evidence.

## TOUCH (사용자 탭) 슬롯

- `TOUCH`는 차량 이벤트가 아니라 팩 슬롯이다. `VehicleSpeechPolicy.voicePackClipEventIds`와 `characterGestureEventIds`에 들어 있고 `characterClipEventIds`(차량 반응)에는 없다.
- 음성 팩 `clips.TOUCH`는 줄마다 `gestureId`(`touch_react`, `touch_react_2`, `touch_react_3`)를 갖고, 캐릭터 팩 `supportedGestures`가 같은 ID를 클립에 연결한다.
- 탭마다 줄이 1→2→3→1 순서로 돌아가며(`touchVariant`) 말하고, 몸짓은 재생 시작 신호 뒤에 같은 번호의 클립으로 시작한다. 쿨다운 1.5초·주행 중 없음은 그대로다.
- 이 슬롯을 가진 음성 팩은 앱이 TOUCH를 허용한 버전 이후에만 읽힌다(허용 목록에 없으면 팩이 INVALID가 된다).
