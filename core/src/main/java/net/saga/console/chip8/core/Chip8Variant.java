package net.saga.console.chip8.core;

/**
 * Supported CHIP-8 hardware/interpreter generations.
 */
public enum Chip8Variant {
    COSMAC_VIP(
        "COSMAC VIP (1977)",
        "VIP",
        "Original Joseph Weisbecker CHIP-8 interpreter (64x32, 4 KB RAM, vblank sync).",
        4096,
        false,
        false,
        false,
        12
    ),
    CHIP_48(
        "CHIP-48 (1990)",
        "CHIP-48",
        "HP-48SX interpreter with in-place shifts and BXNN indexed jumps.",
        4096,
        false,
        false,
        false,
        15
    ),
    SUPER_CHIP_1_0(
        "SUPER-CHIP 1.0",
        "SCHIP 1.0",
        "Adds 128x64 hi-res mode, scrolling, 16x16 sprites, large font, and RPL flags.",
        4096,
        true,
        false,
        false,
        20
    ),
    SUPER_CHIP_1_1(
        "SUPER-CHIP 1.1",
        "SCHIP 1.1",
        "Refined SUPER-CHIP with scroll-down/left/right and no I increment on FX55/FX65.",
        4096,
        true,
        false,
        false,
        25
    ),
    XO_CHIP(
        "XO-CHIP",
        "XO-CHIP",
        "Modern extension: 64 KB RAM, 4-color dual bitplanes, scroll-up, and 16-byte pattern audio.",
        65536,
        true,
        true,
        false,
        35
    ),
    MEGA_CHIP(
        "Mega-Chip (2007)",
        "MEGA-CHIP",
        "Revival Studios superset: 256x192 ARGB8 double-buffered color, 16 MB RAM, blend modes & 8-bit PCM audio.",
        16 * 1024 * 1024,
        true,
        false,
        true,
        1000
    );

    private final String displayName;
    private final String shortName;
    private final String description;
    private final int memorySize;
    private final boolean supportsHiRes;
    private final boolean supportsXoChip;
    private final boolean supportsMegaChip;
    private final int defaultCyclesPerFrame;

    Chip8Variant(
        String displayName,
        String shortName,
        String description,
        int memorySize,
        boolean supportsHiRes,
        boolean supportsXoChip,
        boolean supportsMegaChip,
        int defaultCyclesPerFrame
    ) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.description = description;
        this.memorySize = memorySize;
        this.supportsHiRes = supportsHiRes;
        this.supportsXoChip = supportsXoChip;
        this.supportsMegaChip = supportsMegaChip;
        this.defaultCyclesPerFrame = defaultCyclesPerFrame;
    }

    public String displayName() {
        return displayName;
    }

    public String shortName() {
        return shortName;
    }

    public String description() {
        return description;
    }

    public int memorySize() {
        return memorySize;
    }

    public boolean supportsHiRes() {
        return supportsHiRes;
    }

    public boolean supportsXoChip() {
        return supportsXoChip;
    }

    public boolean supportsMegaChip() {
        return supportsMegaChip;
    }

    public int defaultCyclesPerFrame() {
        return defaultCyclesPerFrame;
    }

    public Quirks defaultQuirks() {
        return switch (this) {
            case COSMAC_VIP -> new Quirks(
                true,
                Quirks.MemoryIncrement.INCREMENT_BY_X_PLUS_1,
                true,
                true,
                true,
                true,
                true,
                false
            );
            case CHIP_48 -> new Quirks(
                false,
                Quirks.MemoryIncrement.INCREMENT_BY_X,
                false,
                true,
                false,
                false,
                false,
                false
            );
            case SUPER_CHIP_1_0 -> new Quirks(
                false,
                Quirks.MemoryIncrement.INCREMENT_BY_X,
                false,
                true,
                false,
                false,
                false,
                false
            );
            case SUPER_CHIP_1_1 -> new Quirks(
                false,
                Quirks.MemoryIncrement.NONE,
                false,
                true,
                false,
                false,
                false,
                false
            );
            case XO_CHIP -> new Quirks(
                false,
                Quirks.MemoryIncrement.INCREMENT_BY_X_PLUS_1,
                false,
                false,
                true,
                true,
                false,
                true
            );
            case MEGA_CHIP -> new Quirks(
                false,
                Quirks.MemoryIncrement.NONE,
                false,
                true,
                false,
                true,
                false,
                false
            );
        };
    }
}
