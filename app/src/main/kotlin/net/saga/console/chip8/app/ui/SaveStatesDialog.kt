package net.saga.console.chip8.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.saga.console.chip8.app.controller.EmulatorController
import net.saga.console.chip8.app.theme.DisplayPalette
import net.saga.console.chip8.core.SaveState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
fun SaveStatesDialog(
    controller: EmulatorController,
    palette: DisplayPalette,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Save States", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Quick hotkeys: F1–F4 to Save • Shift+F1–F4 to Load",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(14.dp))

                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 4) {
                            val slotIdx = row * 4 + col
                            SaveSlotCard(
                                slotIndex = slotIdx,
                                state = controller.saveSlots[slotIdx],
                                palette = palette,
                                canSave = controller.isRomLoaded,
                                onSave = { controller.saveToSlot(slotIdx) },
                                onLoad = {
                                    controller.loadFromSlot(slotIdx)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (row == 0) Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SaveSlotCard(
    slotIndex: Int,
    state: SaveState?,
    palette: DisplayPalette,
    canSave: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (state != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Slot ${slotIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (state != null) {
                    Text(
                        timeFmt.format(state.savedAt()),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.background),
                contentAlignment = Alignment.Center
            ) {
                if (state == null) {
                    Text(
                        "Empty",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.plane0.copy(alpha = 0.45f)
                    )
                } else {
                    val thumb = state.thumbnail()
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = thumb.width()
                        val h = thumb.height()
                        if (thumb.megaMode() && thumb.argbPixels() != null) {
                            val bmp = argbToImageBitmap(w, h, thumb.argbPixels()!!)
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
                                filterQuality = FilterQuality.None
                            )
                            return@Canvas
                        }
                        val cw = size.width / w.toFloat()
                        val ch = size.height / h.toFloat()
                        val pixels = thumb.pixels()
                        for (y in 0 until h) {
                            for (x in 0 until w) {
                                val c = pixels[y * w + x].toInt() and 0b11
                                if (c != 0) {
                                    drawRect(
                                        color = palette.colorForIndex(c),
                                        topLeft = Offset(x * cw, y * ch),
                                        size = Size(cw + 0.35f, ch + 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(14.dp))
                }
                FilledTonalButton(
                    onClick = onLoad,
                    enabled = state != null,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
