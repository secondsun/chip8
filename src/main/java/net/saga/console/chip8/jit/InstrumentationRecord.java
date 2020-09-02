package net.saga.console.chip8.jit;

import java.util.EnumSet;

public class InstrumentationRecord implements Comparable<InstrumentationRecord> {

    private final EnumSet<Flags> flags;
    private final int address;
    private final BasicBlock block;

    @Override
    public int compareTo(InstrumentationRecord o) {
        if (o.block.getId() == block.getId()) {
            return address - o.address;
        }
        return block.getId() - o.block.getId();
    }

    public BasicBlock getBlock() {
        return block;
    }


    public enum Flags {HACK, INSTRUCTION, DATA};

    public InstrumentationRecord(int address, BasicBlock block, Flags... initialFlags) {
        this.address = address;
        this.block = block;
        this.flags = EnumSet.of(Flags.HACK, initialFlags);
    }

    public EnumSet<Flags> getFlags() {
        return EnumSet.copyOf(flags);
    }

    public int getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "InstrumentationRecord{\n" +
                "\t\ttype=" + flags +
                ", address=" + String.format("%05X", address) +
                ", blockId=" + String.format("%d", block==null?0:block.getId()) +
                "\n}";
    }
}
