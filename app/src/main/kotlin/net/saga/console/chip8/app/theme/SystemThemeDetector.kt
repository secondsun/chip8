package net.saga.console.chip8.app.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class OsAppearance(
    val isDark: Boolean,
    val accentColor: Color,
    val platformLabel: String
)

/**
 * Cross-platform native OS dark-mode and system accent tint detector for macOS, Windows, and Linux.
 */
object SystemThemeDetector {
    private val defaultBlue = Color(0xFF007AFF)

    suspend fun detect(): OsAppearance = withContext(Dispatchers.IO) {
        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        when {
            os.contains("mac") -> detectMacOs()
            os.contains("win") -> detectWindows()
            else -> detectLinux()
        }
    }

    private fun detectMacOs(): OsAppearance {
        val style = runCommand("defaults", "read", "-g", "AppleInterfaceStyle")
        val isDark = style?.trim().equals("Dark", ignoreCase = true)

        val accentRaw = runCommand("defaults", "read", "-g", "AppleAccentColor")?.trim()
        val accent = when (accentRaw) {
            "-1" -> Color(0xFF8E8E93) // Graphite
            "0" -> Color(0xFFFF3B30)  // Red
            "1" -> Color(0xFFFF9500)  // Orange
            "2" -> Color(0xFFFFCC00)  // Yellow
            "3" -> Color(0xFF34C759)  // Green
            "4" -> Color(0xFF007AFF)  // Blue
            "5" -> Color(0xFFAF52DE)  // Purple
            "6" -> Color(0xFFFF2D55)  // Pink
            else -> defaultBlue
        }
        return OsAppearance(isDark = isDark, accentColor = accent, platformLabel = "macOS System Tint")
    }

    private fun detectWindows(): OsAppearance {
        val lightOut = runCommand(
            "reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme"
        )
        val isDark = lightOut?.contains("0x0") == true

        val accentOut = runCommand(
            "reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\DWM",
            "/v", "AccentColor"
        )
        val accent = parseWindowsAbgr(accentOut) ?: Color(0xFF0078D4)
        return OsAppearance(isDark = isDark, accentColor = accent, platformLabel = "Windows Accent")
    }

    private fun parseWindowsAbgr(regOutput: String?): Color? {
        if (regOutput == null) return null
        val match = Regex("0x([0-9a-fA-F]{6,8})").find(regOutput) ?: return null
        val value = match.groupValues[1].toLongOrNull(16) ?: return null
        val r = (value and 0xFF).toInt()
        val g = ((value ushr 8) and 0xFF).toInt()
        val b = ((value ushr 16) and 0xFF).toInt()
        return Color(r, g, b)
    }

    private fun detectLinux(): OsAppearance {
        val scheme = runCommand("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
        val theme = runCommand("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
        val isDark = scheme?.contains("dark", ignoreCase = true) == true ||
            theme?.contains("dark", ignoreCase = true) == true

        val accentRaw = runCommand("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
            ?.trim()?.trim('\'', '"')?.lowercase(Locale.ROOT)
        val accent = when (accentRaw) {
            "blue" -> Color(0xFF3584E4)
            "teal" -> Color(0xFF2190A4)
            "green" -> Color(0xFF3A944A)
            "yellow" -> Color(0xFFC88800)
            "orange" -> Color(0xFFED5B00)
            "red" -> Color(0xFFE62D42)
            "pink" -> Color(0xFFD56199)
            "purple" -> Color(0xFF9141AC)
            "slate" -> Color(0xFF6F8396)
            else -> Color(0xFF3584E4)
        }
        return OsAppearance(isDark = isDark, accentColor = accent, platformLabel = "Linux System Accent")
    }

    private fun runCommand(vararg cmd: String): String? {
        return try {
            val proc = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            if (code == 0) output else null
        } catch (_: Exception) {
            null
        }
    }
}
