# CHIP-8 Studio

A modernized, cross-platform **CHIP-8 / CHIP-48 / SUPER-CHIP / XO-CHIP / Mega-Chip** emulator built with **OpenJDK 27 (`--enable-preview`)**, **Gradle 9.9.2**, and **Compose Multiplatform Desktop (Material 3)**.

Originally started as a Java 8 / Maven / Swing TDD tutorial by `secondsun`, this branch (`feat/modern-refresh`) upgrades the entire stack to a multi-module architecture with a pure Java 27 emulator core and a responsive, Material You desktop UI.

## Highlights

- **Pure Java 27 Emulator Core (`:core`)**
  - Uses finalized modern Java (`record`, `sealed interface`, pattern-matching `switch`, `HexFormat`, virtual threads, Java FFM API) alongside JDK 27 preview features:
    - **JEP 531**: `LazyConstant` for font tables and the SHA-1 ROM metadata database
    - **JEP 532**: Primitive types in patterns and guarded `switch`
    - **JEP 533**: `StructuredTaskScope` for concurrent ROM library scanning
- **Full CHIP-8 Family + Quirk Controls**
  - **COSMAC VIP (1977)**, **CHIP-48 (1990)**, **SUPER-CHIP 1.0 / 1.1** (`128×64`, scrolling, `16×16` sprites, large font, RPL flags), **XO-CHIP** (`64 KB` RAM, 2-bitplane 4-color graphics, scroll-up, `F000 NNNN`, `5XY2`/`5XY3`, 16-byte programmable audio pattern buffer), and **Mega-Chip (2007)** (`256×192` double-buffered ARGB8 color, `16 MB` RAM, 24-bit `01NN NNNN`, 256-color palettes, alpha/additive/multiply blend modes, collision colors, and 8-bit digitized PCM audio streaming).
  - Individual toggles for every hardware quirk (`vfReset`, `memoryIncrement`, `displayWait`, `clipping`, `shiftUsesVy`, `jumpUsesV0`, `waitForKeyRelease`, `loResTallSprites`) plus SHA-1 ROM auto-detection.
- **Disgustingly Polished Compose Desktop UI (`:app`)**
  - **Material You Dynamic Theming** via `MaterialKolor 6.0.0`, synced live to macOS tint / Windows accent / Linux XDG color scheme (with **System / Light / Dark / AMOLED** modes and custom/display-matched color seeds).
  - **Adaptive Responsive Layout** across compact, medium, and widescreen windows, plus native drag-and-drop for `.ch8`, `.sc8`, `.xo8`, and `.mc8` ROMs.
  - **CRT Display Suite**: Phosphor persistence (anti-flicker decay), optional scanlines, phosphor bloom/glow, pixel grid, and 10+ 4-color XO-CHIP palettes.
  - **Synth Audio Engine**: Click-free 44.1 kHz synthesizer with Mute, Master Volume, 6 buzzer waveform presets (`Soft Sine`, `Classic Square`, `Warm Triangle`, `Retro Sawtooth`, `25% Chiptune Pulse`, `Filtered Chiptune`), configurable pitch, full XO-CHIP 16-byte pattern playback, and a live toolbar oscilloscope pill.
  - **Keyboard & External Gamepad (LWJGL SDL3 + `input4j`)**: COSMAC 4×4 default layout (`1234 / QWER / ASDF / ZXCV`), built-in game presets, hot-plug USB/Bluetooth controller support across macOS/Windows/Linux, and an interactive visual 4×4 keypad + gamepad rebinding UI.
  - **Power Features**: Auto-scanned ROM Library, Save States (8 slots with live pixel thumbnails), Hold-to-Rewind ring buffer (`Backspace`), Turbo 4× (`Tab`), and a collapsible Live Debugger (registers, breakpoints + disassembler, hex memory viewer, clickable 4×4 keypad).

## Prerequisites

- **OpenJDK 27** (configured as active JDK)

## Running & Testing

```bash
# Run all JUnit 6 tests (core + app)
./gradlew check

# Launch CHIP-8 Studio
./gradlew :app:run

# Build native platform installer (DMG on macOS, MSI on Windows, DEB on Linux)
./gradlew :app:packageDistributionForCurrentOS
```

## Keyboard Shortcuts

| Shortcut | Action |
| --- | --- |
| `1234 / QWER / ASDF / ZXCV` | Default COSMAC 4×4 Hex Keypad |
| `F5` / `F6` / `F8` | Play/Pause • Step Instruction • Reset |
| `Tab` (hold) / `Backspace` (hold) | Turbo 4× • Live Rewind |
| `F1`–`F4` / `Shift + F1`–`F4` | Quick Save / Quick Load (Slots 1–4) |
| `Cmd/Ctrl + L` / `Cmd/Ctrl + D` | Toggle ROM Library / Toggle Live Debugger |
| `Cmd/Ctrl + M` / `Cmd/Ctrl + ,` | Mute Audio / Open Settings |
