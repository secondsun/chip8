package net.saga.console.chip8.jit;


import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class BasicBlock {
    private final int id;

    //List of blocks entered from, order is important for phi functions
    private final SortedSet<Integer> enteredFrom = new TreeSet<>();

    //List of blocks exiting to, order is important for phi functions
    private final SortedSet<Integer> exitsTo = new TreeSet<>();

    private List<InstrumentationRecord> instructions = new ArrayList<>();

    public BasicBlock(int id) {
        this.id = id;
    }

    public BasicBlock addEnteredFrom(Integer blockId) {
        enteredFrom.add(blockId);
        return this;
    }

    public BasicBlock addExitsTo(Integer blockId) {
        exitsTo.add(blockId);
        return this;
    }

    public List<Integer> getEnteredFrom() {
        return List.copyOf(enteredFrom);
    }

    public List<Integer> getExitsTo() {
        return List.copyOf(exitsTo);
    }

    public int getId() {
        return id;
    }

    public void addInstruction(InstrumentationRecord instr) {
        this.instructions.add(instr);
    }

    public List<InstrumentationRecord> getInstructions() {
        return List.copyOf(instructions);
    }
}
