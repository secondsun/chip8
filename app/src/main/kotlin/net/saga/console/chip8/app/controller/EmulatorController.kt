package net.saga.console.chip8.app.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.saga.console.chip8.app.model.AppPreferences
import net.saga.console.chip8.app.model.PreferencesStore
import net.saga.console.chip8.app.model.SavedQuirks
import net.saga.console.chip8.app.model.SeedColorSource
import net.saga.console.chip8.app.model.ThemeMode
import net.saga.console.chip8.app.theme.DisplayPalette
import net.saga.console.chip8.app.theme.DisplayPalettes
import net.saga.console.chip8.app.theme.OsAppearance
import net.saga.console.chip8.app.theme.SystemThemeDetector
import net.saga.console.chip8.core.Chip8Config
import net.saga.console.chip8.core.Chip8Machine
import net.saga.console.chip8.core.Chip8Variant
import net.saga.console.chip8.core.DisplayBuffer
import net.saga.console.chip8.core.Quirks
import net.saga.console.chip8.core.RewindBuffer
import net.saga.console.chip8.core.RomDatabase
import net.saga.console.chip8.core.SaveState
import net.saga.console.chip8.core.audio.AudioConfig
import net.saga.console.chip8.core.audio.AudioEngine
import net.saga.console.chip8.core.input.GamepadManager
import net.saga.console.chip8.core.input.InputPreset
import net.saga.console.chip8.core.input.KeypadState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.max

/**
 * Central application controller coordinating the Java 27 emulator core, 60 Hz frame pacing,
 * phosphor decay buffer, audio engine, input4j gamepad manager, save states, rewind, and persisted settings.
 */
class EmulatorController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var preferences by mutableStateOf(PreferencesStore.load())
        private set

    var osAppearance by mutableStateOf(
        OsAppearance(isDark = true, accentColor = androidx.compose.ui.graphics.Color(0xFF007AFF), platformLabel = "System")
    )
        private set

    private val keypad = KeypadState()
    private val audioEngine = AudioEngine()
    private val gamepadManager = GamepadManager(keypad)
    private val rewindBuffer = RewindBuffer(180) // ~15 seconds at 12 snapshots/sec
    private val machine: Chip8Machine = Chip8Machine(
        preferences.toChip8Config(),
        keypad,
        audioEngine,
        java.util.Random()
    )

    var snapshot by mutableStateOf(machine.snapshot())
        private set

    var phosphorPixels by mutableStateOf(FloatArray(DisplayBuffer.HI_RES_WIDTH * DisplayBuffer.HI_RES_HEIGHT * 4))
        private set
    private var phosphorState = FloatArray(DisplayBuffer.HI_RES_WIDTH * DisplayBuffer.HI_RES_HEIGHT * 4)

    var waveform by mutableStateOf(FloatArray(64))
        private set

    var audioPlaying by mutableStateOf(false)
        private set

    var gamepadStatus by mutableStateOf(gamepadManager.status)
        private set

    var isRomLoaded by mutableStateOf(false)
        private set

    var isPaused by mutableStateOf(true)
        private set

    var isTurbo by mutableStateOf(false)

    var isRewinding by mutableStateOf(false)

    var measuredFps by mutableStateOf(60)
        private set

    var loadedRomTitle by mutableStateOf("No ROM Loaded")
        private set

    var loadedRomPath by mutableStateOf<Path?>(null)
        private set

    var loadedRomControls by mutableStateOf<String?>(null)
        private set

    var loadedRomSha1 by mutableStateOf<String?>(null)
        private set

    var statusBanner by mutableStateOf<String?>(null)
        private set

    var errorBanner by mutableStateOf<String?>(null)
        private set

    var scannedRoms by mutableStateOf<List<RomDatabase.ScannedRom>>(emptyList())
        private set

    var saveSlots by mutableStateOf<List<SaveState?>>(List(8) { null })
        private set

    var rewindCount by mutableStateOf(0)
        private set

    var showSettingsDialog by mutableStateOf(false)
    var settingsInitialTab by mutableStateOf(0)
    var showSaveStatesSheet by mutableStateOf(false)

    // Interactive key/gamepad capture state inside Settings
    var capturingKeyForHex by mutableStateOf<Int?>(null)
    var capturingGamepadForHex by mutableStateOf<Int?>(null)

    private var loopJob: Job? = null
    private var themeJob: Job? = null
    private var frameCounter = 0

    init {
        audioEngine.setConfig(preferences.toAudioConfig())
        audioEngine.setPaused(true)
        audioEngine.start()
        gamepadManager.setBindings(preferences.gamepadBindings)
        gamepadManager.start()
        startThemeWatcher()
        startEmulationLoop()
        rescanRomLibrary()
    }

    fun activeDisplayPalette(): DisplayPalette = DisplayPalettes.resolve(
        id = preferences.paletteId,
        systemAccent = osAppearance.accentColor,
        customColors = preferences.customPaletteArgb
    )

    private fun startThemeWatcher() {
        themeJob = scope.launch {
            while (isActive) {
                osAppearance = SystemThemeDetector.detect()
                delay(2500)
            }
        }
    }

    private fun startEmulationLoop() {
        loopJob = scope.launch {
            val frameDurationNs = 1_000_000_000L / 60L
            var lastFpsSample = System.nanoTime()
            var framesSinceSample = 0

            while (isActive) {
                val frameStart = System.nanoTime()
                frameCounter++

                if (isRomLoaded && !isPaused) {
                    if (isRewinding) {
                        val prev = rewindBuffer.popLatest()
                        if (prev.isPresent) {
                            machine.restoreSaveState(prev.get())
                            rewindCount = rewindBuffer.size()
                            errorBanner = null
                        }
                    } else {
                        try {
                            val multiplier = if (isTurbo) 4 else 1
                            val cpf = preferences.cyclesPerFrame * multiplier
                            val hitBreakpoint = machine.stepFrame(cpf)
                            if (hitBreakpoint) {
                                isPaused = true
                                audioEngine.setPaused(true)
                                statusBanner = "Breakpoint hit at 0x%04X".format(machine.pc)
                            }
                            if (frameCounter % 5 == 0) {
                                rewindBuffer.push(machine.createSaveState())
                                rewindCount = rewindBuffer.size()
                            }
                        } catch (ex: Exception) {
                            isPaused = true
                            audioEngine.setPaused(true)
                            errorBanner = ex.message ?: ex.javaClass.simpleName
                        }
                    }
                }

                val snap = machine.snapshot()
                updatePhosphorBuffer(snap.display())
                snapshot = snap
                waveform = audioEngine.snapshotWaveform()
                audioPlaying = audioEngine.isSoundActive
                if (frameCounter % 15 == 0) {
                    gamepadStatus = gamepadManager.status
                }

                framesSinceSample++
                val now = System.nanoTime()
                if (now - lastFpsSample >= 500_000_000L) {
                    measuredFps = ((framesSinceSample * 1_000_000_000L) / max(1L, now - lastFpsSample)).toInt()
                    framesSinceSample = 0
                    lastFpsSample = now
                }

                val elapsedNs = System.nanoTime() - frameStart
                val sleepMs = (frameDurationNs - elapsedNs) / 1_000_000L
                if (sleepMs > 0) {
                    delay(sleepMs)
                }
            }
        }
    }

    private fun updatePhosphorBuffer(display: DisplayBuffer.DisplaySnapshot) {
        if (display.megaMode()) {
            return
        }
        val w = display.width()
        val h = display.height()
        val total = w * h
        val decay = if (isPaused) 0.0f else preferences.phosphorDecay.coerceIn(0f, 0.94f)
        val pixels = display.pixels()

        // Track 3 independent phosphor channels (for color indices 1, 2, 3) per pixel
        val next = FloatArray(total * 3)
        for (i in 0 until total) {
            val c = pixels[i].toInt() and 0b11
            val base = i * 3
            val old0 = if (base + 2 < phosphorState.size) phosphorState[base] else 0f
            val old1 = if (base + 2 < phosphorState.size) phosphorState[base + 1] else 0f
            val old2 = if (base + 2 < phosphorState.size) phosphorState[base + 2] else 0f

            next[base] = if (c == 1) 1f else old0 * decay
            next[base + 1] = if (c == 2) 1f else old1 * decay
            next[base + 2] = if (c == 3) 1f else old2 * decay
        }
        phosphorState = next
        phosphorPixels = next
    }

    fun loadRom(path: Path) {
        if (!Files.isRegularFile(path)) {
            errorBanner = "ROM file not found: $path"
            return
        }
        try {
            val bytes = Files.readAllBytes(path)
            val sha1 = RomDatabase.sha1Hex(bytes)
            val meta = RomDatabase.lookupByHash(sha1).orElse(null)
            val sidecar = readSidecarTxt(path)

            if (preferences.autoDetectRomConfig) {
                if (meta != null) {
                    val preset = InputPreset.byId(meta.inputPresetId())
                    updatePreferences {
                        it.copy(
                            variant = meta.recommendedVariant().name,
                            quirks = SavedQuirks.fromCore(meta.recommendedQuirks()),
                            cyclesPerFrame = meta.recommendedCyclesPerFrame(),
                            inputPresetId = preset.id(),
                            keyboardBindings = preset.keyboardBindings(),
                            gamepadBindings = preset.gamepadBindings()
                        )
                    }
                    statusBanner = "Auto-configured for ${meta.title()} (${meta.recommendedVariant().shortName()} • ${preset.displayName()})"
                } else {
                    val lower = path.fileName.toString().lowercase()
                    val extVariant = when {
                        lower.endsWith(".mc8") -> Chip8Variant.MEGA_CHIP
                        lower.endsWith(".xo8") -> Chip8Variant.XO_CHIP
                        lower.endsWith(".sc8") -> Chip8Variant.SUPER_CHIP_1_1
                        else -> preferences.coreVariant()
                    }
                    if (extVariant != preferences.coreVariant()) {
                        setVariant(extVariant)
                    }
                    statusBanner = "Loaded ${path.fileName}"
                }
            } else {
                statusBanner = "Loaded ${path.fileName}"
            }

            machine.updateConfig(preferences.toChip8Config(), true)
            machine.loadRom(bytes)
            rewindBuffer.clear()
            rewindCount = 0
            phosphorState = FloatArray(DisplayBuffer.HI_RES_WIDTH * DisplayBuffer.HI_RES_HEIGHT * 3)
            saveSlots = List(8) { null }

            loadedRomPath = path
            loadedRomTitle = meta?.title() ?: path.fileName.toString().replaceFirst(Regex("\\.(ch8|sc8|xo8|mc8)$"), "")
            loadedRomSha1 = sha1
            loadedRomControls = meta?.controlsHint() ?: sidecar
            isRomLoaded = true
            isPaused = false
            audioEngine.setPaused(false)
            errorBanner = null

            val normalized = path.toAbsolutePath().normalize().toString()
            val updatedRecent = (listOf(normalized) + preferences.recentRoms.filterNot { it == normalized }).take(12)
            updatePreferences { it.copy(recentRoms = updatedRecent) }
        } catch (ex: Exception) {
            errorBanner = "Failed to load ROM: ${ex.message}"
        }
    }

    private fun readSidecarTxt(romPath: Path): String? {
        val txt = romPath.resolveSibling(romPath.fileName.toString().replaceFirst(Regex("\\.(ch8|sc8|xo8)$"), ".txt"))
        return if (Files.isRegularFile(txt)) runCatching { Files.readString(txt).trim() }.getOrNull() else null
    }

    fun togglePause() {
        if (!isRomLoaded) return
        isPaused = !isPaused
        audioEngine.setPaused(isPaused)
        if (!isPaused) {
            errorBanner = null
        }
    }

    fun stepOnce() {
        if (!isRomLoaded) return
        isPaused = true
        audioEngine.setPaused(true)
        try {
            machine.stepInstruction()
            errorBanner = null
            snapshot = machine.snapshot()
            updatePhosphorBuffer(snapshot.display())
        } catch (ex: Exception) {
            errorBanner = ex.message ?: ex.javaClass.simpleName
        }
    }

    fun resetMachine() {
        if (!isRomLoaded) return
        machine.reset()
        rewindBuffer.clear()
        rewindCount = 0
        errorBanner = null
        statusBanner = "Reset $loadedRomTitle"
        snapshot = machine.snapshot()
        updatePhosphorBuffer(snapshot.display())
    }

    fun toggleBreakpoint(address: Int) {
        machine.toggleBreakpoint(address)
        snapshot = machine.snapshot()
    }

    fun clearBreakpoints() {
        machine.clearBreakpoints()
        snapshot = machine.snapshot()
    }

    fun saveToSlot(slot: Int) {
        if (!isRomLoaded || slot !in saveSlots.indices) return
        val updated = saveSlots.toMutableList()
        updated[slot] = machine.createSaveState()
        saveSlots = updated
        statusBanner = "Saved state to Slot ${slot + 1}"
    }

    fun loadFromSlot(slot: Int) {
        val state = saveSlots.getOrNull(slot) ?: return
        machine.restoreSaveState(state)
        errorBanner = null
        statusBanner = "Loaded Slot ${slot + 1}"
        snapshot = machine.snapshot()
        updatePhosphorBuffer(snapshot.display())
    }

    fun setVirtualKey(hexKey: Int, pressed: Boolean) {
        keypad.setKey(KeypadState.Source.VIRTUAL_PAD, hexKey, pressed)
    }

    fun copyMemoryBytes(): ByteArray = machine.copyMemoryPrefix(0x10000)

    fun rescanRomLibrary() {
        scope.launch(Dispatchers.IO) {
            val dirs = buildList {
                val localRoms = Path.of("roms")
                if (Files.isDirectory(localRoms)) add(localRoms)
                val parentRoms = Path.of("..", "roms")
                if (Files.isDirectory(parentRoms)) add(parentRoms)
                preferences.romDirectories.forEach { add(Path.of(it)) }
            }.distinct()
            scannedRoms = RomDatabase.scanDirectories(dirs)
        }
    }

    fun addRomDirectory(dir: Path) {
        val normalized = dir.toAbsolutePath().normalize().toString()
        if (normalized !in preferences.romDirectories) {
            updatePreferences { it.copy(romDirectories = it.romDirectories + normalized) }
            rescanRomLibrary()
        }
    }

    fun removeRomDirectory(dir: String) {
        updatePreferences { it.copy(romDirectories = it.romDirectories.filterNot { d -> d == dir }) }
        rescanRomLibrary()
    }

    fun setThemeMode(mode: ThemeMode) = updatePreferences { it.copy(themeMode = mode) }
    fun setSeedColorSource(source: SeedColorSource) = updatePreferences { it.copy(seedColorSource = source) }
    fun setCustomSeedArgb(argb: Long) = updatePreferences { it.copy(customSeedArgb = argb, seedColorSource = SeedColorSource.CUSTOM) }
    fun setPaletteId(id: String) = updatePreferences { it.copy(paletteId = id) }
    fun setCustomPaletteColor(index: Int, argb: Long) {
        val list = preferences.customPaletteArgb.toMutableList()
        if (index in list.indices) {
            list[index] = argb
            updatePreferences { it.copy(customPaletteArgb = list, paletteId = "custom") }
        }
    }

    fun setPhosphorDecay(value: Float) = updatePreferences { it.copy(phosphorDecay = value.coerceIn(0f, 0.94f)) }
    fun setScanlines(enabled: Boolean) = updatePreferences { it.copy(scanlinesEnabled = enabled) }
    fun setPixelGrid(enabled: Boolean) = updatePreferences { it.copy(pixelGridEnabled = enabled) }
    fun setPhosphorGlow(enabled: Boolean) = updatePreferences { it.copy(phosphorGlowEnabled = enabled) }

    fun setVariant(variant: Chip8Variant) {
        updatePreferences {
            it.copy(
                variant = variant.name,
                quirks = SavedQuirks.fromCore(variant.defaultQuirks()),
                cyclesPerFrame = variant.defaultCyclesPerFrame()
            )
        }
        machine.updateConfig(preferences.toChip8Config(), true)
        errorBanner = null
        statusBanner = "Switched to ${variant.displayName()}"
    }

    fun updateQuirks(transform: (Quirks) -> Quirks) {
        val next = transform(preferences.quirks.toCore())
        updatePreferences { it.copy(quirks = SavedQuirks.fromCore(next)) }
        machine.updateConfig(preferences.toChip8Config(), false)
    }

    fun setCyclesPerFrame(cpf: Int) {
        val clamped = cpf.coerceIn(1, 2000)
        updatePreferences { it.copy(cyclesPerFrame = clamped) }
        machine.updateConfig(preferences.toChip8Config(), false)
    }

    fun setAutoDetectRomConfig(enabled: Boolean) = updatePreferences { it.copy(autoDetectRomConfig = enabled) }

    fun toggleMute() = updateAudio { it.copy(audioMuted = !it.audioMuted) }
    fun setAudioVolume(vol: Float) = updateAudio { it.copy(audioVolume = vol.coerceIn(0f, 1f)) }
    fun setAudioWaveform(wf: AudioConfig.BuzzerWaveform) = updateAudio { it.copy(audioWaveform = wf.name) }
    fun setAudioFrequency(hz: Int) = updateAudio { it.copy(audioFrequencyHz = hz.coerceIn(110, 2000)) }

    fun playTestBeep() {
        scope.launch {
            audioEngine.setTestToneActive(true)
            delay(220)
            audioEngine.setTestToneActive(false)
        }
    }

    private fun updateAudio(transform: (AppPreferences) -> AppPreferences) {
        updatePreferences(transform)
        audioEngine.setConfig(preferences.toAudioConfig())
    }

    fun applyInputPreset(preset: InputPreset) {
        updatePreferences {
            it.copy(
                inputPresetId = preset.id(),
                keyboardBindings = preset.keyboardBindings(),
                gamepadBindings = preset.gamepadBindings()
            )
        }
        gamepadManager.setBindings(preset.gamepadBindings())
    }

    fun bindKeyboardKey(keyToken: String, hexKey: Int) {
        val updated = preferences.keyboardBindings.toMutableMap()
        updated.entries.removeIf { it.value == hexKey && it.key == keyToken }
        updated[keyToken.uppercase()] = hexKey
        updatePreferences { it.copy(inputPresetId = "custom", keyboardBindings = updated) }
    }

    fun beginGamepadCapture(hexKey: Int) {
        capturingKeyForHex = null
        capturingGamepadForHex = hexKey
        gamepadManager.captureNextControl { token ->
            val updated = preferences.gamepadBindings.toMutableMap()
            updated[token] = hexKey
            updatePreferences { it.copy(inputPresetId = "custom", gamepadBindings = updated) }
            gamepadManager.setBindings(updated)
            capturingGamepadForHex = null
            statusBanner = "Bound gamepad $token → Key %X".format(hexKey)
        }
    }

    fun cancelCaptures() {
        capturingKeyForHex = null
        capturingGamepadForHex = null
        gamepadManager.cancelCapture()
    }

    fun toggleLibraryPane() = updatePreferences { it.copy(showLibraryPane = !it.showLibraryPane) }
    fun toggleDebuggerPane() = updatePreferences { it.copy(showDebuggerPane = !it.showDebuggerPane) }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.type == KeyEventType.KeyDown
        val isUp = event.type == KeyEventType.KeyUp
        if (!isDown && !isUp) return false

        val token = keyToToken(event.key)

        // If capturing a new key binding in Settings
        val captureTarget = capturingKeyForHex
        if (captureTarget != null && isDown && token != null) {
            if (event.key == Key.Escape) {
                capturingKeyForHex = null
                return true
            }
            bindKeyboardKey(token, captureTarget)
            capturingKeyForHex = null
            statusBanner = "Bound key $token → Key %X".format(captureTarget)
            return true
        }

        // Global shortcuts (Cmd/Ctrl combos & function keys that never collide with game keys)
        val mod = event.isMetaPressed || event.isCtrlPressed
        if (isDown && mod) {
            when (event.key) {
                Key.L -> { toggleLibraryPane(); return true }
                Key.D -> { toggleDebuggerPane(); return true }
                Key.Comma -> { showSettingsDialog = !showSettingsDialog; return true }
                Key.M -> { toggleMute(); return true }
                else -> {}
            }
        }

        if (isDown) {
            when (event.key) {
                Key.F5 -> { togglePause(); return true }
                Key.F6 -> { stepOnce(); return true }
                Key.F8 -> { resetMachine(); return true }
                Key.F1 -> { if (event.isShiftPressed) loadFromSlot(0) else saveToSlot(0); return true }
                Key.F2 -> { if (event.isShiftPressed) loadFromSlot(1) else saveToSlot(1); return true }
                Key.F3 -> { if (event.isShiftPressed) loadFromSlot(2) else saveToSlot(2); return true }
                Key.F4 -> { if (event.isShiftPressed) loadFromSlot(3) else saveToSlot(3); return true }
                else -> {}
            }
        }

        // Hold-to-Turbo (Tab) and Hold-to-Rewind (Backspace)
        if (!showSettingsDialog) {
            if (event.key == Key.Tab) {
                isTurbo = isDown
                return true
            }
            if (event.key == Key.Backspace) {
                isRewinding = isDown
                return true
            }
        }

        // Game keypad routing
        if (!showSettingsDialog && token != null) {
            val mappedHex = preferences.keyboardBindings[token]
            if (mappedHex != null) {
                keypad.setKey(KeypadState.Source.KEYBOARD, mappedHex, isDown)
                return true
            }
        }
        return false
    }

    private fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        val next = transform(preferences)
        preferences = next
        PreferencesStore.save(next)
    }

    fun dispose() {
        loopJob?.cancel()
        themeJob?.cancel()
        gamepadManager.close()
        audioEngine.close()
        scope.cancel()
    }

    companion object {
        fun keyToToken(key: Key): String? = when (key) {
            Key.Zero -> "0"
            Key.One -> "1"
            Key.Two -> "2"
            Key.Three -> "3"
            Key.Four -> "4"
            Key.Five -> "5"
            Key.Six -> "6"
            Key.Seven -> "7"
            Key.Eight -> "8"
            Key.Nine -> "9"
            Key.A -> "A"
            Key.B -> "B"
            Key.C -> "C"
            Key.D -> "D"
            Key.E -> "E"
            Key.F -> "F"
            Key.G -> "G"
            Key.H -> "H"
            Key.I -> "I"
            Key.J -> "J"
            Key.K -> "K"
            Key.L -> "L"
            Key.M -> "M"
            Key.N -> "N"
            Key.O -> "O"
            Key.P -> "P"
            Key.Q -> "Q"
            Key.R -> "R"
            Key.S -> "S"
            Key.T -> "T"
            Key.U -> "U"
            Key.V -> "V"
            Key.W -> "W"
            Key.X -> "X"
            Key.Y -> "Y"
            Key.Z -> "Z"
            Key.DirectionUp -> "UP"
            Key.DirectionDown -> "DOWN"
            Key.DirectionLeft -> "LEFT"
            Key.DirectionRight -> "RIGHT"
            Key.Spacebar -> "SPACE"
            Key.Enter -> "ENTER"
            else -> null
        }
    }
}
