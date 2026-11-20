package net.saga.console.chip8.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class E01ArithmeticOpCodeTest {
    private Chip8Machine chip8;

    @BeforeEach
    void setUp() {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
    }

    @Test
    void testPCInitializedTo0x200() {
        assertEquals(0x0200, chip8.getPC());
    }

    @Test
    void testDataRegistersInitializedTo0() {
        for (int i = 0; i < 16; i++) {
            assertEquals(0x0, chip8.getV(i));
        }
    }

    @Test
    void testLoadConstant() {
        for (int i = 0; i < 16; i++) {
            int val = 0x15 + i * 5;
            chip8.executeOpcode(0x6000 | (i << 8) | val);
            assertEquals(val, chip8.getV(i));
        }
    }

    @Test
    void testAddConstantAndOverflow() {
        chip8.executeOpcode(0x6015);
        chip8.executeOpcode(0x7015);
        assertEquals(0x2A, chip8.getV(0));

        chip8.executeOpcode(0x6A42);
        chip8.executeOpcode(0x7A42);
        assertEquals(0x84, chip8.getV(0xA));

        chip8.executeOpcode(0x6EFF);
        chip8.executeOpcode(0x7E01);
        assertEquals(0x00, chip8.getV(0xE));
    }

    @Test
    void testCopyRegister() {
        chip8.executeOpcode(0x6A42);
        chip8.executeOpcode(0x8EA0);
        assertEquals(0x42, chip8.getV(0xA));
        assertEquals(0x42, chip8.getV(0xE));

        chip8.executeOpcode(0x6ADE);
        chip8.executeOpcode(0x8FA0);
        assertEquals(0x42, chip8.getV(0xE));
        assertEquals(0xDE, chip8.getV(0xF));
    }

    @Test
    void testAddRegisterWithCarry() {
        chip8.executeOpcode(0x6A42);
        chip8.executeOpcode(0x6E42);
        chip8.executeOpcode(0x8FA0);
        chip8.executeOpcode(0x8EA4);
        assertEquals(0x42, chip8.getV(0xA));
        assertEquals(0x84, chip8.getV(0xE));
        assertEquals(0x00, chip8.getV(0xF));

        chip8.executeOpcode(0x6AF0);
        chip8.executeOpcode(0x6E42);
        chip8.executeOpcode(0x8FA0);
        chip8.executeOpcode(0x8EA4);
        assertEquals(0xF0, chip8.getV(0xA));
        assertEquals(0x32, chip8.getV(0xE));
        assertEquals(0x01, chip8.getV(0xF));
    }

    @Test
    void testSubtractRegisterAndEqualBorrowFlag() {
        chip8.executeOpcode(0x6B84);
        chip8.executeOpcode(0x6F84);
        chip8.executeOpcode(0x6D25);
        chip8.executeOpcode(0x8DB5);
        assertEquals(0x84, chip8.getV(0xB));
        assertEquals(161, chip8.getV(0xD));
        assertEquals(0x00, chip8.getV(0xF));

        chip8.executeOpcode(0x6B84);
        chip8.executeOpcode(0x6F84);
        chip8.executeOpcode(0x6D25);
        chip8.executeOpcode(0x8DB7);
        assertEquals(0x84, chip8.getV(0xB));
        assertEquals(95, chip8.getV(0xD));
        assertEquals(0x01, chip8.getV(0xF));

        // Equal subtraction -> no borrow -> VF = 1
        chip8.executeOpcode(0x6042);
        chip8.executeOpcode(0x6142);
        chip8.executeOpcode(0x8015);
        assertEquals(0x00, chip8.getV(0));
        assertEquals(0x01, chip8.getV(0xF));

        chip8.executeOpcode(0x6042);
        chip8.executeOpcode(0x8017);
        assertEquals(0x00, chip8.getV(0));
        assertEquals(0x01, chip8.getV(0xF));

        // VF as target keeps flag
        chip8.executeOpcode(0x6F80);
        chip8.executeOpcode(0x6190);
        chip8.executeOpcode(0x8F14);
        assertEquals(0x01, chip8.getV(0xF));
    }
}
