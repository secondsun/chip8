package net.saga.console.chip8.jit;

public class InstrumentationRecord {

    private final Type type;
    private final int address;
    private final int data;

    public enum Type  {INSTRUCTION, DATA};

    public InstrumentationRecord(Type type, int address, int data) {
        this.type = type;
        this.address = address;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public int getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "InstrumentationRecord{\n" +
                "\t\ttype=" + type +
                ", address=" + String.format("%05X", address) +
                ", data=" + String.format("%05X", data) +
                "\n}";
    }
}
