package net.saga.console.chip8.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.saga.console.chip8.core.audio.AudioEngine;
import net.saga.console.chip8.core.input.KeypadState;

/**
 * Pure Java 27 multi-variant CHIP-8 virtual machine supporting:
 * COSMAC VIP (1977), CHIP-48, SUPER-CHIP 1.0/1.1, and XO-CHIP.
 */
public final class Chip8Machine {
    public static final int PROGRAM_START = 0x200;
    public static final int REGISTER_COUNT = 16;
    public static final int STACK_DEPTH = 16;
    public static final int RPL_FLAG_COUNT = 16;

    private Chip8Config config;
    private byte[] memory;
    private byte[] loadedRom = new byte[0];

    private int pc = PROGRAM_START;
    private int iRegister = 0;
    private int sp = 0;
    private int delayTimer = 0;
    private int soundTimer = 0;
    private boolean halted = false;
    private boolean waitingForVblank = false;
    private int waitingKeyRegister = -1;
    private int pressedKeyWaitingRelease = -1;

    private final int[] registers = new int[REGISTER_COUNT];
    private final int[] stack = new int[STACK_DEPTH];
    private final int[] rplFlags = new int[RPL_FLAG_COUNT];
    private final byte[] audioPattern = new byte[16];
    private int audioPitch = 64;

    private final DisplayBuffer display = new DisplayBuffer();
    private final KeypadState keypad;
    private final AudioEngine audioEngine;
    private final Set<Integer> breakpoints = new HashSet<>();
    private RandomGenerator random;
    private long totalCycles = 0;

    public Chip8Machine() {
        this(Chip8Config.forVariant(Chip8Variant.COSMAC_VIP), new KeypadState(), null, new Random());
    }

    public Chip8Machine(Chip8Config config) {
        this(config, new KeypadState(), null, new Random());
    }

    public Chip8Machine(Chip8Config config, KeypadState keypad, AudioEngine audioEngine, RandomGenerator random) {
        this.config = Objects.requireNonNull(config, "config");
        this.keypad = Objects.requireNonNull(keypad, "keypad");
        this.audioEngine = audioEngine;
        this.random = Objects.requireNonNull(random, "random");
        this.memory = new byte[config.variant().memorySize()];
        resetState(false);
    }

    public void setRandom(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public Chip8Config getConfig() {
        return config;
    }

    public synchronized void updateConfig(Chip8Config newConfig, boolean reloadRomIfMemoryChanged) {
        boolean memSizeChanged = newConfig.variant().memorySize() != this.config.variant().memorySize();
        this.config = Objects.requireNonNull(newConfig, "newConfig");
        if (audioEngine != null) {
            audioEngine.setXoChipPatternMode(newConfig.variant().supportsXoChip());
        }
        if (memSizeChanged && reloadRomIfMemoryChanged) {
            this.memory = new byte[newConfig.variant().memorySize()];
            resetState(true);
        }
    }

    public synchronized void loadRom(byte[] romBytes) {
        Objects.requireNonNull(romBytes, "romBytes");
        int maxRom = memory.length - PROGRAM_START;
        if (romBytes.length > maxRom) {
            if (romBytes.length <= Chip8Variant.XO_CHIP.memorySize() - PROGRAM_START) {
                this.config = this.config.withVariant(Chip8Variant.XO_CHIP);
                this.memory = new byte[Chip8Variant.XO_CHIP.memorySize()];
            } else if (romBytes.length <= Chip8Variant.MEGA_CHIP.memorySize() - PROGRAM_START) {
                this.config = this.config.withVariant(Chip8Variant.MEGA_CHIP);
                this.memory = new byte[Chip8Variant.MEGA_CHIP.memorySize()];
            } else {
                throw new IllegalArgumentException("ROM exceeds 16 MB address space: " + romBytes.length + " bytes");
            }
        }
        this.loadedRom = Arrays.copyOf(romBytes, romBytes.length);
        resetState(true);
    }

    public synchronized void loadRom(Path romPath) throws IOException {
        loadRom(Files.readAllBytes(romPath));
    }

    public synchronized void reset() {
        resetState(true);
    }

    private void resetState(boolean copyRom) {
        Arrays.fill(memory, (byte) 0);
        Fonts.loadInto(memory);
        if (copyRom && loadedRom.length > 0) {
            System.arraycopy(loadedRom, 0, memory, PROGRAM_START, loadedRom.length);
        }
        Arrays.fill(registers, 0);
        Arrays.fill(stack, 0);
        Arrays.fill(audioPattern, 0, 8, (byte) 0xFF);
        Arrays.fill(audioPattern, 8, 16, (byte) 0x00);
        this.pc = PROGRAM_START;
        this.iRegister = 0;
        this.sp = 0;
        this.delayTimer = 0;
        this.soundTimer = 0;
        this.audioPitch = 64;
        this.halted = false;
        this.waitingForVblank = false;
        this.waitingKeyRegister = -1;
        this.pressedKeyWaitingRelease = -1;
        this.totalCycles = 0;
        display.reset();
        if (audioEngine != null) {
            audioEngine.setXoChipPatternMode(config.variant().supportsXoChip());
            audioEngine.loadXoPattern(audioPattern);
            audioEngine.setXoPitch(audioPitch);
            audioEngine.stopMegaSample();
            audioEngine.setSoundActive(false);
        }
    }

    /**
     * Executes one 60 Hz frame: decrements timers once, releases vblank wait,
     * and runs up to {@code cyclesPerFrame} instructions (stopping early if a breakpoint or halt occurs).
     *
     * @return true if a breakpoint was hit during this frame.
     */
    public synchronized boolean stepFrame(int cyclesPerFrame) {
        if (halted) {
            updateAudioState();
            return false;
        }
        waitingForVblank = false;
        tickTimers60Hz();

        int limit = Math.max(1, cyclesPerFrame);
        for (int i = 0; i < limit; i++) {
            if (halted || waitingForVblank) {
                break;
            }
            if (i > 0 && breakpoints.contains(pc)) {
                return true;
            }
            stepInstruction();
            if (breakpoints.contains(pc)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void tickTimers60Hz() {
        if (delayTimer > 0) {
            delayTimer--;
        }
        if (soundTimer > 0) {
            soundTimer--;
        }
        updateAudioState();
    }

    private void updateAudioState() {
        if (audioEngine != null) {
            audioEngine.setSoundActive(!halted && soundTimer > 0);
        }
    }

    /**
     * Fetches and executes a single instruction at PC.
     */
    public synchronized void stepInstruction() {
        if (halted) {
            return;
        }
        if (waitingKeyRegister >= 0) {
            handleKeyWaitTick();
            return;
        }
        int opcode = readWord(pc);
        pc = (pc + 2) & addressMask();
        executeOpcode(opcode);
        totalCycles++;
    }

    /**
     * Executes an explicit 16-bit opcode directly (useful for unit tests and step diagnostics).
     */
    public synchronized void executeOpcode(int opcode) {
        int op = opcode & 0xFFFF;
        int high = op & 0xF000;
        int x = (op & 0x0F00) >>> 8;
        int y = (op & 0x00F0) >>> 4;
        int n = op & 0x000F;
        int nn = op & 0x00FF;
        int nnn = op & 0x0FFF;
        Quirks quirks = config.quirks();

        // JDK 27 JEP 532: Primitive types in switch patterns with guards
        switch (high) {
            case 0x0000 -> executeZeroGroup(op, n);
            case 0x1000 -> pc = nnn;
            case 0x2000 -> {
                if (sp >= stack.length) {
                    throw new IllegalStateException("Stack overflow at PC=0x" + Integer.toHexString(pc));
                }
                stack[sp++] = pc;
                pc = nnn;
            }
            case 0x3000 -> {
                if (getV(x) == nn) {
                    skipNextInstruction();
                }
            }
            case 0x4000 -> {
                if (getV(x) != nn) {
                    skipNextInstruction();
                }
            }
            case 0x5000 -> executeFiveGroup(op, x, y, n);
            case 0x6000 -> setV(x, nn);
            case 0x7000 -> setV(x, getV(x) + nn);
            case 0x8000 -> executeAluGroup(op, x, y, n, quirks);
            case 0x9000 -> {
                if (n != 0) {
                    throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
                }
                if (getV(x) != getV(y)) {
                    skipNextInstruction();
                }
            }
            case 0xA000 -> iRegister = nnn;
            case 0xB000 -> {
                int base = quirks.jumpUsesV0() ? getV(0) : getV(x);
                pc = (nnn + base) & addressMask();
            }
            case 0xC000 -> setV(x, random.nextInt(0x100) & nn);
            case 0xD000 -> {
                boolean allowTall = config.variant().supportsHiRes() && (display.isHiRes() || quirks.loResTallSprites());
                boolean collided = display.drawSprite(memory, iRegister, getV(x), getV(y), n, quirks.clipping(), allowTall);
                registers[0xF] = collided ? 1 : 0;
                if (quirks.displayWait()) {
                    waitingForVblank = true;
                }
            }
            case 0xE000 -> {
                switch (nn) {
                    case 0x9E -> {
                        if (keypad.isPressed(getV(x) & 0xF)) {
                            skipNextInstruction();
                        }
                    }
                    case 0xA1 -> {
                        if (!keypad.isPressed(getV(x) & 0xF)) {
                            skipNextInstruction();
                        }
                    }
                    default -> throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
                }
            }
            case 0xF000 -> executeFGroup(op, x, nn, quirks);
            default -> throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
        }
    }

    private void executeZeroGroup(int op, int n) {
        switch (op) {
            case 0x0010 -> {
                ensureMegaChipSupported(op);
                display.setMegaMode(false);
            }
            case 0x0011 -> {
                ensureMegaChipSupported(op);
                display.setMegaMode(true);
            }
            case 0x00E0 -> {
                if (display.isMegaMode()) {
                    display.flipMegaBuffer(true);
                    waitingForVblank = true;
                } else {
                    display.clearSelectedPlanes();
                }
            }
            case 0x00EE -> {
                if (sp <= 0) {
                    throw new IllegalStateException("Stack underflow");
                }
                pc = stack[--sp];
            }
            case 0x00FB -> {
                ensureHiResSupported(op);
                display.scrollRight(display.isMegaMode() || display.isHiRes() || config.variant().supportsXoChip() ? 4 : 2);
            }
            case 0x00FC -> {
                ensureHiResSupported(op);
                display.scrollLeft(display.isMegaMode() || display.isHiRes() || config.variant().supportsXoChip() ? 4 : 2);
            }
            case 0x00FD -> {
                ensureHiResSupported(op);
                halted = true;
                updateAudioState();
            }
            case 0x00FE -> {
                ensureHiResSupported(op);
                if (!display.isMegaMode()) {
                    display.setHiRes(false);
                    if (config.variant().supportsXoChip()) {
                        display.clearSelectedPlanes();
                    }
                }
            }
            case 0x00FF -> {
                ensureHiResSupported(op);
                if (!display.isMegaMode()) {
                    display.setHiRes(true);
                    if (config.variant().supportsXoChip()) {
                        display.clearSelectedPlanes();
                    }
                }
            }
            case 0x0700 -> {
                ensureMegaChipSupported(op);
                if (audioEngine != null) {
                    audioEngine.stopMegaSample();
                }
            }
            case int code when (code & 0xFFF0) == 0x00B0 -> {
                ensureMegaChipSupported(code);
                display.scrollUp(n);
            }
            case int code when (code & 0xFFF0) == 0x00C0 -> {
                ensureHiResSupported(code);
                display.scrollDown(n);
            }
            case int code when (code & 0xFFF0) == 0x00D0 -> {
                ensureXoChipSupported(code);
                display.scrollUp(n);
            }
            case int code when (code & 0xFF00) == 0x0100 -> {
                ensureMegaChipSupported(code);
                iRegister = (((code & 0xFF) << 16) | readWord(pc)) & addressMask();
                pc = (pc + 2) & addressMask();
            }
            case int code when (code & 0xFF00) == 0x0200 -> {
                ensureMegaChipSupported(code);
                display.loadMegaPalette(memory, iRegister, code & 0xFF);
            }
            case int code when (code & 0xFF00) == 0x0300 -> {
                ensureMegaChipSupported(code);
                display.setMegaSpriteWidth(code & 0xFF);
            }
            case int code when (code & 0xFF00) == 0x0400 -> {
                ensureMegaChipSupported(code);
                display.setMegaSpriteHeight(code & 0xFF);
            }
            case int code when (code & 0xFF00) == 0x0500 -> {
                ensureMegaChipSupported(code);
                display.setMegaScreenAlpha(code & 0xFF);
            }
            case int code when (code & 0xFFF0) == 0x0600 -> {
                ensureMegaChipSupported(code);
                playMegaSound(n == 0);
            }
            case int code when (code & 0xFFF0) == 0x0800 -> {
                ensureMegaChipSupported(code);
                display.setMegaBlendMode(n);
            }
            case int code when (code & 0xFF00) == 0x0900 -> {
                ensureMegaChipSupported(code);
                display.setMegaCollisionColor(code & 0xFF);
            }
            default -> throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
        }
    }

    private void playMegaSound(boolean loop) {
        if (audioEngine == null) {
            return;
        }
        int mask = addressMask();
        int sampleRate = ((memory[iRegister & mask] & 0xFF) << 8) | (memory[(iRegister + 1) & mask] & 0xFF);
        int length = ((memory[(iRegister + 2) & mask] & 0xFF) << 16)
            | ((memory[(iRegister + 3) & mask] & 0xFF) << 8)
            | (memory[(iRegister + 4) & mask] & 0xFF);
        int clampedLen = Math.min(length, 4 * 1024 * 1024);
        if (sampleRate <= 0 || clampedLen <= 0) {
            audioEngine.stopMegaSample();
            return;
        }
        byte[] samples = new byte[clampedLen];
        int dataStart = iRegister + 6;
        for (int i = 0; i < clampedLen; i++) {
            samples[i] = memory[(dataStart + i) & mask];
        }
        audioEngine.playMegaSample(samples, sampleRate, loop);
    }

    private void executeFiveGroup(int op, int x, int y, int n) {
        switch (n) {
            case 0x0 -> {
                if (getV(x) == getV(y)) {
                    skipNextInstruction();
                }
            }
            case 0x2 -> {
                ensureXoChipSupported(op);
                int step = x <= y ? 1 : -1;
                int offset = 0;
                for (int r = x; ; r += step) {
                    memory[(iRegister + offset) & addressMask()] = (byte) getV(r);
                    offset++;
                    if (r == y) {
                        break;
                    }
                }
            }
            case 0x3 -> {
                ensureXoChipSupported(op);
                int step = x <= y ? 1 : -1;
                int offset = 0;
                for (int r = x; ; r += step) {
                    setV(r, memory[(iRegister + offset) & addressMask()] & 0xFF);
                    offset++;
                    if (r == y) {
                        break;
                    }
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
        }
    }

    private void executeAluGroup(int op, int x, int y, int n, Quirks quirks) {
        switch (n) {
            case 0x0 -> setV(x, getV(y));
            case 0x1 -> {
                setV(x, getV(x) | getV(y));
                if (quirks.vfReset()) {
                    registers[0xF] = 0;
                }
            }
            case 0x2 -> {
                setV(x, getV(x) & getV(y));
                if (quirks.vfReset()) {
                    registers[0xF] = 0;
                }
            }
            case 0x3 -> {
                setV(x, getV(x) ^ getV(y));
                if (quirks.vfReset()) {
                    registers[0xF] = 0;
                }
            }
            case 0x4 -> {
                int sum = getV(x) + getV(y);
                setV(x, sum);
                registers[0xF] = sum > 0xFF ? 1 : 0;
            }
            case 0x5 -> {
                int diff = getV(x) - getV(y);
                setV(x, diff);
                registers[0xF] = diff >= 0 ? 1 : 0;
            }
            case 0x6 -> {
                int src = quirks.shiftUsesVy() ? getV(y) : getV(x);
                setV(x, src >>> 1);
                registers[0xF] = src & 0x01;
            }
            case 0x7 -> {
                int diff = getV(y) - getV(x);
                setV(x, diff);
                registers[0xF] = diff >= 0 ? 1 : 0;
            }
            case 0xE -> {
                int src = quirks.shiftUsesVy() ? getV(y) : getV(x);
                setV(x, src << 1);
                registers[0xF] = (src >>> 7) & 0x01;
            }
            default -> throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
        }
    }

    private void executeFGroup(int op, int x, int nn, Quirks quirks) {
        if (op == 0xF000) {
            ensureXoChipSupported(op);
            iRegister = readWord(pc);
            pc = (pc + 2) & addressMask();
            return;
        }
        if (op == 0xF002) {
            ensureXoChipSupported(op);
            for (int i = 0; i < 16; i++) {
                audioPattern[i] = memory[(iRegister + i) & addressMask()];
            }
            if (audioEngine != null) {
                audioEngine.loadXoPattern(audioPattern);
            }
            return;
        }

        switch (nn) {
            case 0x01 -> {
                ensureXoChipSupported(op);
                display.setActivePlanes(x & 0b11);
            }
            case 0x07 -> setV(x, delayTimer);
            case 0x0A -> {
                if (display.isMegaMode()) {
                    display.flipMegaBuffer(false);
                }
                int pressed = keypad.firstPressedKey();
                if (!quirks.waitForKeyRelease()) {
                    if (pressed == -1) {
                        pc = (pc - 2) & addressMask();
                    } else {
                        setV(x, pressed);
                    }
                } else {
                    // COSMAC VIP: wait for a press, then wait until that key is released
                    waitingKeyRegister = x;
                    pressedKeyWaitingRelease = pressed;
                }
            }
            case 0x15 -> delayTimer = getV(x);
            case 0x18 -> {
                soundTimer = getV(x);
                updateAudioState();
            }
            case 0x1E -> iRegister = (iRegister + getV(x)) & addressMask();
            case 0x29 -> iRegister = Fonts.smallFontAddress(getV(x));
            case 0x30 -> {
                ensureHiResSupported(op);
                iRegister = Fonts.bigFontAddress(getV(x));
            }
            case 0x33 -> {
                int value = getV(x);
                int mask = addressMask();
                memory[iRegister & mask] = (byte) (value / 100);
                memory[(iRegister + 1) & mask] = (byte) ((value / 10) % 10);
                memory[(iRegister + 2) & mask] = (byte) (value % 10);
            }
            case 0x3A -> {
                ensureXoChipSupported(op);
                audioPitch = getV(x);
                if (audioEngine != null) {
                    audioEngine.setXoPitch(audioPitch);
                }
            }
            case 0x55 -> {
                int mask = addressMask();
                for (int i = 0; i <= x; i++) {
                    memory[(iRegister + i) & mask] = (byte) getV(i);
                }
                applyMemoryIncrement(x, quirks);
            }
            case 0x65 -> {
                int mask = addressMask();
                for (int i = 0; i <= x; i++) {
                    setV(i, memory[(iRegister + i) & mask] & 0xFF);
                }
                applyMemoryIncrement(x, quirks);
            }
            case 0x75 -> {
                ensureHiResSupported(op);
                int max = Math.min(x, config.variant().supportsXoChip() ? 15 : 7);
                for (int i = 0; i <= max; i++) {
                    rplFlags[i] = getV(i);
                }
            }
            case 0x85 -> {
                ensureHiResSupported(op);
                int max = Math.min(x, config.variant().supportsXoChip() ? 15 : 7);
                for (int i = 0; i <= max; i++) {
                    setV(i, rplFlags[i]);
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported opcode: 0x" + Integer.toHexString(op));
        }
    }

    private void handleKeyWaitTick() {
        if (pressedKeyWaitingRelease == -1) {
            int key = keypad.firstPressedKey();
            if (key != -1) {
                pressedKeyWaitingRelease = key;
            }
        } else if (!keypad.isPressed(pressedKeyWaitingRelease)) {
            setV(waitingKeyRegister, pressedKeyWaitingRelease);
            waitingKeyRegister = -1;
            pressedKeyWaitingRelease = -1;
        }
    }

    private void applyMemoryIncrement(int x, Quirks quirks) {
        int mask = addressMask();
        switch (quirks.memoryIncrement()) {
            case INCREMENT_BY_X_PLUS_1 -> iRegister = (iRegister + x + 1) & mask;
            case INCREMENT_BY_X -> iRegister = (iRegister + x) & mask;
            case NONE -> {}
        }
    }

    private void skipNextInstruction() {
        int nextOpcode = readWord(pc);
        boolean isFourByte = (config.variant().supportsXoChip() && nextOpcode == 0xF000)
            || (config.variant().supportsMegaChip() && (nextOpcode & 0xFF00) == 0x0100);
        if (isFourByte) {
            pc = (pc + 4) & addressMask();
        } else {
            pc = (pc + 2) & addressMask();
        }
    }

    private void ensureHiResSupported(int opcode) {
        if (!config.variant().supportsHiRes()) {
            throw new UnsupportedOperationException(
                "Opcode 0x" + Integer.toHexString(opcode) + " requires SUPER-CHIP, XO-CHIP, or Mega-Chip mode"
            );
        }
    }

    private void ensureXoChipSupported(int opcode) {
        if (!config.variant().supportsXoChip()) {
            throw new UnsupportedOperationException(
                "Opcode 0x" + Integer.toHexString(opcode) + " requires XO-CHIP mode"
            );
        }
    }

    private void ensureMegaChipSupported(int opcode) {
        if (!config.variant().supportsMegaChip()) {
            throw new UnsupportedOperationException(
                "Opcode 0x" + Integer.toHexString(opcode) + " requires Mega-Chip mode"
            );
        }
    }

    private int readWord(int addr) {
        int mask = addressMask();
        int hi = memory[addr & mask] & 0xFF;
        int lo = memory[(addr + 1) & mask] & 0xFF;
        return (hi << 8) | lo;
    }

    private int addressMask() {
        return memory.length - 1;
    }

    private void setV(int index, int value) {
        registers[index & 0xF] = value & 0xFF;
    }

    public synchronized int getV(int index) {
        return registers[index & 0xF] & 0xFF;
    }

    public synchronized int getPC() {
        return pc;
    }

    public synchronized int getI() {
        return iRegister;
    }

    public synchronized int getSP() {
        return sp;
    }

    public synchronized int getDelayTimer() {
        return delayTimer;
    }

    public synchronized int getSoundTimer() {
        return soundTimer;
    }

    public synchronized boolean isHalted() {
        return halted;
    }

    public synchronized long getTotalCycles() {
        return totalCycles;
    }

    public synchronized int getAudioPitch() {
        return audioPitch;
    }

    public KeypadState keypad() {
        return keypad;
    }

    public DisplayBuffer display() {
        return display;
    }

    public synchronized byte[] copyMemory() {
        return Arrays.copyOf(memory, memory.length);
    }

    public synchronized byte[] copyMemoryPrefix(int maxBytes) {
        return Arrays.copyOf(memory, Math.min(memory.length, Math.max(0, maxBytes)));
    }

    public synchronized void writeMemoryByte(int address, byte value) {
        memory[address & addressMask()] = value;
    }

    public synchronized void toggleBreakpoint(int address) {
        int aligned = address & addressMask();
        if (!breakpoints.remove(aligned)) {
            breakpoints.add(aligned);
        }
    }

    public synchronized Set<Integer> getBreakpoints() {
        return Set.copyOf(breakpoints);
    }

    public synchronized void clearBreakpoints() {
        breakpoints.clear();
    }

    public synchronized SaveState createSaveState() {
        int activeLen = Math.min(memory.length, Math.max(0x10000, loadedRom.length + PROGRAM_START));
        return new SaveState(
            Instant.now(),
            config,
            pc,
            iRegister,
            sp,
            delayTimer,
            soundTimer,
            halted,
            display.isHiRes(),
            display.activePlanes(),
            audioPitch,
            memory.length,
            registers,
            stack,
            rplFlags,
            Arrays.copyOf(memory, activeLen),
            display.copyRawPlane0(),
            display.copyRawPlane1(),
            audioPattern,
            display.copyMegaState(),
            display.snapshot()
        );
    }

    public synchronized void restoreSaveState(SaveState state) {
        Objects.requireNonNull(state, "state");
        this.config = state.config();
        if (this.memory.length != state.totalMemorySize()) {
            this.memory = new byte[state.totalMemorySize()];
        } else {
            Arrays.fill(this.memory, (byte) 0);
        }
        System.arraycopy(state.activeMemory(), 0, this.memory, 0, Math.min(state.activeMemory().length, this.memory.length));
        this.pc = state.pc();
        this.iRegister = state.iRegister();
        this.sp = state.sp();
        this.delayTimer = state.delayTimer();
        this.soundTimer = state.soundTimer();
        this.halted = state.halted();
        this.audioPitch = state.audioPitch();
        this.waitingForVblank = false;
        this.waitingKeyRegister = -1;
        this.pressedKeyWaitingRelease = -1;
        System.arraycopy(state.registers(), 0, this.registers, 0, REGISTER_COUNT);
        System.arraycopy(state.stack(), 0, this.stack, 0, STACK_DEPTH);
        System.arraycopy(state.rplFlags(), 0, this.rplFlags, 0, RPL_FLAG_COUNT);
        System.arraycopy(state.audioPattern(), 0, this.audioPattern, 0, 16);
        display.restoreState(state.hiRes(), state.activePlanes(), state.plane0(), state.plane1(), state.megaDisplayState());
        if (audioEngine != null) {
            audioEngine.setXoChipPatternMode(config.variant().supportsXoChip());
            audioEngine.loadXoPattern(audioPattern);
            audioEngine.setXoPitch(audioPitch);
            updateAudioState();
        }
    }

    public synchronized ExecutionSnapshot snapshot() {
        return new ExecutionSnapshot(
            pc,
            iRegister,
            sp,
            delayTimer,
            soundTimer,
            halted,
            display.isHiRes(),
            display.activePlanes(),
            audioPitch,
            totalCycles,
            Arrays.copyOf(registers, REGISTER_COUNT),
            Arrays.copyOf(stack, STACK_DEPTH),
            Arrays.copyOf(rplFlags, RPL_FLAG_COUNT),
            keypad.combinedMask(),
            display.snapshot(),
            Disassembler.disassembleWindow(memory, pc, 4, 16),
            Set.copyOf(breakpoints)
        );
    }

    public record ExecutionSnapshot(
        int pc,
        int iRegister,
        int sp,
        int delayTimer,
        int soundTimer,
        boolean halted,
        boolean hiRes,
        int activePlanes,
        int audioPitch,
        long totalCycles,
        int[] registers,
        int[] stack,
        int[] rplFlags,
        int keypadMask,
        DisplayBuffer.DisplaySnapshot display,
        java.util.List<Disassembler.DisassembledLine> disassembly,
        Set<Integer> breakpoints
    ) {}
}
