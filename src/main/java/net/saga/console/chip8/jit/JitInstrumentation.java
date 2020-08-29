package net.saga.console.chip8.jit;


import net.saga.console.chip8.Chip8;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementations record information about the running program
 */
public class JitInstrumentation {

    private final Map<Integer, InstrumentationRecord> instrumentations = new HashMap<>();
    private final Chip8 vmAndRom;

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
        instrumentations.put(memoryAddress, new InstrumentationRecord(InstrumentationRecord.Type.INSTRUCTION, memoryAddress, vmAndRom.getInstruction(memoryAddress)));
    }

    public void hitSprite(int memoryAddress) {
        instrumentations.put(memoryAddress, new InstrumentationRecord(InstrumentationRecord.Type.DATA, memoryAddress, vmAndRom.getMemory()[memoryAddress]));
    }

    @Override
    public String toString() {
        return "JitInstrumentation{" +
                "\n\tinstrumentations=" + instrumentations +
                "\n}";
    }
}
