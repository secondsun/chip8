package net.saga.console.chip8.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class E04FlowControlTest {
    private Chip8Machine chip8;

    @BeforeEach
    void setUp() {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        chip8.executeOpcode(0x6064);
        chip8.executeOpcode(0x6127);
        chip8.executeOpcode(0x6212);
        chip8.executeOpcode(0x63AE);
        chip8.executeOpcode(0x64FF);
    }

    @Test
    void testJumpAndBxnnQuirk() {
        chip8.executeOpcode(0x1DAE);
        assertEquals(0xDAE, chip8.getPC());

        // BNNN + V0 (0x432 + 0x64 = 1174)
        chip8.executeOpcode(0xB432);
        assertEquals(1174, chip8.getPC());

        // CHIP-48 / SCHIP BXNN quirk: B432 jumps to 0x432 + V4 (0x432 + 0xFF = 0x531)
        Chip8Machine schip = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.SUPER_CHIP_1_1));
        schip.executeOpcode(0x64FF);
        schip.executeOpcode(0xB432);
        assertEquals(0x531, schip.getPC());
    }

    @Test
    void testSubroutinesAndStackBounds() {
        chip8.executeOpcode(0x2DAE);
        assertEquals(0xDAE, chip8.getPC());
        assertEquals(1, chip8.getSP());

        chip8.executeOpcode(0x00EE);
        assertEquals(0x200, chip8.getPC());
        assertEquals(0, chip8.getSP());

        assertThrows(IllegalStateException.class, () -> chip8.executeOpcode(0x00EE));

        for (int i = 0; i < 16; i++) {
            chip8.executeOpcode(0x2300);
        }
        assertThrows(IllegalStateException.class, () -> chip8.executeOpcode(0x2300));
    }

    @Test
    void testConditionalSkipsAndInvalidOpcodes() {
        chip8.executeOpcode(0x3064);
        assertEquals(0x202, chip8.getPC());

        chip8.executeOpcode(0x3164);
        assertEquals(0x202, chip8.getPC());

        chip8.executeOpcode(0x6764);
        chip8.executeOpcode(0x5070);
        assertEquals(0x204, chip8.getPC());

        chip8.executeOpcode(0x4164);
        assertEquals(0x206, chip8.getPC());

        chip8.executeOpcode(0x9170);
        assertEquals(0x208, chip8.getPC());

        assertThrows(UnsupportedOperationException.class, () -> chip8.executeOpcode(0x5011));
        assertThrows(UnsupportedOperationException.class, () -> chip8.executeOpcode(0x9011));
    }
}
