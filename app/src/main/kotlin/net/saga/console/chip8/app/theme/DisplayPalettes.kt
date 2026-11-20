package net.saga.console.chip8.app.theme

import androidx.compose.ui.graphics.Color

/**
 * 4-color display palette supporting standard 2-color CHIP-8/SCHIP (bg, plane0)
 * and 4-color XO-CHIP (bg, plane0, plane1, plane0+1 overlap).
 */
data class DisplayPalette(
    val id: String,
    val name: String,
    val background: Color,
    val plane0: Color,
    val plane1: Color,
    val bothPlanes: Color
) {
    fun colorForIndex(index: Int): Color = when (index and 0b11) {
        0 -> background
        1 -> plane0
        2 -> plane1
        else -> bothPlanes
    }
}

object DisplayPalettes {
    val PHOSPHOR_GREEN = DisplayPalette(
        id = "phosphor_green",
        name = "Phosphor Green",
        background = Color(0xFF061109),
        plane0 = Color(0xFF39FF88),
        plane1 = Color(0xFF15803D),
        bothPlanes = Color(0xFFD9F99D)
    )

    val AMBER_CRT = DisplayPalette(
        id = "amber_crt",
        name = "Amber CRT",
        background = Color(0xFF140B03),
        plane0 = Color(0xFFFFB000),
        plane1 = Color(0xFFC2410C),
        bothPlanes = Color(0xFFFEF08A)
    )

    val CRISP_MONO = DisplayPalette(
        id = "crisp_mono",
        name = "Crisp Monochrome",
        background = Color(0xFF0B0E14),
        plane0 = Color(0xFFF3F6FC),
        plane1 = Color(0xFF64748B),
        bothPlanes = Color(0xFF38BDF8)
    )

    val GAMEBOY_DMG = DisplayPalette(
        id = "gameboy_dmg",
        name = "Game Boy DMG",
        background = Color(0xFF0F380F),
        plane0 = Color(0xFF8BAC0F),
        plane1 = Color(0xFF306230),
        bothPlanes = Color(0xFF9BBC0F)
    )

    val OCTO_CLASSIC = DisplayPalette(
        id = "octo_classic",
        name = "Octo Classic",
        background = Color(0xFF1F1926),
        plane0 = Color(0xFFF2C97D),
        plane1 = Color(0xFFE06C75),
        bothPlanes = Color(0xFF98C379)
    )

    val CATPPUCCIN_MOCHA = DisplayPalette(
        id = "catppuccin",
        name = "Catppuccin Mocha",
        background = Color(0xFF181825),
        plane0 = Color(0xFFCBA6F7),
        plane1 = Color(0xFF89B4FA),
        bothPlanes = Color(0xFFF5E0DC)
    )

    val NORD_FROST = DisplayPalette(
        id = "nord",
        name = "Nord Frost",
        background = Color(0xFF242933),
        plane0 = Color(0xFF88C0D0),
        plane1 = Color(0xFF5E81AC),
        bothPlanes = Color(0xFFECEFF4)
    )

    val DRACULA = DisplayPalette(
        id = "dracula",
        name = "Dracula",
        background = Color(0xFF1E1F29),
        plane0 = Color(0xFFBD93F9),
        plane1 = Color(0xFFFF79C6),
        bothPlanes = Color(0xFF50FA7B)
    )

    val SOLARIZED = DisplayPalette(
        id = "solarized",
        name = "Solarized Dark",
        background = Color(0xFF002B36),
        plane0 = Color(0xFF2AA198),
        plane1 = Color(0xFFB58900),
        bothPlanes = Color(0xFFFDF6E3)
    )

    val PRESETS: List<DisplayPalette> = listOf(
        PHOSPHOR_GREEN,
        AMBER_CRT,
        CRISP_MONO,
        GAMEBOY_DMG,
        OCTO_CLASSIC,
        CATPPUCCIN_MOCHA,
        NORD_FROST,
        DRACULA,
        SOLARIZED
    )

    fun resolve(
        id: String,
        systemAccent: Color,
        customColors: List<Long>
    ): DisplayPalette {
        if (id == "system_accent") {
            return DisplayPalette(
                id = "system_accent",
                name = "System Accent",
                background = Color(0xFF0D1017),
                plane0 = systemAccent,
                plane1 = systemAccent.copy(alpha = 0.55f),
                bothPlanes = Color(0xFFF8FAFC)
            )
        }
        if (id == "custom" && customColors.size >= 4) {
            return DisplayPalette(
                id = "custom",
                name = "Custom Palette",
                background = Color(customColors[0].toInt()),
                plane0 = Color(customColors[1].toInt()),
                plane1 = Color(customColors[2].toInt()),
                bothPlanes = Color(customColors[3].toInt())
            )
        }
        return PRESETS.find { it.id == id } ?: PHOSPHOR_GREEN
    }
}
