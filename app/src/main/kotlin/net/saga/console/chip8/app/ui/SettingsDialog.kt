package net.saga.console.chip8.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.saga.console.chip8.app.controller.EmulatorController
import net.saga.console.chip8.app.model.SeedColorSource
import net.saga.console.chip8.app.model.ThemeMode
import net.saga.console.chip8.app.theme.DisplayPalettes
import net.saga.console.chip8.core.Chip8Variant
import net.saga.console.chip8.core.Quirks
import net.saga.console.chip8.core.audio.AudioConfig
import net.saga.console.chip8.core.input.InputPreset
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    controller: EmulatorController,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(controller.settingsInitialTab) { mutableStateOf(controller.settingsInitialTab) }
    val tabs = listOf(
        "Display & Theme" to Icons.Rounded.Palette,
        "Emulation & Quirks" to Icons.Rounded.Memory,
        "Audio & Buzzer" to Icons.AutoMirrored.Rounded.VolumeUp,
        "Controls & Gamepad" to Icons.Rounded.Gamepad
    )

    DisposableEffect(Unit) {
        onDispose { controller.cancelCaptures() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(780.dp).height(640.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CHIP-8 Studio Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Settings")
                    }
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    tabs.forEachIndexed { idx, (label, icon) ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    when (selectedTab) {
                        0 -> DisplayAndThemeTab(controller)
                        1 -> EmulationAndQuirksTab(controller)
                        2 -> AudioAndBuzzerTab(controller)
                        3 -> ControlsAndGamepadTab(controller)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayAndThemeTab(controller: EmulatorController) {
    val prefs = controller.preferences
    val os = controller.osAppearance
    val seedSwatches = listOf(
        0xFF007AFFL, 0xFF6366F1L, 0xFF10B981L, 0xFFF59E0BL,
        0xFFEC4899L, 0xFFEF4444L, 0xFF06B6D4L, 0xFFA855F7L
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Application Theme (Material You)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = prefs.themeMode == mode,
                    onClick = { controller.setThemeMode(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        Text(
            "Dynamic Color Seed (${os.platformLabel})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SeedColorSource.entries.forEach { src ->
                FilterChip(
                    selected = prefs.seedColorSource == src,
                    onClick = { controller.setSeedColorSource(src) },
                    label = { Text(src.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            seedSwatches.forEach { argb ->
                val selected = prefs.seedColorSource == SeedColorSource.CUSTOM && prefs.customSeedArgb == argb
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { controller.setCustomSeedArgb(argb) },
                    shape = CircleShape,
                    color = Color(argb.toInt()),
                    border = if (selected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null
                ) {}
            }
        }

        HorizontalDivider()
        SectionTitle("CRT Display Palette (4-Color XO-CHIP Ready)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val allOptions = DisplayPalettes.PRESETS + listOf(
                DisplayPalettes.resolve("system_accent", os.accentColor, prefs.customPaletteArgb)
            )
            allOptions.forEach { pal ->
                val selected = prefs.paletteId == pal.id
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { controller.setPaletteId(pal.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf(pal.background, pal.plane0, pal.plane1, pal.bothPlanes).forEach { c ->
                                Box(Modifier.size(12.dp).clip(CircleShape).background(c))
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(pal.name, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        HorizontalDivider()
        SectionTitle("CRT Visual Effects")

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phosphor Persistence (Anti-Flicker)", style = MaterialTheme.typography.bodyMedium)
                Text("${(prefs.phosphorDecay * 100).roundToInt()}%", fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = prefs.phosphorDecay,
                onValueChange = { controller.setPhosphorDecay(it) },
                valueRange = 0f..0.92f
            )
        }

        ToggleRow("CRT Scanlines", "Subtle horizontal scanline overlay", prefs.scanlinesEnabled) { controller.setScanlines(it) }
        ToggleRow("Phosphor Bloom / Glow", "Soft glow halo around lit pixels", prefs.phosphorGlowEnabled) { controller.setPhosphorGlow(it) }
        ToggleRow("Pixel Grid", "Crisp pixel cell dividers", prefs.pixelGridEnabled) { controller.setPixelGrid(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmulationAndQuirksTab(controller: EmulatorController) {
    val prefs = controller.preferences
    val quirks = prefs.quirks.toCore()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("CHIP-8 Hardware Variant")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip8Variant.entries.forEach { variant ->
                val selected = prefs.coreVariant() == variant
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { controller.setVariant(variant) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(variant.displayName(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val memLabel = if (variant.memorySize() >= 1024 * 1024) "${variant.memorySize() / (1024 * 1024)} MB" else "${variant.memorySize() / 1024} KB"
                            val resLabel = when {
                                variant.supportsMegaChip() -> "256×192 ARGB"
                                variant.supportsHiRes() -> "128×64"
                                else -> "64×32"
                            }
                            Text(
                                "$memLabel • $resLabel",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            variant.description(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalDivider()
        ToggleRow(
            "Auto-Detect ROM Variant & Controls (SHA-1 DB)",
            "Automatically picks recommended variant, quirks, speed, and keypad preset when loading known ROMs",
            prefs.autoDetectRomConfig
        ) { controller.setAutoDetectRomConfig(it) }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CPU Speed (Instructions / 60 Hz Frame)", style = MaterialTheme.typography.bodyMedium)
                Text("${prefs.cyclesPerFrame} IPF (${prefs.cyclesPerFrame * 60} Hz)", fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = prefs.cyclesPerFrame.toFloat().coerceIn(4f, 1000f),
                onValueChange = { controller.setCyclesPerFrame(it.roundToInt()) },
                valueRange = 4f..1000f
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12 to "720 Hz (VIP)", 20 to "1.2 kHz (SCHIP)", 100 to "6 kHz (XO)", 500 to "30 kHz (XO)", 1000 to "60 kHz (XO Max)").forEach { (ipf, label) ->
                    FilterChip(
                        selected = prefs.cyclesPerFrame == ipf,
                        onClick = { controller.setCyclesPerFrame(ipf) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        HorizontalDivider()
        SectionTitle("Fine-Grained Quirk Overrides")

        Text("Memory Increment on FX55 / FX65", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Quirks.MemoryIncrement.entries.forEach { mode ->
                FilterChip(
                    selected = quirks.memoryIncrement() == mode,
                    onClick = { controller.updateQuirks { it.withMemoryIncrement(mode) } },
                    label = { Text(mode.label(), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        ToggleRow("VF Reset on Logic Ops", "8XY1/8XY2/8XY3 clear VF to 0 (COSMAC VIP)", quirks.vfReset()) { v ->
            controller.updateQuirks { it.withVfReset(v) }
        }
        ToggleRow("Display Wait (VBlank Sync)", "DXYN pauses until the next 60 Hz refresh", quirks.displayWait()) { v ->
            controller.updateQuirks { it.withDisplayWait(v) }
        }
        ToggleRow("Sprite Clipping", "Clip sprites at screen edges instead of wrapping around", quirks.clipping()) { v ->
            controller.updateQuirks { it.withClipping(v) }
        }
        ToggleRow("Shift Uses VY", "8XY6/8XYE shift VY into VX instead of shifting VX in-place", quirks.shiftUsesVy()) { v ->
            controller.updateQuirks { it.withShiftUsesVy(v) }
        }
        ToggleRow("Jump Uses V0 (BNNN)", "Enabled: BNNN + V0 • Disabled: BXNN + VX", quirks.jumpUsesV0()) { v ->
            controller.updateQuirks { it.withJumpUsesV0(v) }
        }
        ToggleRow("Wait for Key Release (FX0A)", "Wait for press AND release like COSMAC VIP hardware", quirks.waitForKeyRelease()) { v ->
            controller.updateQuirks { it.withWaitForKeyRelease(v) }
        }
        ToggleRow("16×16 Sprites in Lo-Res", "Allow DXY0 tall sprites even in 64×32 mode (XO-CHIP)", quirks.loResTallSprites()) { v ->
            controller.updateQuirks { it.withLoResTallSprites(v) }
        }
    }
}

@Composable
private fun AudioAndBuzzerTab(controller: EmulatorController) {
    val prefs = controller.preferences
    val audioCfg = prefs.toAudioConfig()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Sound & Buzzer Synthesizer")

        ToggleRow("Mute All Audio", "Completely silence CHIP-8 buzzer and XO-CHIP pattern audio", prefs.audioMuted) {
            controller.toggleMute()
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Master Volume", style = MaterialTheme.typography.bodyMedium)
                Text("${(prefs.audioVolume * 100).roundToInt()}%", fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = prefs.audioVolume,
                onValueChange = { controller.setAudioVolume(it) },
                valueRange = 0f..1f
            )
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Buzzer Pitch", style = MaterialTheme.typography.bodyMedium)
                Text("${prefs.audioFrequencyHz} Hz", fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = prefs.audioFrequencyHz.toFloat(),
                onValueChange = { controller.setAudioFrequency(it.roundToInt()) },
                valueRange = 180f..1760f
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { controller.playTestBeep() }) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Test Buzzer Tone")
            }
            Text(
                "XO-CHIP ROMs automatically use the 16-byte pattern synthesizer (F002 / FX3A).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()
        SectionTitle("Buzzer Waveform Preset")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AudioConfig.BuzzerWaveform.entries.forEach { wf ->
                val selected = audioCfg.waveform() == wf
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            controller.setAudioWaveform(wf)
                            controller.playTestBeep()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(wf.displayName(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(wf.description(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsAndGamepadTab(controller: EmulatorController) {
    val prefs = controller.preferences
    val gpStatus = controller.gamepadStatus

    val reverseKeys = remember(prefs.keyboardBindings) {
        prefs.keyboardBindings.entries.groupBy({ it.value }, { it.key })
    }
    val reverseGp = remember(prefs.gamepadBindings) {
        prefs.gamepadBindings.entries.groupBy({ it.value }, { it.key })
    }

    val keypadRows = listOf(
        listOf(0x1, 0x2, 0x3, 0xC),
        listOf(0x4, 0x5, 0x6, 0xD),
        listOf(0x7, 0x8, 0x9, 0xE),
        listOf(0xA, 0x0, 0xB, 0xF)
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("External Gamepad (SDL3 + input4j)")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Rounded.Gamepad,
                    contentDescription = null,
                    tint = if (gpStatus.connectedNames().isNotEmpty()) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(gpStatus.statusMessage(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Connect any Bluetooth or USB controller (Xbox, PlayStation, Switch Pro, 8BitDo) — hot-plug supported.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionTitle("Input Presets")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InputPreset.ALL.forEach { preset ->
                FilterChip(
                    selected = prefs.inputPresetId == preset.id(),
                    onClick = { controller.applyInputPreset(preset) },
                    label = { Text(preset.displayName()) }
                )
            }
        }

        if (controller.capturingKeyForHex != null || controller.capturingGamepadForHex != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val prompt = if (controller.capturingKeyForHex != null) {
                        "Press any keyboard key to bind to CHIP-8 Key %X (Esc to cancel)".format(controller.capturingKeyForHex)
                    } else {
                        "Press any gamepad button or D-Pad direction to bind to CHIP-8 Key %X".format(controller.capturingGamepadForHex)
                    }
                    Text(prompt, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { controller.cancelCaptures() }) { Text("Cancel") }
                }
            }
        }

        SectionTitle("Interactive 4×4 Hex Keypad Mapper")
        keypadRows.forEach { rowHex ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowHex.forEach { hex ->
                    val kbText = reverseKeys[hex]?.take(2)?.joinToString("/") ?: "—"
                    val gpText = reverseGp[hex]?.firstOrNull() ?: "—"
                    val isCapturing = controller.capturingKeyForHex == hex || controller.capturingGamepadForHex == hex
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCapturing) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (isCapturing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("0x%X".format(hex), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("KB: $kbText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("GP: $gpText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        controller.cancelCaptures()
                                        controller.capturingKeyForHex = hex
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Keyboard, null, modifier = Modifier.size(14.dp))
                                }
                                OutlinedButton(
                                    onClick = { controller.beginGamepadCapture(hex) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Gamepad, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
