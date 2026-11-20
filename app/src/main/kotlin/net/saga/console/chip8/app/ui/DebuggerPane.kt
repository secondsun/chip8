package net.saga.console.chip8.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.saga.console.chip8.app.controller.EmulatorController

@Composable
fun DebuggerPane(
    controller: EmulatorController,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("State", "Asm", "Mem", "Pad")

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (selectedTab) {
                    0 -> CpuStateTab(controller)
                    1 -> DisassemblerTab(controller)
                    2 -> MemoryViewerTab(controller)
                    3 -> InteractiveKeypadTab(controller)
                }
            }
        }
    }
}

@Composable
private fun CpuStateTab(controller: EmulatorController) {
    val snap = controller.snapshot
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Special Registers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatTile("PC", "0x%04X".format(snap.pc()), Modifier.weight(1f))
            StatTile("I", "0x%04X".format(snap.iRegister()), Modifier.weight(1f))
            StatTile("SP", "%d".format(snap.sp()), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatTile("DT", "%02d".format(snap.delayTimer()), Modifier.weight(1f))
            StatTile("ST", "%02d".format(snap.soundTimer()), Modifier.weight(1f))
            StatTile("Planes", "0b%s".format(snap.activePlanes().toString(2).padStart(2, '0')), Modifier.weight(1f))
        }

        HorizontalDivider()
        Text("Data Registers (V0–VF)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        val regs = snap.registers()
        for (row in 0 until 4) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 4) {
                    val idx = row * 4 + col
                    val value = regs[idx]
                    val highlight = idx == 0xF && value != 0
                    StatTile(
                        label = "V%X".format(idx),
                        value = "0x%02X".format(value),
                        modifier = Modifier.weight(1f),
                        accent = highlight
                    )
                }
            }
        }

        HorizontalDivider()
        Text("Call Stack (${snap.sp()} / 16)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        if (snap.sp() == 0) {
            Text("Empty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val stack = snap.stack()
            for (i in snap.sp() - 1 downTo 0) {
                Text(
                    text = "#%02d → 0x%04X".format(i, stack[i]),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (accent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DisassemblerTab(controller: EmulatorController) {
    val snap = controller.snapshot
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Click row to toggle breakpoint", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (snap.breakpoints().isNotEmpty()) {
                AssistChip(onClick = { controller.clearBreakpoints() }, label = { Text("Clear (${snap.breakpoints().size})") })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(snap.disassembly(), key = { it.address() }) { line ->
                val isPc = line.address() == snap.pc()
                val hasBp = snap.breakpoints().contains(line.address())
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { controller.toggleBreakpoint(line.address()) },
                    color = when {
                        isPc -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasBp) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                        )
                        Text(
                            text = line.formattedAddress(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = line.formattedOpcode(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = line.mnemonic(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isPc) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryViewerTab(controller: EmulatorController) {
    val snap = controller.snapshot
    val memory = remember(snap.totalCycles(), controller.loadedRomPath) { controller.copyMemoryBytes() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0x200 / 8)
    val scope = rememberCoroutineScope()
    val totalRows = memory.size / 8

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(onClick = { scope.launch { listState.scrollToItem(0) } }, label = { Text("Font") })
            AssistChip(onClick = { scope.launch { listState.scrollToItem(0x200 / 8) } }, label = { Text("0x200") })
            AssistChip(onClick = { scope.launch { listState.scrollToItem((snap.pc() / 8).coerceIn(0, totalRows - 1)) } }, label = { Text("PC") })
            AssistChip(onClick = { scope.launch { listState.scrollToItem((snap.iRegister() / 8).coerceIn(0, totalRows - 1)) } }, label = { Text("I") })
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(totalRows) { rowIndex ->
                val baseAddr = rowIndex * 8
                val hexBytes = buildString {
                    for (i in 0 until 8) {
                        val b = memory[baseAddr + i].toInt() and 0xFF
                        append("%02X ".format(b))
                    }
                }
                val isPcRow = snap.pc() in baseAddr until (baseAddr + 8)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isPcRow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .padding(vertical = 2.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "%04X:".format(baseAddr),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = hexBytes,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveKeypadTab(controller: EmulatorController) {
    val keypadMask = controller.snapshot.keypadMask()
    val reverseKeyLabels = remember(controller.preferences.keyboardBindings) {
        val map = mutableMapOf<Int, String>()
        controller.preferences.keyboardBindings.forEach { (keyName, hex) ->
            map.putIfAbsent(hex, keyName)
        }
        map
    }

    val layout = listOf(
        listOf(0x1, 0x2, 0x3, 0xC),
        listOf(0x4, 0x5, 0x6, 0xD),
        listOf(0x7, 0x8, 0x9, 0xE),
        listOf(0xA, 0x0, 0xB, 0xF)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Touch / Click 4×4 Hex Keypad",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        layout.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { hex ->
                    val pressed = (keypadMask and (1 shl hex)) != 0
                    val keyLegend = reverseKeyLabels[hex] ?: "—"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.1f)
                            .pointerInput(hex) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    controller.setVirtualKey(hex, true)
                                    waitForUpOrCancellation()
                                    controller.setVirtualKey(hex, false)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(
                            1.dp,
                            if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        tonalElevation = if (pressed) 6.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "%X".format(hex),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = keyLegend,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pressed) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
