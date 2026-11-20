package net.saga.console.chip8.core;

import java.util.ArrayDeque;
import java.util.Optional;

/**
 * Bounded ring buffer storing periodic {@link SaveState} snapshots for live gameplay rewinding.
 */
public final class RewindBuffer {
    private final int maxEntries;
    private final ArrayDeque<SaveState> history = new ArrayDeque<>();

    public RewindBuffer(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
    }

    public synchronized void push(SaveState state) {
        if (history.size() >= maxEntries) {
            history.removeFirst();
        }
        history.addLast(state);
    }

    public synchronized Optional<SaveState> popLatest() {
        if (history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.removeLast());
    }

    public synchronized void clear() {
        history.clear();
    }

    public synchronized int size() {
        return history.size();
    }

    public int capacity() {
        return maxEntries;
    }
}
