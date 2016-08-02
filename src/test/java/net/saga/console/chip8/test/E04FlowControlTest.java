package net.saga.console.chip8.test;

import net.saga.console.chip8.Chip8;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * This test suite will work with flow control op codes. They will adjust the
 * program counter to change the next command which will be executed.
 *
 * @author summers
 */
public class E04FlowControlTest {

    private Chip8 chip8;

    public E04FlowControlTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        this.chip8 = new Chip8();
        this.chip8.execute(0x6064);
        this.chip8.execute(0x6127);
        this.chip8.execute(0x6212);
        this.chip8.execute(0x63AE);
        this.chip8.execute(0x64FF);
        this.chip8.execute(0x65B4);
        this.chip8.execute(0x6642);
        this.chip8.execute(0x6F25);
    }

    @After
    public void tearDown() {
    }

    /**
     * Jump instructions are the most basic flow control. They simply tell the
     * processor to start executing programs from a different area of memory.
     *
     * Chip-8 has two jump instructions : 1NNN This instruction sets the program
     * counter to execute from memory address NNN BNNN This instruction sets the
     * program counter to execute from memory address (NNN + V0)
     */
    @Test
    public void testJump() {
        chip8.execute(0x1DAE);
        assertEquals(0xDAE, chip8.getPC());

        chip8.execute(0xB432);
        assertEquals(1174, chip8.getPC());

    }

    /**
     * Chip8 has build in support for subroutines.
     *
     * There are two subroutine opcodes : 2NNN start executing subroutine at NNN
     * 00EE return from the subroutine
     *
     * For this we will have to do manipulations to the stack. 2NNN should write
     * the current instruction address to the stack and increment the stack
     * pointer. Most chip8 systems support 16 entries in the stack pointer.
     *
     */
    @Test
    public void testSubroutines() {
        chip8.execute(0x2DAE);
        assertEquals(0xDAE, chip8.getPC());

        chip8.execute(0x00EE);
        assertEquals(0x200, chip8.getPC());

    }

    /**
     * We will now implement conditional skipping. Conditional skips are
     * basically jumps if some condition is met like a register having a value.
     * 
     * Opcodes : 
     *  3XNN : Skip the next instruction if the value in Vx equals NN
     *  5XY0 : Skip the next instruction if the value in Vx equals Vy
     *  4XNN : Skip the next instruction if the value in Vx does not equal NN
     *  9XY0 : Skip the next instruction if the value in Vx does not equal Vy
     * 
     * This test will be running the program counter forward so it will also test that
     * we aren't doing anything too wrong with it.
     * 
     * Also, don't forget we have the follow constants loaded into registers already :
     * V0 = 0x64 
     * V1 = 0x27 
     * V2 = 0x12 
     * V3 = 0xAE 
     * V4 = 0xFF 
     * V5 = 0xB4 
     * V6 = 0x42 
     * VF = 0x25
     * 
     */
    @Test
    public void testEqualJumps() {
       chip8.execute(0x3064); // Skip if V0 == 0x64
       assertEquals(0x202, chip8.getPC());//Increment the PC by 2
       
       chip8.execute(0x3164); // Skip if V1 == 0x64, doesn't skip because V1 == 0x27
       assertEquals(0x202, chip8.getPC());//Do not increment the PC
       
       chip8.execute(0x6764); // Set V7 to 64
       chip8.execute(0x5070); // Skip if V0 == V7
       assertEquals(0x204, chip8.getPC());//Increment the PC by 2
       
       chip8.execute(0x5170); // Skip if V1 == V7 (It doesn't)
       assertEquals(0x204, chip8.getPC());//Increment the PC by 2
    }   
       
    @Test
    public void testNonEqualJumps() {   
       chip8.execute(0x4064); // Skip if V0 != 0x64 (it won't skip)
       assertEquals(0x200, chip8.getPC());//Increment the PC by 2
       
       chip8.execute(0x4164); // Skip if V1 == 0x64, skips because V1 == 0x27
       assertEquals(0x202, chip8.getPC());//Do not increment the PC
       
       chip8.execute(0x6764); // Set V7 to 64
       chip8.execute(0x9070); // Skip if V0 != V7(It won't skip)
       assertEquals(0x202, chip8.getPC());//Increment the PC by 2
       
       chip8.execute(0x9170); // Skip if V1 != V7 
       assertEquals(0x204, chip8.getPC());//Increment the PC by 2
       
       
    }
}
