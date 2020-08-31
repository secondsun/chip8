package net.saga.console.chip8.jit;


import net.saga.console.chip8.Chip8;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Implementations record information about the running program
 */
public class JitInstrumentation {

    private final Map<Integer, InstrumentationRecord> instrumentations = new HashMap<>();
    private final Chip8 vmAndRom;
    private AtomicInteger blockIdCounter = new AtomicInteger(1);
    private boolean incrementBlockId = false;

    public JitInstrumentation(Chip8 chip8) {
        this.vmAndRom = chip8;
    }

    /**
     * Return the current instrumentation for a entry in memory
     *
     * @param memoryAddress the address in memory to view the instrumentation for
     * @return the instrumentation at memoryAddress 0, default is DefaultInstrumentation
     */
    public InstrumentationRecord getInstrumentationAt(int memoryAddress) {
        return instrumentations.get(memoryAddress);
    }

    public void hitInstruction(int memoryAddress) {
        instrumentations.computeIfAbsent(memoryAddress, (address) -> {
            if (incrementBlockId) {
                blockIdCounter.incrementAndGet();
                incrementBlockId = false;
            }
            return new InstrumentationRecord(memoryAddress, InstrumentationRecord.Flags.INSTRUCTION).setBlockId(blockIdCounter.get());
        });
    }

    /**
     * A jump in interpretation has occured and we need to update the blockIdCounter the next time we calculate an instruction.
     */
    public void scheduleIncrement() {
        incrementBlockId = true;
    }

    public void hitSprite(int memoryAddress) {
        instrumentations.computeIfAbsent(memoryAddress, (address) -> {
            return new InstrumentationRecord(memoryAddress, InstrumentationRecord.Flags.DATA);
        });
    }

    @Override
    public String toString() {
        return "JitInstrumentation{" +
                "\n\tinstrumentations=" + instrumentations +
                "\n}";
    }

    public Stream<InstrumentationRecord> stream() {
        return instrumentations.values().stream();
    }
}
