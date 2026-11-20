package net.saga.console.chip8.app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.saga.console.chip8.core.Chip8Config
import net.saga.console.chip8.core.Chip8Variant
import net.saga.console.chip8.core.Quirks
import net.saga.console.chip8.core.audio.AudioConfig
import net.saga.console.chip8.core.input.InputPreset
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
}

@Serializable
enum class SeedColorSource(val label: String) {
    SYSTEM_ACCENT("OS Accent"),
    DISPLAY_PALETTE("Display Palette"),
    CUSTOM("Custom Swatch")
}

@Serializable
data class SavedQuirks(
    val vfReset: Boolean = true,
    val memoryIncrement: String = Quirks.MemoryIncrement.INCREMENT_BY_X_PLUS_1.name,
    val displayWait: Boolean = true,
    val clipping: Boolean = true,
    val shiftUsesVy: Boolean = true,
    val jumpUsesV0: Boolean = true,
    val waitForKeyRelease: Boolean = true,
    val loResTallSprites: Boolean = false
) {
    fun toCore(): Quirks = Quirks(
        vfReset,
        runCatching { Quirks.MemoryIncrement.valueOf(memoryIncrement) }
            .getOrDefault(Quirks.MemoryIncrement.INCREMENT_BY_X_PLUS_1),
        displayWait,
        clipping,
        shiftUsesVy,
        jumpUsesV0,
        waitForKeyRelease,
        loResTallSprites
    )

    companion object {
        fun fromCore(q: Quirks): SavedQuirks = SavedQuirks(
            vfReset = q.vfReset(),
            memoryIncrement = q.memoryIncrement().name,
            displayWait = q.displayWait(),
            clipping = q.clipping(),
            shiftUsesVy = q.shiftUsesVy(),
            jumpUsesV0 = q.jumpUsesV0(),
            waitForKeyRelease = q.waitForKeyRelease(),
            loResTallSprites = q.loResTallSprites()
        )
    }
}

@Serializable
data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColorSource: SeedColorSource = SeedColorSource.SYSTEM_ACCENT,
    val customSeedArgb: Long = 0xFF6366F1,
    val paletteId: String = "phosphor_green",
    val customPaletteArgb: List<Long> = listOf(0xFF0A0F14, 0xFF38BDF8, 0xFFF472B6, 0xFFFDE047),
    val phosphorDecay: Float = 0.72f,
    val scanlinesEnabled: Boolean = true,
    val pixelGridEnabled: Boolean = false,
    val phosphorGlowEnabled: Boolean = true,
    val audioMuted: Boolean = false,
    val audioVolume: Float = 0.45f,
    val audioWaveform: String = AudioConfig.BuzzerWaveform.RETRO_PULSE_25.name,
    val audioFrequencyHz: Int = 440,
    val variant: String = Chip8Variant.COSMAC_VIP.name,
    val quirks: SavedQuirks = SavedQuirks.fromCore(Chip8Variant.COSMAC_VIP.defaultQuirks()),
    val cyclesPerFrame: Int = 12,
    val autoDetectRomConfig: Boolean = true,
    val inputPresetId: String = InputPreset.COSMAC.id(),
    val keyboardBindings: Map<String, Int> = InputPreset.COSMAC.keyboardBindings(),
    val gamepadBindings: Map<String, Int> = InputPreset.COSMAC.gamepadBindings(),
    val romDirectories: List<String> = emptyList(),
    val recentRoms: List<String> = emptyList(),
    val showLibraryPane: Boolean = true,
    val showDebuggerPane: Boolean = false
) {
    fun coreVariant(): Chip8Variant =
        runCatching { Chip8Variant.valueOf(variant) }.getOrDefault(Chip8Variant.COSMAC_VIP)

    fun toChip8Config(): Chip8Config = Chip8Config(coreVariant(), quirks.toCore(), cyclesPerFrame.coerceIn(1, 2000))

    fun toAudioConfig(): AudioConfig = AudioConfig(
        audioMuted,
        audioVolume.coerceIn(0f, 1f),
        runCatching { AudioConfig.BuzzerWaveform.valueOf(audioWaveform) }
            .getOrDefault(AudioConfig.BuzzerWaveform.RETRO_PULSE_25),
        audioFrequencyHz.coerceIn(110, 2000)
    )
}

object PreferencesStore {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun configFile(): Path {
        val home = System.getProperty("user.home", ".")
        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        val dir = when {
            os.contains("mac") -> Path.of(home, "Library", "Application Support", "Chip8Studio")
            os.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: Path.of(home, "AppData", "Roaming").toString()
                Path.of(appData, "Chip8Studio")
            }
            else -> {
                val xdg = System.getenv("XDG_CONFIG_HOME") ?: Path.of(home, ".config").toString()
                Path.of(xdg, "chip8-studio")
            }
        }
        return dir.resolve("preferences.json")
    }

    fun load(): AppPreferences {
        val file = configFile()
        if (!Files.isRegularFile(file)) {
            return AppPreferences()
        }
        return runCatching {
            json.decodeFromString<AppPreferences>(Files.readString(file))
        }.getOrDefault(AppPreferences())
    }

    fun save(prefs: AppPreferences) {
        try {
            val file = configFile()
            Files.createDirectories(file.parent)
            Files.writeString(file, json.encodeToString(prefs))
        } catch (_: Exception) {
            // Ignore read-only config dir failures
        }
    }
}
