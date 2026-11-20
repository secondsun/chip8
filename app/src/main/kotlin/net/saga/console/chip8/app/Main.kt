package net.saga.console.chip8.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.saga.console.chip8.app.controller.EmulatorController
import net.saga.console.chip8.app.theme.Chip8StudioTheme
import net.saga.console.chip8.app.ui.CrtViewport
import net.saga.console.chip8.app.ui.DebuggerPane
import net.saga.console.chip8.app.ui.RomLibraryPane
import net.saga.console.chip8.app.ui.SaveStatesDialog
import net.saga.console.chip8.app.ui.SettingsDialog
import net.saga.console.chip8.core.RomDatabase
import java.awt.FileDialog
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.nio.file.Path
import javax.swing.JFileChooser

fun main() = application {
    val controller = remember { EmulatorController() }
    val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))

    DisposableEffect(Unit) {
        onDispose { controller.dispose() }
    }

    Window(
        onCloseRequest = {
            controller.dispose()
            exitApplication()
        },
        state = windowState,
        title = if (controller.isRomLoaded) "CHIP-8 Studio — ${controller.loadedRomTitle}" else "CHIP-8 Studio",
        onPreviewKeyEvent = { controller.handleKeyEvent(it) }
    ) {
        // Attach native file drag-and-drop onto the Compose window
        LaunchedEffect(window) {
            attachRomDropTarget(window) { path ->
                controller.loadRom(path)
            }
        }

        val displayPalette = controller.activeDisplayPalette()

        Chip8StudioTheme(
            preferences = controller.preferences,
            osAppearance = controller.osAppearance,
            displayPalette = displayPalette
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val openRomPicker: () -> Unit = {
                    pickRomFile(window)?.let { controller.loadRom(it) }
                }
                val addFolderPicker: () -> Unit = {
                    pickDirectory(window)?.let { controller.addRomDirectory(it) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopStudioBar(
                        controller = controller,
                        onOpenRom = openRomPicker
                    )

                    // Responsive adaptive workspace
                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val isCompact = maxWidth < 760.dp
                        val isExpanded = maxWidth >= 1120.dp
                        val prefs = controller.preferences

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnimatedVisibility(visible = prefs.showLibraryPane && !isCompact) {
                                RomLibraryPane(
                                    controller = controller,
                                    onOpenFilePicker = openRomPicker,
                                    onAddFolderPicker = addFolderPicker,
                                    modifier = Modifier.width(if (isExpanded) 280.dp else 240.dp)
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                CrtViewport(
                                    controller = controller,
                                    palette = displayPalette,
                                    onOpenFilePicker = openRomPicker
                                )
                            }

                            AnimatedVisibility(visible = prefs.showDebuggerPane && !isCompact) {
                                DebuggerPane(
                                    controller = controller,
                                    modifier = Modifier.width(if (isExpanded) 320.dp else 280.dp)
                                )
                            }
                        }

                        // Compact window modal overlays when Library or Debugger toggled
                        if (isCompact && prefs.showLibraryPane) {
                            RomLibraryPane(
                                controller = controller,
                                onOpenFilePicker = openRomPicker,
                                onAddFolderPicker = addFolderPicker,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isCompact && prefs.showDebuggerPane) {
                            DebuggerPane(
                                controller = controller,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (controller.showSettingsDialog) {
                    SettingsDialog(
                        controller = controller,
                        onDismiss = { controller.showSettingsDialog = false }
                    )
                }

                if (controller.showSaveStatesSheet) {
                    SaveStatesDialog(
                        controller = controller,
                        palette = displayPalette,
                        onDismiss = { controller.showSaveStatesSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopStudioBar(
    controller: EmulatorController,
    onOpenRom: () -> Unit
) {
    val prefs = controller.preferences

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Library toggle, Open ROM, Active ROM Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                FilledIconToggleButton(
                    checked = prefs.showLibraryPane,
                    onCheckedChange = { controller.toggleLibraryPane() }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.LibraryBooks, contentDescription = "Toggle ROM Library")
                }

                FilledTonalIconButton(onClick = onOpenRom) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open ROM File")
                }

                val titleHoverSource = remember { MutableInteractionSource() }
                val titleHovered by titleHoverSource.collectIsHoveredAsState()
                val marqueeTitle = controller.isRomLoaded || titleHovered

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 8.dp)
                        .hoverable(titleHoverSource)
                ) {
                    Text(
                        text = controller.loadedRomTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = if (marqueeTitle) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (marqueeTitle) {
                                    Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = if (titleHovered) 250 else 1000,
                                        repeatDelayMillis = 1800,
                                        velocity = 38.dp
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                    Text(
                        text = controller.statusBanner ?: "Java 27 • Compose Material 3 • ${prefs.coreVariant().displayName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = if (titleHovered) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (titleHovered) {
                                    Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 250,
                                        repeatDelayMillis = 1600,
                                        velocity = 38.dp
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }

            // Center: Transport controls (Play/Pause, Step, Reset, Rewind, Turbo)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { controller.togglePause() },
                    enabled = controller.isRomLoaded
                ) {
                    Icon(
                        imageVector = if (controller.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (controller.isPaused) "Resume (F5)" else "Pause (F5)"
                    )
                }

                IconButton(
                    onClick = { controller.stepOnce() },
                    enabled = controller.isRomLoaded
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Step Instruction (F6)")
                }

                IconButton(
                    onClick = { controller.resetMachine() },
                    enabled = controller.isRomLoaded
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset (F8)")
                }

                // Hold-to-Rewind button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (controller.isRewinding) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(40.dp)
                        .pointerInput(controller.isRomLoaded) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                if (controller.isRomLoaded && controller.rewindCount > 0) {
                                    controller.isRewinding = true
                                }
                                waitForUpOrCancellation()
                                controller.isRewinding = false
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            contentDescription = "Hold to Rewind (Backspace)",
                            tint = if (controller.rewindCount > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                }

                FilledIconToggleButton(
                    checked = controller.isTurbo,
                    onCheckedChange = { controller.isTurbo = it },
                    enabled = controller.isRomLoaded
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = "Turbo 4x (Hold Tab)")
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right: Save States, Debugger toggle, Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { controller.showSaveStatesSheet = true },
                    enabled = controller.isRomLoaded
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save States")
                }

                FilledIconToggleButton(
                    checked = prefs.showDebuggerPane,
                    onCheckedChange = { controller.toggleDebuggerPane() }
                ) {
                    Icon(Icons.Rounded.BugReport, contentDescription = "Toggle Debugger")
                }

                FilledTonalIconButton(onClick = {
                    controller.settingsInitialTab = 0
                    controller.showSettingsDialog = true
                }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

private fun attachRomDropTarget(window: ComposeWindow, onRomDropped: (Path) -> Unit) {
    window.dropTarget = DropTarget(window, object : DropTargetAdapter() {
        @Suppress("UNCHECKED_CAST")
        override fun drop(dtde: DropTargetDropEvent) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                val files = dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                val rom = files?.firstOrNull { RomDatabase.isRomFile(it.toPath()) }
                if (rom != null) {
                    onRomDropped(rom.toPath())
                    dtde.dropComplete(true)
                } else {
                    dtde.dropComplete(false)
                }
            } catch (_: Exception) {
                dtde.dropComplete(false)
            }
        }
    })
}

private fun pickRomFile(window: ComposeWindow): Path? {
    val dialog = FileDialog(window, "Open CHIP-8 ROM", FileDialog.LOAD).apply {
        setFilenameFilter { _, name ->
            val lower = name.lowercase()
            lower.endsWith(".ch8") || lower.endsWith(".sc8") || lower.endsWith(".xo8") || lower.endsWith(".mc8")
        }
        isVisible = true
    }
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return Path.of(dir, file)
}

private fun pickDirectory(window: ComposeWindow): Path? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Add ROM Directory"
    }
    return if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.toPath()
    } else {
        null
    }
}
