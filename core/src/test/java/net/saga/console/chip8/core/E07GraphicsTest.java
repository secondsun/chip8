package net.saga.console.chip8.core;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class E07GraphicsTest {
    private Chip8Machine chip8;

    @BeforeEach
    void setUp() throws IOException {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        try (var stream = getClass().getResourceAsStream("/E07GraphicsRom.ch8")) {
            chip8.loadRom(stream.readAllBytes());
        }
        chip8.executeOpcode(0x6127); // 39
        chip8.executeOpcode(0x6212); // 18
        chip8.executeOpcode(0x673F); // 63
        chip8.executeOpcode(0x681F); // 31
    }

    @Test
    void testIRegister() {
        chip8.executeOpcode(0xA123);
        assertEquals(0x123, chip8.getI());
        chip8.executeOpcode(0xF11E);
        assertEquals(0x123 + 0x27, chip8.getI());
    }

    @Test
    void testDrawSpriteAndCollisionAndWrapping() {
        chip8.executeOpcode(0xA202);
        chip8.executeOpcode(0xD122);
        assertEquals(0, chip8.getV(0xF));
        assertEquals((byte) 0xFF, chip8.display().getSpriteRow(39, 18));
        assertEquals((byte) 0x3C, chip8.display().getSpriteRow(39, 19));

        chip8.executeOpcode(0xD122);
        assertEquals(1, chip8.getV(0xF));
        assertEquals((byte) 0x00, chip8.display().getSpriteRow(39, 18));

        // Bottom-right edge wrapping
        chip8.executeOpcode(0xD781);
        assertEquals(0, chip8.getV(0xF));
        assertEquals((byte) 0xFF, chip8.display().getSpriteRow(63, 31));
        assertEquals(1, chip8.display().getPixelColorIndex(0, 31));
    }

    @Test
    void testHexFonts() {
        chip8.executeOpcode(0x6100);
        chip8.executeOpcode(0x6200);
        for (int i = 0; i <= 0xF; i++) {
            chip8.executeOpcode(0x6000 + i);
            chip8.executeOpcode(0xF029);
            chip8.executeOpcode(0xD125);
            assertTrue(chip8.display().getSpriteRow(0, 0) != 0);
            chip8.executeOpcode(0x00E0);
        }
    }

    @Test
    void testSuperChipHiResAndScrollingAndLargeFont() {
        Chip8Machine schip = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.SUPER_CHIP_1_1));
        schip.executeOpcode(0x00FF); // Enable 128x64 hi-res
        assertTrue(schip.display().isHiRes());
        assertEquals(128, schip.display().width());
        assertEquals(64, schip.display().height());

        // Draw big font '0' (10 rows) at (0, 0)
        schip.executeOpcode(0x6000);
        schip.executeOpcode(0xF030);
        schip.executeOpcode(0xD00A);
        assertEquals((byte) 0x3C, schip.display().getSpriteRow(0, 0));

        // Scroll down by 2 rows
        schip.executeOpcode(0x00C2);
        assertEquals((byte) 0x00, schip.display().getSpriteRow(0, 0));
        assertEquals((byte) 0x3C, schip.display().getSpriteRow(0, 2));

        // Scroll right by 4 pixels
        schip.executeOpcode(0x00FB);
        assertEquals((byte) 0x3C, schip.display().getSpriteRow(4, 2));

        // Scroll left by 4 pixels
        schip.executeOpcode(0x00FC);
        assertEquals((byte) 0x3C, schip.display().getSpriteRow(0, 2));
    }

    @Test
    void testXoChipDualBitplanesAndScrollUp() {
        Chip8Machine xo = new Chip8Machine(Chip8Config.forVariant(Chip8Variant.XO_CHIP));
        xo.writeMemoryByte(0x300, (byte) 0xF0); // Plane 0 row
        xo.writeMemoryByte(0x301, (byte) 0x3C); // Plane 1 row
        xo.executeOpcode(0xA300);
        xo.executeOpcode(0xF301); // Select both plane 0 and plane 1
        xo.executeOpcode(0xD001); // Draw 1 row across both planes

        assertEquals(1, xo.display().getPixelColorIndex(0, 0)); // plane 0 only
        assertEquals(3, xo.display().getPixelColorIndex(2, 0)); // plane 0 + plane 1
        assertEquals(2, xo.display().getPixelColorIndex(4, 0)); // plane 1 only
        assertEquals(0, xo.display().getPixelColorIndex(6, 0)); // neither

        // Move row down then scroll up
        xo.executeOpcode(0x00C2);
        assertEquals(3, xo.display().getPixelColorIndex(2, 2));
        xo.executeOpcode(0x00D1);
        assertEquals(3, xo.display().getPixelColorIndex(2, 1));
    }
}
