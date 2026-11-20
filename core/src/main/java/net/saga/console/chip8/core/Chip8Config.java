package net.saga.console.chip8.core;

import java.util.Objects;

/**
 * Immutable machine configuration combining a CHIP-8 variant, quirk settings, and target CPU speed.
 */
public record Chip8Config(
    Chip8Variant variant,
    Quirks quirks,
    int cyclesPerFrame
) {
    public Chip8Config {
        Objects.requireNonNull(variant, "variant must not be null");
        Objects.requireNonNull(quirks, "quirks must not be null");
        if (cyclesPerFrame < 1 || cyclesPerFrame > 10_000) {
            throw new IllegalArgumentException("cyclesPerFrame must be between 1 and 10000: " + cyclesPerFrame);
        }
    }

    public static Chip8Config forVariant(Chip8Variant variant) {
        return new Chip8Config(variant, variant.defaultQuirks(), variant.defaultCyclesPerFrame());
    }

    public static Chip8Config legacyTutorial() {
        return new Chip8Config(Chip8Variant.COSMAC_VIP, Quirks.legacyTutorialCompatible(), 15);
    }

    public Chip8Config withVariant(Chip8Variant newVariant) {
        return new Chip8Config(newVariant, newVariant.defaultQuirks(), newVariant.defaultCyclesPerFrame());
    }

    public Chip8Config withQuirks(Quirks newQuirks) {
        return new Chip8Config(variant, newQuirks, cyclesPerFrame);
    }

    public Chip8Config withCyclesPerFrame(int newCyclesPerFrame) {
        return new Chip8Config(variant, quirks, newCyclesPerFrame);
    }
}
