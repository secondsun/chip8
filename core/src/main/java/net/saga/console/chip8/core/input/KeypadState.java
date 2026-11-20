package net.saga.console.chip8.core.input;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, instance-based 16-key CHIP-8 keypad state.
 * Combines keyboard, on-screen keypad, and external gamepad inputs via independent source masks
 * so releasing a key on one device never stomps a held button on another.
 */
public final class KeypadState {
    private final AtomicInteger keyboardMask = new AtomicInteger(0);
    private final AtomicInteger virtualMask = new AtomicInteger(0);
    private final AtomicInteger gamepadMask = new AtomicInteger(0);

    public enum Source {
        KEYBOARD,
        VIRTUAL_PAD,
        GAMEPAD
    }

    public void setKey(Source source, int key, boolean pressed) {
        if (key < 0 || key > 0xF) {
            return;
        }
        int bit = 1 << key;
        AtomicInteger target = maskFor(source);
        if (pressed) {
            target.getAndUpdate(m -> m | bit);
        } else {
            target.getAndUpdate(m -> m & ~bit);
        }
    }

    public void setMask(Source source, int mask16) {
        maskFor(source).set(mask16 & 0xFFFF);
    }

    public void press(int key) {
        setKey(Source.KEYBOARD, key, true);
    }

    public void release(int key) {
        setKey(Source.KEYBOARD, key, false);
    }

    public void releaseAll() {
        keyboardMask.set(0);
        virtualMask.set(0);
        gamepadMask.set(0);
    }

    public int combinedMask() {
        return (keyboardMask.get() | virtualMask.get() | gamepadMask.get()) & 0xFFFF;
    }

    public boolean isPressed(int key) {
        if (key < 0 || key > 0xF) {
            return false;
        }
        return (combinedMask() & (1 << key)) != 0;
    }

    /**
     * Returns the lowest-index currently pressed key (0..15), or -1 if none are pressed.
     */
    public int firstPressedKey() {
        int mask = combinedMask();
        if (mask == 0) {
            return -1;
        }
        return Integer.numberOfTrailingZeros(mask);
    }

    private AtomicInteger maskFor(Source source) {
        return switch (source) {
            case KEYBOARD -> keyboardMask;
            case VIRTUAL_PAD -> virtualMask;
            case GAMEPAD -> gamepadMask;
        };
    }
}
