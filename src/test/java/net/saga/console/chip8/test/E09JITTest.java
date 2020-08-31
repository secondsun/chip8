package net.saga.console.chip8.test;


import net.saga.console.chip8.Chip8;
import net.saga.console.chip8.jit.InstrumentationRecord;
import net.saga.console.chip8.jit.JitInstrumentation;
import net.saga.console.chip8.util.Chip8Utils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Create a jitting compiler.
 *
 * Heres some info
 * My day of learning started here :
 * https://www.baeldung.com/graal-java-jit-compiler
 * Which lead to this video and blog post
 * https://chrisseaton.com/truffleruby/jokerconf17/
 * Which lead to this paper
 * https://www.oracle.com/technetwork/java/javase/tech/c2-ir95-150110.pdf
 * Which lead to THIS wikipedia
 * https://en.wikipedia.org/wiki/Static_single_assignment_form
 * and then to
 * https://www.zybuluo.com/SmashStack/note/847030
 * and finally
 * https://yurichev.com/mirrors/vanEmmerik_ssa.pdf
 *
 * This is really a jitting decompiler than transforms into java bytecode
 *
 */
public class E09JITTest {

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

        var instrumentation = chip8.getInstrumentation();
        var jumpInstrumentation = instrumentation.getInstrumentationAt(0x23a);
        var rawInstruction = chip8.getInstruction(0x23A);

        assertEquals(0x12B8, rawInstruction);
        assertEquals(0x23a, jumpInstrumentation.getAddress());
        assertTrue(jumpInstrumentation.getFlags().contains(InstrumentationRecord.Flags.INSTRUCTION));
        assertFalse(jumpInstrumentation.getFlags().contains(InstrumentationRecord.Flags.DATA));

        for (int i = 0x321; i < 0x326; i++) {
            var spriteInstrumentation = instrumentation.getInstrumentationAt(i);

            assertEquals(i, spriteInstrumentation.getAddress());
            assertTrue(spriteInstrumentation.getFlags().contains(InstrumentationRecord.Flags.DATA));
            assertFalse(spriteInstrumentation.getFlags().contains(InstrumentationRecord.Flags.INSTRUCTION));
        }

        System.out.println(instrumentation);

    }

    /**
     * A JIT is, fundamentally, a compiler. Chip-8 ROMS are, fundamentally, machine code. We are going to decompile the
     * Chip-8 machine code into an intermediate representation called Single Static Assignment (SSA). SSA formed programs
     * follow a few basic rules.
     * * Every variable is unique and only assigned once
     * * Control flow statements break code into blocks
     * * Blocks with multiple entries require phi functions to pick the variables used in that function
     * * TODO Learn SSA
     * <p>
     * The first step in this decompiling is to transform our ROM into basic blocks. Basic blocks are a set of
     * instructions run in order and split up by control statements.
     * <p>
     * The original paper provides a more complete explanation :
     * https://www.cs.utexas.edu/~pingali/CS380C/2010/papers/ssaCytron.pdf
     * <p>
     * Our JIT will add to instrumentation a `blockId` property. This property will identify each block starting with 1
     * and incrementing.
     */
    @Test
    @DisplayName("Can create basic blocks")
    public void categoryTest2() throws IOException {

        var chip8 = Chip8Utils.createFromRom(getClass().getResource("/Particle.ch8"));


        //Give enough time for the instrumentation to run.
        //Chip-8 has no "clock", but 10k instruction executions
        //should hit everything in our simple program
        for (int i = 0; i < 10000; i++) {
            chip8.cycle();
        }

        var instrumentation = chip8.getInstrumentation();
        var firstInstructionInstr = instrumentation.getInstrumentationAt(0x200);
        var afterJumpInstructionInstr = instrumentation.getInstrumentationAt(0x2b8);

        //There's an initialization block for the first 0xB8 bytes
        assertEquals(1, firstInstructionInstr.getBlockId());

        //Jump target creates a new block
        assertEquals(2, afterJumpInstructionInstr.getBlockId());

        fail("Need to add more tests and walk people through tagging blocks better");
        sortAndPrint(instrumentation, chip8);

    }

    @Test
    @DisplayName("Create Block Flow Paths")
    public void createBlockFlow() {
        fail("This tests that code blocks have entrances and exits");
    }

    @Test
    @DisplayName("Create Variable Nodes")
    public void createVariableNodes() {
        fail("This test will create the variable nodes");
    }

    @Test
    @DisplayName("Create Phi Nodes")
    public void createPhiNodes() {
        fail("TODO: Create phi funcitons");
    }

    private void sortAndPrint(JitInstrumentation instrumentation, Chip8 chip8) {
        var instructions = instrumentation.stream()
                .filter(instruction -> instruction.getFlags().contains(InstrumentationRecord.Flags.INSTRUCTION))
                .sorted()
                .map(inst -> "block" + inst.getBlockId() + " - " + String.format("%4x", inst.getAddress()) + "@" + String.format("%4x", chip8.getInstruction(inst.getAddress())))
                .reduce("", (s, s2) -> s + "\n" + s2);

        System.out.println(instructions);
    }

    /**
     * This test is disabled because I don't think that I should begin with turning instructions into bytecode without
     * examining the runtime behavior. This idea has given birth to the classifier tests.
     * <p>
     * This test takes our example program from E07GraphicsTest and jits it. This program is an infinite loop.
     *
     * @throws IOException
     */
    @Disabled
    @Test
    @DisplayName("JIT an infinite loop")
    public void jitProgram() throws IOException {
        var chip8 = Chip8Utils.createFromRom(getClass().getResource("/E07GraphicsRom.ch8"));
        //See if the first instruction is to goto 0
        for (int i = 0; i < 10; i++) {
            chip8.cycle();//Cycle to prime the JIT
        }

        fail("not implemented");
    }


}
