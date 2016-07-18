package net.saga.console.chip8.test;

import net.saga.console.chip8.Chip8;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * The first part of writing a Chip-8 emulator is setting up the main memory correctly.
 * 
 * Specifically there are several constants which are found below 0x0200 and programs typically start at 0x0200
 */
public class E01RegisterTests {

    private Chip8 chip8;
    
    public E01RegisterTests() {
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
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    /**
     * This chip 8 program counter starts at 0x200.  This seems like an obvious first test.
     */
    public void testPCInitializedto0x200() {
        Assert.assertEquals(0x0200, chip8.getPC());
    }

    @Test
    /**
     * CHIP-8 has 16 8 bit data registers numbered V0 - VF.
     * 
     * The next few tests test the initial values of the registers and several 
     * simple opcodes.
     * 
     */
    public void testDataRegistersInitializedTo0() {
        Assert.assertEquals(0, chip8.getV0());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV1());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV2());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV3());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV4());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV5());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV6());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV7());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV8());//Test initialized to 0
        Assert.assertEquals(0, chip8.getV9());//Test initialized to 0
        Assert.assertEquals(0, chip8.getVA());//Test initialized to 0
        Assert.assertEquals(0, chip8.getVB());//Test initialized to 0
        Assert.assertEquals(0, chip8.getVC());//Test initialized to 0
        Assert.assertEquals(0, chip8.getVD());//Test initialized to 0
        Assert.assertEquals(0, chip8.getVE());//Test initialized to 0
        Assert.assertEquals(0, chip8.getVF());//Test initialized to 0
    }
    
    @Test
    /**
     * The opcode 0x6XNN stores the constant NN into register VX.
     * 
     * IE 0x6A42 would store 42 into register VA.  It should be noted at this 
     * point that CHIP-8 is big-endian.
     * 
     * 
     * 
     */
    public void testLoadConstant() {
        chip8.execute(0x6015);
        Assert.assertEquals(15, chip8.getV0());//Test loads 15 to V0
        
        chip8.execute(0x6120);
        Assert.assertEquals(20, chip8.getV1());//etc
        
        chip8.execute(0x6225);
        Assert.assertEquals(25, chip8.getV2());
        
        chip8.execute(0x6330);
        Assert.assertEquals(30, chip8.getV3());
        
        chip8.execute(0x6435);
        Assert.assertEquals(35, chip8.getV4());
        
        chip8.execute(0x6540);
        Assert.assertEquals(40, chip8.getV5());
        
        chip8.execute(0x6645);
        Assert.assertEquals(45, chip8.getV6());
        
        chip8.execute(0x6750);
        Assert.assertEquals(50, chip8.getV7());
        
        chip8.execute(0x6855);
        Assert.assertEquals(55, chip8.getV8());
        
        chip8.execute(0x6960);
        Assert.assertEquals(60, chip8.getV9());
        
        chip8.execute(0x6A65);
        Assert.assertEquals(65, chip8.getVA());
        
        chip8.execute(0x6B70);
        Assert.assertEquals(70, chip8.getVB());
        
        chip8.execute(0x6C75);
        Assert.assertEquals(75, chip8.getVC());
        
        chip8.execute(0x6D80);
        Assert.assertEquals(80, chip8.getVD());

        chip8.execute(0x6E85);
        Assert.assertEquals(85, chip8.getVE());
        
        chip8.execute(0x6F90);
        Assert.assertEquals(90, chip8.getVF());
    }
    
    @Test
    /**
     * The opcode 0x7XNN adds the constant NN into the value of register VX.
     * 
     * IE 0x6A42
     *    0x7A42
     * 
     * would store 42 into register VA and then add 42 to get 84.
     * 
     * A special note, registers are 8 bit and should overflow appropriately.
     * 
     * We will use fewer tests now, but feel free to add more to test edge cases 
     * and other questions you might have about your code.
     * 
     */
    public void testAddConstant() {
        chip8.execute(0x6015);
        chip8.execute(0x7015);
        Assert.assertEquals(30, chip8.getV0());
        
        
        chip8.execute(0x6A42);
        chip8.execute(0x7A42);
        Assert.assertEquals(84, chip8.getVA());//Test loads 15 to V0
        
        
        chip8.execute(0x6EFF);
        chip8.execute(0x7E01);
        Assert.assertEquals(0, chip8.getVE());//Test Overflow
        
    }
    
    @Test
    /**
     * The opcode 8XY0 stores the value of register VY into VX
     * 
     * IE 0x6A42
     *    0x8EA0
     * 
     * would store 42 into register VA and then copy it to register VE.
     * 
     * 
     */
    public void testCopyRegister() {
        chip8.execute(0x6A42);
        chip8.execute(0x8EA0);
        Assert.assertEquals(42, chip8.getVA());
        Assert.assertEquals(42, chip8.getVE());
        
        chip8.execute(0x6ADE);
        chip8.execute(0x8FA0);
        Assert.assertEquals(42, chip8.getVE());
        Assert.assertEquals(0xDE, chip8.getVF());
        
        
    }
    
    
    @Test
    /**
     * The opcode 8XY4 adds the value of register VY to VX
     * 
     * IE 0x6A42
     *    0x6E42
     *    0x8EA4
     * 
     * would store 42 into register VA and VE and then add VA into register VE.
     * This instruction will modify register VF.  If there is an overflow then 
     * VF will be set to 01.  If there is not an overflow it will be set to 00.
     * 
     * This means that VF is always modified.
     * 
     * 
     */
    public void testAddRegister() {
        throw new RuntimeException("not implemented");
    }
    
    
    
    
    
}
