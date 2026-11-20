package net.saga.console.chip8.core.input;

import java.util.List;
import java.util.Map;

/**
 * Named keyboard & gamepad mapping presets tailored to classic CHIP-8 control conventions.
 */
public record InputPreset(
    String id,
    String displayName,
    String description,
    Map<String, Integer> keyboardBindings,
    Map<String, Integer> gamepadBindings
) {
    /**
     * Standard COSMAC VIP 4x4 hex keypad mapped to 1234 / QWER / ASDF / ZXCV.
     */
    public static final InputPreset COSMAC = new InputPreset(
        "cosmac",
        "COSMAC 4x4 Standard",
        "Classic 1234 / QWER / ASDF / ZXCV hex keypad + D-Pad & face buttons",
        Map.ofEntries(
            Map.entry("1", 0x1), Map.entry("2", 0x2), Map.entry("3", 0x3), Map.entry("4", 0xC),
            Map.entry("Q", 0x4), Map.entry("W", 0x5), Map.entry("E", 0x6), Map.entry("R", 0xD),
            Map.entry("A", 0x7), Map.entry("S", 0x8), Map.entry("D", 0x9), Map.entry("F", 0xE),
            Map.entry("Z", 0xA), Map.entry("X", 0x0), Map.entry("C", 0xB), Map.entry("V", 0xF)
        ),
        Map.ofEntries(
            Map.entry("DPAD_UP", 0x2),
            Map.entry("DPAD_DOWN", 0x8),
            Map.entry("DPAD_LEFT", 0x4),
            Map.entry("DPAD_RIGHT", 0x6),
            Map.entry("BTN_0", 0x5),
            Map.entry("BTN_1", 0x6),
            Map.entry("BTN_2", 0x4),
            Map.entry("BTN_3", 0x2),
            Map.entry("BTN_4", 0x1),
            Map.entry("BTN_5", 0xC),
            Map.entry("BTN_6", 0xA),
            Map.entry("BTN_7", 0xF),
            Map.entry("BTN_8", 0x0),
            Map.entry("BTN_9", 0x5)
        )
    );

    public static final InputPreset DIRECTIONAL = new InputPreset(
        "directional",
        "Directional (Arrows + Space)",
        "Arrow keys / WASD on 2-4-6-8 with Space/Enter on 5",
        Map.ofEntries(
            Map.entry("UP", 0x2), Map.entry("DOWN", 0x8), Map.entry("LEFT", 0x4), Map.entry("RIGHT", 0x6),
            Map.entry("W", 0x2), Map.entry("S", 0x8), Map.entry("A", 0x4), Map.entry("D", 0x6),
            Map.entry("SPACE", 0x5), Map.entry("ENTER", 0x5),
            Map.entry("1", 0x1), Map.entry("3", 0x3), Map.entry("E", 0xE), Map.entry("Q", 0x7),
            Map.entry("Z", 0xA), Map.entry("X", 0x0), Map.entry("C", 0xB), Map.entry("V", 0xF)
        ),
        Map.ofEntries(
            Map.entry("DPAD_UP", 0x2),
            Map.entry("DPAD_DOWN", 0x8),
            Map.entry("DPAD_LEFT", 0x4),
            Map.entry("DPAD_RIGHT", 0x6),
            Map.entry("BTN_0", 0x5),
            Map.entry("BTN_1", 0x0),
            Map.entry("BTN_2", 0xA),
            Map.entry("BTN_3", 0xF),
            Map.entry("BTN_9", 0x5)
        )
    );

    public static final InputPreset PONG = new InputPreset(
        "pong",
        "Pong (1P / 2P)",
        "P1: W/S or 1/Q -> 1/4 • P2: Up/Down or 4/R -> C/D",
        Map.ofEntries(
            Map.entry("1", 0x1), Map.entry("Q", 0x4),
            Map.entry("W", 0x1), Map.entry("S", 0x4),
            Map.entry("UP", 0xC), Map.entry("DOWN", 0xD),
            Map.entry("4", 0xC), Map.entry("R", 0xD),
            Map.entry("2", 0x2), Map.entry("3", 0x3), Map.entry("E", 0x6),
            Map.entry("A", 0x7), Map.entry("D", 0x9), Map.entry("F", 0xE),
            Map.entry("Z", 0xA), Map.entry("X", 0x0), Map.entry("C", 0xB), Map.entry("V", 0xF)
        ),
        Map.ofEntries(
            Map.entry("DPAD_UP", 0x1),
            Map.entry("DPAD_DOWN", 0x4),
            Map.entry("BTN_3", 0xC),
            Map.entry("BTN_0", 0xD),
            Map.entry("BTN_4", 0x1),
            Map.entry("BTN_5", 0xC)
        )
    );

    public static final InputPreset TETRIS = new InputPreset(
        "tetris",
        "Tetris",
        "Left/Right (A/D or Arrows) -> 5/6, Rotate (W/Up/Space) -> 4, Drop (S/Down) -> 7",
        Map.ofEntries(
            Map.entry("LEFT", 0x5), Map.entry("RIGHT", 0x6), Map.entry("UP", 0x4), Map.entry("DOWN", 0x7),
            Map.entry("A", 0x5), Map.entry("D", 0x6), Map.entry("W", 0x4), Map.entry("S", 0x7),
            Map.entry("SPACE", 0x4),
            Map.entry("Q", 0x4), Map.entry("E", 0x6),
            Map.entry("1", 0x1), Map.entry("2", 0x2), Map.entry("3", 0x3), Map.entry("4", 0xC),
            Map.entry("Z", 0xA), Map.entry("X", 0x0), Map.entry("C", 0xB), Map.entry("V", 0xF)
        ),
        Map.ofEntries(
            Map.entry("DPAD_LEFT", 0x5),
            Map.entry("DPAD_RIGHT", 0x6),
            Map.entry("DPAD_DOWN", 0x7),
            Map.entry("DPAD_UP", 0x4),
            Map.entry("BTN_0", 0x4),
            Map.entry("BTN_1", 0x4),
            Map.entry("BTN_2", 0x7)
        )
    );

    public static final InputPreset SPACE_INVADERS = new InputPreset(
        "space_invaders",
        "Space Invaders",
        "Left/Right (A/D or Arrows) -> 4/6, Fire/Start (Space/W) -> 5",
        Map.ofEntries(
            Map.entry("LEFT", 0x4), Map.entry("RIGHT", 0x6),
            Map.entry("A", 0x4), Map.entry("D", 0x6),
            Map.entry("SPACE", 0x5), Map.entry("W", 0x5), Map.entry("UP", 0x5), Map.entry("ENTER", 0x5),
            Map.entry("Q", 0x4), Map.entry("E", 0x6),
            Map.entry("1", 0x1), Map.entry("2", 0x2), Map.entry("3", 0x3), Map.entry("4", 0xC),
            Map.entry("S", 0x8), Map.entry("F", 0xE),
            Map.entry("Z", 0xA), Map.entry("X", 0x0), Map.entry("C", 0xB), Map.entry("V", 0xF)
        ),
        Map.ofEntries(
            Map.entry("DPAD_LEFT", 0x4),
            Map.entry("DPAD_RIGHT", 0x6),
            Map.entry("BTN_0", 0x5),
            Map.entry("BTN_1", 0x5),
            Map.entry("BTN_9", 0x5)
        )
    );

    /**
     * Octo / Modern SCHIP & XO-CHIP layout: standard COSMAC 4x4 (1234/QWER/ASDF/ZXCV)
     * plus Arrow keys mapped to W/A/S/D (5/7/8/9) and Space/Enter mapped to E (6).
     */
    public static final InputPreset OCTO = new InputPreset(
        "octo",
        "Octo (WASD/Arrows + COSMAC)",
        "Full 1234/QWER/ASDF/ZXCV keypad + Arrows→5/7/8/9 (WASD) & Space→6 (E)",
        Map.ofEntries(
            Map.entry("1", 0x1), Map.entry("2", 0x2), Map.entry("3", 0x3), Map.entry("4", 0xC),
            Map.entry("Q", 0x4), Map.entry("W", 0x5), Map.entry("E", 0x6), Map.entry("R", 0xD),
            Map.entry("A", 0x7), Map.entry("S", 0x8), Map.entry("D", 0x9), Map.entry("F", 0xE),
            Map.entry("Z", 0xA), Map.entry("X", 0x0), Map.entry("C", 0xB), Map.entry("V", 0xF),
            Map.entry("UP", 0x5), Map.entry("DOWN", 0x8), Map.entry("LEFT", 0x7), Map.entry("RIGHT", 0x9),
            Map.entry("SPACE", 0x6), Map.entry("ENTER", 0x6)
        ),
        Map.ofEntries(
            Map.entry("DPAD_UP", 0x5),
            Map.entry("DPAD_DOWN", 0x8),
            Map.entry("DPAD_LEFT", 0x7),
            Map.entry("DPAD_RIGHT", 0x9),
            Map.entry("BTN_0", 0x6),
            Map.entry("BTN_1", 0x4),
            Map.entry("BTN_2", 0xA),
            Map.entry("BTN_3", 0xF),
            Map.entry("BTN_4", 0x1),
            Map.entry("BTN_5", 0xC),
            Map.entry("BTN_9", 0x6)
        )
    );

    public static final List<InputPreset> ALL = List.of(COSMAC, OCTO, DIRECTIONAL, PONG, TETRIS, SPACE_INVADERS);

    public static InputPreset byId(String id) {
        if (id == null) {
            return COSMAC;
        }
        return ALL.stream()
            .filter(p -> p.id().equalsIgnoreCase(id))
            .findFirst()
            .orElse(COSMAC);
    }
}
