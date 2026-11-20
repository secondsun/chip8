package net.saga.console.chip8.core;

import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class E02BitwiseOpCodeTest {
    private Chip8Machine chip8;

    @BeforeEach
    void setUp() {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        chip8.setRandom(new Random(42));
        chip8.executeOpcode(0x6064);
        chip8.executeOpcode(0x6127);
        chip8.executeOpcode(0x6212);
        chip8.executeOpcode(0x63AE);
        chip8.executeOpcode(0x64FF);
        chip8.executeOpcode(0x65B4);
        chip8.executeOpcode(0x6642);
        chip8.executeOpcode(0x6F25);
    }

    @Test
    void testAndOpCodes() {
        chip8.executeOpcode(0x8012);
        assertEquals(36, chip8.getV(0));
        assertEquals(0x27, chip8.getV(1));

        chip8.executeOpcode(0x8232);
        assertEquals(2, chip8.getV(2));
        assertEquals(0xAE, chip8.getV(3));

        chip8.executeOpcode(0x8FE2);
        assertEquals(0, chip8.getV(0xF));
    }

    @Test
    void testOrOpCodesAndVfResetQuirk() {
        chip8.executeOpcode(0x8011);
        assertEquals(103, chip8.getV(0));
        assertEquals(0x25, chip8.getV(0xF)); // vfReset is false in legacyTutorial

        Chip8Machine vip = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.COSMAC_VIP));
        vip.executeOpcode(0x6064);
        vip.executeOpcode(0x6127);
        vip.executeOpcode(0x6F99);
        vip.executeOpcode(0x8011);
        assertEquals(103, vip.getV(0));
        assertEquals(0x00, vip.getV(0xF)); // COSMAC VIP resets VF to 0
    }

    @Test
    void testXorOpCodes() {
        chip8.executeOpcode(0x8013);
        assertEquals(67, chip8.getV(0));
        assertEquals(0x27, chip8.getV(1));

        chip8.executeOpcode(0x8233);
        assertEquals(188, chip8.getV(2));
        assertEquals(0xAE, chip8.getV(3));
    }

    @Test
    void testShiftRightAndLeftWithQuirkToggle() {
        // In-place shift (CHIP-48 / SCHIP / legacyTutorial)
        chip8.executeOpcode(0x8016);
        assertEquals(0x32, chip8.getV(0));
        assertEquals(0x0, chip8.getV(0xF));

        chip8.executeOpcode(0x8446);
        assertEquals(127, chip8.getV(4));
        assertEquals(0x1, chip8.getV(0xF));

        chip8.executeOpcode(0x823E);
        assertEquals(36, chip8.getV(2));
        assertEquals(0x0, chip8.getV(0xF));

        // COSMAC VIP shifts VY into VX
        Chip8Machine vip = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.COSMAC_VIP));
        vip.executeOpcode(0x6064);
        vip.executeOpcode(0x6127);
        vip.executeOpcode(0x8016); // V0 = V1 >> 1 = 0x13, VF = 1
        assertEquals(0x13, vip.getV(0));
        assertEquals(0x1, vip.getV(0xF));
    }

    @Test
    void testRandomCoversFullByteRange() {
        chip8.setRandom(new Random(42));
        int expected1 = new Random(42).nextInt(256) & 0xFF;
        chip8.executeOpcode(0xC1FF);
        assertEquals(expected1, chip8.getV(1));
    }
}
