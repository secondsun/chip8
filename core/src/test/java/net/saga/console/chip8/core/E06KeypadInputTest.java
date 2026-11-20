package net.saga.console.chip8.core;

import java.io.IOException;
import net.saga.console.chip8.core.input.KeypadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class E06KeypadInputTest {
    private Chip8Machine chip8;

    @BeforeEach
    void setUp() throws IOException {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        try (var stream = getClass().getResourceAsStream("/E06KeypadLoop.ch8")) {
            chip8.loadRom(stream.readAllBytes());
        }
    }

    @Test
    void testChip8WaitsAndContinuesAfterKeyboardInput() {
        int pc = chip8.getPC();
        chip8.stepInstruction();
        chip8.stepInstruction();
        assertEquals(pc, chip8.getPC());

        chip8.keypad().press(0xA);
        chip8.stepInstruction();
        chip8.stepInstruction();
        assertEquals(0xA, chip8.getV(6));
    }

    @Test
    void testCosmacVipWaitForKeyRelease() {
        Chip8Machine vip = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.COSMAC_VIP));
        vip.writeMemoryByte(0x200, (byte) 0xF3);
        vip.writeMemoryByte(0x201, (byte) 0x0A); // LD V3, K
        vip.writeMemoryByte(0x202, (byte) 0x60);
        vip.writeMemoryByte(0x203, (byte) 0x99); // LD V0, 0x99

        vip.stepInstruction(); // Enters wait state
        vip.keypad().press(0xB);
        vip.stepInstruction(); // Registers press, still waiting for release
        assertEquals(0x00, vip.getV(3));

        vip.keypad().release(0xB);
        vip.stepInstruction(); // Completes FX0A on release
        assertEquals(0xB, vip.getV(3));

        vip.stepInstruction(); // Advances to next instruction
        assertEquals(0x99, vip.getV(0));
    }

    @Test
    void testSkipIfPressedAndNotPressedAndMultiSourceKeypad() {
        chip8.keypad().setKey(KeypadState.Source.KEYBOARD, 0x2, true);
        chip8.keypad().setKey(KeypadState.Source.GAMEPAD, 0x2, true);
        chip8.keypad().setKey(KeypadState.Source.KEYBOARD, 0x2, false);
        assertTrue(chip8.keypad().isPressed(0x2)); // Gamepad still holding key 2

        chip8.executeOpcode(0x6002);
        chip8.executeOpcode(0xE09E);
        assertEquals(0x202, chip8.getPC());

        chip8.keypad().setKey(KeypadState.Source.GAMEPAD, 0x2, false);
        assertFalse(chip8.keypad().isPressed(0x2));

        chip8.executeOpcode(0xE0A1);
        assertEquals(0x204, chip8.getPC());
    }
}
