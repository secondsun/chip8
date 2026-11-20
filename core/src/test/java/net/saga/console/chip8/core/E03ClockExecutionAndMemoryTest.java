package net.saga.console.chip8.core;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class E03ClockExecutionAndMemoryTest {
    private Chip8Machine chip8;

    @BeforeEach
    void loadMemory() throws IOException {
        chip8 = new Chip8Machine(Chip8Config.legacyTutorial());
        try (var stream = getClass().getResourceAsStream("/E03TestRom.ch8")) {
            chip8.loadRom(stream.readAllBytes());
        }
    }

    @Test
    void testCycle() {
        chip8.stepInstruction();
        chip8.stepInstruction();
        chip8.stepInstruction();
        chip8.stepInstruction();
        assertEquals(0x15, chip8.getV(0));
        assertEquals(0x20, chip8.getV(1));
        assertEquals(0x25, chip8.getV(2));
        assertEquals(0x30, chip8.getV(3));
        assertEquals(0x208, chip8.getPC());
    }
}
