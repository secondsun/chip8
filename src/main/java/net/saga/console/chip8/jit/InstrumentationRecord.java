package net.saga.console.chip8.jit;

import java.util.EnumSet;

public class InstrumentationRecord implements Comparable<InstrumentationRecord> {

    private final EnumSet<Flags> flags;
    private final int address;
    private int blockId;

    public int getBlockId() {
        return blockId;
    }

    public InstrumentationRecord setBlockId(int blockId) {
        this.blockId = blockId;
        return this;
    }

    @Override
    public int compareTo(InstrumentationRecord o) {
        if (o.blockId == blockId) {
            return address - o.address;
        }
        return blockId - o.blockId;
    }

    public enum Flags {HACK, INSTRUCTION, DATA};

    public InstrumentationRecord(int address, Flags... initialFlags) {
        this.address = address;
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
                ", blockId=" + String.format("%d", blockId) +
                "\n}";
    }
}
