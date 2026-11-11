package com.example.voicecompanion.domain.model

/** 같은 순간에 여러 이벤트가 오면 순서가 앞선 쪽 하나만 말한다. 기획서 §6: 안전 > 상태 보고 > 연출. */
enum class Rank { SAFETY, STATUS, FLOURISH }

/**
 * 반응할 차량 이벤트. 대사는 기획서 v0.2 §5 예시 그대로다.
 * ponytail: cooldownMs는 기획서에서 N 미정 — 임시값. 에뮬레이터 실측 뒤 조정.
 */
enum class VehicleEvent(
    val label: String,
    val rank: Rank,
    val cooldownMs: Long,
    val face: String,
    val defaultLine: String,
    val neutral: Boolean = false, // 연출 없이 중립 음성으로(§6 "안전 이벤트는 연출하지 않습니다")
    val speakAfterStop: Boolean = false, // 주행 중엔 버리지 않고 보관했다가 정차한 뒤에 말한다(주행 중 축하 말은 하지 않는다)
) {
    BRAKE("브레이크 밟기", Rank.FLOURISH, 10_000, "😱", "으앗!"),
    TURN("방향지시등 켜기", Rank.STATUS, 8_000, "😉", "깜빡이 켰어~"),
    MODE("드라이브 모드 바꾸기", Rank.FLOURISH, 3_000, "😎", "모드 체인지!"),
    TIRE("타이어 공기압 낮음", Rank.STATUS, 60_000, "🥺", "발이 좀 아파…"),
    WARNING("차량 경고", Rank.SAFETY, 10_000, "😐", "차량 경고가 있어요.", neutral = true),
    LIGHT("조명 변경", Rank.FLOURISH, 0, "💡", "조명 설정이 바뀌었어."),
    WINDOW("창문 변경", Rank.FLOURISH, 0, "🚘", "창문 설정이 바뀌었어."),
    DOOR("도어 변경", Rank.FLOURISH, 0, "🚪", "도어 상태가 바뀌었어."),
    COMFORT("공조·편의 설정 변경", Rank.FLOURISH, 0, "🌡️", "편의 설정이 바뀌었어."),
    ENERGY_SUFFICIENT("에너지 여유", Rank.STATUS, 60_000, "🔋", "에너지가 충분해."),
    ENERGY_COMFORTABLE("에너지 보통", Rank.STATUS, 60_000, "🔋", "에너지 상태 괜찮아."),
    ENERGY_ATTENTION("에너지 확인", Rank.STATUS, 60_000, "🔋", "에너지를 확인해 줘."),
    ENERGY_URGENT("에너지 부족 경고", Rank.SAFETY, 60_000, "😐", "에너지가 부족합니다. 충전 또는 주유가 필요해요.", neutral = true),
    CHARGE_PORT("충전구 상태", Rank.STATUS, 10_000, "🔌", "충전구 상태가 바뀌었어."),

    // 앱이 신호값에서 직접 만든 한 줄 대사. 음성 팩 슬롯이 아니라 기본 음성(SYSTEM_TTS)이 읽고 몸짓은 없다.
    // ponytail: 쿨다운·말하는 기준은 임시값이다. 실차·에뮬레이터 실측 뒤에 조정한다.
    HIGH_BEAM_ON("상향등 켜짐", Rank.STATUS, 30_000, "💡", "상향등이 켜졌어요.", neutral = true),
    LOW_BEAM_ON("하향등 켜짐", Rank.STATUS, 30_000, "💡", "하향등이 켜졌어요.", neutral = true),
    // 하향등과 함께 켜지는 후미등이 같은 창에서 하향등을 이기지 않게 한 단계 낮춘다.
    TAIL_LAMP_ON("후미등 켜짐", Rank.FLOURISH, 30_000, "💡", "후미등이 켜졌어요.", neutral = true),
    FRONT_FOG_ON("앞 안개등 켜짐", Rank.STATUS, 30_000, "🌫️", "앞 안개등이 켜졌어요.", neutral = true),
    REAR_FOG_ON("뒤 안개등 켜짐", Rank.STATUS, 30_000, "🌫️", "뒤 안개등이 켜졌어요.", neutral = true),
    SHIFT_TO_DRIVE("기어 D로 변경", Rank.STATUS, 30_000, "🚗", "출발해요.", neutral = true),
    SHIFT_TO_PARK("기어 P로 변경", Rank.STATUS, 30_000, "🅿️", "도착했어요.", neutral = true),
    SHIFT_TO_REVERSE("기어 R로 변경", Rank.STATUS, 30_000, "↩️", "후진 중이에요. 뒤를 살펴 주세요.", neutral = true),
    CRUISE_ON("크루즈 켜짐", Rank.STATUS, 10_000, "🛣️", "크루즈가 켜졌어요.", neutral = true),
    CRUISE_OFF("크루즈 해제", Rank.SAFETY, 10_000, "🛣️", "크루즈가 해제됐어요. 직접 운전해 주세요.", neutral = true),
    CRUISE_TARGET_SET("크루즈 속도 변경", Rank.STATUS, 10_000, "⏱️", "크루즈 속도를 바꿨어요.", neutral = true),
    STEER_BIG("핸들 크게 꺾음", Rank.FLOURISH, 60_000, "🛞", "핸들을 크게 꺾었어요. 주변을 살펴 주세요.", neutral = true),
    ODO_MILESTONE("주행거리 이정표", Rank.FLOURISH, 60_000, "🏁", "누적 주행거리 이정표를 지났어요.", neutral = true, speakAfterStop = true),
    RANGE_LOW("남은 거리 적음", Rank.STATUS, 300_000, "⛽", "주행 가능 거리가 줄었어요.", neutral = true),
    RANGE_CRITICAL("남은 거리 매우 적음", Rank.SAFETY, 120_000, "⛽", "주행 가능 거리가 많이 부족해요. 충전이나 주유를 준비해 주세요.", neutral = true),
}

enum class DropReason { COOLDOWN, PRIORITY, FOCUS, DRIVING_SILENT, POLICY_SILENT }

/** 차량 이벤트가 어떤 종류의 음성 경로를 사용할 수 있는지 고정한 정책 테이블. */
enum class SpeechOutput { CHARACTER_CLIP, NEUTRAL_CLIP, SYSTEM_TTS, SILENCE }

enum class SpeechContent { PREDEFINED_EVENT, EXACT_VALUE, DIAGNOSTIC, UNSUPPORTED }

object VehicleSpeechPolicy {
    private val eventOutputs = mapOf(
        VehicleEvent.BRAKE to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.TURN to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.MODE to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.TIRE to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.WARNING to SpeechOutput.NEUTRAL_CLIP,
        VehicleEvent.LIGHT to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.WINDOW to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.DOOR to SpeechOutput.CHARACTER_CLIP,
        VehicleEvent.COMFORT to SpeechOutput.CHARACTER_CLIP,
        // The v1 release matrix has one canonical ENERGY_LOW clip; other energy bands stay silent.
        VehicleEvent.ENERGY_SUFFICIENT to SpeechOutput.SILENCE,
        VehicleEvent.ENERGY_COMFORTABLE to SpeechOutput.SILENCE,
        VehicleEvent.ENERGY_ATTENTION to SpeechOutput.SILENCE,
        VehicleEvent.ENERGY_URGENT to SpeechOutput.NEUTRAL_CLIP,
        VehicleEvent.CHARGE_PORT to SpeechOutput.CHARACTER_CLIP,
        // 신호값에서 직접 만든 한 줄 대사는 비공개 음성 팩의 슬롯이 아니라서 기본 음성이 읽는다(몸짓 없음).
        VehicleEvent.HIGH_BEAM_ON to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.LOW_BEAM_ON to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.TAIL_LAMP_ON to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.FRONT_FOG_ON to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.REAR_FOG_ON to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.SHIFT_TO_DRIVE to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.SHIFT_TO_PARK to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.SHIFT_TO_REVERSE to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.CRUISE_ON to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.CRUISE_OFF to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.CRUISE_TARGET_SET to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.STEER_BIG to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.ODO_MILESTONE to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.RANGE_LOW to SpeechOutput.SYSTEM_TTS,
        VehicleEvent.RANGE_CRITICAL to SpeechOutput.SYSTEM_TTS,
    )

    /** 일반 이벤트는 캐릭터 클립을 우선 사용한다. 안전 이벤트는 중립 문구의 팩 클립을 우선한다. */
    fun outputFor(event: VehicleEvent): SpeechOutput = eventOutputs.getValue(event)

    /** Runtime names adapt to the canonical IDs consumed by voice and character manifests. */
    fun canonicalEventId(event: VehicleEvent): String = when (event) {
        VehicleEvent.LIGHT -> "AMBIENT_LIGHT"
        VehicleEvent.COMFORT -> "HVAC"
        VehicleEvent.CHARGE_PORT -> "CHARGE"
        VehicleEvent.ENERGY_URGENT -> "ENERGY_LOW"
        else -> event.name
    }

    /** 실시간 수치·진단·지원하지 않는 콘텐츠는 사용자 요청형 TTS 기능이 생기기 전까지 말하지 않는다. */
    fun outputFor(content: SpeechContent): SpeechOutput = when (content) {
        SpeechContent.PREDEFINED_EVENT -> SpeechOutput.CHARACTER_CLIP
        SpeechContent.EXACT_VALUE, SpeechContent.DIAGNOSTIC, SpeechContent.UNSUPPORTED -> SpeechOutput.SILENCE
    }

    val characterClipEventIds: Set<String>
        get() = eventOutputs.filterValues { it == SpeechOutput.CHARACTER_CLIP }.keys.map(::canonicalEventId).toSet()

    /** 안전 문구도 WAV로 제공할 수 있지만 캐릭터 gesture는 허용하지 않는다. */
    val neutralClipEventIds: Set<String>
        get() = eventOutputs.filterValues { it == SpeechOutput.NEUTRAL_CLIP }.keys.map(::canonicalEventId).toSet()

    val voicePackClipEventIds: Set<String>
        // WIPER clips exist in the new character packs and release dialogue matrix, even though
        // the current vehicle adapter reports wiper state as data only (no reaction event yet).
        get() = characterClipEventIds + neutralClipEventIds + "WIPER"

    val characterGestureEventIds: Set<String>
        get() = characterClipEventIds

    val safetyEvents: Set<VehicleEvent>
        get() = eventOutputs.filterValues { it == SpeechOutput.NEUTRAL_CLIP }.keys
}

val VehicleEvent.isEnergyEvent: Boolean
    get() = name.startsWith("ENERGY_")

sealed interface LogEntry {
    val event: VehicleEvent

    /** 말하기를 '요청'했다. 실제로 재생됐는지는 플랫폼이 따로 알려 준다. */
    data class Speak(override val event: VehicleEvent, val text: String) : LogEntry

    data class Drop(
        override val event: VehicleEvent,
        val why: DropReason,
        val waitMs: Long = 0,
        val by: VehicleEvent? = null,
    ) : LogEntry
}
