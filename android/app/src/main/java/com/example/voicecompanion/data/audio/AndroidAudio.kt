package com.example.voicecompanion.data.audio

import com.example.voicecompanion.voicepack.data.audio.AudioTrackOutput
import com.example.voicecompanion.voicepack.domain.BuiltinPack
import com.example.voicecompanion.voicepack.domain.PcmOutput
import com.example.voicecompanion.voicepack.domain.VoicePack


import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import com.example.voicecompanion.domain.usecase.FocusChange

/**
 * 오디오 포커스 + 기본 음성. 콜백은 아무 스레드에서나 올 수 있다.
 * 기본 음성은 Pleos 차량에선 Pleos TextToSpeech SDK([PleosSpeech]), 그 밖(폰)에선 안드로이드 TextToSpeech([AndroidTtsVoice])다.
 * 어느 팩이 말할지는 CompanionRuntime이 PackSelector로 정한다.
 */
class AndroidAudio(
    context: Context,
    private val onFocusChange: (FocusChange) -> Unit,
    onState: (String) -> Unit, // 음성 합성 준비 상태가 바뀔 때
) {
    private val audio = context.getSystemService(AudioManager::class.java)

    // ponytail: 서드파티 앱이 쓸 오디오 컨텍스트가 문서에 없어 일반 미디어(MUSIC)+음성으로 둔다. Pleos 확인 뒤 usage만 바꾼다.
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { change ->
            val mapped = when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> FocusChange.GAIN
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusChange.CAN_DUCK
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> FocusChange.LOSS_TRANSIENT
                AudioManager.AUDIOFOCUS_LOSS -> FocusChange.LOSS
                else -> null
            }
            Log.i(TAG, "오디오 포커스 변화: $mapped (raw=$change)")
            // 모르는 값은 안전하게 멈추는 쪽으로 처리한다(이전과 같은 동작)
            onFocusChange(mapped ?: FocusChange.LOSS)
        }
        .build()

    /** 기본 음성을 감싼 팩. 항상 있고 모든 폴백의 끝이다. */
    val builtinPack: VoicePack = BuiltinPack(
        if (PleosSpeech.isAvailable(context)) PleosSpeech(context, attrs, onState) else AndroidTtsVoice(context, attrs, onState),
    )

    /** 클립 팩이 쓸 출력. 음성과 같은 오디오 속성으로 AudioTrack을 연다. */
    fun newPcmOutput(): PcmOutput = AudioTrackOutput(attrs)

    /** 말하기 전에 부른다. 허가되면 true. */
    fun requestFocus(): Boolean {
        val result = audio.requestAudioFocus(focusRequest)
        Log.i(TAG, "오디오 포커스 요청: usage=MEDIA gain=TRANSIENT_MAY_DUCK → " + (if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "허가" else "거절($result)"))
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonFocus() {
        audio.abandonAudioFocusRequest(focusRequest)
    }

    private companion object {
        const val TAG = "VoiceCompanion"
    }
}
