package net.saga.console.chip8.core;

/**
 * Built-in CHIP-8 small (4x5) and SUPER-CHIP large (8x10) hexadecimal font sprites.
 * Backed by JDK 27 {@link LazyConstant} (JEP 531).
 */
public final class Fonts {
    public static final int SMALL_FONT_BASE = 0x000;
    public static final int SMALL_FONT_BYTES_PER_CHAR = 5;
    public static final int BIG_FONT_BASE = 0x050;
    public static final int BIG_FONT_BYTES_PER_CHAR = 10;

    private static final LazyConstant<byte[]> SMALL_FONT = LazyConstant.of(() -> new byte[] {
        (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
        (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
        (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
        (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
        (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
        (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
        (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
        (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
        (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
        (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
        (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
        (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
        (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
        (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
        (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
        (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  // F
    });

    private static final LazyConstant<byte[]> BIG_FONT = LazyConstant.of(() -> new byte[] {
        // 0
        (byte) 0x3C, (byte) 0x7E, (byte) 0xE7, (byte) 0xC3, (byte) 0xC3,
        (byte) 0xC3, (byte) 0xC3, (byte) 0xE7, (byte) 0x7E, (byte) 0x3C,
        // 1
        (byte) 0x18, (byte) 0x38, (byte) 0x58, (byte) 0x18, (byte) 0x18,
        (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x3C,
        // 2
        (byte) 0x3E, (byte) 0x7F, (byte) 0xC3, (byte) 0x06, (byte) 0x0C,
        (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0xFF, (byte) 0xFF,
        // 3
        (byte) 0x3C, (byte) 0x7E, (byte) 0xC3, (byte) 0x03, (byte) 0x0E,
        (byte) 0x0E, (byte) 0x03, (byte) 0xC3, (byte) 0x7E, (byte) 0x3C,
        // 4
        (byte) 0x06, (byte) 0x0E, (byte) 0x1E, (byte) 0x36, (byte) 0x66,
        (byte) 0xC6, (byte) 0xFF, (byte) 0xFF, (byte) 0x06, (byte) 0x06,
        // 5
        (byte) 0xFF, (byte) 0xFF, (byte) 0xC0, (byte) 0xC0, (byte) 0xFC,
        (byte) 0xFE, (byte) 0x03, (byte) 0xC3, (byte) 0x7E, (byte) 0x3C,
        // 6
        (byte) 0x3E, (byte) 0x7C, (byte) 0xC0, (byte) 0xC0, (byte) 0xFC,
        (byte) 0xFE, (byte) 0xC3, (byte) 0xC3, (byte) 0x7E, (byte) 0x3C,
        // 7
        (byte) 0xFF, (byte) 0xFF, (byte) 0x03, (byte) 0x06, (byte) 0x0C,
        (byte) 0x18, (byte) 0x30, (byte) 0x60, (byte) 0x60, (byte) 0x60,
        // 8
        (byte) 0x3C, (byte) 0x7E, (byte) 0xC3, (byte) 0xC3, (byte) 0x7E,
        (byte) 0x7E, (byte) 0xC3, (byte) 0xC3, (byte) 0x7E, (byte) 0x3C,
        // 9
        (byte) 0x3C, (byte) 0x7E, (byte) 0xC3, (byte) 0xC3, (byte) 0x7F,
        (byte) 0x3F, (byte) 0x03, (byte) 0x03, (byte) 0x3E, (byte) 0x7C,
        // A
        (byte) 0x3C, (byte) 0x7E, (byte) 0xC3, (byte) 0xC3, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xC3, (byte) 0xC3, (byte) 0xC3, (byte) 0xC3,
        // B
        (byte) 0xFC, (byte) 0xFE, (byte) 0xC3, (byte) 0xC3, (byte) 0xFC,
        (byte) 0xFE, (byte) 0xC3, (byte) 0xC3, (byte) 0xFE, (byte) 0xFC,
        // C
        (byte) 0x3C, (byte) 0x7E, (byte) 0xC3, (byte) 0xC0, (byte) 0xC0,
        (byte) 0xC0, (byte) 0xC0, (byte) 0xC3, (byte) 0x7E, (byte) 0x3C,
        // D
        (byte) 0xFC, (byte) 0xFE, (byte) 0xC3, (byte) 0xC3, (byte) 0xC3,
        (byte) 0xC3, (byte) 0xC3, (byte) 0xC3, (byte) 0xFE, (byte) 0xFC,
        // E
        (byte) 0xFF, (byte) 0xFF, (byte) 0xC0, (byte) 0xC0, (byte) 0xFC,
        (byte) 0xFC, (byte) 0xC0, (byte) 0xC0, (byte) 0xFF, (byte) 0xFF,
        // F
        (byte) 0xFF, (byte) 0xFF, (byte) 0xC0, (byte) 0xC0, (byte) 0xFC,
        (byte) 0xFC, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0, (byte) 0xC0
    });

    private Fonts() {}

    public static void loadInto(byte[] memory) {
        byte[] small = SMALL_FONT.get();
        System.arraycopy(small, 0, memory, SMALL_FONT_BASE, small.length);
        if (memory.length >= BIG_FONT_BASE + BIG_FONT.get().length) {
            byte[] big = BIG_FONT.get();
            System.arraycopy(big, 0, memory, BIG_FONT_BASE, big.length);
        }
    }

    public static int smallFontAddress(int digit) {
        return SMALL_FONT_BASE + (digit & 0xF) * SMALL_FONT_BYTES_PER_CHAR;
    }

    public static int bigFontAddress(int digit) {
        return BIG_FONT_BASE + (digit & 0xF) * BIG_FONT_BYTES_PER_CHAR;
    }
}
