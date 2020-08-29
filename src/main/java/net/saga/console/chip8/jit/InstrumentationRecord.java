package net.saga.console.chip8.jit;

public class InstrumentationRecord {

    private final Type type;
    private final int address;

    public enum Type  {INSTRUCTION, DATA};

    public InstrumentationRecord(Type type, int address) {
        this.type = type;
        this.address = address;
    }

    public Type getType() {
        return type;
    }

    public int getAddress() {
        return address;
    }
}
