package net.saga.console.chip8.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.DynamicMaterialTheme
import com.materialkolor.material3.rememberDynamicMaterialThemeState
import net.saga.console.chip8.app.model.AppPreferences
import net.saga.console.chip8.app.model.SeedColorSource
import net.saga.console.chip8.app.model.ThemeMode

@Composable
fun Chip8StudioTheme(
    preferences: AppPreferences,
    osAppearance: OsAppearance,
    displayPalette: DisplayPalette,
    content: @Composable () -> Unit
) {
    val isDark = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> osAppearance.isDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val isAmoled = preferences.themeMode == ThemeMode.AMOLED

    val seedColor = when (preferences.seedColorSource) {
        SeedColorSource.SYSTEM_ACCENT -> osAppearance.accentColor
        SeedColorSource.DISPLAY_PALETTE -> displayPalette.plane0
        SeedColorSource.CUSTOM -> Color(preferences.customSeedArgb.toInt())
    }

    val themeState = rememberDynamicMaterialThemeState(
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        isAmoled = isAmoled,
        seedColor = seedColor
    )

    DynamicMaterialTheme(
        state = themeState,
        animate = true,
        typography = Typography(),
        content = content
    )
}
