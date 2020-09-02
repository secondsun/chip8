package net.saga.console.chip8.jit;


import net.saga.console.chip8.Chip8;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Implementations record information about the running program
 */
public class JitInstrumentation {

    private final Map<Integer, InstrumentationRecord> instrumentations = new HashMap<>();
    private final Chip8 vmAndRom;
    private BasicBlock currentBlock = new BasicBlock(1);
    private final Set<BasicBlock> blocks = new HashSet<>();

    public JitInstrumentation(Chip8 chip8) {
        this.vmAndRom = chip8;
        blocks.add(currentBlock);
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
            var instr = new InstrumentationRecord(memoryAddress, currentBlock, InstrumentationRecord.Flags.INSTRUCTION);
            currentBlock.addInstruction(instr);
            return instr;
        });
    }

    /**
     * A jump in interpretation has occured and we need to update the current block to point out and into the new block
     *
     * @param address memory addressof the instruction starting the new block. We don't create a block if one already exists
     */
    public void newBlockIfNeeded(int address) {

        var newBlock = instrumentations.get(address)==null?null:instrumentations.get(address).getBlock();
        if (newBlock == null) {
            newBlock = new BasicBlock(currentBlock.getId() + 1);
            blocks.add(currentBlock);
        }
        currentBlock.addExitsTo(newBlock.getId());
        newBlock.addEnteredFrom(currentBlock.getId());
        currentBlock = newBlock;

    }

    public void hitSprite(int memoryAddress) {
        instrumentations.computeIfAbsent(memoryAddress, (address) -> {
            return new InstrumentationRecord(memoryAddress, null, InstrumentationRecord.Flags.DATA);
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

    public List<BasicBlock> getBlocks() {
        var list = new ArrayList<>(blocks);
        list.sort((b1,b2)->b1.getId()-b2.getId());
        return  list;
    }
}
