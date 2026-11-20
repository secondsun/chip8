package net.saga.console.chip8.core;

/**
 * Fine-grained behavioral quirks across CHIP-8 interpreters.
 *
 * @param vfReset           Whether 8XY1/8XY2/8XY3 clear VF to 0 (COSMAC VIP behavior).
 * @param memoryIncrement   How FX55/FX65 modify the I register.
 * @param displayWait       Whether DXYN waits for the 60 Hz vertical blank interrupt.
 * @param clipping          Whether sprites clip at screen edges (true) or wrap around (false).
 * @param shiftUsesVy       Whether 8XY6/8XYE shift VY into VX (true) or shift VX in-place (false).
 * @param jumpUsesV0        Whether BNNN jumps to NNN + V0 (true) or NNN + VX (false, BXNN).
 * @param waitForKeyRelease Whether FX0A waits for a key press followed by release (true on VIP).
 * @param loResTallSprites  Whether DXY0 draws 16x16 sprites in lo-res mode (true on XO-CHIP).
 */
public record Quirks(
    boolean vfReset,
    MemoryIncrement memoryIncrement,
    boolean displayWait,
    boolean clipping,
    boolean shiftUsesVy,
    boolean jumpUsesV0,
    boolean waitForKeyRelease,
    boolean loResTallSprites
) {
    public enum MemoryIncrement {
        INCREMENT_BY_X_PLUS_1("I += X + 1 (VIP / XO-CHIP)"),
        INCREMENT_BY_X("I += X (CHIP-48 / SCHIP 1.0)"),
        NONE("Leave I unchanged (SCHIP 1.1)");

        private final String label;

        MemoryIncrement(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Preset matching Octo's default SUPER-CHIP behavior (shift uses VY, I += X+1, BNNN uses V0, wrapping sprites).
     */
    public static Quirks octoSuperChip() {
        return new Quirks(
            false,
            MemoryIncrement.INCREMENT_BY_X_PLUS_1,
            false,
            false,
            true,
            true,
            false,
            false
        );
    }

    /**
     * Preset matching the original Java 8 tutorial unit tests (wrapping sprites, in-place shift, I += X + 1).
     */
    public static Quirks legacyTutorialCompatible() {
        return new Quirks(
            false,
            MemoryIncrement.INCREMENT_BY_X_PLUS_1,
            false,
            false,
            false,
            true,
            false,
            false
        );
    }

    public Quirks withVfReset(boolean value) {
        return new Quirks(value, memoryIncrement, displayWait, clipping, shiftUsesVy, jumpUsesV0, waitForKeyRelease, loResTallSprites);
    }

    public Quirks withMemoryIncrement(MemoryIncrement value) {
        return new Quirks(vfReset, value, displayWait, clipping, shiftUsesVy, jumpUsesV0, waitForKeyRelease, loResTallSprites);
    }

    public Quirks withDisplayWait(boolean value) {
        return new Quirks(vfReset, memoryIncrement, value, clipping, shiftUsesVy, jumpUsesV0, waitForKeyRelease, loResTallSprites);
    }

    public Quirks withClipping(boolean value) {
        return new Quirks(vfReset, memoryIncrement, displayWait, value, shiftUsesVy, jumpUsesV0, waitForKeyRelease, loResTallSprites);
    }

    public Quirks withShiftUsesVy(boolean value) {
        return new Quirks(vfReset, memoryIncrement, displayWait, clipping, value, jumpUsesV0, waitForKeyRelease, loResTallSprites);
    }

    public Quirks withJumpUsesV0(boolean value) {
        return new Quirks(vfReset, memoryIncrement, displayWait, clipping, shiftUsesVy, value, waitForKeyRelease, loResTallSprites);
    }

    public Quirks withWaitForKeyRelease(boolean value) {
        return new Quirks(vfReset, memoryIncrement, displayWait, clipping, shiftUsesVy, jumpUsesV0, value, loResTallSprites);
    }

    public Quirks withLoResTallSprites(boolean value) {
        return new Quirks(vfReset, memoryIncrement, displayWait, clipping, shiftUsesVy, jumpUsesV0, waitForKeyRelease, value);
    }
}
