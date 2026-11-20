package net.saga.console.chip8.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.saga.console.chip8.app.controller.EmulatorController
import net.saga.console.chip8.app.theme.DisplayPalette
import net.saga.console.chip8.core.Chip8Variant
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrtViewport(
    controller: EmulatorController,
    palette: DisplayPalette,
    onOpenFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = controller.preferences
    val snapshot = controller.snapshot
    val display = snapshot.display()
    val phosphor = controller.phosphorPixels
    val isRunning = controller.isRomLoaded && !controller.isPaused

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error banner with one-click variant recovery
        AnimatedVisibility(visible = controller.errorBanner != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null)
                    Text(
                        text = controller.errorBanner ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (controller.errorBanner?.contains("Mega-Chip") == true) {
                        FilledTonalButton(onClick = {
                            controller.setVariant(Chip8Variant.MEGA_CHIP)
                            controller.resetMachine()
                        }) { Text("Switch to Mega-Chip") }
                    } else if (controller.errorBanner?.contains("SUPER-CHIP") == true) {
                        FilledTonalButton(onClick = {
                            controller.setVariant(Chip8Variant.SUPER_CHIP_1_1)
                            controller.resetMachine()
                        }) { Text("Switch to SCHIP 1.1") }
                    } else if (controller.errorBanner?.contains("XO-CHIP") == true) {
                        FilledTonalButton(onClick = {
                            controller.setVariant(Chip8Variant.XO_CHIP)
                            controller.resetMachine()
                        }) { Text("Switch to XO-CHIP") }
                    }
                }
            }
        }

        // Main CRT bezel frame
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isRunning) palette.plane0.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Screen area (2:1 aspect ratio centered cleanly inside bezel)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (!controller.isRomLoaded) {
                        EmptyViewportOverlay(
                            controller = controller,
                            palette = palette,
                            onOpenFilePicker = onOpenFilePicker
                        )
                    } else {
                        val isMega = display.megaMode() && display.argbPixels() != null
                        val screenAspect = if (isMega) (256f / 192f) else 2f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .aspectRatio(screenAspect)
                                    .fillMaxSize()
                            ) {
                                val w = display.width()
                                val h = display.height()

                                if (isMega) {
                                    val argb = display.argbPixels()!!
                                    val bmp = argbToImageBitmap(w, h, argb)
                                    drawImage(
                                        image = bmp,
                                        dstOffset = IntOffset.Zero,
                                        dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
                                        filterQuality = FilterQuality.None
                                    )
                                    if (prefs.scanlinesEnabled) {
                                        val scanColor = Color.Black.copy(alpha = 0.16f)
                                        val step = max(2.5f, size.height / h.toFloat())
                                        var sy = 0f
                                        while (sy < size.height) {
                                            drawLine(scanColor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
                                            sy += step
                                        }
                                    }
                                    return@Canvas
                                }

                                val cellW = size.width / w.toFloat()
                                val cellH = size.height / h.toFloat()
                                val pixels = display.pixels()

                                // Fill CRT background
                                drawRect(color = palette.background)

                                // Subtle phosphor glow behind lit cells
                                val glowEnabled = prefs.phosphorGlowEnabled
                                val glowColor = palette.plane0.copy(alpha = 0.14f)

                                for (y in 0 until h) {
                                    val rowOffset = y * w
                                    val py = y * cellH
                                    for (x in 0 until w) {
                                        val idx = rowOffset + x
                                        val base = idx * 3
                                        val p1 = if (base + 2 < phosphor.size) phosphor[base] else if (pixels[idx].toInt() == 1) 1f else 0f
                                        val p2 = if (base + 2 < phosphor.size) phosphor[base + 1] else if (pixels[idx].toInt() == 2) 1f else 0f
                                        val p3 = if (base + 2 < phosphor.size) phosphor[base + 2] else if (pixels[idx].toInt() == 3) 1f else 0f

                                        val peak = max(p1, max(p2, p3))
                                        if (peak < 0.02f) continue

                                        val activeColor = when {
                                            p3 >= p1 && p3 >= p2 -> lerp(palette.background, palette.bothPlanes, p3)
                                            p2 >= p1 -> lerp(palette.background, palette.plane1, p2)
                                            else -> lerp(palette.background, palette.plane0, p1)
                                        }

                                        val px = x * cellW
                                        if (glowEnabled && peak > 0.55f) {
                                            drawRect(
                                                color = glowColor,
                                                topLeft = Offset(px - cellW * 0.25f, py - cellH * 0.25f),
                                                size = Size(cellW * 1.5f, cellH * 1.5f)
                                            )
                                        }
                                        drawRect(
                                            color = activeColor,
                                            topLeft = Offset(px, py),
                                            size = Size(cellW + 0.45f, cellH + 0.45f)
                                        )
                                    }
                                }

                                // Optional hairline pixel grid
                                if (prefs.pixelGridEnabled) {
                                    val gridColor = palette.background.copy(alpha = 0.45f)
                                    for (x in 1 until w) {
                                        val gx = x * cellW
                                        drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 0.7f)
                                    }
                                    for (y in 1 until h) {
                                        val gy = y * cellH
                                        drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 0.7f)
                                    }
                                }

                                // Optional subtle CRT scanlines
                                if (prefs.scanlinesEnabled) {
                                    val scanColor = Color.Black.copy(alpha = 0.18f)
                                    val step = max(2.5f, cellH / 2f)
                                    var sy = 0f
                                    while (sy < size.height) {
                                        drawLine(scanColor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
                                        sy += step
                                    }
                                }
                            }
                        }

                        // Subtle pause/rewind/turbo floating badges
                        if (controller.isPaused || controller.isRewinding || controller.isTurbo) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                tonalElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    when {
                                        controller.isRewinding -> {
                                            Icon(Icons.Rounded.FastRewind, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                                            Text("REWINDING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        controller.isTurbo -> {
                                            Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("TURBO 4x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        controller.isPaused -> {
                                            Text("PAUSED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // HUD Status Strip inside the CRT bezel
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HudPill(
                        icon = Icons.Rounded.Memory,
                        label = "${prefs.coreVariant().shortName()} • ${display.width()}×${display.height()}",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    HudPill(
                        label = "${controller.measuredFps} FPS • ${prefs.cyclesPerFrame * 60} Hz",
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    AudioOscilloscopePill(
                        waveform = controller.waveform,
                        muted = prefs.audioMuted,
                        active = controller.audioPlaying,
                        onClick = { controller.toggleMute() }
                    )
                    val gp = controller.gamepadStatus
                    HudPill(
                        icon = Icons.Rounded.Gamepad,
                        label = if (gp.connectedNames().isNotEmpty()) gp.connectedNames().first() else "No Gamepad",
                        accent = if (gp.connectedNames().isNotEmpty()) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline
                    )
                    controller.loadedRomControls?.let { hint ->
                        val hintInteraction = remember { MutableInteractionSource() }
                        val hintHovered by hintInteraction.collectIsHoveredAsState()
                        val singleLineHint = hint.lineSequence().firstOrNull() ?: ""
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                            modifier = Modifier.hoverable(hintInteraction)
                        ) {
                            Text(
                                text = singleLineHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = if (hintHovered) TextOverflow.Clip else TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .then(
                                        if (hintHovered) {
                                            Modifier.basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                initialDelayMillis = 250,
                                                repeatDelayMillis = 1500,
                                                velocity = 38.dp
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyViewportOverlay(
    controller: EmulatorController,
    palette: DisplayPalette,
    onOpenFilePicker: () -> Unit
) {
    Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = palette.plane0.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, palette.plane0.copy(alpha = 0.45f)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = palette.plane0,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = "CHIP-8 Studio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = palette.plane0
        )
        Text(
            text = "Drop a .ch8, .sc8, .xo8, or .mc8 ROM anywhere onto this window, or choose one below.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.plane0.copy(alpha = 0.78f)
        )
        Button(onClick = onOpenFilePicker) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open ROM File")
        }

        val quickPicks = controller.scannedRoms.filter {
            it.displayName().contains("Pong", true) ||
                it.displayName().contains("Tetris", true) ||
                it.displayName().contains("Space Invaders", true) ||
                it.displayName().contains("IBM", true)
        }.take(4)

        if (quickPicks.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPicks.forEach { rom ->
                    AssistChip(
                        onClick = { controller.loadRom(rom.path()) },
                        label = { Text(rom.displayName()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HudPill(
    label: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            } else {
                Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AudioOscilloscopePill(
    waveform: FloatArray,
    muted: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val strokeColor = when {
        muted -> MaterialTheme.colorScheme.outline
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, strokeColor.copy(alpha = 0.35f)),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = if (muted) "Unmute" else "Mute",
                tint = strokeColor,
                modifier = Modifier.size(14.dp)
            )
            Canvas(modifier = Modifier.width(42.dp).height(14.dp)) {
                val midY = size.height / 2f
                if (muted || waveform.isEmpty()) {
                    drawLine(strokeColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.4f)
                } else {
                    val path = Path()
                    val stepX = size.width / (waveform.size - 1).coerceAtLeast(1)
                    for (i in waveform.indices) {
                        val x = i * stepX
                        val y = midY - (waveform[i].coerceIn(-1f, 1f) * (size.height * 0.45f))
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = strokeColor, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

internal fun argbToImageBitmap(width: Int, height: Int, argb: IntArray): ImageBitmap {
    val bytes = ByteArray(width * height * 4)
    var out = 0
    for (i in argb.indices) {
        val px = argb[i]
        bytes[out++] = (px and 0xFF).toByte()          // B
        bytes[out++] = ((px ushr 8) and 0xFF).toByte() // G
        bytes[out++] = ((px ushr 16) and 0xFF).toByte()// R
        bytes[out++] = ((px ushr 24) and 0xFF).toByte()// A
    }
    val skiaBitmap = Bitmap()
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    skiaBitmap.allocPixels(info)
    skiaBitmap.installPixels(info, bytes, width * 4)
    return skiaBitmap.asComposeImageBitmap()
}
