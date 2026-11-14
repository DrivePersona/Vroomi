package com.example.voicecompanion.app

import com.example.voicecompanion.BuildConfig
import com.example.voicecompanion.data.audio.AndroidAudio
import com.example.voicecompanion.data.repository.PleosVehicleRepository
import com.example.voicecompanion.domain.Platform
import com.example.voicecompanion.domain.ReactionEngine
import com.example.voicecompanion.domain.model.DropReason
import com.example.voicecompanion.domain.model.LogEntry
import com.example.voicecompanion.domain.model.VehicleEvent
import com.example.voicecompanion.domain.model.VehicleInput
import com.example.voicecompanion.domain.model.VehicleDataCatalog
import com.example.voicecompanion.domain.model.VehicleDataState
import com.example.voicecompanion.domain.model.VehicleEnergyProfile
import com.example.voicecompanion.domain.model.VehicleAvailability
import com.example.voicecompanion.domain.model.VehicleSpeechPolicy
import com.example.voicecompanion.domain.model.SpeechOutput
import com.example.voicecompanion.domain.model.isEnergyEvent
import com.example.voicecompanion.domain.model.VoicePreset
import com.example.voicecompanion.domain.repository.VehicleRepository
import com.example.voicecompanion.domain.usecase.ProcessVehicleInputUseCase
import com.example.voicecompanion.domain.usecase.VehicleInputAction
import com.example.voicecompanion.domain.usecase.FocusChange
import com.example.voicecompanion.domain.usecase.handleFocusChange
import com.example.voicecompanion.presentation.model.CompanionUiState
import com.example.voicecompanion.presentation.model.ReactionLogItem
import com.example.voicecompanion.presentation.model.VoiceDialogueRow
import com.example.voicecompanion.presentation.CompanionUiRuntime
import com.example.voicecompanion.app.release.ReleaseRoster
import com.example.voicecompanion.app.release.CharacterDialogueMatrix
import com.example.voicecompanion.app.release.CharacterGestureResolver
import com.example.voicecompanion.ui.overlay.CharacterOverlayHost
import com.example.voicecompanion.ui.overlay.OverlayShowResult
import com.example.voicecompanion.ui.overlay.VrOverlayInput
import com.example.voicecompanion.ui.overlay.OverlayPort
import com.example.voicecompanion.ui.overlay.VrOverlayPolicy
import com.example.voicecompanion.domain.model.CharacterCollection
import com.example.voicecompanion.domain.model.CharacterRef
import com.example.voicecompanion.presentation.model.OverlayUiState
import com.example.voicecompanion.voicepack.data.AssetPackSource
import com.example.voicecompanion.voicepack.data.PackRepository
import com.example.voicecompanion.voicepack.domain.ClipPackFactory
import com.example.voicecompanion.voicepack.domain.PackSelector
import com.example.voicecompanion.voicepack.domain.ClipGesture
import com.example.voicecompanion.voicepack.domain.GREETING_EVENT
import com.example.voicecompanion.voicepack.domain.SpeakRequest
import com.example.voicecompanion.voicepack.domain.VoicePack
import com.example.voicecompanion.characterpack.data.AssetCharacterPackSource
import com.example.voicecompanion.characterpack.data.CharacterPackRepository
import com.example.voicecompanion.characterpack.domain.CharacterPackSelector
import com.example.voicecompanion.characterpack.model.CharacterPack
import com.example.voicecompanion.characterpack.model.CharacterPackState
import com.example.voicecompanion.characterpack.model.isRenderable


import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 반응 기록 한 줄. 재생 결과('재생됨'·'끊김')는 나중에 note에 덧붙는다. */
class LogRow(val time: String, val text: String) {
    var note by mutableStateOf("")
}

/**
 * 앱이 사는 동안 하나. 화면(Activity)이 닫혀도 — 주행 중엔 UI가 막힌다 — 엔진과 차량 연결은 이어진다.
 * 상태는 전부 Compose 상태라 화면이 다시 만들어져도 그대로 보인다.
 */
class CompanionRuntime(private val app: Application) : Platform, CompanionUiRuntime {
    private val main = Handler(Looper.getMainLooper())
    private val prefs = app.getSharedPreferences("voice-companion", Context.MODE_PRIVATE)
    private val vehicleInputs: VehicleRepository = PleosVehicleRepository(app)
    private val processVehicleInput = ProcessVehicleInputUseCase()
    private val vehicleCatalogLock = Any()
    private var pendingVehicleCatalog: List<VehicleDataState>? = null
    private var vehicleCatalogPublishScheduled = false
    private val vehicleStatusLock = Any()
    private val pendingVehicleStatuses = mutableMapOf<VehicleInput.Source, String>()
    private var vehicleStatusPublishScheduled = false
    private val lastSpeakRow = HashMap<VehicleEvent, LogRow>()
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)

    // 화면 상태
    var driving by mutableStateOf(false)
        private set
    var voiceWhileDriving by mutableStateOf(true)
        private set
    var navBusy by mutableStateOf(false)
        private set
    var face by mutableStateOf("🙂")
        private set
    var bubble by mutableStateOf(IDLE_BUBBLE)
        private set
    var preset by mutableStateOf(VoicePreset.BRIGHT)
        private set
    var pleosStatus by mutableStateOf("시작 전")
        private set
    var vehicleData by mutableStateOf(VehicleDataCatalog.initialState)
        private set
    var carUxStatus by mutableStateOf("시작 전")
        private set
    var ttsStatus by mutableStateOf("음성 합성을 준비하는 중이에요")
        private set
    val typedLines = mutableStateMapOf<VehicleEvent, String>()
    val logs = mutableStateListOf<LogRow>()

    // 위 상태 프로퍼티보다 뒤에 둔다. 앞에 두면 생성 중에 콜백이 아직 만들어지지 않은 상태 값을 건드려 NPE가 난다.
    private val audio = AndroidAudio(
        app,
        onFocusChange = { change -> onMain { onFocusChange(change) } },
        onState = { s -> onMain { ttsStatus = s } },
    )

    // 어느 팩이 말할지·대사가 무엇인지는 여기서 정한다. 설치된 파일 팩은 시작할 때 백그라운드에서 읽어 채운다.
    @Volatile
    private var installed: List<VoicePack> = emptyList()
    private var candidateVoicePacks: List<VoicePack> = emptyList()
    private val packs = PackSelector(audio.builtinPack) { installed }
    private val repository = PackRepository(
        source = AssetPackSource(app.assets, "voicepacks"),
        factories = mapOf(ClipPackFactory.KIND to ClipPackFactory(newOutput = audio::newPcmOutput)),
        allowedEvents = VehicleSpeechPolicy.voicePackClipEventIds,
        appVersion = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode,
    )
    private val releaseRoster = runCatching {
        app.assets.open(ReleaseRoster.ASSET_PATH).bufferedReader().use { ReleaseRoster.parse(it.readText()) }
    }.onFailure { Log.e(TAG, "출시 캐릭터 로스터를 읽지 못했어요. 캐릭터를 모두 숨깁니다.", it) }
        .getOrElse { ReleaseRoster(emptyList()) }
    // 캐릭터 팩(GLB·2D 스프라이트)은 음성 팩과 완전히 별개다. 표현 파일·선택·검증만 맡고 음성 경로에는 관여하지 않는다.
    @Volatile
    private var installedCharacters: List<CharacterPack> = emptyList()
    private var candidateCharacters: List<CharacterPack> = emptyList()
    private val characters = CharacterPackSelector { installedCharacters }
    private val characterRepository = CharacterPackRepository(
        source = AssetCharacterPackSource(app.assets, "characterpacks"),
        allowedEvents = VehicleSpeechPolicy.characterGestureEventIds,
        appVersion = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode,
    )
    private var speakSeq = 0 // 가장 최근 말하기 요청의 번호. 늦게 온 이전 요청의 done이 새 요청의 포커스를 놓지 않게 한다
    private var previewSeq = -1 // 주행 시작 시 진행 중인 대사 미리듣기를 끊는다
    private var engineSpeechSeq = 0 // 가장 최근 엔진 반응 음성(차량 신호 반응·안전 안내)의 요청 번호
    private var engineSpeaking = false // 그 음성이 아직 끝나지 않았다. TOUCH·시작 인사는 이 동안 말하지 않는다(안전 안내가 연출에 밀리면 안 된다)
    private var gestureStart: Runnable? = null

    // 출시 캐릭터는 manifest voicePackId, 내부 후보는 previewVoicePackId로 음성 팩을 결정한다.
    var activePack by mutableStateOf(audio.builtinPack)
        private set
    private var packsLoaded = false

    // 캐릭터 팩 화면 상태. 선택한 캐릭터는 설정 화면이 아닌 WindowManager 오버레이에만 올린다.
    var characterPackList by mutableStateOf<List<CharacterPack>>(emptyList())
        private set
    var activeCharacterPack by mutableStateOf<CharacterPack?>(null)
        private set
    var characterPacksLoaded by mutableStateOf(false)
        private set
    var characterPackNotice by mutableStateOf<String?>(null)
        private set
    var characterAnimation by mutableStateOf("idle")
        private set
    var vrEnabled by mutableStateOf(false)
        private set
    var vrOverlayStatus by mutableStateOf("VR 모드가 꺼져 있어요")
        private set
    var vrOverlayPermissionRequired by mutableStateOf(false)
        private set
    private var settingsVisible = false

    // 캐릭터 곁 오버레이(탭·floating 메뉴·시트)가 보는 상태와 하는 일. 가챠 보유는 획득 정책(#39)이 정해지기 전까지 어디서도 늘리지 않는다.
    private var ownedCharacterIds by mutableStateOf<Set<String>>(emptySet())
    private var greetingEnabled by mutableStateOf(true)
    private var greetingChanceUsed = false // 세션 첫 표시 때 한 번만 인사를 정한다. 그때 설정이 꺼져 있거나 말할 수 없어도 기회는 지나간 것이다
    private var lastTouchAt = -TOUCH_COOLDOWN_MS
    private val overlayPort = object : OverlayPort {
        override fun state() = overlayState()
        override fun touchCharacter() = this@CompanionRuntime.touchCharacter()
        override fun equipCharacter(characterId: String) = this@CompanionRuntime.equipCharacter(characterId)
        override fun previewDialogue(eventId: String, variantIndex: Int) = this@CompanionRuntime.previewDialogue(eventId, variantIndex)
        override fun setGreetingEnabled(enabled: Boolean) = changeGreetingEnabled(enabled)
        override fun setParkedAvatarVisible(enabled: Boolean) = changeVrEnabled(enabled)
    }
    private val characterOverlay = CharacterOverlayHost(app, prefs, overlayPort)

    /** 내부 디버그 APK에서만 후보 GLB를 확인한다. 출시 빌드는 반드시 로스터를 따른다. */
    private val internalCandidatePreview: Boolean
        get() = BuildConfig.DEBUG && releaseRoster.entries.isEmpty()

    /** 'VR 시작'을 눌러도 되는지. 이 값이 false면 설정 화면은 그대로 남는다. */
    val canStartVr: Boolean
        get() = vrEnabled &&
            !driving &&
            activeCharacterPack?.isRenderable == true &&
            characterOverlay.permissionGranted

    val packStatus: String?
        get() = activeCharacterPack?.let { character ->
            val linkedId = linkedVoicePackId(character)
            when {
                linkedId == activePack.id && character.info?.voicePackId != null -> activePack.name
                internalCandidatePreview && linkedId == activePack.id -> "내부 미리보기 · ${activePack.name}"
                internalCandidatePreview && linkedId == null -> "내부 미리보기 · 기본 음성"
                else -> "연결된 음성 팩을 사용할 수 없어요"
            }
        }

    val engine = ReactionEngine(
        platform = this,
        now = SystemClock::elapsedRealtime,
        schedule = { delayMs, block ->
            val r = Runnable(block)
            main.postDelayed(r, delayMs)
            val cancel: () -> Unit = { main.removeCallbacks(r) }
            cancel
        },
        maxVolume = MAX_VOLUME,
    )

    private var started = false
    override fun start() {
        if (started) return
        started = true
        // Remove the legacy independent voice choice. The active voice is now derived from the selected character manifest.
        prefs.edit().remove("voicepack.selected").apply()
        // overlay-first: 정차 중 캐릭터 표시는 항상 켠다. 끄는 UI가 없고(켜 둘 화면도 없다), 이전에 저장된 끔 값은 무시한다.
        vrEnabled = true
        ownedCharacterIds = prefs.getStringSet(PREF_OWNED_CHARACTERS, emptySet()).orEmpty().toSet()
        greetingEnabled = prefs.getBoolean(PREF_GREETING, true)
        VehicleEvent.entries.forEach { e ->
            val saved = prefs.getString("line.${e.name}", null)?.take(MAX_LINE)
            typedLines[e] = saved ?: e.defaultLine
        }
        refreshLines()
        prefs.getString("preset", null)?.let { name -> VoicePreset.entries.find { it.name == name }?.let { preset = it } }

        engine.setVoiceWhileDriving(voiceWhileDriving)
        // 자동차에선 주행 규제가 알려 줄 때까지 주행으로 본다(닫힌 쪽이 안전). 폰에선 정차로 시작해 시뮬레이터를 쓸 수 있게 한다.
        val automotive = app.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        changeDriving(automotive)
        // Pleos의 capability 조회와 구독 등록은 초기 콜백을 대량으로 만들 수 있다.
        // Compose 첫 프레임을 막지 않도록 시작 자체는 UI 스레드 밖에서 수행한다.
        Thread({ vehicleInputs.start(::acceptVehicleInput) }, "vehicle-input-start").apply {
            isDaemon = true
        }.start()
        Thread({
            val list = repository.load()
            onMain {
                candidateVoicePacks = list
                packsLoaded = true
                refreshPacks()
                refreshCharacterPacks()
                restoreCharacterPackIfLoaded()
            }
        }, "voicepack-load").apply { isDaemon = true }.start()
        Thread({
            val list = characterRepository.load()
            onMain {
                candidateCharacters = list
                characterPacksLoaded = true
                refreshPacks()
                refreshCharacterPacks()
                restoreCharacterPackIfLoaded()
            }
        }, "characterpack-load").apply { isDaemon = true }.start()
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    /** Repository가 SDK별 콜백을 공통 차량 입력 모델로 바꾼 뒤 이곳으로 보낸다. */
    private fun acceptVehicleInput(input: VehicleInput) {
        onMain {
            // Compose 상태(energyProfileAvailable 포함)는 메인 스레드에서만 읽는다.
            val action = processVehicleInput.execute(input, energyProfileAvailable())
            when (action) {
                is VehicleInputAction.Catalog -> scheduleVehicleCatalog(action.states)
                is VehicleInputAction.Status -> scheduleVehicleStatus(action.source, action.message)
                is VehicleInputAction.Emit -> if (!driving || action.allowWhileDriving) engine.emit(action.event)
                is VehicleInputAction.Driving -> changeDriving(action.isDriving)
                VehicleInputAction.Ignore -> Unit
            }
        }
    }

    private fun scheduleVehicleCatalog(states: List<VehicleDataState>) {
        synchronized(vehicleCatalogLock) {
            pendingVehicleCatalog = states
            if (vehicleCatalogPublishScheduled) return
            vehicleCatalogPublishScheduled = true
        }
        main.postDelayed({
            val latest = synchronized(vehicleCatalogLock) {
                vehicleCatalogPublishScheduled = false
                pendingVehicleCatalog.also { pendingVehicleCatalog = null }
            }
            if (latest != null) vehicleData = latest
        }, VEHICLE_CATALOG_DEBOUNCE_MS)
    }

    private fun scheduleVehicleStatus(source: VehicleInput.Source, message: String) {
        synchronized(vehicleStatusLock) {
            pendingVehicleStatuses[source] = message
            if (vehicleStatusPublishScheduled) return
            vehicleStatusPublishScheduled = true
        }
        main.postDelayed({
            val latest = synchronized(vehicleStatusLock) {
                vehicleStatusPublishScheduled = false
                pendingVehicleStatuses.toMap().also { pendingVehicleStatuses.clear() }
            }
            latest[VehicleInput.Source.CAR_UX]?.let { carUxStatus = it }
            latest[VehicleInput.Source.PLEOS_VEHICLE]?.let { pleosStatus = it }
        }, VEHICLE_CATALOG_DEBOUNCE_MS)
    }

    /** ViewModel이 수집하는 화면 전용 스냅샷. Compose 가변 상태를 바깥으로 노출하지 않는다. */
    override fun snapshot(): CompanionUiState = CompanionUiState(
        driving = driving,
        voiceWhileDriving = voiceWhileDriving,
        navBusy = navBusy,
        preset = preset,
        pleosStatus = pleosStatus,
        vehicleData = vehicleData,
        carUxStatus = carUxStatus,
        ttsStatus = ttsStatus,
        typedLines = typedLines.toMap(),
        logs = logs.map { ReactionLogItem(it.time, it.text, it.note) },
        packStatus = packStatus,
        dialogueRows = dialogueRows(),
        fixedLines = VehicleEvent.entries.associateWith(::fixedLine),
        characterPackList = characterPackList,
        activeCharacterPack = activeCharacterPack,
        characterPacksLoaded = characterPacksLoaded,
        characterPackNotice = characterPackNotice,
        vrEnabled = vrEnabled,
        vrOverlayStatus = vrOverlayStatus,
        vrOverlayPermissionRequired = vrOverlayPermissionRequired,
        canStartVr = canStartVr,
    )

    // ---- 입력 (Pleos·주행 규제·시뮬레이터 버튼이 같은 길로 들어온다) ----
    override fun emit(e: VehicleEvent) = onMain {
        if (!e.isEnergyEvent || energyProfileAvailable()) engine.emit(e)
    }

    private fun energyProfileAvailable(): Boolean = vehicleData.any {
        it.definition.dataKey == "vehicle-energy-profile" &&
            it.availability == VehicleAvailability.ACTIVE &&
            it.value?.let { value -> runCatching { VehicleEnergyProfile.valueOf(value) }.getOrNull() }?.let { it != VehicleEnergyProfile.UNAVAILABLE } == true
    }

    override fun burst() = onMain { listOf(VehicleEvent.BRAKE, VehicleEvent.TURN, VehicleEvent.WARNING).forEach(engine::emit) }

    override fun changeDriving(value: Boolean) = onMain {
        driving = value
        engine.setDriving(value)
        if (value) {
            cancelGesture()
            if (previewSeq == speakSeq) stop()
        } else {
            ensureDefaultCharacter()
        }
        syncCharacterOverlay()
    }

    override fun changeVoiceWhileDriving(value: Boolean) = onMain {
        voiceWhileDriving = value
        engine.setVoiceWhileDriving(value)
    }

    override fun changeNavBusy(value: Boolean) = onMain {
        navBusy = value
        if (value) engine.focusLost()
    }

    override fun choosePreset(value: VoicePreset) {
        preset = value
        prefs.edit().putString("preset", value.name).apply()
    }

    override fun setLine(e: VehicleEvent, text: String) {
        if (VehicleSpeechPolicy.outputFor(e) == SpeechOutput.NEUTRAL_CLIP) return
        val t = text.take(MAX_LINE)
        typedLines[e] = t
        engine.setLine(e, packs.lineFor(VehicleSpeechPolicy.canonicalEventId(e), t))
        prefs.edit().putString("line.${e.name}", t).apply()
    }

    /** 활성 팩이 정한 대사(자유 문장 팩이면 사용자 대사 그대로)를 엔진에 넣는다. */
    private fun refreshLines() {
        VehicleEvent.entries.forEach { e ->
            val fallback = if (VehicleSpeechPolicy.outputFor(e) == SpeechOutput.NEUTRAL_CLIP) e.defaultLine
            else typedLines[e] ?: e.defaultLine
            engine.setLine(e, packs.lineFor(VehicleSpeechPolicy.canonicalEventId(e), fallback))
        }
    }

    private fun dialogueRows(): List<VoiceDialogueRow> {
        return CharacterDialogueMatrix.rows(linkedVoicePackId(activeCharacterPack), activePack)
    }

    private fun linkedVoicePackId(character: CharacterPack?): String? = character?.info?.voicePackId
        ?: character?.info?.previewVoicePackId?.takeIf { internalCandidatePreview }

    override fun clearLogs() = logs.clear()

    // ---- 음성 팩 ----
    /** 선택기의 상태를 화면 상태로 옮기고 대사를 다시 채운다. 설치된 팩을 읽은 뒤와 팩을 고른 뒤에 부른다. */
    private fun refreshPacks() {
        installed = if (internalCandidatePreview) candidateVoicePacks
        else releaseRoster.eligibleVoicePacks(candidateCharacters, candidateVoicePacks)
        activePack = packs.active()
        refreshLines()
    }

    // ---- 캐릭터 팩 ----
    private fun refreshCharacterPacks() {
        installed = if (internalCandidatePreview) candidateVoicePacks
        else releaseRoster.eligibleVoicePacks(candidateCharacters, candidateVoicePacks)
        releaseRoster.entries.forEach { entry ->
            val character = candidateCharacters.firstOrNull { it.id == entry.characterId }
            val voicePack = candidateVoicePacks.firstOrNull { it.id == entry.voicePackId }
            val failures = ReleaseRoster.eligibilityFailures(entry, character, voicePack)
            if (failures.isNotEmpty()) Log.w(TAG, "출시 로스터 제외 ${entry.characterId}: ${failures.joinToString()}")
        }
        installedCharacters = if (internalCandidatePreview) candidateCharacters
        else releaseRoster.eligibleCharacters(candidateCharacters, candidateVoicePacks)
        characterPackList = installedCharacters
        activeCharacterPack = characters.active()
        val pairedVoicePack = linkedVoicePackId(activeCharacterPack)?.let { id -> installed.firstOrNull { it.id == id } }
        if (pairedVoicePack != null) {
            packs.select(pairedVoicePack.id)
        } else {
            packs.select(audio.builtinPack.id)
        }
        activePack = packs.active()
        refreshLines()
        characterAnimation = activeCharacterPack?.animations?.idle ?: "idle"
        syncCharacterOverlay()
    }

    private fun restoreCharacterPackIfLoaded() {
        if (!packsLoaded || !characterPacksLoaded) return
        characterPackNotice = characters.restore(prefs.getString(PREF_CHARACTER_PACK, null))
        refreshCharacterPacks()
        ensureDefaultCharacter()
    }

    /**
     * 설정 Activity가 없는 overlay-first 흐름에서는 처음 설치한 사용자가 고를 화면이 없다.
     * 저장된 선택이 없으면 로스터 순서상 첫 READY 캐릭터를 정차 중에 한 번 고른다. 후보가 없으면(출시 로스터 0개) 아무것도 띄우지 않는다.
     */
    private fun ensureDefaultCharacter() {
        if (driving || !packsLoaded || !characterPacksLoaded) return
        if (activeCharacterPack != null || prefs.getString(PREF_CHARACTER_PACK, null) != null) return
        characterPackList.firstOrNull { it.state is CharacterPackState.Ready }?.let { selectCharacterPack(it.id) }
    }

    /** Boot/service 진입점: 설정을 전면에 두지 않고 시작하며, CarUx가 정차를 알려 줄 때까지 닫힌 쪽(주행)으로 둔다. */
    fun startBackgroundSession() = onMain {
        settingsVisible = false
        start()
        syncCharacterOverlay()
    }

    /** 정차 중에만 READY 캐릭터를 고르고 저장한다. 선택한 표현은 다음 오버레이 표시부터 적용된다. */
    override fun selectCharacterPack(id: String) = onMain {
        val pack = characterPackList.firstOrNull { it.id == id } ?: return@onMain
        if (driving || pack.state !is CharacterPackState.Ready) return@onMain
        val voicePackId = linkedVoicePackId(pack)
        val pairedVoicePack = voicePackId?.let { id -> installed.firstOrNull { it.id == id } }
        val previewOnly = internalCandidatePreview && pack.info?.voicePackId == null
        if (!previewOnly && (voicePackId == null || pairedVoicePack == null)) {
            characterPackNotice = "출시 로스터에 연결된 준비된 음성 팩이 없어요"
            Log.e(TAG, "캐릭터 ${pack.id} 선택 거부: voicePackId가 없거나 READY 팩이 없음")
            return@onMain
        }
        val previousVoicePackId = packs.selectedId
        packs.stopAll()
        val selectedVoicePackId = if (voicePackId != null) voicePackId else audio.builtinPack.id
        packs.select(selectedVoicePackId)?.let { why ->
            Log.e(TAG, "캐릭터와 음성 팩을 함께 선택하지 못했어요: $why")
            return@onMain
        }
        characters.select(id)?.let { why ->
            packs.select(previousVoicePackId)
            Log.w(TAG, "캐릭터와 음성 팩을 함께 선택하지 못했어요: $why")
            return@onMain
        }
        prefs.edit().putString(PREF_CHARACTER_PACK, id).apply()
        characterPackNotice = null
        refreshCharacterPacks()
    }

    /** 설정 Activity가 앞에 있을 때는 오버레이를 숨긴다. 뒤로 가면 저장된 VR 상태대로 다시 판단한다. */
    override fun setSettingsVisible(value: Boolean) = onMain {
        settingsVisible = value
        syncCharacterOverlay()
    }

    /** 사용자가 고른 VR 사용 여부를 저장한다. 실제 표시는 권한·정차·팩 준비 조건을 모두 확인한다. */
    override fun changeVrEnabled(value: Boolean) = onMain {
        vrEnabled = value
        prefs.edit().putBoolean(PREF_VR_ENABLED, value).apply()
        syncCharacterOverlay()
    }

    /** 설정을 닫아 배경 위의 캐릭터를 바로 보여 주기 위한 진입점. */
    override fun beginVr(): Boolean {
        if (!canStartVr) {
            syncCharacterOverlay()
            return false
        }
        settingsVisible = false
        syncCharacterOverlay()
        return true
    }

    override fun openVrOverlayPermissionSettings() {
        if (!characterOverlay.openPermissionSettings()) {
            vrOverlayStatus = "이 기기에서는 VR 표시 권한을 앱에서 켤 수 없어요 (권한 설정 화면이 없어요)"
        }
    }

    private var lastLoggedOverlayInput: VrOverlayInput? = null

    private fun syncCharacterOverlay() {
        val pack = activeCharacterPack
        val permissionGranted = characterOverlay.permissionGranted
        vrOverlayPermissionRequired = vrEnabled && !permissionGranted
        val input = VrOverlayInput(
            enabled = vrEnabled,
            stationary = !driving,
            settingsVisible = settingsVisible,
            readyCharacter = pack?.isRenderable == true,
            overlayPermissionGranted = permissionGranted,
        )
        // Activity가 없는 흐름에서는 왜 안 보이는지 알 길이 로그뿐이다. 입력이 바뀔 때만 한 줄 남긴다.
        if (input != lastLoggedOverlayInput) {
            lastLoggedOverlayInput = input
            Log.i(TAG, "overlay sync: $input character=${pack?.id}")
        }
        if (VrOverlayPolicy.shouldShow(input)) {
            when (characterOverlay.show(pack!!, characterAnimation)) {
                OverlayShowResult.SHOWING -> {
                    vrOverlayStatus = "정차 배경 위에 ${pack.name}를 표시 중이에요 · 캐릭터를 길게 누르면 메뉴가 열려요"
                    greetOnce()
                }
                OverlayShowResult.PERMISSION_REQUIRED -> vrOverlayStatus = "VR 표시 권한이 필요해요"
                OverlayShowResult.REJECTED -> vrOverlayStatus = "이 기기에서는 VR 오버레이 창을 열지 못했어요"
            }
            return
        }

        characterOverlay.hide()
        vrOverlayStatus = when {
            !vrEnabled -> "VR 모드가 꺼져 있어요"
            driving -> "주행 중에는 VR 캐릭터를 즉시 숨겨요"
            settingsVisible -> "설정 화면을 보고 있어요 · 뒤로 가거나 VR 시작을 누르면 배경에 표시해요"
            pack?.isRenderable != true -> "표시할 준비된 캐릭터 팩이 없어요"
            else -> "VR 표시 권한이 필요해요"
        }
    }

    /** 활성 팩이 이 반응의 대사를 정해 두었으면 그 대사(자막). 아니면 null — 사용자가 바꿀 수 있다. */
    fun fixedLine(e: VehicleEvent): String? =
        if (VehicleSpeechPolicy.outputFor(e) == SpeechOutput.NEUTRAL_CLIP) engine.line(e)
        else activePack.takeIf { !it.freeText && it.canSpeak(VehicleSpeechPolicy.canonicalEventId(e)) }
            ?.lineFor(VehicleSpeechPolicy.canonicalEventId(e), e.defaultLine)

    /** One-shot playback for the exact selected-pack event and zero-based clip variant. */
    override fun previewDialogue(eventId: String, variantIndex: Int) = onMain {
        if (driving || linkedVoicePackId(activeCharacterPack) != activePack.id) return@onMain
        if (variantIndex !in 0 until activePack.variantCount(eventId)) return@onMain
        val isGreeting = eventId == GREETING_EVENT
        if (!isGreeting && !ReleaseRoster.REQUIRED_VOICE_EVENT_IDS.contains(eventId)) return@onMain
        val seq = ++speakSeq
        previewSeq = seq
        cancelGesture()
        packs.stopAll()
        if (!requestFocus()) return@onMain
        val text = activePack.lineFor(eventId, variantIndex) ?: ""
        val clipGesture = activePack.gestureFor(eventId, variantIndex)
        val request = SpeakRequest(
            eventId = eventId,
            text = text,
            neutral = eventId in VehicleSpeechPolicy.neutralClipEventIds,
            volume = MAX_VOLUME,
            pitch = preset.pitch,
            rate = preset.rate,
            variantIndex = variantIndex,
        )
        val started = activePack.voice.speak(
            request,
            note = { msg ->
                onMain {
                    Log.d(TAG, "대사 미리듣기 $eventId[$variantIndex]: $msg")
                    if (msg == PLAYBACK_STARTED && eventId in VehicleSpeechPolicy.characterClipEventIds) {
                        startGesture(eventId, seq, clipGesture)
                    }
                }
            },
            done = { onMain { if (seq == speakSeq) { finishGesture(seq); audio.abandonFocus() } } },
        )
        if (!started) audio.abandonFocus()
    }

    // ---- 캐릭터 곁 오버레이: 탭(TOUCH)·메뉴·시트 ----
    private fun overlayState(): OverlayUiState = OverlayUiState(
        collection = characterCollection(),
        activeCharacterId = activeCharacterPack?.id,
        activeCharacterName = activeCharacterPack?.name,
        dialogueRows = dialogueRows(),
        greetingEnabled = greetingEnabled,
        parkedAvatarVisible = vrEnabled,
        notice = characterPackNotice,
        internalPreview = internalCandidatePreview,
    )

    private fun characterCollection(): CharacterCollection {
        // 칸의 등급(기본/후보)은 걸러내기 전의 출시 로스터 순서로 정한다. 읽을 수 없거나 부적격인 캐릭터는 칸을 비워 둘 뿐
        // 뒤의 후보를 앞으로 당기지 않는다(당기면 보유 없이 기본이 되어 장착할 수 있다). 빈 칸은 눌러도 아무 일이 없다.
        // 내부 후보 미리보기는 출시 로스터가 아니라 후보 목록 순서를 따른다.
        val order = if (internalCandidatePreview) candidateCharacters.map { it.id } else releaseRoster.entries.map { it.characterId }
        val usable = characterPackList.filter { it.state is CharacterPackState.Ready }.associateBy { it.id }
        return CharacterCollection.of(
            roster = order.map { id -> usable[id]?.let { CharacterRef(it.id, it.name) } },
            owned = ownedCharacterIds,
            equippedId = activeCharacterPack?.id,
        )
    }

    /** 캐릭터를 짧게 탭했다(TOUCH). 활성 음성 팩에 TOUCH 클립이 있어야 재생하고, 없으면 다른 대사로 대신하지 않고 요청만 남긴다. */
    private fun touchCharacter() = onMain {
        if (driving) return@onMain
        val character = activeCharacterPack ?: return@onMain
        // 차량 반응이나 안전 안내가 나오는 중이면 끊지 않는다. 건너뛴 탭은 쿨다운을 쓰지 않는다.
        if (engineSpeaking) {
            Log.d(TAG, "TOUCH ${character.id}: 반응 음성이 나오는 중이라 건너뛰어요")
            return@onMain
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastTouchAt < TOUCH_COOLDOWN_MS) return@onMain
        lastTouchAt = now
        val ready = linkedVoicePackId(character) == activePack.id &&
            activePack.canSpeak(TOUCH_EVENT) && activePack.variantCount(TOUCH_EVENT) > 0
        Log.d(TAG, "TOUCH ${character.id}: ${if (ready) "클립 재생" else "TOUCH 클립이 없어요"}")
        if (ready) playClip(TOUCH_EVENT, 0, "TOUCH")
    }

    /** 시트에서 캐릭터를 골랐다. 기본 5개이거나 보유한 가챠 후보만 장착하고 나머지는 거절한다. 캐릭터와 음성 팩은 [selectCharacterPack]이 함께 바꾼다(이슈 #4). */
    private fun equipCharacter(characterId: String) = onMain {
        if (driving) return@onMain
        if (!characterCollection().isEquippable(characterId)) {
            Log.w(TAG, "캐릭터 $characterId 장착 거부: 기본 5개나 보유한 가챠 후보가 아니에요")
            return@onMain
        }
        selectCharacterPack(characterId)
    }

    private fun changeGreetingEnabled(enabled: Boolean) {
        greetingEnabled = enabled
        prefs.edit().putBoolean(PREF_GREETING, enabled).apply()
    }

    /**
     * 세션(지금은 이 프로세스)에서 캐릭터가 **처음** 나타났을 때 한 번만 인사를 정한다. 설정이 꺼져 있거나 그때 말할 수 없어도 그 기회는
     * 지나간 것이라, 나중에 설정을 켜도 소급해서 인사하지 않는다. [syncCharacterOverlay]는 몸짓 갱신마다 불려 SHOWING이 반복되므로 이 한 번의
     * 결정이 필요하다(인사를 재생하다 몸짓이 바뀌어 다시 불리는 재진입도 첫 줄이 막는다). 시작 통로(#36)가 생기면 그쪽 세션 정의로 바꾼다.
     */
    private fun greetOnce() {
        if (greetingChanceUsed) return
        greetingChanceUsed = true
        if (!greetingEnabled || driving) return
        val character = activeCharacterPack ?: return
        if (linkedVoicePackId(character) != activePack.id || activePack.variantCount(GREETING_EVENT) < 1) return
        // 차량 반응이나 안전 안내가 나오는 중이면 인사로 끊지 않는다.
        if (engineSpeaking) {
            Log.d(TAG, "시작 인사를 건너뛰었어요: 반응 음성이 나오는 중이에요 (다시 시도하지 않아요)")
            return
        }
        // 말하지 못하면 건너뛰고 다시 시도하지 않는다(내비 안내 같은 다른 소리 위에 늦게 끼어들지 않는다).
        if (!playClip(GREETING_EVENT, 0, "시작 인사")) {
            Log.d(TAG, "시작 인사를 건너뛰었어요: 오디오 포커스를 얻지 못했거나 재생할 수 없어요 (다시 시도하지 않아요)")
        }
    }

    /** 활성 음성 팩의 클립 하나를 한 번 재생한다. [previewDialogue]와 같은 순서이고, 이벤트 허용 목록 검사만 없다. 재생을 시작했으면 true다. */
    private fun playClip(eventId: String, variantIndex: Int, label: String): Boolean {
        val seq = ++speakSeq
        previewSeq = seq
        cancelGesture()
        packs.stopAll()
        if (!requestFocus()) return false
        val request = SpeakRequest(
            eventId = eventId,
            text = activePack.lineFor(eventId, variantIndex) ?: "",
            neutral = false,
            volume = MAX_VOLUME,
            pitch = preset.pitch,
            rate = preset.rate,
            variantIndex = variantIndex,
        )
        val clipGesture = activePack.gestureFor(eventId, variantIndex)
        val started = activePack.voice.speak(
            request,
            note = { msg ->
                onMain {
                    Log.d(TAG, "$label $eventId[$variantIndex]: $msg")
                    if (msg == PLAYBACK_STARTED && eventId in VehicleSpeechPolicy.characterClipEventIds) {
                        startGesture(eventId, seq, clipGesture)
                    }
                }
            },
            done = { onMain { if (seq == speakSeq) { finishGesture(seq); audio.abandonFocus() } } },
        )
        if (!started) audio.abandonFocus()
        return started
    }

    // ---- Platform: 엔진이 바깥 세계와 만나는 곳 ----
    override fun speak(event: VehicleEvent, text: String, neutral: Boolean, volume: Float) {
        val row = lastSpeakRow[event] // 엔진이 speak 직전에 log했으니 방금 그 행이다
        val output = VehicleSpeechPolicy.outputFor(event)
        if (output == SpeechOutput.SILENCE) {
            row?.note = "음성 정책상 무음"
            audio.abandonFocus()
            return
        }
        // 중립 안전 문구는 승인된 WAV를 우선하고 GLB gesture는 실행하지 않는다.
        val neutralDelivery = neutral || output == SpeechOutput.NEUTRAL_CLIP || output == SpeechOutput.SYSTEM_TTS
        val useSystemTtsOnly = output == SpeechOutput.SYSTEM_TTS
        val seq = ++speakSeq
        engineSpeechSeq = seq
        engineSpeaking = true
        cancelGesture()
        val gestureEventId = VehicleSpeechPolicy.canonicalEventId(event).takeIf {
            !driving && !neutralDelivery && output == SpeechOutput.CHARACTER_CLIP && activePack.canSpeak(it)
        }
        val request = SpeakRequest(
            eventId = VehicleSpeechPolicy.canonicalEventId(event),
            text = text,
            // A neutral-clip event may use an approved WAV. Only SYSTEM_TTS forces the built-in voice.
            neutral = useSystemTtsOnly,
            volume = volume,
            pitch = if (neutralDelivery) 1f else preset.pitch,
            rate = if (neutralDelivery) 1f else preset.rate,
        )
        var usedFallback = false
        var selectedClipGesture: ClipGesture? = null
        packs.speak(
            request,
            note = { msg -> onMain {
                row?.let { it.note += " · $msg" }
                if (msg == PLAYBACK_STARTED && !usedFallback) gestureEventId?.let { startGesture(it, seq, selectedClipGesture) }
            } },
            onVariantSelected = { variantIndex ->
                selectedClipGesture = activePack.gestureFor(request.eventId, variantIndex)
            },
            // 포커스는 가장 최근 요청이 끝났을 때만 놓는다. 끊긴 이전 요청의 늦은 done이 새 요청의 포커스를 놓으면 안 된다.
            // 엔진 음성이 끝났다는 표시는 그와 별개다: 미리듣기가 speakSeq를 올렸어도 이 음성이 끝난 사실은 반영한다.
            done = { onMain {
                if (seq == engineSpeechSeq) engineSpeaking = false
                if (seq == speakSeq) { finishGesture(seq); audio.abandonFocus() }
            } },
            onFallback = { why ->
                usedFallback = true
                row?.let { it.note += " · $why" }
            },
        )
    }

    /** 포커스 변화에 응답한다. 클립은 음량만 낮추고(CAN_DUCK), TTS는 낮출 수 없어 멈춘다. 규칙은 [handleFocusChange]. */
    private fun onFocusChange(change: FocusChange) {
        handleFocusChange(
            change,
            duck = { on ->
                packs.duckAll(on).also { Log.i(TAG, "ducking ${if (on) "시작" else "해제"} → ${if (it) "음량 조절함" else "조절할 음성 없음"}") }
            },
            stop = engine::focusLost,
        )
    }

    override fun stop() {
        engineSpeaking = false // 모든 음성을 끊었다. done이 오기 전에 TOUCH가 막히지 않게 한다
        cancelGesture()
        packs.stopAll()
        audio.abandonFocus()
    }

    private fun startGesture(eventId: String, speechSequence: Int, clipGesture: ClipGesture? = null) {
        if (speechSequence != speakSeq || driving) return
        gestureStart?.let(main::removeCallbacks)
        gestureStart = null
        val character = activeCharacterPack ?: return
        val gesture = CharacterGestureResolver.resolve(character.animations, eventId, clipGesture?.gestureId) ?: return
        val begin = Runnable {
            gestureStart = null
            if (speechSequence != speakSeq || driving || activeCharacterPack !== character) return@Runnable
            characterAnimation = gesture.animationName
            syncCharacterOverlay()
            // CharacterSurfaceView plays the declared GLB clip once and holds its final pose until audio ends.
        }
        if (clipGesture == null || clipGesture.startOffsetMs == 0) begin.run()
        else {
            gestureStart = begin
            main.postDelayed(begin, clipGesture.startOffsetMs.toLong())
        }
    }

    private fun finishGesture(speechSequence: Int) {
        if (speechSequence == speakSeq) resetGesture()
    }

    private fun cancelGesture() {
        gestureStart?.let(main::removeCallbacks)
        gestureStart = null
        activeCharacterPack?.let { character ->
            characterAnimation = character.animations?.idle ?: "idle"
            syncCharacterOverlay()
        }
    }

    private fun resetGesture() {
        gestureStart?.let(main::removeCallbacks)
        gestureStart = null
        characterAnimation = activeCharacterPack?.animations?.idle ?: "idle"
        syncCharacterOverlay()
    }

    override fun requestFocus(): Boolean = !navBusy && audio.requestFocus()

    override fun showAvatar(face: String, text: String) {
        this.face = face
        bubble = text
    }

    override fun hideAvatar() {
        // 다음 정차는 대기 동작부터 시작한다. 주행 중에는 sync가 WindowManager 창도 즉시 뗀다.
        characterAnimation = activeCharacterPack?.animations?.idle ?: "idle"
        syncCharacterOverlay()
    }

    override fun log(entry: LogEntry) {
        val what = when (entry) {
            is LogEntry.Speak -> "말하기 요청 \"${entry.text}\""
            is LogEntry.Drop -> "버림 · " + when (entry.why) {
                DropReason.COOLDOWN -> "쿨다운 ${(entry.waitMs + 999) / 1000}초 남음"
                DropReason.PRIORITY -> "더 급한 반응(${entry.by?.label})이 먼저였어요"
                DropReason.FOCUS -> "오디오 포커스를 잃은 상태예요 (내비 등이 말하는 중)"
                DropReason.DRIVING_SILENT -> "주행 중 음성이 불허된 상태예요"
                DropReason.POLICY_SILENT -> "음성 정책상 무음이에요"
            }
        }
        val row = LogRow(clock.format(Date()), "${entry.event.label} → $what")
        logs.add(0, row)
        if (logs.size > MAX_LOGS) logs.removeAt(logs.lastIndex)
        if (entry is LogEntry.Speak) lastSpeakRow[entry.event] = row
    }

    companion object {
        const val IDLE_BUBBLE = "차량 이벤트가 생기면 여기서 말해요."
        private const val PLAYBACK_STARTED = "재생됨"
        const val MAX_LINE = 60
        private const val MAX_LOGS = 50
        private const val VEHICLE_CATALOG_DEBOUNCE_MS = 100L
        private const val MAX_VOLUME = 0.8f // 음량 상한(§6). 엔진과 미리듣기가 같은 값을 쓴다
        private const val PREF_CHARACTER_PACK = "characterpack.selected"
        private const val PREF_VR_ENABLED = "characterpack.vr.enabled"
        private const val PREF_OWNED_CHARACTERS = "collection.owned"
        private const val PREF_GREETING = "settings.greeting"
        private const val TOUCH_EVENT = "TOUCH"
        private const val TOUCH_COOLDOWN_MS = 1500L // 잦은 탭 반복을 제한한다(PRD)
        private const val TAG = "VoiceCompanion"
    }
}
