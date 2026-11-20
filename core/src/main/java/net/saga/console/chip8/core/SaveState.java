package net.saga.console.chip8.core;

import java.time.Instant;
import java.util.Arrays;

/**
 * Immutable snapshot of complete CHIP-8 / SUPER-CHIP / XO-CHIP / Mega-Chip machine state
 * for save-state slots and rewind history.
 */
public record SaveState(
    Instant savedAt,
    Chip8Config config,
    int pc,
    int iRegister,
    int sp,
    int delayTimer,
    int soundTimer,
    boolean halted,
    boolean hiRes,
    int activePlanes,
    int audioPitch,
    int totalMemorySize,
    int[] registers,
    int[] stack,
    int[] rplFlags,
    byte[] activeMemory,
    byte[] plane0,
    byte[] plane1,
    byte[] audioPattern,
    DisplayBuffer.MegaDisplayState megaDisplayState,
    DisplayBuffer.DisplaySnapshot thumbnail
) {
    public SaveState {
        registers = Arrays.copyOf(registers, registers.length);
        stack = Arrays.copyOf(stack, stack.length);
        rplFlags = Arrays.copyOf(rplFlags, rplFlags.length);
        activeMemory = Arrays.copyOf(activeMemory, activeMemory.length);
        plane0 = Arrays.copyOf(plane0, plane0.length);
        plane1 = Arrays.copyOf(plane1, plane1.length);
        audioPattern = Arrays.copyOf(audioPattern, audioPattern.length);
    }
}
