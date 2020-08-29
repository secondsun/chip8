package net.saga.console.chip8.test;


import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.jit.InstrumentationRecord;
import net.saga.console.chip8.util.Chip8Utils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class E09JITTest {

    private Chip8 chip8;



    @Test
    @DisplayName("Classify memory addresses as data or instructions")
    /**
     * We want to know which addresses in memory represent instructions and which represent data. As we move further
     * we will respond to behaviors in runtime to add more properties as appropriate. These properties will include
     * type for data (sprint, binary coded decimal, number), whether data is a constant or not, placing instructions in
     * subroutines, etc.
     *
     * For now let's just flag executed bits of code.
     *
     */
    public void classifyTest1() throws IOException {
        var chip8 = Chip8Utils.createFromRom(getClass().getResource("/Particle.ch8"));


        //Give enough time for the instrumentation to run.
        //Chip-8 has no "clock", but 10k instruction executions
        //should hit everything in our simple program
        for (int i = 0; i < 10000; i++) {
            chip8.cycle();
        }

        var instrumentation= chip8.getInstrumentation();
        var jumpInstrumentation = instrumentation.getInstrumentationAt(0x23a);
        var rawInstruction = chip8.getInstruction(0x23A);

        assertEquals(0x12B8, rawInstruction);
        assertEquals(0x23a, jumpInstrumentation.getAddress());
        assertEquals(InstrumentationRecord.Type.INSTRUCTION, jumpInstrumentation.getType());

        for (int i = 0x321; i < 0x326; i++) {
            var spriteInstrumentation = instrumentation.getInstrumentationAt(i);

            assertEquals(i, spriteInstrumentation.getAddress());
            assertEquals(InstrumentationRecord.Type.DATA, spriteInstrumentation.getType());
        }

        System.out.println(instrumentation);

    }

    /**
     * BLocks of code should be tagged together. Blocks are the whole program, then split up into subroutines and also
     * targets for jump instructions.
     */
    @Test
    @DisplayName("Test tagging blocks")
    public void categoryTest2() {
        fail("not implemented");
    }

    /**
     *
     * This test is disabled because I don't think that I should begin with turning instructions into bytecode without
     * examining the runtime behavior. This idea has given birth to the classifier tests.
     *
     * This test takes our example program from E07GraphicsTest and jits it. This program is an infinite loop.
     * @throws IOException
     */
    @Disabled
    @Test
    @DisplayName("JIT an infinite loop")
    public void jitProgram() throws IOException {
        this.chip8 = Chip8Utils.createFromRom(getClass().getResource("/E07GraphicsRom.ch8"));
        //See if the first instruction is to goto 0
        for (int i = 0; i < 10; i++) {
            this.chip8.cycle();//Cycle to prime the JIT
        }

        fail("not implemented");
    }


}
