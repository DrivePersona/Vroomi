---
type: spec
update: rewrite
---

# Speech and gesture synchronization

For a selected READY character/voice pair, clip playback's `재생됨` callback starts the one-shot gesture mapped from the same canonical `eventId`. The resolver accepts only a character manifest mapping whose gesture exists and is declared `oneShot`. Greetings, neutral safety clips, unmapped events, and playback fallbacks do not start a gesture.

The same callback path is used for dialogue previews and vehicle-event speech. When audio finishes, is replaced, loses focus, or the vehicle enters Drive, the runtime returns the character to its idle animation. The character SDK uses the declared gesture duration to stop the GLB one-shot and hold its last pose until the runtime changes it to idle.

The voice-pack schema can declare per-clip `gestureId` and `startOffsetMs`; the selected variant callback lets the app choose that clip's gesture. The runtime starts it after the declared offset and returns to idle when audio ends. The LivLiv view executes the resolved gesture name from the character manifest.

## LivLiv mouth synchronization

The internal LivLiv character uses the exact PCM submitted to `AudioTrack`: the voice-pack runtime calculates one 0–255 RMS mouth level per 20 ms, including the 150 ms silent preroll. After playback starts, `SpeakRequest.onClipPlayback` provides this envelope and the track's playback position. The app samples the envelope every 40 ms and calls `LivLivSurfaceView.setMouthOpen(0..1)`. Missing playback position keeps the mouth closed. The view retains only the latest value while its WebView loads.

This applies to parked character clips, dialogue previews, touch lines, and greetings. Neutral safety speech, system TTS, fallback speech, and driving do not animate the mouth. Completion, interruption, audio-focus stop, character change, and overlay removal close it. The app keeps audio playback independent of the WebView; a visual callback failure cannot prevent speech.

The start/end contract and PCM envelope are covered by unit tests. Pleos AVD verification must separately confirm SDK vehicle input, selected clip playback, visible mouth motion, and recovery after interruption. The local standalone APK evidence is not a Vroomi `main` validation.

## TOUCH (사용자 탭) 슬롯

- `TOUCH`는 차량 이벤트가 아니라 팩 슬롯이다. `VehicleSpeechPolicy.voicePackClipEventIds`와 `characterGestureEventIds`에 들어 있고 `characterClipEventIds`(차량 반응)에는 없다.
- 음성 팩 `clips.TOUCH`는 줄마다 `gestureId`(`touch_react`, `touch_react_2`, `touch_react_3`)를 갖고, 캐릭터 팩 `supportedGestures`가 같은 ID를 클립에 연결한다.
- 탭마다 줄이 1→2→3→1 순서로 돌아가며(`touchVariant`) 말하고, 몸짓은 재생 시작 신호 뒤에 같은 번호의 클립으로 시작한다. 쿨다운 1.5초·주행 중 없음은 그대로다.
- 이 슬롯을 가진 음성 팩은 앱이 TOUCH를 허용한 버전 이후에만 읽힌다(허용 목록에 없으면 팩이 INVALID가 된다).
