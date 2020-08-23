/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.saga.console.chip8.test;

import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.util.Chip8Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * We will take our chip 8 processor and have it begin executing instructions
 * from memory.
 *
 * @author summers
 */
public class E03ClockExecutionAndMemoryTest {

    private Chip8 chip8;

    /**
     * Until now we have only executed instructions manually. Eventually for a
     * working virtual machine we will need to load a program from an external
     * source.
     *
     * This provided Chip8Utils class has utility methods for creating a Chip8
     * system with memory loaded from a file.
     *
     *
     * @throws java.io.IOException
     */
    @BeforeEach
    public void loadMemory() throws IOException {
        this.chip8 = Chip8Utils.createFromRom(E03ClockExecutionAndMemoryTest.class.getResource("/E03TestRom.ch8"));
    }

    /**
     * A cycle loop of the chip8 processor should 
     *  
     *  fetch the instruction pointed at by the PC 
     *  increment the PC 
     *  decode the instruction 
     *  execute the instruction 
     *  repeat
     *
     * This will be modified slightly in E05Timers to update timers as well.
     * 
     * Some notes, an instruction is two bytes, however the memory is addressed 
     * as bytes.  This means you will need to load two bytes and combine them 
     * into a single instruction.
     * 
     */
    @Test
    public void testCycle() {
        chip8.cycle();
        chip8.cycle();
        chip8.cycle();
        chip8.cycle();
        assertEquals(0x15, chip8.getV0());
        assertEquals(0x20, chip8.getV1());
        assertEquals(0x25, chip8.getV2());
        assertEquals(0x30, chip8.getV3());
        assertEquals(0x208, chip8.getPC());
    }
}
