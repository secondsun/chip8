package net.saga.console.chip8.app

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.saga.console.chip8.app.model.AppPreferences
import net.saga.console.chip8.app.model.ThemeMode
import net.saga.console.chip8.app.theme.DisplayPalettes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppPreferencesAndPaletteTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun preferencesRoundTripJson() {
        val original = AppPreferences(
            themeMode = ThemeMode.AMOLED,
            paletteId = "amber_crt",
            audioMuted = true,
            audioVolume = 0.7f,
            cyclesPerFrame = 30
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppPreferences>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun resolvesAllPalettesAndSystemAccent() {
        val accent = Color(0xFF007AFF)
        val resolved = DisplayPalettes.resolve("system_accent", accent, emptyList())
        assertEquals(accent, resolved.plane0)
        assertEquals(DisplayPalettes.AMBER_CRT, DisplayPalettes.resolve("amber_crt", accent, emptyList()))
    }
}
